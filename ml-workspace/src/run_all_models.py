import json
from pathlib import Path

import joblib
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
from scipy import stats
from sklearn.cluster import AgglomerativeClustering, DBSCAN, KMeans
from sklearn.compose import ColumnTransformer
from sklearn.decomposition import PCA
from sklearn.ensemble import GradientBoostingClassifier, GradientBoostingRegressor, RandomForestClassifier, RandomForestRegressor
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LinearRegression, LogisticRegression
from sklearn.metrics import (
    ConfusionMatrixDisplay,
    accuracy_score,
    average_precision_score,
    calinski_harabasz_score,
    davies_bouldin_score,
    f1_score,
    mean_absolute_error,
    mean_squared_error,
    precision_score,
    precision_recall_curve,
    r2_score,
    recall_score,
    roc_auc_score,
    roc_curve,
    silhouette_score,
)
from sklearn.base import clone
from sklearn.model_selection import StratifiedKFold, cross_val_score, train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import MinMaxScaler, OneHotEncoder, StandardScaler


SEED = 42


def ensure_dirs(root: Path):
    for rel in ["results/plots", "results/metrics", "artifacts"]:
        (root / rel).mkdir(parents=True, exist_ok=True)


def _build_inference_defaults(X: pd.DataFrame) -> dict:
    """Per-column defaults for Java/runtime: median for numeric, mode for categorical."""
    defaults = {}
    for col in X.columns:
        s = X[col]
        if pd.api.types.is_numeric_dtype(s):
            defaults[col] = float(s.median())
        else:
            mode = s.mode(dropna=True)
            defaults[col] = str(mode.iloc[0]) if len(mode) else ""
    return defaults


def export_success_classifier_artifacts(root: Path, df: pd.DataFrame, best_pipe: Pipeline, features: list[str]):
    """Persist joblib model, feature order, and imputation defaults for the inference service."""
    art = root / "artifacts"
    art.mkdir(parents=True, exist_ok=True)
    X = df[features]
    full_pipe = clone(best_pipe)
    full_pipe.fit(X, df["success_flag"])
    joblib.dump(full_pipe, art / "success_classifier.joblib")
    (art / "inference_feature_order.json").write_text(json.dumps({"features": features}, indent=2), encoding="utf-8")
    defaults = _build_inference_defaults(X)
    (art / "inference_defaults.json").write_text(json.dumps(defaults, indent=2), encoding="utf-8")


def export_satisfaction_regressor_artifacts(root: Path, df: pd.DataFrame, best_pipe: Pipeline, features: list[str]):
    """Persist satisfaction regression model and (shared) inference sidecars."""
    art = root / "artifacts"
    art.mkdir(parents=True, exist_ok=True)
    X = df[features]
    full_pipe = clone(best_pipe)
    full_pipe.fit(X, df["client_satisfaction_score"])
    joblib.dump(full_pipe, art / "satisfaction_regressor.joblib")
    (art / "inference_feature_order.json").write_text(json.dumps({"features": features}, indent=2), encoding="utf-8")
    defaults = _build_inference_defaults(X)
    (art / "inference_defaults.json").write_text(json.dumps(defaults, indent=2), encoding="utf-8")


def export_client_segmentation_artifacts(root: Path, clients: pd.DataFrame, projects: pd.DataFrame):
    """Persist kmeans-based client segmentation model + sidecars for inference."""
    art = root / "artifacts"
    art.mkdir(parents=True, exist_ok=True)

    agg = projects.groupby("client_id", as_index=False).agg(
        mean_completion_ratio=("completion_ratio", "mean"),
        mean_delay_days=("avg_task_delay_days", "mean"),
        project_success_rate=("success_flag", "mean"),
        mean_budget_usd=("budget_usd", "mean"),
        avg_complexity=("complexity_score", "mean"),
        total_projects=("project_id", "count"),
    )
    df = clients.merge(agg, on="client_id", how="left")
    X = df.drop(columns=["client_id"])

    num_cols = X.select_dtypes(include=[np.number]).columns.tolist()
    cat_cols = X.select_dtypes(exclude=[np.number]).columns.tolist()
    prep = ColumnTransformer(
        transformers=[
            ("num", Pipeline([("imputer", SimpleImputer(strategy="median")), ("scaler", StandardScaler())]), num_cols),
            ("cat", Pipeline([("imputer", SimpleImputer(strategy="most_frequent")), ("enc", OneHotEncoder(handle_unknown="ignore"))]), cat_cols),
        ]
    )
    model = KMeans(n_clusters=4, random_state=SEED, n_init=10)
    pipe = Pipeline([("prep", prep), ("model", model)])
    pipe.fit(X)
    joblib.dump(pipe, art / "client_segmentation_kmeans.joblib")

    (art / "clustering_feature_order.json").write_text(json.dumps({"features": X.columns.tolist()}, indent=2), encoding="utf-8")
    defaults = _build_inference_defaults(X)
    (art / "clustering_defaults.json").write_text(json.dumps(defaults, indent=2), encoding="utf-8")

    labels = pipe.named_steps["model"].labels_
    prof = []
    cluster_count = int(pipe.named_steps["model"].n_clusters)
    for cid in range(cluster_count):
        mask = labels == cid
        size = int(mask.sum())
        if size == 0:
            prof.append({"segmentId": cid, "size": 0, "label": f"Segment {cid}"})
            continue
        sub = X.loc[mask]
        completion = float(sub["mean_completion_ratio"].mean()) if "mean_completion_ratio" in sub else 0.0
        success = float(sub["project_success_rate"].mean()) if "project_success_rate" in sub else 0.0
        delay = float(sub["mean_delay_days"].mean()) if "mean_delay_days" in sub else 0.0
        label = "Growth stable"
        if success >= 0.7 and delay <= 5:
            label = "Reliable delivery"
        elif success < 0.5 or delay >= 9:
            label = "Needs support"
        elif completion >= 0.65:
            label = "Execution improving"
        prof.append(
            {
                "segmentId": cid,
                "size": size,
                "label": label,
                "avg_completion_ratio": round(completion, 3),
                "avg_success_rate": round(success, 3),
                "avg_delay_days": round(delay, 2),
            }
        )
    (art / "clustering_segment_profiles.json").write_text(json.dumps({"profiles": prof}, indent=2), encoding="utf-8")


def export_preprocessing_diagnostics(root: Path, df: pd.DataFrame):
    """Figures for validation report (missingness + scaling effect)."""
    missing = df.isnull().sum().sort_values(ascending=False)
    plt.figure(figsize=(8, max(3.5, 0.22 * min(len(missing), 25))))
    top = missing.head(25)
    sns.barplot(x=top.values, y=top.index.astype(str), color="#3b82f6")
    plt.xlabel("Nombre de valeurs manquantes")
    plt.ylabel("Colonne")
    plt.title("Diagnostic pré-traitement — valeurs manquantes (avant imputation)")
    save_plot(root / "results/plots" / "preprocessing_missing_values.png")

    num_cols = df.select_dtypes(include=[np.number]).columns.tolist()
    col = "completion_ratio" if "completion_ratio" in num_cols else (num_cols[0] if num_cols else None)
    if col is None:
        return
    raw = df[col].dropna().astype(float).values.reshape(-1, 1)
    scaler = StandardScaler()
    scaled = scaler.fit_transform(raw).ravel()
    fig, axes = plt.subplots(1, 2, figsize=(10, 4))
    sns.histplot(raw.ravel(), bins=30, kde=True, ax=axes[0], color="#2563eb")
    axes[0].set_title(f"Distribution brute — {col}")
    axes[0].set_xlabel(col)
    sns.histplot(scaled, bins=30, kde=True, ax=axes[1], color="#059669")
    axes[1].set_title(f"Après StandardScaler — {col}")
    axes[1].set_xlabel("z-score")
    plt.suptitle("Effet de la standardisation (illustration sur une variable numérique)", y=1.02)
    save_plot(root / "results/plots" / "preprocessing_scaling_effect.png")


def save_plot(path: Path):
    plt.tight_layout()
    plt.savefig(path, dpi=160, bbox_inches="tight")
    plt.close()


def classification_section(root: Path, df: pd.DataFrame):
    target = "success_flag"
    drop_cols = ["project_id", "title", "status", "client_satisfaction_score", target]
    features = [c for c in df.columns if c not in drop_cols]
    X = df[features]
    y = df[target]

    num_cols = X.select_dtypes(include=[np.number]).columns.tolist()
    cat_cols = X.select_dtypes(exclude=[np.number]).columns.tolist()

    preprocessor = ColumnTransformer(
        transformers=[
            ("num", Pipeline([("imputer", SimpleImputer(strategy="median")), ("scaler", StandardScaler())]), num_cols),
            ("cat", Pipeline([("imputer", SimpleImputer(strategy="most_frequent")), ("enc", OneHotEncoder(handle_unknown="ignore"))]), cat_cols),
        ]
    )

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.22, random_state=SEED, stratify=y)

    models = {
        "logistic_regression": LogisticRegression(max_iter=400, random_state=SEED),
        "random_forest_classifier": RandomForestClassifier(n_estimators=240, random_state=SEED),
        "gradient_boosting_classifier": GradientBoostingClassifier(random_state=SEED),
    }

    rows = []
    roc_curves = []
    pr_curves = []
    fitted_pipes = {}
    for name, model in models.items():
        pipe = Pipeline([("prep", clone(preprocessor)), ("model", model)])
        pipe.fit(X_train, y_train)
        fitted_pipes[name] = pipe
        cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=SEED)
        cv_scores = cross_val_score(pipe, X_train, y_train, cv=cv, scoring="f1")
        y_pred = pipe.predict(X_test)
        if hasattr(pipe.named_steps["model"], "predict_proba"):
            y_score = pipe.predict_proba(X_test)[:, 1]
        else:
            score_raw = pipe.decision_function(X_test)
            y_score = (score_raw - score_raw.min()) / (score_raw.max() - score_raw.min() + 1e-9)

        rows.append(
            {
                "algorithm": name,
                "accuracy": float(accuracy_score(y_test, y_pred)),
                "precision": float(precision_score(y_test, y_pred, zero_division=0)),
                "recall": float(recall_score(y_test, y_pred, zero_division=0)),
                "sensitivity": float(recall_score(y_test, y_pred, zero_division=0)),
                "f1": float(f1_score(y_test, y_pred, zero_division=0)),
                "roc_auc": float(roc_auc_score(y_test, y_score)),
                "pr_auc": float(average_precision_score(y_test, y_score)),
                "cross_validation_f1_mean": float(cv_scores.mean()),
                "cross_validation_f1_std": float(cv_scores.std()),
            }
        )

        disp = ConfusionMatrixDisplay.from_predictions(y_test, y_pred, cmap="Blues")
        disp.ax_.set_title(f"Confusion Matrix - {name}")
        save_plot(root / "results/plots" / f"classification_confusion_{name}.png")

        fpr, tpr, _ = roc_curve(y_test, y_score)
        roc_curves.append((name, fpr, tpr))
        pr, rc, _ = precision_recall_curve(y_test, y_score)
        pr_curves.append((name, rc, pr))

    plt.figure(figsize=(7, 5))
    for name, fpr, tpr in roc_curves:
        plt.plot(fpr, tpr, label=name)
    plt.plot([0, 1], [0, 1], linestyle="--", color="gray")
    plt.xlabel("False Positive Rate")
    plt.ylabel("True Positive Rate")
    plt.title("ROC Curves - Classification Algorithms")
    plt.legend()
    save_plot(root / "results/plots" / "classification_roc_all_algorithms.png")

    plt.figure(figsize=(7, 5))
    for name, rc, pr in pr_curves:
        plt.plot(rc, pr, label=name)
    plt.xlabel("Recall")
    plt.ylabel("Precision")
    plt.title("Precision-Recall Curves - Classification Algorithms")
    plt.legend()
    save_plot(root / "results/plots" / "classification_pr_all_algorithms.png")

    metrics_df = pd.DataFrame(rows).sort_values("f1", ascending=False)
    metrics_df.to_csv(root / "results/metrics" / "classification_metrics.csv", index=False)
    (root / "results/metrics" / "classification_metrics.json").write_text(
        metrics_df.to_json(orient="records", indent=2), encoding="utf-8"
    )

    best_name = metrics_df.iloc[0]["algorithm"]
    export_success_classifier_artifacts(root, df, fitted_pipes[best_name], features)

    return metrics_df


def regression_section(root: Path, df: pd.DataFrame):
    target = "client_satisfaction_score"
    drop_cols = ["project_id", "title", "status", "success_flag", target]
    features = [c for c in df.columns if c not in drop_cols]
    X = df[features]
    y = df[target]

    num_cols = X.select_dtypes(include=[np.number]).columns.tolist()
    cat_cols = X.select_dtypes(exclude=[np.number]).columns.tolist()

    preprocessor = ColumnTransformer(
        transformers=[
            ("num", Pipeline([("imputer", SimpleImputer(strategy="median")), ("norm", MinMaxScaler()), ("std", StandardScaler())]), num_cols),
            ("cat", Pipeline([("imputer", SimpleImputer(strategy="most_frequent")), ("enc", OneHotEncoder(handle_unknown="ignore"))]), cat_cols),
        ]
    )

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.22, random_state=SEED)
    models = {
        "linear_regression": LinearRegression(),
        "random_forest_regressor": RandomForestRegressor(n_estimators=260, random_state=SEED),
        "gradient_boosting_regressor": GradientBoostingRegressor(random_state=SEED),
    }

    rows = []
    fitted_pipes = {}
    for name, model in models.items():
        pipe = Pipeline([("prep", clone(preprocessor)), ("model", model)])
        pipe.fit(X_train, y_train)
        fitted_pipes[name] = pipe
        pred = pipe.predict(X_test)
        resid = y_test - pred
        rows.append(
            {
                "algorithm": name,
                "mae": float(mean_absolute_error(y_test, pred)),
                "rmse": float(np.sqrt(mean_squared_error(y_test, pred))),
                "r2": float(r2_score(y_test, pred)),
            }
        )

        plt.figure(figsize=(6, 5))
        plt.scatter(y_test, pred, alpha=0.55)
        lims = [min(y_test.min(), pred.min()), max(y_test.max(), pred.max())]
        plt.plot(lims, lims, "r--")
        plt.xlabel("Actual")
        plt.ylabel("Predicted")
        plt.title(f"Predicted vs Actual - {name}")
        save_plot(root / "results/plots" / f"regression_pred_vs_actual_{name}.png")

        fig, axes = plt.subplots(1, 2, figsize=(10, 4))
        sns.histplot(resid, bins=25, kde=True, ax=axes[0])
        axes[0].set_title(f"Residual Distribution - {name}")
        stats.probplot(resid, dist="norm", plot=axes[1])
        axes[1].set_title(f"QQ Plot - {name}")
        save_plot(root / "results/plots" / f"regression_residuals_{name}.png")

    metrics_df = pd.DataFrame(rows).sort_values("r2", ascending=False)
    metrics_df.to_csv(root / "results/metrics" / "regression_metrics.csv", index=False)
    (root / "results/metrics" / "regression_metrics.json").write_text(
        metrics_df.to_json(orient="records", indent=2), encoding="utf-8"
    )
    best_name = metrics_df.iloc[0]["algorithm"]
    export_satisfaction_regressor_artifacts(root, df, fitted_pipes[best_name], features)
    return metrics_df


def clustering_section(root: Path, clients: pd.DataFrame, projects: pd.DataFrame):
    agg = projects.groupby("client_id", as_index=False).agg(
        mean_completion_ratio=("completion_ratio", "mean"),
        mean_delay_days=("avg_task_delay_days", "mean"),
        project_success_rate=("success_flag", "mean"),
        mean_budget_usd=("budget_usd", "mean"),
        avg_complexity=("complexity_score", "mean"),
        total_projects=("project_id", "count"),
    )
    df = clients.merge(agg, on="client_id", how="left")
    df = df.fillna(df.median(numeric_only=True))

    X = df.drop(columns=["client_id"])
    num_cols = X.select_dtypes(include=[np.number]).columns.tolist()
    cat_cols = X.select_dtypes(exclude=[np.number]).columns.tolist()

    prep = ColumnTransformer(
        transformers=[
            ("num", Pipeline([("imputer", SimpleImputer(strategy="median")), ("scaler", StandardScaler())]), num_cols),
            ("cat", Pipeline([("imputer", SimpleImputer(strategy="most_frequent")), ("enc", OneHotEncoder(handle_unknown="ignore"))]), cat_cols),
        ]
    )
    Xp = prep.fit_transform(X)
    X_dense = Xp.toarray() if hasattr(Xp, "toarray") else Xp
    pca = PCA(n_components=2, random_state=SEED)
    X2 = pca.fit_transform(X_dense)

    algorithms = {
        "kmeans": KMeans(n_clusters=4, random_state=SEED, n_init=10),
        "agglomerative": AgglomerativeClustering(n_clusters=4),
        "dbscan": DBSCAN(eps=1.25, min_samples=7),
    }

    rows = []
    for name, algo in algorithms.items():
        labels = algo.fit_predict(X_dense)
        valid = labels != -1
        unique = np.unique(labels[valid]) if valid.any() else np.array([])
        if len(unique) > 1 and valid.sum() > len(unique):
            sil = float(silhouette_score(X_dense[valid], labels[valid]))
            ch = float(calinski_harabasz_score(X_dense[valid], labels[valid]))
            db = float(davies_bouldin_score(X_dense[valid], labels[valid]))
        else:
            sil, ch, db = np.nan, np.nan, np.nan

        rows.append(
            {
                "algorithm": name,
                "num_clusters": int(len(np.unique(labels[labels != -1]))),
                "noise_ratio": float((labels == -1).mean()),
                "silhouette": sil,
                "calinski_harabasz": ch,
                "davies_bouldin": db,
            }
        )

        plt.figure(figsize=(7, 5))
        sns.scatterplot(x=X2[:, 0], y=X2[:, 1], hue=labels, palette="tab10", s=42, legend=False)
        plt.title(f"Cluster Projection (PCA) - {name}")
        plt.xlabel("PCA 1")
        plt.ylabel("PCA 2")
        save_plot(root / "results/plots" / f"clustering_projection_{name}.png")

    ks = range(2, 9)
    inertias, silhouettes = [], []
    for k in ks:
        km = KMeans(n_clusters=k, random_state=SEED, n_init=10)
        labels = km.fit_predict(X_dense)
        inertias.append(km.inertia_)
        silhouettes.append(silhouette_score(X_dense, labels))

    fig, axes = plt.subplots(1, 2, figsize=(11, 4))
    axes[0].plot(list(ks), inertias, marker="o")
    axes[0].set_title("KMeans Elbow Curve")
    axes[0].set_xlabel("k")
    axes[0].set_ylabel("Inertia")
    axes[1].plot(list(ks), silhouettes, marker="o")
    axes[1].set_title("KMeans Silhouette Trend")
    axes[1].set_xlabel("k")
    axes[1].set_ylabel("Silhouette Score")
    save_plot(root / "results/plots" / "clustering_kmeans_curves.png")

    metrics_df = pd.DataFrame(rows).sort_values("silhouette", ascending=False, na_position="last")
    metrics_df.to_csv(root / "results/metrics" / "clustering_metrics.csv", index=False)
    (root / "results/metrics" / "clustering_metrics.json").write_text(
        metrics_df.to_json(orient="records", indent=2), encoding="utf-8"
    )
    return metrics_df


def main():
    root = Path(__file__).resolve().parents[1]
    ensure_dirs(root)

    data = pd.read_csv(root / "data/raw/modeling_dataset.csv")
    clients = pd.read_csv(root / "data/raw/clients.csv")
    projects = pd.read_csv(root / "data/raw/projects.csv")

    export_preprocessing_diagnostics(root, data)

    cls = classification_section(root, data)
    reg = regression_section(root, data)
    clu = clustering_section(root, clients, projects)
    export_client_segmentation_artifacts(root, clients, projects)

    summary = {
        "classification_best": cls.iloc[0].to_dict(),
        "regression_best": reg.iloc[0].to_dict(),
        "clustering_best": clu.iloc[0].to_dict(),
    }
    (root / "results/metrics" / "dashboard_summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print("Training complete. Metrics and plots exported.")


if __name__ == "__main__":
    main()

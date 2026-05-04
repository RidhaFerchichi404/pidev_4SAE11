"""
FastAPI service: project success probability and failure risk %.
Loads artifacts produced by src/run_all_models.py (success_classifier.joblib, JSON sidecars).
"""
from __future__ import annotations

import json
import math
from contextlib import asynccontextmanager
from pathlib import Path

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

ROOT = Path(__file__).resolve().parent
ARTIFACTS = ROOT / "artifacts"
RISK_MODEL_PATH = ARTIFACTS / "success_classifier.joblib"
SATISFACTION_MODEL_PATH = ARTIFACTS / "satisfaction_regressor.joblib"
FEATURES_PATH = ARTIFACTS / "inference_feature_order.json"
DEFAULTS_PATH = ARTIFACTS / "inference_defaults.json"
CLUSTER_MODEL_PATH = ARTIFACTS / "client_segmentation_kmeans.joblib"
CLUSTER_FEATURES_PATH = ARTIFACTS / "clustering_feature_order.json"
CLUSTER_DEFAULTS_PATH = ARTIFACTS / "clustering_defaults.json"
CLUSTER_PROFILES_PATH = ARTIFACTS / "clustering_segment_profiles.json"

_risk_pipe = None
_satisfaction_pipe = None
_feature_order: list[str] = []
_defaults: dict = {}
_cluster_pipe = None
_cluster_feature_order: list[str] = []
_cluster_defaults: dict = {}
_cluster_profiles: dict[int, dict] = {}

# Human-readable labels for “reasons” (marginal vs training defaults).
FEATURE_LABELS: dict[str, str] = {
    "client_id": "Client profile",
    "category": "Project category",
    "budget_usd": "Project budget",
    "deadline_days": "Timeline to deadline",
    "created_day_index": "Project start timing",
    "complexity_score": "Scope complexity (skills)",
    "task_count": "Task count",
    "task_completed_count": "Completed tasks",
    "task_delayed_count": "Delayed tasks",
    "task_blocked_count": "Blocked tasks",
    "avg_task_delay_days": "Average task delay",
    "deadline_overrun_days": "Deadline overrun",
    "completion_ratio": "Task completion progress",
    "client_type": "Client type",
    "communication_score": "Communication score",
    "strictness_score": "Requirements strictness",
    "payment_delay_days": "Payment delay history",
    "repeat_hiring_rate": "Repeat hiring rate",
    "avg_budget_usd": "Client historical budget",
    "project_count_history": "Client project history volume",
    "avg_review_rating": "Review ratings",
    "review_count": "Number of reviews",
}


def _load_artifacts():
    global _risk_pipe, _satisfaction_pipe, _feature_order, _defaults
    global _cluster_pipe, _cluster_feature_order, _cluster_defaults, _cluster_profiles
    if not RISK_MODEL_PATH.is_file():
        raise RuntimeError(f"Missing model: {RISK_MODEL_PATH} — run python src/run_all_models.py")
    if not SATISFACTION_MODEL_PATH.is_file():
        raise RuntimeError(f"Missing model: {SATISFACTION_MODEL_PATH} — run python src/run_all_models.py")
    if not CLUSTER_MODEL_PATH.is_file():
        raise RuntimeError(f"Missing model: {CLUSTER_MODEL_PATH} — run python src/run_all_models.py")
    _risk_pipe = joblib.load(RISK_MODEL_PATH)
    _satisfaction_pipe = joblib.load(SATISFACTION_MODEL_PATH)
    _cluster_pipe = joblib.load(CLUSTER_MODEL_PATH)
    _feature_order = json.loads(FEATURES_PATH.read_text(encoding="utf-8"))["features"]
    _defaults = json.loads(DEFAULTS_PATH.read_text(encoding="utf-8"))
    _cluster_feature_order = json.loads(CLUSTER_FEATURES_PATH.read_text(encoding="utf-8"))["features"]
    _cluster_defaults = json.loads(CLUSTER_DEFAULTS_PATH.read_text(encoding="utf-8"))
    profiles = json.loads(CLUSTER_PROFILES_PATH.read_text(encoding="utf-8")).get("profiles", [])
    _cluster_profiles = {
        int(p.get("segmentId")): p
        for p in profiles
        if p is not None and p.get("segmentId") is not None
    }


@asynccontextmanager
async def lifespan(app: FastAPI):
    _load_artifacts()
    yield


app = FastAPI(title="Project success risk", version="1.0.0", lifespan=lifespan)


class PredictRequest(BaseModel):
    """Feature values; omitted keys use inference_defaults.json (training medians/modes)."""

    features: dict[str, object] = Field(default_factory=dict)


class PredictResponse(BaseModel):
    successProbability: float
    riskPercent: float
    reasons: list[str] = Field(default_factory=list)


class PredictSatisfactionResponse(BaseModel):
    satisfactionScore: float
    satisfactionPercent: float
    reasons: list[str] = Field(default_factory=list)


class PredictSegmentResponse(BaseModel):
    segmentId: int
    segmentLabel: str
    confidence: float
    reasons: list[str] = Field(default_factory=list)


class SegmentClientRequest(BaseModel):
    clientId: int | None = None
    features: dict[str, object] = Field(default_factory=dict)


class SegmentBatchRequest(BaseModel):
    clients: list[SegmentClientRequest] = Field(default_factory=list)


class SegmentClientResult(BaseModel):
    clientId: int | None = None
    segmentId: int
    segmentLabel: str
    confidence: float
    reasons: list[str] = Field(default_factory=list)


class SegmentBatchResponse(BaseModel):
    segments: list[SegmentClientResult] = Field(default_factory=list)
    summaryCounts: dict[str, int] = Field(default_factory=dict)


def _values_equal(actual: object, default: object) -> bool:
    if isinstance(actual, float) and isinstance(default, float):
        return math.isclose(actual, default, rel_tol=1e-5, abs_tol=1e-6)
    if isinstance(actual, (int, float)) and isinstance(default, (int, float)):
        return math.isclose(float(actual), float(default), rel_tol=1e-5, abs_tol=1e-6)
    return actual == default


def _predict_proba_row(row: dict) -> float:
    frame = pd.DataFrame([{k: row[k] for k in _feature_order}])
    return float(_risk_pipe.predict_proba(frame)[0, 1])


def _predict_satisfaction_row(row: dict) -> float:
    frame = pd.DataFrame([{k: row[k] for k in _feature_order}])
    return float(_satisfaction_pipe.predict(frame)[0])


def _compute_classification_reasons(row: dict, base_proba: float, top_n: int = 5, min_delta: float = 0.002) -> list[str]:
    """
    One-feature counterfactuals: replace each feature with its training default while
    keeping others at the request values; rank by |Δ P(success)|.
    """
    contributions: list[tuple[str, float]] = []
    for key in _feature_order:
        if key not in row or key not in _defaults:
            continue
        actual = row[key]
        default = _defaults[key]
        if _values_equal(actual, default):
            continue
        alt = dict(row)
        alt[key] = default
        try:
            p_alt = _predict_proba_row(alt)
        except Exception:
            continue
        delta = base_proba - p_alt
        if abs(delta) < min_delta:
            continue
        contributions.append((key, delta))

    contributions.sort(key=lambda t: abs(t[1]), reverse=True)
    reasons: list[str] = []
    for key, delta in contributions[:top_n]:
        label = FEATURE_LABELS.get(key, key.replace("_", " ").title())
        pct = round(abs(delta) * 100.0, 1)
        if delta > 0:
            reasons.append(
                f"{label}: compared with the model baseline, this factor supports a higher success outlook "
                f"(about +{pct}% success probability vs. substituting a typical value)."
            )
        else:
            reasons.append(
                f"{label}: compared with the model baseline, this factor tilts the estimate toward failure "
                f"(about −{pct}% success probability vs. substituting a typical value)."
            )

    if not reasons:
        reasons.append(
            "Most inputs are close to the training baseline, or no single factor moves the score enough to "
            "highlight — the result reflects the overall combination of signals."
        )
    return reasons


def _compute_regression_reasons(row: dict, base_score: float, top_n: int = 5, min_delta: float = 0.03) -> list[str]:
    """
    One-feature counterfactuals for regression:
    compare predicted score to variants where each feature is replaced by baseline.
    """
    contributions: list[tuple[str, float]] = []
    for key in _feature_order:
        if key not in row or key not in _defaults:
            continue
        actual = row[key]
        default = _defaults[key]
        if _values_equal(actual, default):
            continue
        alt = dict(row)
        alt[key] = default
        try:
            s_alt = _predict_satisfaction_row(alt)
        except Exception:
            continue
        delta = base_score - s_alt
        if abs(delta) < min_delta:
            continue
        contributions.append((key, delta))

    contributions.sort(key=lambda t: abs(t[1]), reverse=True)
    reasons: list[str] = []
    for key, delta in contributions[:top_n]:
        label = FEATURE_LABELS.get(key, key.replace("_", " ").title())
        pts = round(abs(delta), 2)
        if delta > 0:
            reasons.append(
                f"{label}: compared with baseline inputs, this factor improves predicted satisfaction "
                f"(about +{pts} points on a 1-10 scale)."
            )
        else:
            reasons.append(
                f"{label}: compared with baseline inputs, this factor lowers predicted satisfaction "
                f"(about -{pts} points on a 1-10 scale)."
            )

    if not reasons:
        reasons.append(
            "Inputs are mostly close to baseline patterns, or no single factor changes the predicted satisfaction enough to highlight."
        )
    return reasons


def _prepare_row(req: PredictRequest) -> dict:
    row = dict(_defaults)
    row.update(req.features)
    missing = [c for c in _feature_order if c not in row]
    if missing:
        for c in missing:
            row[c] = _defaults[c]
    return row


def _prepare_clustering_row(features: dict[str, object]) -> dict:
    row = dict(_cluster_defaults)
    row.update(features)
    missing = [c for c in _cluster_feature_order if c not in row]
    if missing:
        for c in missing:
            row[c] = _cluster_defaults[c]
    return row


def _cluster_label(segment_id: int) -> str:
    profile = _cluster_profiles.get(segment_id)
    if profile and str(profile.get("label", "")).strip():
        return str(profile["label"])
    return f"Segment {segment_id}"


def _compute_clustering_reasons(row: dict, top_n: int = 3) -> list[str]:
    deltas: list[tuple[str, str]] = []
    for key in _cluster_feature_order:
        if key not in row or key not in _cluster_defaults:
            continue
        val = row[key]
        base = _cluster_defaults[key]
        if _values_equal(val, base):
            continue
        label = FEATURE_LABELS.get(key, key.replace("_", " ").title())
        if isinstance(val, (int, float)) and isinstance(base, (int, float)):
            direction = "higher" if float(val) > float(base) else "lower"
            deltas.append((key, f"{label} is {direction} than the typical baseline ({float(val):.2f} vs {float(base):.2f})."))
        else:
            deltas.append((key, f"{label} differs from baseline ({val} vs {base})."))
    if not deltas:
        return ["Most inputs are close to baseline values; segment is mainly determined by combined client history patterns."]
    return [line for _, line in deltas[:top_n]]


def _predict_segment_from_row(row: dict) -> tuple[int, float]:
    frame = pd.DataFrame([{k: row[k] for k in _cluster_feature_order}])
    sid = int(_cluster_pipe.predict(frame)[0])
    confidence = 0.0
    model = _cluster_pipe.named_steps.get("model")
    prep = _cluster_pipe.named_steps.get("prep")
    if hasattr(model, "transform") and prep is not None:
        x_trans = prep.transform(frame)
        dists = model.transform(x_trans)[0]
        if len(dists) > 1:
            sorted_d = sorted(float(d) for d in dists)
            margin = max(0.0, sorted_d[1] - sorted_d[0])
            confidence = round(min(1.0, margin / (sorted_d[1] + 1e-9)), 4)
        elif len(dists) == 1:
            confidence = 1.0
    return sid, confidence


@app.get("/health")
def health():
    ok = (
        _risk_pipe is not None
        and _satisfaction_pipe is not None
        and _cluster_pipe is not None
        and bool(_feature_order)
        and bool(_cluster_feature_order)
    )
    return {"status": "ok" if ok else "uninitialized"}


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    if _risk_pipe is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    row = _prepare_row(req)
    try:
        frame = pd.DataFrame([{k: row[k] for k in _feature_order}])
    except KeyError as e:
        raise HTTPException(status_code=400, detail=f"Invalid feature key: {e}") from e

    try:
        proba = float(_risk_pipe.predict_proba(frame)[0, 1])
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Prediction failed: {e}") from e

    risk = round((1.0 - proba) * 100.0, 1)
    reasons = _compute_classification_reasons(row, proba)
    return PredictResponse(
        successProbability=round(proba, 6),
        riskPercent=risk,
        reasons=reasons,
    )


@app.post("/predict-satisfaction", response_model=PredictSatisfactionResponse)
def predict_satisfaction(req: PredictRequest):
    if _satisfaction_pipe is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    row = _prepare_row(req)
    try:
        frame = pd.DataFrame([{k: row[k] for k in _feature_order}])
    except KeyError as e:
        raise HTTPException(status_code=400, detail=f"Invalid feature key: {e}") from e

    try:
        raw_score = float(_satisfaction_pipe.predict(frame)[0])
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Prediction failed: {e}") from e

    score = round(max(1.0, min(10.0, raw_score)), 2)
    percent = round(((score - 1.0) / 9.0) * 100.0, 1)
    reasons = _compute_regression_reasons(row, score)
    return PredictSatisfactionResponse(
        satisfactionScore=score,
        satisfactionPercent=percent,
        reasons=reasons,
    )


@app.post("/predict-segment", response_model=PredictSegmentResponse)
def predict_segment(req: PredictRequest):
    if _cluster_pipe is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    row = _prepare_clustering_row(req.features)
    try:
        segment_id, confidence = _predict_segment_from_row(row)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Clustering failed: {e}") from e
    return PredictSegmentResponse(
        segmentId=segment_id,
        segmentLabel=_cluster_label(segment_id),
        confidence=confidence,
        reasons=_compute_clustering_reasons(row),
    )


@app.post("/segment-clients", response_model=SegmentBatchResponse)
def segment_clients(req: SegmentBatchRequest):
    if _cluster_pipe is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    results: list[SegmentClientResult] = []
    summary: dict[str, int] = {}
    for item in req.clients:
        row = _prepare_clustering_row(item.features)
        try:
            segment_id, confidence = _predict_segment_from_row(row)
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Clustering failed: {e}") from e
        label = _cluster_label(segment_id)
        summary[label] = summary.get(label, 0) + 1
        results.append(
            SegmentClientResult(
                clientId=item.clientId,
                segmentId=segment_id,
                segmentLabel=label,
                confidence=confidence,
                reasons=_compute_clustering_reasons(row),
            )
        )
    return SegmentBatchResponse(segments=results, summaryCounts=summary)

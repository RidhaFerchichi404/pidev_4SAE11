#!/usr/bin/env python3
import argparse
import base64
import os
from pathlib import Path


def parse_kv_file(path: Path) -> dict:
    data = {}
    if not path.exists():
        return data
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        data[k.strip()] = v.strip()
    return data


def b64(value: str) -> str:
    return base64.b64encode(value.encode("utf-8")).decode("ascii")


def b64_file(path: Path) -> str:
    return base64.b64encode(path.read_bytes()).decode("ascii")


def resolve_file(repo_root: Path, configured_path: str, fallbacks: list[str]) -> Path | None:
    candidates = []
    if configured_path:
        p = Path(configured_path)
        candidates.append(p if p.is_absolute() else (repo_root / p))
    for rel in fallbacks:
        candidates.append(repo_root / rel)
    for c in candidates:
        if c.exists() and c.is_file():
            return c
    return None


def main() -> None:
    parser = argparse.ArgumentParser(description="Render Kubernetes secrets from mdp.local and repo-local files.")
    parser.add_argument("--repo-root", default=".", help="Repository root path")
    parser.add_argument("--mdp-file", default="mdp.local", help="Local mdp secrets file")
    parser.add_argument("--output", default="k8s/02-secrets.generated.yaml", help="Output YAML path")
    parser.add_argument("--namespace", default="smart-freelance", help="Kubernetes namespace")
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    mdp_path = (repo_root / args.mdp_file).resolve()
    kv = parse_kv_file(mdp_path)

    def get(name: str, default: str = "") -> str:
        return os.getenv(name, kv.get(name, default))

    firebase_path = resolve_file(
        repo_root,
        get("FIREBASE_CREDENTIALS_PATH"),
        [
            "firebase-credentials/firebase-credentials.json",
            "firebase-credentials/notificationsystem-3bd2f-firebase-adminsdk.json",
        ],
    )
    planning_calendar_path = resolve_file(
        repo_root,
        get("PLANNING_CALENDAR_CREDENTIALS_PATH"),
        ["credentials/calendar-service-account.json", "googleproject-pidev4sae11-8cb63fec98c3.json"],
    )
    meeting_calendar_path = resolve_file(
        repo_root,
        get("MEETING_CALENDAR_CREDENTIALS_PATH"),
        ["credentials/meeting-service-account.json", "googleproject-pidev4sae11-8cb63fec98c3.json"],
    )

    github_token = get("GITHUB_TOKEN")
    github_token_file_setting = get("GITHUB_TOKEN_FILE")
    github_token_file = resolve_file(
        repo_root,
        github_token_file_setting,
        ["githubToken.txt", "credentials/github-token.txt"],
    )
    if not github_token and github_token_file:
        github_token = github_token_file.read_text(encoding="utf-8").strip()

    out = f"""apiVersion: v1
kind: Secret
metadata:
  name: mysql-secret
  namespace: {args.namespace}
type: Opaque
data:
  MYSQL_ROOT_PASSWORD: {b64(get("MYSQL_ROOT_PASSWORD", "rootpassword"))}
  SPRING_DATASOURCE_USERNAME: {b64(get("MYSQL_APP_USERNAME", "root"))}
  SPRING_DATASOURCE_PASSWORD: {b64(get("MYSQL_APP_PASSWORD", get("MYSQL_ROOT_PASSWORD", "rootpassword")))}
---
apiVersion: v1
kind: Secret
metadata:
  name: keycloak-secret
  namespace: {args.namespace}
type: Opaque
data:
  KEYCLOAK_ADMIN: {b64(get("KEYCLOAK_ADMIN", "admin"))}
  KEYCLOAK_ADMIN_PASSWORD: {b64(get("KEYCLOAK_ADMIN_PASSWORD", "admin"))}
  KEYCLOAK_ADMIN_USERNAME: {b64(get("KEYCLOAK_ADMIN_USERNAME", get("KEYCLOAK_ADMIN", "admin")))}
  KEYCLOAK_ADMIN_CLIENT_SECRET: {b64(get("KEYCLOAK_ADMIN_CLIENT_SECRET", "smart-freelance-internal-secret"))}
  KEYCLOAK_CLIENT_SECRET: {b64(get("KEYCLOAK_CLIENT_SECRET", ""))}
---
apiVersion: v1
kind: Secret
metadata:
  name: mail-secret
  namespace: {args.namespace}
type: Opaque
data:
  SPRING_MAIL_USERNAME: {b64(get("SPRING_MAIL_USERNAME", ""))}
  SPRING_MAIL_PASSWORD: {b64(get("SPRING_MAIL_PASSWORD", ""))}
---
apiVersion: v1
kind: Secret
metadata:
  name: external-api-secret
  namespace: {args.namespace}
type: Opaque
data:
  GITHUB_TOKEN: {b64(github_token)}
  AI_API_KEY: {b64(get("AI_API_KEY", ""))}
---
apiVersion: v1
kind: Secret
metadata:
  name: firebase-secret
  namespace: {args.namespace}
type: Opaque
data:
  firebase-credentials.json: {b64_file(firebase_path) if firebase_path else ""}
---
apiVersion: v1
kind: Secret
metadata:
  name: calendar-secret
  namespace: {args.namespace}
type: Opaque
data:
  calendar-service-account.json: {b64_file(planning_calendar_path) if planning_calendar_path else ""}
  meeting-service-account.json: {b64_file(meeting_calendar_path) if meeting_calendar_path else ""}
"""

    out_path = (repo_root / args.output).resolve()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(out, encoding="utf-8")

    print(f"Generated: {out_path}")
    if not mdp_path.exists():
        print(f"Warning: {mdp_path} not found. Defaults/environment values were used.")
    if firebase_path is None:
        print("Warning: Firebase credentials file not found. firebase-secret will be empty.")
    if planning_calendar_path is None or meeting_calendar_path is None:
        print("Warning: Google Calendar credential file(s) not found. calendar-secret may be empty.")
    if not github_token:
        print("Warning: GITHUB_TOKEN not found in env, mdp.local, or GitHub token file fallback.")


if __name__ == "__main__":
    main()

# Google Calendar integration – Planning microservice

## Two types of credentials

### 1. Service account (for **backend** Calendar API)

The Planning microservice uses a **service account** JSON key to talk to the Google Calendar API (create/list/delete events). This is **not** the OAuth “client secret” file.

- **Where to get it:** Google Cloud Console → IAM & Admin → Service accounts → Create key (JSON) for a service account that has Calendar API access.
- **Config (e.g. in Config Server `planning.properties` or local override):**
  - `google.calendar.enabled=true`
  - `google.calendar.credentials-path=<absolute-or-relative-path-to-service-account.json>`
- **Do not commit** the JSON file; it is listed in `.gitignore` (`googleproject-*.json`, `*google*credentials*.json`).

When `google.calendar.enabled=false` or the credentials file is missing, the calendar still works: events are built from the database (progress “next due” dates and project deadlines from the Project service).

### 2. OAuth client (e.g. `client_secret_...apps.googleusercontent.com.json`)

This file is for **OAuth 2.0 Web client** flows (e.g. “Sign in with Google” or a future “Connect your Google Calendar” in the **frontend**). The Planning **backend does not use this file** for the current Calendar integration; `GoogleCredentials.fromStream()` expects a service account JSON, not an OAuth client secret.

- **Use it for:** Frontend redirect flows (authorized JavaScript origins, redirect URIs). Your file already has `http://localhost:4200` and `http://localhost:4200/auth/callback`.
- **Do not set** `google.calendar.credentials-path` to this OAuth file; the backend will not accept it.
- **Do not commit** it; it is listed in `.gitignore` (`client_secret_*.json`).

## Summary

| Goal                         | Credential to use        | Config / usage                          |
|-----------------------------|--------------------------|-----------------------------------------|
| Backend Calendar (Planning) | **Service account** JSON | `google.calendar.enabled=true`, `credentials-path` = path to that file |
| Frontend “Sign in with Google” / user Calendar | **OAuth client** (`client_secret_*.json`) | Use in frontend or future auth service; not in Planning `credentials-path` |

With no Google credentials, the app still shows calendar events from the database (progress updates and project deadlines).

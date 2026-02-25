---
name: api-rest-endpoint-design
description: Design and implement RESTful API endpoints with consistent HTTP methods, status codes, and URL structure. Use when creating or refactoring API resources, defining frontend-backend contracts, or when the user mentions REST API, endpoints, or API design.
---

# API REST Endpoint Design

Design and implement RESTful API endpoints that are consistent, scalable, and follow industry best practices.

## When to Apply

- Creating a new API resource or sub-resource
- Refactoring existing endpoints to meet REST standards
- Defining the contract between frontend and backend

## Procedure

### 1. Identify resource

- Use a **plural noun** for the resource (e.g. `/users`, `/orders`).
- For sub-resources use hierarchy: `/resources/{id}/sub-resources`.

### 2. Select HTTP method

| Method  | Use for           |
|---------|-------------------|
| `GET`   | Retrieval         |
| `POST`  | Creation          |
| `PUT`   | Full replacement  |
| `PATCH` | Partial update    |
| `DELETE`| Removal           |

- Prefer REST over RPC-style URLs; avoid verbs in paths (e.g. prefer `POST /sessions` over `POST /users/login` unless session is the resource).

### 3. Define URL structure

- Hierarchy: `/resources/{id}/sub-resources`.
- No verbs in URLs; use resources and HTTP methods to express actions.

### 4. Define request

- **POST/PUT/PATCH**: JSON body structure, required/optional fields.
- **GET**: Query parameters for filtering, pagination, sorting (e.g. `?page=1&size=20&sort=createdAt`).

### 5. Define response shape

- **Success**: Consistent envelope (e.g. `{ "data": ... }`).
- **Errors**: Consistent shape (e.g. `{ "error": { "code": "...", "message": "..." } }`).

### 6. Select status codes

| Code | Use for                |
|------|------------------------|
| 200  | OK (GET, PUT, PATCH)   |
| 201  | Created (POST)         |
| 204  | No Content (DELETE)    |
| 400  | Bad Request            |
| 401  | Unauthorized           |
| 403  | Forbidden              |
| 404  | Not Found              |
| 500  | Internal Server Error  |

## Constraints

- **IDs**: Prefer UUIDs (or external identifiers); do not expose internal DB IDs if avoidable.
- **JSON**: Use one casing consistently (prefer **camelCase** for JS/Node/frontend).
- **Statelessness**: All endpoints must be stateless (no server-side session state for the API contract).

## Expected output

Deliver a fully specified route/controller method that includes:

1. Route and HTTP method
2. Request validation (body and/or query)
3. Call to business logic (service layer)
4. Standardized JSON response (success or error) and correct status code

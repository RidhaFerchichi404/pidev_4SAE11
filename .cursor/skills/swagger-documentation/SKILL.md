---
name: swagger-documentation
description: Generates interactive Swagger/OpenAPI API documentation from code annotations or schemas. Use when exposing APIs to other teams, defining API contracts, standardizing API development, or when the user mentions Swagger, OpenAPI, api-docs, or interactive API documentation.
---

# Swagger Documentation Generation

## When to Apply

- Exposing APIs to other teams or the public
- Strictly defining API contracts
- Standardizing API development across the project
- User asks for Swagger, OpenAPI, or interactive API docs

## Procedure

### 1. Install tools

- **Node/Express**: `swagger-jsdoc`, `swagger-ui-express`
- **Other stacks**: Use the OpenAPI/Swagger tooling for that language (e.g. SpringDoc, drf-spectacular)

### 2. Configure the definition

Create a config file (e.g. `swaggerDef.js`) with:

- API title and version
- Server base URL
- Optional security schemes

### 3. Annotate the code

**Option A – JSDoc/YAML above route handlers**

Describe for each endpoint:

- Path and HTTP method
- Parameters (path, query, header) and request body
- Response codes and response schemas

**Option B – Schema-driven**

Use a library that builds OpenAPI from validation schemas (e.g. `zod-to-openapi`, `joi-to-swagger`) so docs stay in sync with validation.

### 4. Mount the UI

Add a route (e.g. `/api-docs`) that serves Swagger UI using the generated spec. Ensure the spec is built from the same annotations/schemas as the implementation.

### 5. Secure the docs (if needed)

If the API is private, protect the documentation route (auth, IP allowlist, or separate internal-only URL).

## Constraints

- Documentation must match the actual implementation (same paths, methods, and payloads).
- Keep schemas reusable: use `$ref` for shared request/response models.
- Do not expose sensitive or internal-only endpoints in public documentation.

## Expected output

A live `/api-docs` (or chosen path) endpoint that serves interactive Swagger UI, generated from code annotations or schemas and updated when the API changes.

## Minimal JSDoc example (Express)

```javascript
/**
 * @openapi
 * /users:
 *   get:
 *     summary: List users
 *     responses:
 *       200:
 *         description: List of users
 */
router.get('/users', getUsers);
```

Use the same structure for `post`, `put`, `delete`, and document parameters and request body as required.

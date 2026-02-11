# Smart Freelance App (Frontend)

Angular frontend for the Smart Freelance and Project Matching Platform. It consumes microservices via the **API Gateway** (default: `http://localhost:8078`).

## Features

- **Login** – Email/password sign-in; on success redirects to the success page; on failure shows "Invalid email or password".
- **Sign up** – Registration (email, password, first name, last name, role); on success shows a message and you can log in; on failure shows an error.
- **Success** – Shown after login with a "Login successful" message and a **Log out** button that clears the session and returns to the login page.

Protected routes (e.g. `/success`) require a valid token; otherwise you are redirected to `/login`.

**Requirements:** API Gateway on port **8078** and the Keycloak auth service (e.g. registered in Eureka as `keycloak-auth`) must be running so login and signup requests can be proxied to the auth microservice.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

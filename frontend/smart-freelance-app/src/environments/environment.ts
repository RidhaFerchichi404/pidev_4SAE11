export const environment = {
  production: false,
  /** API Gateway (8078) – all requests go here; gateway routes to keycloak-auth (8079), user, etc. */
  apiGatewayUrl: 'http://localhost:8078',
  authApiPrefix: 'keycloak-auth/api/auth',
};

export const environment = {
  production: false,
  /** API Gateway = 8078; Keycloak auth service = 8079 (reached via gateway). */
  apiGatewayUrl: 'http://localhost:8078',
  authApiPrefix: 'keycloak-auth/api/auth',
};

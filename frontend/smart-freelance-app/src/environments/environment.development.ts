export const environment = {
  production: false,
  /**
   * API Gateway (8078). Do not use 8080 here — that port is Keycloak; microservice calls will 502.
   * `localGatewayHostInterceptor` rewrites mistaken 8080 → 8078 in dev for known gateway paths.
   */
  apiGatewayUrl: 'http://localhost:8078',
  authApiPrefix: 'keycloak-auth/api/auth',
  elevenLabsApiKey: '',
  showAiUi: false,
};

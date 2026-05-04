export const environment = {
  production: true,
  /** Replaced at Docker build via API_GATEWAY_PUBLIC_URL (Ingress host, e.g. http://api.smartfreelance.example.com). */
  apiGatewayUrl: '__API_GATEWAY_PUBLIC_URL__',
  authApiPrefix: 'keycloak-auth/api/auth',
  elevenLabsApiKey: '',
  showAiUi: false,
};

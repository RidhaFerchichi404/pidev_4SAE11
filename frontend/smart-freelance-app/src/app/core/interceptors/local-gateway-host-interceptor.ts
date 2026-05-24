import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

/**
 * Keycloak often runs on 8080; this app's API Gateway is 8078. If {@code environment.apiGatewayUrl}
 * is mistakenly set to {@code http://localhost:8080}, microservice routes return 502 from Keycloak.
 * In non-production builds, rewrite those API paths to the gateway port.
 */
const LOCAL_GATEWAY_REWRITES: ReadonlyArray<{ bad: string; good: string }> = [
  { bad: 'http://localhost:8080', good: 'http://localhost:8078' },
  { bad: 'http://127.0.0.1:8080', good: 'http://127.0.0.1:8078' },
];

/** First path segment after host — matches gateway routes in {@code application.yml}. */
const GATEWAY_FIRST_SEGMENTS = new Set([
  'task',
  'notification',
  'user',
  'keycloak-auth',
  'project',
  'planning',
  'offer',
  'contract',
  'portfolio',
  'review',
  'gamification',
  'ticket',
  'subcontracting',
  'freelancia-job',
  'meeting',
  'chat',
  'aimodel',
]);

function gatewayPathOnWrongPort(url: string): boolean {
  try {
    const path = new URL(url).pathname;
    const seg = path.startsWith('/') ? path.slice(1).split('/')[0] : '';
    return seg !== '' && GATEWAY_FIRST_SEGMENTS.has(seg);
  } catch {
    return false;
  }
}

export const localGatewayHostInterceptor: HttpInterceptorFn = (req, next) => {
  if (environment.production) {
    return next(req);
  }
  const url = req.url;
  for (const { bad, good } of LOCAL_GATEWAY_REWRITES) {
    if (url.startsWith(bad + '/') || url.startsWith(bad + '?')) {
      if (gatewayPathOnWrongPort(url)) {
        return next(req.clone({ url: good + url.slice(bad.length) }));
      }
      return next(req);
    }
  }
  return next(req);
};

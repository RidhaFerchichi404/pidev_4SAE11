import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth-interceptor';
import { localGatewayHostInterceptor } from './core/interceptors/local-gateway-host-interceptor';
import { unauthorizedInterceptor } from './core/interceptors/unauthorized-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([
        localGatewayHostInterceptor,
        authInterceptor,
        unauthorizedInterceptor,
      ])
    ),
  ]
};

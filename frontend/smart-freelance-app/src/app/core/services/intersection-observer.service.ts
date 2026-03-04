import { Injectable, NgZone } from '@angular/core';

export interface IOOptions {
  threshold?: number | number[];
  rootMargin?: string;
  root?: Element | null;
}

@Injectable({ providedIn: 'root' })
export class IntersectionObserverService {
  constructor(private zone: NgZone) {}

  /**
   * Observes `element` and fires `callback(isIntersecting)` whenever
   * visibility crosses the given threshold.
   *
   * Returns a teardown function — call it to disconnect the observer.
   */
  observe(
    element: Element,
    callback: (isIntersecting: boolean) => void,
    options: IOOptions = {},
  ): () => void {
    const observer = new IntersectionObserver(
      (entries) => {
        this.zone.run(() => {
          entries.forEach(e => callback(e.isIntersecting));
        });
      },
      { threshold: 0, rootMargin: '200px', ...options },
    );

    observer.observe(element);
    return () => observer.disconnect();
  }
}

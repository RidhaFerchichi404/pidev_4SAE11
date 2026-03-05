import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

const API = `${environment.apiGatewayUrl}/portfolio/api/views`;

export interface ProfileViewStats {
  totalViews: number;
  thisWeekViews: number;
  lastWeekViews: number;
}

export interface DailyViewStat {
  date: string;   // "yyyy-MM-dd"
  count: number;
}

export interface ProfileViewItem {
  viewerId: number | null;
  viewedAt: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileViewService {
  constructor(private http: HttpClient) {}

  /**
   * Records a visit to a freelancer's profile.
   * Fire-and-forget — errors are swallowed so they never break the UI.
   */
  recordView(profileUserId: number, viewerId?: number | null): void {
    this.http
      .post(`${API}/record`, { profileUserId, viewerId: viewerId ?? null })
      .pipe(catchError(() => of(null)))
      .subscribe();
  }

  /** Total all-time view count for a freelancer's profile. */
  getTotalCount(userId: number): Observable<number> {
    return this.http
      .get<number>(`${API}/user/${userId}/count`)
      .pipe(catchError(() => of(0)));
  }

  /** Recent viewers for a profile (newest first). */
  getRecentViewers(userId: number, limit = 10): Observable<ProfileViewItem[]> {
    return this.http
      .get<ProfileViewItem[]>(`${API}/user/${userId}/recent`, { params: { limit } })
      .pipe(catchError(() => of([])));
  }

  /** Total + this-week + last-week view counts for the analytics panel. */
  getStats(userId: number): Observable<ProfileViewStats> {
    return this.http
      .get<ProfileViewStats>(`${API}/user/${userId}/stats`)
      .pipe(catchError(() => of({ totalViews: 0, thisWeekViews: 0, lastWeekViews: 0 })));
  }

  /** Per-day view counts for the last `days` days (chart data). */
  getDailyStats(userId: number, days = 30): Observable<DailyViewStat[]> {
    return this.http
      .get<DailyViewStat[]>(`${API}/user/${userId}/daily`, { params: { days } })
      .pipe(catchError(() => of([])));
  }
}

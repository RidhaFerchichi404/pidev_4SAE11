import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

const OFFER_API = `${environment.apiGatewayUrl}/offer/api/offers`;
const APPLICATION_API = `${environment.apiGatewayUrl}/offer/api/applications`;
const DASHBOARD_API = `${environment.apiGatewayUrl}/offer/api/dashboard`;

export type OfferStatus =
  | 'DRAFT'
  | 'AVAILABLE'
  | 'IN_PROGRESS'
  | 'ACCEPTED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'EXPIRED'
  | 'CLOSED';

export type ApplicationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN' | 'SHORTLISTED';

export interface Offer {
  id: number;
  freelancerId: number;
  title: string;
  domain: string;
  description: string;
  price: number;
  durationType: 'hourly' | 'fixed' | 'monthly';
  offerStatus: OfferStatus;
  deadline?: string;
  category?: string;
  rating?: number;
  communicationScore?: number;
  tags?: string;
  imageUrl?: string;
  viewsCount?: number;
  isFeatured?: boolean;
  isActive?: boolean;
  createdAt: string;
  updatedAt?: string;
  publishedAt?: string;
  expiredAt?: string;
  applicationsCount?: number;
  pendingApplicationsCount?: number;
  canReceiveApplications?: boolean;
  isValid?: boolean;
  basicPrice?: number;
  standardPrice?: number;
  premiumPrice?: number;
}

export interface OfferRequest {
  freelancerId: number;
  title: string;
  domain: string;
  description: string;
  price: number;
  durationType: 'hourly' | 'fixed' | 'monthly';
  deadline?: string;
  category?: string;
  tags?: string;
  imageUrl?: string;
  isFeatured?: boolean;
}

export interface OfferFilterRequest {
  keyword?: string;
  domain?: string;
  category?: string;
  offerStatus?: OfferStatus;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  durationType?: string;
  isFeatured?: boolean;
  isActive?: boolean;
  freelancerId?: number;
  createdAtFrom?: string; // ISO date YYYY-MM-DD
  createdAtTo?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export interface OfferApplication {
  id: number;
  offerId: number;
  offerTitle?: string;
  clientId: number;
  message: string;
  proposedBudget: number;
  portfolioUrl?: string;
  attachmentUrl?: string;
  estimatedDuration?: number;
  status: ApplicationStatus;
  rejectionReason?: string;
  isRead?: boolean;
  appliedAt: string;
  respondedAt?: string;
  acceptedAt?: string;
  canBeModified?: boolean;
}

export interface OfferApplicationRequest {
  offerId: number;
  clientId: number;
  message: string;
  proposedBudget: number;
  portfolioUrl?: string;
  attachmentUrl?: string;
  estimatedDuration?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/** Statistiques du dashboard freelancer (offres, contrats, candidatures). */
export interface DashboardStats {
  activeContracts: number;
  activeContractsThisWeek: number;
  totalSpentLast30Days: number;
  totalProjectsPosted: number;
  pendingApplications: number;
  activeOffers: number;
}

@Injectable({ providedIn: 'root' })
export class OfferService {
  constructor(private http: HttpClient) {}

  // ---------- Offers ----------
  getActiveOffers(page = 0, size = 10): Observable<PageResponse<Offer>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PageResponse<Offer>>(OFFER_API, { params }).pipe(
      catchError((err) => { console.error('[OfferService] getActiveOffers error:', err); return throwError(() => err); })
    );
  }

  getOfferById(id: number): Observable<Offer | null> {
    return this.http.get<Offer>(`${OFFER_API}/${id}`).pipe(catchError(() => of(null)));
  }

  getOffersByFreelancer(freelancerId: number): Observable<Offer[]> {
    return this.http.get<Offer[]>(`${OFFER_API}/freelancer/${freelancerId}`).pipe(catchError(() => of([])));
  }

  getFeaturedOffers(): Observable<Offer[]> {
    return this.http.get<Offer[]>(`${OFFER_API}/featured`).pipe(catchError(() => of([])));
  }

  searchOffers(filter: OfferFilterRequest): Observable<PageResponse<Offer>> {
    return this.http.post<PageResponse<Offer>>(`${OFFER_API}/search`, filter).pipe(
      catchError((err) => { console.error('[OfferService] searchOffers error:', err); return throwError(() => err); })
    );
  }

  createOffer(request: OfferRequest): Observable<Offer | null> {
    return this.http.post<Offer>(OFFER_API, request).pipe(
      catchError((err) => throwError(() => err))
    );
  }

  updateOffer(id: number, request: OfferRequest): Observable<Offer | null> {
    return this.http.put<Offer>(`${OFFER_API}/${id}`, request).pipe(
      catchError((err) => throwError(() => err))
    );
  }

  publishOffer(id: number, freelancerId: number): Observable<Offer | null> {
    return this.http
      .patch<Offer>(`${OFFER_API}/${id}/publish`, null, {
        params: { freelancerId: String(freelancerId) },
      })
      .pipe(catchError(() => of(null)));
  }

  changeOfferStatus(id: number, status: OfferStatus, freelancerId: number): Observable<Offer | null> {
    return this.http
      .patch<Offer>(`${OFFER_API}/${id}/status`, null, {
        params: { status, freelancerId: String(freelancerId) },
      })
      .pipe(catchError(() => of(null)));
  }

  deleteOffer(id: number, freelancerId: number): Observable<boolean> {
    return this.http
      .delete(`${OFFER_API}/${id}`, { params: { freelancerId: String(freelancerId) }, observe: 'response' })
      .pipe(
        map((res) => res.status >= 200 && res.status < 300),
        catchError((err) => throwError(() => err))
      );
  }

  // ---------- Applications ----------
  applyToOffer(request: OfferApplicationRequest): Observable<OfferApplication | null> {
    return this.http.post<OfferApplication>(APPLICATION_API, request).pipe(
      catchError((err) => throwError(() => err))
    );
  }

  getApplicationById(id: number): Observable<OfferApplication | null> {
    return this.http.get<OfferApplication>(`${APPLICATION_API}/${id}`).pipe(catchError(() => of(null)));
  }

  getApplicationsByOffer(offerId: number, page = 0, size = 20): Observable<PageResponse<OfferApplication>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PageResponse<OfferApplication>>(`${APPLICATION_API}/offer/${offerId}`, { params }).pipe(
      catchError(() =>
        of({ content: [], totalElements: 0, totalPages: 0, size, number: page, first: true, last: true })
      )
    );
  }

  getApplicationsByClient(clientId: number, page = 0, size = 20): Observable<PageResponse<OfferApplication>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PageResponse<OfferApplication>>(`${APPLICATION_API}/client/${clientId}`, { params }).pipe(
      catchError(() =>
        of({ content: [], totalElements: 0, totalPages: 0, size, number: page, first: true, last: true })
      )
    );
  }

  acceptApplication(id: number, freelancerId: number): Observable<OfferApplication> {
    return this.http.patch<OfferApplication>(`${APPLICATION_API}/${id}/accept`, null, {
      params: { freelancerId: String(freelancerId) },
    }).pipe(catchError((err) => { console.error('[OfferService] acceptApplication error:', err); return throwError(() => err); }));
  }

  rejectApplication(id: number, freelancerId: number, reason?: string): Observable<OfferApplication> {
    let params: HttpParams = new HttpParams().set('freelancerId', String(freelancerId));
    if (reason != null && reason !== '') params = params.set('reason', reason);
    return this.http.patch<OfferApplication>(`${APPLICATION_API}/${id}/reject`, null, { params })
      .pipe(catchError((err) => { console.error('[OfferService] rejectApplication error:', err); return throwError(() => err); }));
  }

  markApplicationAsRead(id: number, freelancerId: number): Observable<OfferApplication | null> {
    return this.http
      .patch<OfferApplication>(`${APPLICATION_API}/${id}/mark-read`, null, {
        params: { freelancerId: String(freelancerId) },
      })
      .pipe(catchError(() => of(null)));
  }

  withdrawApplication(id: number, clientId: number): Observable<OfferApplication | null> {
    return this.http
      .patch<OfferApplication>(`${APPLICATION_API}/${id}/withdraw`, null, {
        params: { clientId: String(clientId) },
      })
      .pipe(catchError(() => of(null)));
  }

  // ---------- Dashboard (statistiques freelancer) ----------
  getFreelancerDashboardStats(freelancerId: number): Observable<DashboardStats | null> {
    return this.http
      .get<DashboardStats>(`${DASHBOARD_API}/freelancer/${freelancerId}`)
      .pipe(catchError(() => of(null)));
  }

  /** Nombre d'offres par statut (backend agrégation) */
  getOffersCountByStatus(freelancerId: number): Observable<{ countByStatus: Record<string, number> } | null> {
    return this.http
      .get<{ countByStatus: Record<string, number> }>(`${OFFER_API}/stats/freelancer/${freelancerId}/by-status`)
      .pipe(catchError(() => of(null)));
  }

  /** Taux d'acceptation des candidatures */
  getAcceptanceRate(freelancerId: number): Observable<AcceptanceRate | null> {
    return this.http
      .get<AcceptanceRate>(`${OFFER_API}/stats/freelancer/${freelancerId}/acceptance-rate`)
      .pipe(catchError(() => of(null)));
  }

  /** Évolution mensuelle des offres */
  getMonthlyEvolution(freelancerId: number, year: number): Observable<MonthlyEvolution | null> {
    return this.http
      .get<MonthlyEvolution>(`${OFFER_API}/stats/freelancer/${freelancerId}/monthly-evolution`, {
        params: { year: String(year) },
      })
      .pipe(catchError(() => of(null)));
  }

  /** Traduction d'une offre (titre + description) – FR / EN / AR */
  translateOffer(offerId: number, targetLanguage: string): Observable<TranslateOfferResult> {
    return this.http
      .post<TranslateOfferResult>(`${OFFER_API}/${offerId}/translate`, { targetLanguage })
      .pipe(catchError((err) => throwError(() => err)));
  }

  /** Traduction de textes libres (formulaire add/edit) – FR / EN / AR */
  translateTexts(texts: string[], targetLanguage: string): Observable<string[]> {
    return this.http
      .post<{ translations: string[] }>(`${OFFER_API}/translate-texts`, { texts, targetLanguage })
      .pipe(
        map((r) => r.translations ?? []),
        catchError((err) => throwError(() => err))
      );
  }

  /** Smart Matching : recommandations d'offres pour un client (historique + comportement) */
  getRecommendedOffers(clientId: number, limit = 20): Observable<Offer[]> {
    return this.http
      .get<Offer[]>(`${OFFER_API}/recommendations/client/${clientId}`, { params: { limit: String(limit) } })
      .pipe(catchError(() => of([])));
  }

  /** Enregistrer une vue d'offre par un client (pour Smart Matching / comportement) */
  recordOfferView(clientId: number, offerId: number): Observable<void> {
    return this.http.post<void>(`${OFFER_API}/views`, { clientId, offerId }).pipe(catchError(() => of(undefined)));
  }

  /** Q&A : liste des questions-réponses pour une offre */
  getOfferQuestions(offerId: number): Observable<OfferQuestionResponse[]> {
    return this.http
      .get<OfferQuestionResponse[]>(`${OFFER_API}/${offerId}/questions`)
      .pipe(catchError(() => of([])));
  }

  /** Q&A : un client pose une question */
  addOfferQuestion(offerId: number, clientId: number, questionText: string): Observable<OfferQuestionResponse | null> {
    return this.http
      .post<OfferQuestionResponse>(`${OFFER_API}/${offerId}/questions`, { questionText }, { params: { clientId: String(clientId) } })
      .pipe(catchError(() => of(null)));
  }

  /** Q&A : le freelancer répond à une question */
  answerOfferQuestion(questionId: number, freelancerId: number, answerText: string): Observable<OfferQuestionResponse | null> {
    return this.http
      .patch<OfferQuestionResponse>(`${OFFER_API}/questions/${questionId}/answer`, { answerText }, { params: { freelancerId: String(freelancerId) } })
      .pipe(catchError(() => of(null)));
  }
}

export interface OfferQuestionResponse {
  id: number;
  offerId: number;
  clientId: number;
  questionText: string;
  answerText: string | null;
  askedAt: string;
  answeredAt: string | null;
  answered: boolean;
}

export interface TranslateOfferResult {
  title: string;
  description: string;
}

export interface AcceptanceRate {
  totalApplications: number;
  acceptedCount: number;
  rate: number;
}

export interface MonthlyEvolution {
  year: number;
  months: { month: number; count: number }[];
}

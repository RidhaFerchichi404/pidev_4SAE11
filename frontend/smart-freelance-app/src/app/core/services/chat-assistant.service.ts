import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of, catchError, timeout, TimeoutError } from 'rxjs';
import { environment } from '../../../environments/environment';

const CHAT_API = `${environment.apiGatewayUrl}/offer/api/chat`;

/** Délai max avant de considérer l'API comme indisponible (repli local) */
const CHAT_API_TIMEOUT_MS = 28_000;

export interface ChatMessageDto {
  role: 'user' | 'assistant';
  text: string;
}

export interface ChatAssistantRequest {
  message: string;
  history?: ChatMessageDto[];
}

export interface ChatAssistantResponse {
  reply: string;
}

/**
 * Service pour les réponses du chatbot Browse Offers.
 * Appelle le backend (API OpenAI) ; en cas d'erreur ou de timeout, le composant utilise le repli local.
 */
@Injectable({ providedIn: 'root' })
export class ChatAssistantService {
  constructor(private http: HttpClient) {}

  getReply(request: ChatAssistantRequest): Observable<string | null> {
    return this.http.post<ChatAssistantResponse>(`${CHAT_API}/assistant`, request, {
      observe: 'response',
      responseType: 'json',
    }).pipe(
      timeout(CHAT_API_TIMEOUT_MS),
      map((res) => {
        if (res.status === 204 || !res.body?.reply?.trim()) {
          return null;
        }
        return res.body.reply.trim();
      }),
      catchError((err) => {
        if (err instanceof TimeoutError) {
          console.warn('[ChatAssistant] API timeout, using local fallback');
        }
        return of(null);
      }),
    );
  }
}

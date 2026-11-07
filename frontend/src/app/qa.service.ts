import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface AskResponse {
  answered: boolean;
  answer: string;
  sourceDocId: string | null;
  sourceTitle: string | null;
  confidence: number;
}

export interface Article {
  doc_id: string;
  title: string;
  content: string;
}

export interface FeedbackPayload {
  question: string;
  answer: string;
  sourceDocId: string | null;
  helpful: boolean;
}

@Injectable({ providedIn: 'root' })
export class QaService {
  private readonly http = inject(HttpClient);

  ask(question: string): Observable<AskResponse> {
    return this.http.post<AskResponse>('/api/ask', { question });
  }

  article(docId: string): Observable<Article> {
    return this.http.get<Article>(`/api/docs/${docId}`);
  }

  feedback(payload: FeedbackPayload): Observable<void> {
    return this.http.post<void>('/api/feedback', payload);
  }
}

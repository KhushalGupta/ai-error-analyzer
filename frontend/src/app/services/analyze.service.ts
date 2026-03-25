import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AnalyzeRequest {
  log: string;
}

export interface AnalyzeResponse {
  analysis: string;
  relatedErrors: string;
}

@Injectable({ providedIn: 'root' })
export class AnalyzeService {
  private readonly http = inject(HttpClient);

  analyze(log: string): Observable<AnalyzeResponse> {
    return this.http.post<AnalyzeResponse>('/api/analyze', { log } satisfies AnalyzeRequest);
  }
}

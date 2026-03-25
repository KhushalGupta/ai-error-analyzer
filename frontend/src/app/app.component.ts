import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AnalyzeService } from './services/analyze.service';

@Component({
  selector: 'app-root',
  imports: [],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {
  private readonly analyzeService = inject(AnalyzeService);

  readonly logInput = signal('');
  readonly analysis = signal<string | null>(null);
  readonly relatedErrors = signal<string | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  onLogInput(value: string): void {
    this.logInput.set(value);
    this.error.set(null);
  }

  analyze(): void {
    const log = this.logInput().trim();
    if (!log) {
      this.error.set('Please paste an error log to analyze.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.analysis.set(null);
    this.relatedErrors.set(null);

    this.analyzeService.analyze(log).subscribe({
      next: (response) => {
        this.analysis.set(response.analysis);
        this.relatedErrors.set(response.relatedErrors || null);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? err?.message ?? 'Analysis failed. Please try again.');
        this.loading.set(false);
      }
    });
  }

  clear(): void {
    this.logInput.set('');
    this.analysis.set(null);
    this.relatedErrors.set(null);
    this.error.set(null);
  }
}

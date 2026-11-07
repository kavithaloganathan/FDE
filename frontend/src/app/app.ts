import { PercentPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Article, AskResponse, QaService } from './qa.service';

@Component({
  selector: 'app-root',
  imports: [FormsModule, PercentPipe],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly qa = inject(QaService);

  protected readonly question = signal('');
  protected readonly response = signal<AskResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly feedbackSent = signal(false);
  protected readonly article = signal<Article | null>(null);

  protected readonly examples = [
    'How do I reset my password?',
    "What's the maximum number of rows I can export to CSV?",
    'What are the API rate limits for the Growth plan?',
    'Can I connect Solstice to Salesforce?',
  ];

  protected ask(text: string = this.question()): void {
    const q = text.trim();
    if (!q) {
      return;
    }
    this.question.set(q);
    this.loading.set(true);
    this.error.set(null);
    this.response.set(null);
    this.feedbackSent.set(false);
    this.article.set(null);

    this.qa.ask(q).subscribe({
      next: (r) => {
        this.response.set(r);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not reach the server. Is the backend running on port 8080?');
        this.loading.set(false);
      },
    });
  }

  protected sendFeedback(helpful: boolean): void {
    const r = this.response();
    if (!r) {
      return;
    }
    this.qa
      .feedback({ question: this.question(), answer: r.answer, sourceDocId: r.sourceDocId, helpful })
      .subscribe({
        next: () => this.feedbackSent.set(true),
        error: () => this.error.set('Could not save feedback.'),
      });
  }

  protected toggleArticle(): void {
    if (this.article()) {
      this.article.set(null);
      return;
    }
    const docId = this.response()?.sourceDocId;
    if (!docId) {
      return;
    }
    this.qa.article(docId).subscribe({
      next: (a) => this.article.set(a),
      error: () => this.error.set('Could not load the full article.'),
    });
  }
}

import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  imports: [FormsModule, RouterLink],
  selector: 'app-login',
  styleUrl: './login.css',
  templateUrl: './login.html',
})
export class Login {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);

  username = '';
  password = '';
  // signals, because they change after an await (the app runs without zone.js)
  message = signal('');
  messageKind = signal<'error' | 'success' | ''>('');
  submitting = signal(false);
  showPassword = signal(false);
  submitted = false;

  async submit(): Promise<void> {
    this.submitted = true;

    if (this.username.length < 3 || !this.password) {
      this.message.set('Enter your username (3+ characters) and password.');
      this.messageKind.set('error');
      return;
    }

    this.submitting.set(true);
    this.message.set('');
    try {
      await this.auth.login(this.username, this.password);
      this.submitted = false;
      // a returnUrl for another role's page (e.g. an old /dashboard link opened by an analyst) is bounced by
      // roleGuard to the user's own home page
      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? this.auth.homeUrl();
      this.router.navigateByUrl(returnUrl);
    } catch (error) {
      this.message.set(this.errorMessage(error));
      this.messageKind.set('error');
    } finally {
      this.submitting.set(false);
    }
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        return 'Invalid username or password.';
      }
      if (error.status === 0 || error.status === 502 || error.status === 504) {
        return "Couldn't reach the login service. Is auth-service running?";
      }
    }
    return 'Login failed. Please try again.';
  }
}

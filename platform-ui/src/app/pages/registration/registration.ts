import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';

// the same rules the backend applies to the client details (OpenAccountRequestDTO)
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const MINIMUM_AGE = 18;

// full years between a yyyy-mm-dd birth date and today
function ageOn(birthDate: string, today: Date): number {
  const [year, month, day] = birthDate.split('-').map(Number);
  let age = today.getFullYear() - year;
  const birthdayPassed = today.getMonth() + 1 > month || (today.getMonth() + 1 === month && today.getDate() >= day);
  if (!birthdayPassed) {
    age--;
  }
  return age;
}

@Component({
  imports: [FormsModule, RouterLink],
  selector: 'app-registration',
  styleUrl: './registration.css',
  templateUrl: './registration.html',
})
export class Registration {
  private auth = inject(AuthService);
  private account = inject(AccountService);
  private router = inject(Router);

  username = '';
  password = '';
  confirmPassword = '';

  // the date picker stops at the latest birth date that makes someone 18 today
  readonly latestBirthDate = (() => {
    const date = new Date();
    date.setFullYear(date.getFullYear() - MINIMUM_AGE);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  })();
  // saved by the backend as the user's client details when their trading account is opened
  firstName = '';
  lastName = '';
  email = '';
  dob = '';
  // signals, because they change after an await (the app runs without zone.js)
  message = signal('');
  messageKind = signal<'error' | 'success' | ''>('');
  submitting = signal(false);
  showPassword = signal(false);
  submitted = false;

  async submit(): Promise<void> {
    this.submitted = true;

    const problem = this.validate();
    if (problem) {
      this.message.set(problem);
      this.messageKind.set('error');
      return;
    }

    this.submitting.set(true);
    this.message.set('');

    // The auth service checks username and email before creating the client login.
    try {
      await this.auth.register(this.username, this.password, this.email.trim());
    } catch (error) {
      this.showError(this.registerErrorMessage(error));
      return;
    }

    // Log in, then open the trading account with the validated client details.
    try {
      await this.auth.login(this.username, this.password);
    } catch {
      this.showError('Your login was created, but sign-in failed. Please contact support.');
      return;
    }

    try {
      await this.account.openAccount({
        firstName: this.firstName.trim(),
        lastName: this.lastName.trim(),
        email: this.email.trim(),
        birthDate: this.dob,
      });
    } catch (error) {
      this.showError(this.openAccountErrorMessage(error));
      return;
    }

    this.submitting.set(false);
    this.submitted = false;
    this.router.navigate(['/dashboard']);
  }

  private validate(): string | null {
    if (this.username.length < 3 || this.password.length < 8
        || this.firstName.trim().length < 2 || this.lastName.trim().length < 2 || !this.dob) {
      return 'Please complete all fields using valid values.';
    }
    if (this.confirmPassword !== this.password) {
      return "The passwords don't match.";
    }
    if (!EMAIL_PATTERN.test(this.email.trim())) {
      return 'Please enter a valid email address.';
    }
    if (ageOn(this.dob, new Date()) < MINIMUM_AGE) {
      return `You must be at least ${MINIMUM_AGE} to open an account.`;
    }
    return null;
  }

  private showError(message: string): void {
    this.message.set(message);
    this.messageKind.set('error');
    this.submitting.set(false);
  }

  private registerErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 409) {
        const message = error.error?.message;
        return typeof message === 'string' ? message : 'That username or email is already registered.';
      }
      if (error.status === 400) {
        const message = error.error?.message;
        return Array.isArray(message) ? message.join('. ') : message ?? 'Please check your details.';
      }
      if (error.status === 0 || error.status === 502 || error.status === 504) {
        return "Couldn't reach the login service. Is auth-service running?";
      }
    }
    return 'Registration could not be completed.';
  }

  private openAccountErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 409) {
        return (error.error?.message ?? 'Those details are already in use.') + ' Your login was created; please contact support.';
      }
      if (error.status === 400) {
        return 'Your login was created, but account details were rejected.';
      }
    }
    return "Your login was created, but we couldn't open your trading account. Please contact support.";
  }
}

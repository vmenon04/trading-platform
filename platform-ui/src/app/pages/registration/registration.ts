import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';

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
  // saved as the user's client details when their trading account is opened
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

    const valid = this.username.length >= 3
      && this.password.length >= 8
      && this.firstName.length >= 2
      && this.lastName.length >= 2
      && this.email.length > 0
      && this.dob.length > 0;

    if (!valid) {
      this.message.set('Please complete all fields using valid values.');
      this.messageKind.set('error');
      return;
    }

    this.submitting.set(true);
    this.message.set('');
    try {
      await this.auth.register(this.username, this.password);
      // log straight in so the user doesn't have to type their details again
      await this.auth.login(this.username, this.password);
      try {
        // open their trading account from the rest of the form; it starts with no cash
        await this.account.openAccount({
          firstName: this.firstName,
          lastName: this.lastName,
          email: this.email,
          birthDate: this.dob,
        });
      } catch {
        // the login exists; Portfolio offers to open the account again
      }
      this.submitted = false;
      this.router.navigate(['/portfolio']);
    } catch (error) {
      this.message.set(this.errorMessage(error));
      this.messageKind.set('error');
    } finally {
      this.submitting.set(false);
    }
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 409) {
        return `The username "${this.username}" is already taken.`;
      }
      if (error.status === 400) {
        const message = error.error?.message;
        return Array.isArray(message) ? message.join('. ') : message ?? 'Please check your details.';
      }
      if (error.status === 0 || error.status === 502 || error.status === 504) {
        return "Couldn't reach the login service. Is auth-service running?";
      }
    }
    return 'Registration failed. Please try again.';
  }
}

import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  imports: [FormsModule],
  selector: 'app-login',
  styleUrl: './login.css',
  templateUrl: './login.html',
})
export class Login {
  username = '';
  password = '';
  message = '';
  messageKind: 'error' | 'success' | '' = '';
  submitted = false;

  submit(): void {
    this.submitted = true;

    if (this.username.length < 3 || this.password.length < 8) {
      this.message = 'Username needs 3+ chars, password needs 8+.';
      this.messageKind = 'error';
      return;
    }

    this.message = 'Successfully submitted';
    this.messageKind = 'success';
    this.username = '';
    this.password = '';
    this.submitted = false;
  }
}

import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  imports: [FormsModule, RouterLink],
  selector: 'app-registration',
  styleUrl: './registration.css',
  templateUrl: './registration.html',
})
export class Registration {
  username = '';
  password = '';
  firstName = '';
  lastName = '';
  email = '';
  dob = '';
  message = '';
  messageKind: 'error' | 'success' | '' = '';
  submitted = false;

  submit(): void {
    this.submitted = true;

    const valid = this.username.length >= 3
      && this.password.length >= 8
      && this.firstName.length >= 2
      && this.lastName.length >= 2
      && this.email.length > 0
      && this.dob.length > 0;

    if (!valid) {
      this.message = 'Please complete all fields using valid values.';
      this.messageKind = 'error';
      return;
    }

    this.message = 'Successfully submitted';
    this.messageKind = 'success';
    this.username = '';
    this.password = '';
    this.firstName = '';
    this.lastName = '';
    this.email = '';
    this.dob = '';
    this.submitted = false;
  }
}

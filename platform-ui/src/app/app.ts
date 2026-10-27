import { Component, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Navbar } from './components/navbar/navbar';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('platform-ui');

  constructor(protected readonly router: Router) {}

  // the landing, login and registration pages fill the window, without the app's navbar
  protected fullPage(): boolean {
    const path = this.router.url.split(/[?#]/)[0];
    return path === '/' || path.startsWith('/login') || path.startsWith('/registration');
  }
}

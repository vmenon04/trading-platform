import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** The root: just the router. Each area (see app.routes.ts) brings its own frame. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
export class App {}

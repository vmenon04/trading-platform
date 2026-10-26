// The landing page's animation: the logo frog hops between perches and catches "money flies" with a tongue
// drawn as a rising stock chart. Plain TypeScript with no Angular, so it can be stepped in tests.

interface Point { x: number; y: number; }

interface Fly {
  cx: number; cy: number; ax: number; ay: number; wx: number; wy: number; p1: number; p2: number;
  born: number; alive: boolean; respawn: number; value: number;
}

type Phase = 'idle' | 'out' | 'in' | 'crouch' | 'hop' | 'land';

export interface FlyView { transform: string; wings: string; opacity: number; }

export interface SceneView {
  frog: string;
  shadow: string;
  pupilX: number;
  glintX: number;
  tongue: string | null;
  arrow: string;
  flies: FlyView[];
  pop: { x: number; y: number; opacity: number; text: string } | null;
  caught: number;
  total: string;
}

export const FLY_COUNT = 6;

// lily pads and stumps the frog sits on; y is where its base rests
const PERCHES: Point[] = [{ x: 250, y: 513 }, { x: 560, y: 575 }, { x: 835, y: 581 }, { x: 1120, y: 506 }];

// stock-chart shapes for the tongue as [fraction of the way across, fraction of the way up]; the first is the logo's
const CHARTS: [number, number][][] = [
  [[0, 0], [0.26, 0.35], [0.49, 0.81], [0.69, 0.43], [1, 1]],
  [[0, 0], [0.2, 0.24], [0.33, 0.1], [0.56, 0.64], [0.74, 0.44], [1, 1]],
  [[0, 0], [0.3, 0.44], [0.45, 0.28], [0.63, 0.72], [0.8, 0.56], [1, 1]],
];

const MOUTH = { x: 50, y: -62.5 };
const RETRACT = 0.24;

export class FrogGame {
  private t = 0;
  private flies: Fly[] = [];
  private perch = 0;
  private x = PERCHES[0].x;
  private y = PERCHES[0].y;
  private dir = 1;
  private hopH = 0;
  private phase: Phase = 'idle';
  private pt = 0;
  private wait = 0.9;
  private dur = 0;
  private target = -1;
  private chart = 0;
  private since = 0;
  private caught = 0;
  private total = 0;
  private caughtAt: Point = { x: 0, y: 0 };
  private hop = { to: 0, x0: 0, y0: 0, x1: 0, y1: 0, h: 0 };
  private pop: { x: number; y: number; t0: number; text: string } | null = null;
  // the part of the 1440-wide scene that is on screen; narrow screens crop the sides
  private minX = 0;
  private maxX = 1440;

  constructor(private readonly random: () => number = Math.random) {
    for (let i = 0; i < FLY_COUNT; i++) this.flies.push(this.newFly(i, true));
  }

  // keeps the frog and the flies inside the visible part of the scene
  setVisibleRange(minX: number, maxX: number): void {
    if (Math.abs(minX - this.minX) < 1 && Math.abs(maxX - this.maxX) < 1) return;
    this.minX = minX;
    this.maxX = maxX;
    const [lo, hi] = this.flyRange();
    this.flies.forEach((f, i) => {
      if (f.alive && (f.cx < lo || f.cx > hi)) this.flies[i] = this.newFly(i, false);
    });
  }

  step(dt: number): void {
    this.t += dt;
    this.pt += dt;
    this.flies.forEach((f, i) => {
      if (!f.alive && this.t > f.respawn) this.flies[i] = this.newFly(i, false);
    });
    if (this.pop && this.t - this.pop.t0 > 1.2) this.pop = null;

    switch (this.phase) {
      case 'idle':
        if (this.pt >= this.wait) this.decide();
        break;
      case 'out':
        if (this.pt >= this.dur) this.catchTarget();
        break;
      case 'in':
        if (this.pt >= RETRACT) this.enter('idle', this.between(0.35, 0.9));
        break;
      case 'crouch':
        if (this.pt >= 0.14) this.enter('hop');
        break;
      case 'hop': {
        const k = Math.min(1, this.pt / this.dur);
        const h = this.hop;
        this.x = h.x0 + (h.x1 - h.x0) * k;
        this.y = h.y0 + (h.y1 - h.y0) * k;
        this.hopH = h.h * 4 * k * (1 - k);
        if (k >= 1) {
          this.hopH = 0;
          this.perch = h.to;
          this.since = 0;
          this.enter('land');
        }
        break;
      }
      case 'land':
        if (this.pt >= 0.18) this.enter('idle', this.between(0.25, 0.6));
        break;
    }
  }

  view(): SceneView {
    const s = this.squash();
    const tongue = this.tongue();
    const aiming = this.phase === 'out' || this.phase === 'in';
    const flies = this.flies.map((f, i): FlyView => {
      const flap = 0.3 + 0.7 * Math.abs(Math.sin(this.t * 36 + i * 1.7));
      let x = -100, y = -100, scale = 1.7, face = 1, opacity = 0;
      if (f.alive) {
        const p = this.flyPos(f);
        const grown = Math.min(1, (this.t - f.born) / 0.7);
        x = p.x; y = p.y; opacity = grown; scale = 1.7 * (0.4 + 0.6 * grown);
        face = Math.cos(f.wx * this.t + f.p1) >= 0 ? 1 : -1;
      } else if (i === this.target && this.phase === 'in' && tongue) {
        x = tongue.tip.x; y = tongue.tip.y; opacity = 1; face = -1;
      }
      return {
        transform: `translate(${x.toFixed(1)} ${y.toFixed(1)}) scale(${(scale * face).toFixed(3)} ${scale.toFixed(3)})`,
        wings: `scale(1 ${flap.toFixed(3)})`,
        opacity,
      };
    });
    let pop: SceneView['pop'] = null;
    if (this.pop) {
      const k = Math.min(1, (this.t - this.pop.t0) / 1.2);
      pop = { x: this.pop.x, y: this.pop.y - 34 - 46 * k, opacity: 1 - k * k, text: this.pop.text };
    }
    const shadow = Math.max(0.45, 1 - this.hopH / 260);
    return {
      frog: `translate(${this.x.toFixed(1)} ${(this.y - this.hopH).toFixed(1)}) scale(${(this.dir * s.sx).toFixed(3)} ${s.sy.toFixed(3)})`,
      shadow: `translate(${this.x.toFixed(1)} ${this.y.toFixed(1)}) scale(${shadow.toFixed(3)})`,
      pupilX: aiming ? 43 : 42,
      glintX: aiming ? 47.2 : 46.2,
      tongue: tongue ? tongue.points : null,
      arrow: tongue ? tongue.arrow : '',
      flies,
      pop,
      caught: this.caught,
      total: this.total.toFixed(2),
    };
  }

  private between(a: number, b: number): number {
    return a + this.random() * (b - a);
  }

  private enter(phase: Phase, wait = 0): void {
    this.phase = phase;
    this.pt = 0;
    this.wait = wait;
  }

  private perches(): number[] {
    const fits = PERCHES.map((_, i) => i).filter((i) => PERCHES[i].x - 90 >= this.minX && PERCHES[i].x + 90 <= this.maxX);
    return fits.length >= 2 ? fits : [1, 2];
  }

  // flies stay right of the leftmost perch (the frog only shoots rightwards) and on screen
  private flyRange(): [number, number] {
    const lo = Math.max(this.minX + 60, PERCHES[this.perches()[0]].x + 150);
    return [lo, Math.max(lo + 200, this.maxX - 60)];
  }

  private newFly(i: number, initial: boolean): Fly {
    const [lo, hi] = this.flyRange();
    const slot = (hi - lo) / FLY_COUNT;
    return {
      cx: lo + slot * (i + 0.5) + this.between(-slot * 0.3, slot * 0.3), cy: this.between(345, 425),
      ax: this.between(25, 70), ay: this.between(10, 26), wx: this.between(0.4, 0.9), wy: this.between(0.8, 1.5),
      p1: this.between(0, 6.28), p2: this.between(0, 6.28),
      born: initial ? -10 : this.t, alive: true, respawn: 0, value: this.between(1.5, 48),
    };
  }

  private flyPos(f: Fly): Point {
    const t = this.t;
    return {
      x: f.cx + f.ax * Math.sin(f.wx * t + f.p1) + 3 * Math.sin(t * 11 + f.p2),
      y: f.cy + f.ay * Math.sin(f.wy * t + f.p2) + 3 * Math.cos(t * 13 + f.p1),
    };
  }

  private squash(): { sx: number; sy: number } {
    if (this.phase === 'hop') return { sx: 0.94, sy: 1.08 };
    if (this.phase === 'land') {
      const k = 1 - Math.min(1, this.pt / 0.18);
      return { sx: 1 + 0.1 * k, sy: 1 - 0.14 * k };
    }
    if (this.phase === 'crouch') {
      const k = Math.min(1, this.pt / 0.14);
      return { sx: 1 + 0.08 * k, sy: 1 - 0.12 * k };
    }
    return { sx: 1, sy: 1 + 0.015 * Math.sin(this.t * 3.2) };
  }

  private mouth(): Point {
    const s = this.squash();
    return { x: this.x + this.dir * MOUTH.x * s.sx, y: this.y - this.hopH + MOUTH.y * s.sy };
  }

  // a fly is in reach when it is ahead of the frog (the chart reads left to right) and not too steeply above
  private reach(f: Fly): number {
    const p = this.flyPos(f);
    const dx = p.x - (this.x + MOUTH.x);
    const rise = this.y + MOUTH.y - p.y;
    return dx >= 120 && dx <= 560 && rise >= -20 && rise <= dx * 0.75 ? dx : -1;
  }

  private decide(): void {
    if (!this.perches().includes(this.perch)) {
      this.startHop();
      return;
    }
    if (this.dir !== 1) {
      this.dir = 1;
      this.enter('land');
      return;
    }
    let best = -1, bestDx = Infinity;
    this.flies.forEach((f, i) => {
      if (!f.alive || this.t - f.born < 0.7) return;
      const dx = this.reach(f);
      if (dx >= 0 && dx < bestDx) { bestDx = dx; best = i; }
    });
    if (best >= 0 && this.since < 3) {
      this.target = best;
      this.chart = Math.floor(this.random() * CHARTS.length);
      this.enter('out');
      this.dur = 0.4 + bestDx / 1400;
    } else {
      this.startHop();
    }
  }

  private catchTarget(): void {
    const f = this.flies[this.target];
    const p = this.flyPos(f);
    f.alive = false;
    f.respawn = this.t + 1.8;
    this.caughtAt = p;
    this.caught += 1;
    this.total += f.value;
    this.since += 1;
    this.pop = { x: p.x, y: p.y, t0: this.t, text: `+$${f.value.toFixed(2)}` };
    this.enter('in');
  }

  // hop to the perch with a fly about where the frog likes to shoot from
  private startHop(): void {
    let to = this.perch, best = Infinity;
    this.perches().forEach((i) => {
      const perch = PERCHES[i];
      if (i === this.perch) return;
      let score = 600 + this.random() * 200;
      for (const f of this.flies) {
        if (!f.alive) continue;
        const p = this.flyPos(f);
        score = Math.min(score, Math.abs(p.x - (perch.x + 330)) + 0.5 * Math.abs(p.y - (perch.y - 170)));
      }
      score += this.random() * 60;
      if (score < best) { best = score; to = i; }
    });
    const dest = PERCHES[to];
    const dx = dest.x - this.x;
    this.hop = { to, x0: this.x, y0: this.y, x1: dest.x, y1: dest.y, h: 110 + Math.abs(dx) * 0.18 };
    this.dir = dx >= 0 ? 1 : -1;
    this.enter('crouch');
    this.dur = 0.5 + Math.abs(dx) / 1600;
  }

  // the tongue as a partly drawn stock chart from the mouth to the fly, ending in the logo's arrowhead
  private tongue(): { points: string; arrow: string; tip: Point } | null {
    let progress = 0;
    let end: Point | null = null;
    if (this.phase === 'out') {
      const k = Math.min(1, this.pt / this.dur);
      progress = 1 - (1 - k) * (1 - k);
      end = this.flyPos(this.flies[this.target]);
    } else if (this.phase === 'in') {
      const k = Math.min(1, this.pt / RETRACT);
      progress = 1 - k * k;
      end = this.caughtAt;
    }
    if (!end || progress <= 0.001) return null;

    const m = this.mouth();
    const dx = end.x - m.x, dy = end.y - m.y;
    const swing = Math.max(-dy, dx * 0.3);
    const pts = CHARTS[this.chart].map(([s, r]) => ({ x: m.x + dx * s, y: m.y + dy * s - (r - s) * swing }));

    const lengths = pts.slice(1).map((p, i) => Math.hypot(p.x - pts[i].x, p.y - pts[i].y));
    let left = lengths.reduce((a, b) => a + b, 0) * progress;
    const drawn: Point[] = [pts[0]];
    let tip = pts[0], ux = 1, uy = 0;
    for (let i = 1; i < pts.length; i++) {
      const d = lengths[i - 1] || 1, a = pts[i - 1], b = pts[i];
      ux = (b.x - a.x) / d; uy = (b.y - a.y) / d;
      if (left >= d) { drawn.push(b); tip = b; left -= d; }
      else { tip = { x: a.x + ux * left, y: a.y + uy * left }; drawn.push(tip); break; }
    }

    const h = 24, w = 12;
    const bx = tip.x - ux * h * 0.7, by = tip.y - uy * h * 0.7;
    const head = [[tip.x + ux * h * 0.3, tip.y + uy * h * 0.3], [bx - uy * w, by + ux * w], [bx + uy * w, by - ux * w]];
    const fmt = (x: number, y: number) => `${x.toFixed(1)},${y.toFixed(1)}`;
    return {
      points: drawn.map((p) => fmt(p.x, p.y)).join(' '),
      arrow: head.map(([x, y]) => fmt(x, y)).join(' '),
      tip,
    };
  }
}

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InstrumentSummary } from './instrument-summary';

describe('InstrumentSummary', () => {
  let component: InstrumentSummary;
  let fixture: ComponentFixture<InstrumentSummary>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InstrumentSummary],
    }).compileComponents();

    fixture = TestBed.createComponent(InstrumentSummary);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

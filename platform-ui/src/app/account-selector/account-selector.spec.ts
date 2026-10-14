import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccountSelector } from './account-selector';

describe('AccountSelector', () => {
  let component: AccountSelector;
  let fixture: ComponentFixture<AccountSelector>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountSelector],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountSelector);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlanningService, CalendarEventDto } from '../../../core/services/planning.service';
import { AuthService } from '../../../core/services/auth.service';

export interface CalendarDay {
  date: Date;
  isCurrentMonth: boolean;
  isToday: boolean;
  dayOfMonth: number;
  events: CalendarEventDto[];
}

const WEEKDAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './calendar.html',
  styleUrl: './calendar.scss',
})
export class Calendar implements OnInit {
  currentMonth: Date;
  events: CalendarEventDto[] = [];
  loading = false;
  selectedDay: Date | null = null;
  readonly weekdayLabels = WEEKDAY_LABELS;

  constructor(
    private planning: PlanningService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    const now = new Date();
    this.currentMonth = new Date(now.getFullYear(), now.getMonth(), 1);
  }

  ngOnInit(): void {
    this.loadEvents();
  }

  get monthTitle(): string {
    return this.currentMonth.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  }

  get calendarDays(): CalendarDay[][] {
    const year = this.currentMonth.getFullYear();
    const month = this.currentMonth.getMonth();
    const first = new Date(year, month, 1);
    const last = new Date(year, month + 1, 0);
    const startOffset = first.getDay();
    const daysInMonth = last.getDate();

    const prevMonth = new Date(year, month, 0);
    const prevDays = prevMonth.getDate();
    const rows: CalendarDay[][] = [];
    let row: CalendarDay[] = [];
    let dayIndex = 0;

    // Leading days from previous month
    for (let i = 0; i < startOffset; i++) {
      const d = prevDays - startOffset + i + 1;
      const date = new Date(year, month - 1, d);
      row.push(this.toDay(date, false));
      dayIndex++;
    }

    // Current month
    for (let d = 1; d <= daysInMonth; d++) {
      const date = new Date(year, month, d);
      row.push(this.toDay(date, true));
      dayIndex++;
      if (row.length === 7) {
        rows.push(row);
        row = [];
      }
    }

    // Trailing days from next month
    let nextD = 1;
    while (row.length > 0 && row.length < 7) {
      const date = new Date(year, month + 1, nextD);
      row.push(this.toDay(date, false));
      nextD++;
    }
    if (row.length > 0) rows.push(row);

    return rows;
  }

  private toDay(date: Date, isCurrentMonth: boolean): CalendarDay {
    const normalized = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const today = new Date();
    const isToday =
      normalized.getDate() === today.getDate() &&
      normalized.getMonth() === today.getMonth() &&
      normalized.getFullYear() === today.getFullYear();
    return {
      date: normalized,
      isCurrentMonth,
      isToday,
      dayOfMonth: normalized.getDate(),
      events: this.getEventsForDate(normalized),
    };
  }

  private getEventsForDate(date: Date): CalendarEventDto[] {
    const y = date.getFullYear();
    const m = date.getMonth();
    const d = date.getDate();
    return this.events.filter((ev) => {
      if (!ev.start) return false;
      const evDate = new Date(ev.start);
      return evDate.getFullYear() === y && evDate.getMonth() === m && evDate.getDate() === d;
    });
  }

  loadEvents(): void {
    this.loading = true;
    const start = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth(), 1);
    const end = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth() + 2, 0);
    const timeMin = start.toISOString();
    const timeMax = end.toISOString();
    const userId = this.auth.getUserId();
    const role = this.auth.getUserRole();
    this.planning.getCalendarEvents({ timeMin, timeMax, userId: userId ?? undefined, role: role ?? undefined }).subscribe({
      next: (list) => {
        this.events = list ?? [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.events = [];
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  goPrevMonth(): void {
    this.currentMonth = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth() - 1, 1);
    this.loadEvents();
    this.selectedDay = null;
  }

  goNextMonth(): void {
    this.currentMonth = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth() + 1, 1);
    this.loadEvents();
    this.selectedDay = null;
  }

  goToToday(): void {
    const now = new Date();
    this.currentMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    this.loadEvents();
    this.selectedDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  }

  selectDay(day: CalendarDay): void {
    this.selectedDay = day.date;
  }

  getSelectedDayEvents(): CalendarEventDto[] {
    if (!this.selectedDay) return [];
    return this.getEventsForDate(this.selectedDay);
  }

  get selectedDayLabel(): string {
    if (!this.selectedDay) return '';
    return this.selectedDay.toLocaleDateString('en-US', {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    });
  }

  formatDate(s: string | null): string {
    if (!s) return '—';
    const d = new Date(s);
    return isNaN(d.getTime()) ? s : d.toLocaleString(undefined, { dateStyle: 'short', timeStyle: 'short' });
  }

  formatTime(s: string | null): string {
    if (!s) return '';
    const d = new Date(s);
    return isNaN(d.getTime()) ? '' : d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
  }
}

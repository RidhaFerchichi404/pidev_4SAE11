import { Injectable } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { UserService, User } from './user.service';
import { PortfolioService, Skill, Experience, EvaluationResult } from './portfolio.service';
import { ReviewService } from './review.service';

// ── Interfaces ─────────────────────────────────────────────────────────────────

export interface ProfileSkill {
  name: string;
  category: string;
  level: 'Beginner' | 'Intermediate' | 'Expert';
  verified: boolean;
}

export interface ProfileExperience {
  role: string;
  company: string;
  duration: string;
  description: string;
}

export interface ProfileProject {
  id: number;
  title: string;
  clientName: string;
  completionDate: string;
  rating: number;
  thumbnail: string;
}

export interface ProfileTestimonial {
  clientName: string;
  clientInitials: string;
  clientColor: string;
  projectName: string;
  rating: number;
  review: string;
}

export interface FreelancerCard {
  userId: number;
  firstName: string;
  lastName: string;
  title: string;
  avatarUrl?: string;
  rating: number;
  totalReviews: number;
  skills: ProfileSkill[];
}

export interface FreelancerProfile {
  userId: number;
  firstName: string;
  lastName: string;
  title: string;
  location: string;
  memberSince: string;
  avatarUrl?: string;
  rating: number;
  totalReviews: number;
  projectsCompleted: number;
  responseRate: number;
  hourlyRate: number;
  bio: string;
  yearsExperience: number;
  skills: ProfileSkill[];
  experiences: ProfileExperience[];
  recentProjects: ProfileProject[];
  testimonials: ProfileTestimonial[];
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const DOMAIN_TITLE_MAP: Record<string, string> = {
  WEB_DEVELOPMENT:         'Web Developer',
  MOBILE_DEV:              'Mobile Developer',
  DATA_SCIENCE:            'Data Scientist',
  ARTIFICIAL_INTELLIGENCE: 'AI Engineer',
  DEVOPS:                  'DevOps Engineer',
  CLOUD_COMPUTING:         'Cloud Engineer',
  CYBERSECURITY:           'Security Engineer',
  BLOCKCHAIN:              'Blockchain Developer',
  GAME_DEVELOPMENT:        'Game Developer',
  EMBEDDED_SYSTEMS:        'Embedded Systems Engineer',
  DATABASE_ADMINISTRATION: 'Database Administrator',
  UI_UX_DESIGN:            'UI/UX Designer',
  GRAPHIC_DESIGN:          'Graphic Designer',
  PHOTOGRAPHY:             'Photographer',
  VIDEO_MAKING:            'Video Producer',
  CONTENT_CREATION:        'Content Creator',
  MUSIC_PRODUCTION:        'Music Producer',
  ANIMATION:               'Animator',
  ILLUSTRATION:            'Illustrator',
  MARKETING:               'Marketing Specialist',
  COPYWRITING:             'Copywriter',
  TRANSLATION:             'Translator',
  ARCHITECTURE:            'Architect',
  FINANCE:                 'Finance Specialist',
  OTHER:                   'Freelancer',
};

function titleFromSkills(skills: Skill[]): string {
  const domain = skills?.[0]?.domains?.[0];
  return domain ? (DOMAIN_TITLE_MAP[domain] ?? 'Freelancer') : 'Freelancer';
}

/**
 * Maps a Portfolio Skill to a ProfileSkill.
 * Pass the evaluations map (skillId → EvaluationResult) to derive level + verified.
 * If no evaluation exists for the skill, defaults to Intermediate/unverified.
 */
function skillFromApi(
  s: Skill,
  evalMap: Map<number, EvaluationResult>,
): ProfileSkill {
  const evaluation = s.id != null ? evalMap.get(s.id) : undefined;
  const score = evaluation?.score ?? -1;
  const level: ProfileSkill['level'] =
    score >= 80 ? 'Expert' : score >= 50 ? 'Intermediate' : score >= 0 ? 'Beginner' : 'Intermediate';
  const verified = !!(evaluation?.passed);
  const category = s.domains?.[0]
    ? s.domains[0].replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())
    : 'General';
  return { name: s.name, category, level, verified };
}

function expFromApi(e: Experience): ProfileExperience {
  const start = e.startDate ? new Date(e.startDate).getFullYear() : '?';
  const end   = e.endDate   ? new Date(e.endDate).getFullYear()   : 'Present';
  return {
    role:        e.title,
    company:     e.companyOrClientName,
    duration:    `${start} – ${end}`,
    description: e.description ?? '',
  };
}

/** Returns years since the earliest experience start date (or 0). */
function calcYearsExperience(experiences: Experience[]): number {
  if (!experiences.length) return 0;
  const oldest = experiences.reduce((min, e) => {
    const t = e.startDate ? new Date(e.startDate).getTime() : Date.now();
    return t < min ? t : min;
  }, Date.now());
  return Math.max(0, Math.round((Date.now() - oldest) / (365.25 * 24 * 60 * 60 * 1000)));
}

// ── Service ───────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class FreelancerService {
  constructor(
    private userSvc: UserService,
    private portfolioSvc: PortfolioService,
    private reviewSvc: ReviewService,
  ) {}

  /**
   * Returns a card-summary list of all freelancers.
   * Fetches: users → (skills + review stats) per freelancer in parallel.
   */
  getAllFreelancers(): Observable<FreelancerCard[]> {
    return this.userSvc.getFreelancers().pipe(
      switchMap((users: User[]) => {
        if (!users.length) return of([]);

        return forkJoin(
          users.map(u =>
            forkJoin({
              skills: this.portfolioSvc.getUserSkills(u.id).pipe(catchError(() => of([]))),
              stats:  this.reviewSvc.getStatsByReviewee(u.id).pipe(catchError(() => of(null))),
            }).pipe(
              map(({ skills, stats }) => ({
                userId:       u.id,
                firstName:    u.firstName,
                lastName:     u.lastName,
                title:        titleFromSkills(skills),
                avatarUrl:    u.avatarUrl,
                rating:       Math.round((stats?.averageRating ?? 0) * 10) / 10,
                totalReviews: stats?.totalCount ?? 0,
                // No evaluations for cards (too many calls); skills show without level
                skills:       skills.map(s => skillFromApi(s, new Map())).slice(0, 4),
              } as FreelancerCard)),
            ),
          ),
        );
      }),
      catchError(() => of([])),
    );
  }

  /**
   * Returns the full profile for a single freelancer.
   * Fetches: user, skills, experiences, reviews, review stats, evaluations
   * — then resolves reviewer names for testimonials.
   */
  getFreelancerProfile(userId: number): Observable<FreelancerProfile | null> {
    return this.userSvc.getById(userId).pipe(
      switchMap(user => {
        if (!user || user.role !== 'FREELANCER') return of(null);

        return forkJoin({
          skills:      this.portfolioSvc.getUserSkills(userId).pipe(catchError(() => of([]))),
          experiences: this.portfolioSvc.getExperiences(userId).pipe(catchError(() => of([]))),
          reviews:     this.reviewSvc.getByRevieweeId(userId).pipe(catchError(() => of([]))),
          stats:       this.reviewSvc.getStatsByReviewee(userId).pipe(catchError(() => of(null))),
          evaluations: this.portfolioSvc.getUserEvaluations(userId).pipe(catchError(() => of([]))),
        }).pipe(
          switchMap(({ skills, experiences, reviews, stats, evaluations }) => {
            // Build evaluation map: skillId → EvaluationResult
            const evalMap = new Map<number, EvaluationResult>(
              evaluations
                .filter(e => e.skill?.id != null)
                .map(e => [e.skill!.id, e]),
            );

            // Resolve reviewer names for testimonials
            const reviewsWithComment = reviews.filter(r => r.comment);
            const uniqueReviewerIds = [...new Set(reviewsWithComment.map(r => r.reviewerId))];

            const reviewerCalls: Observable<(User | null)[]> = uniqueReviewerIds.length
              ? forkJoin(uniqueReviewerIds.map(id => this.userSvc.getById(id).pipe(catchError(() => of(null)))))
              : of([]);

            return reviewerCalls.pipe(
              map(reviewers => {
                const reviewerMap = new Map<number, User>(
                  reviewers
                    .filter((r): r is User => r != null)
                    .map((r, i) => [uniqueReviewerIds[i], r]),
                );

                const testimonials: ProfileTestimonial[] = reviewsWithComment
                  .slice(0, 4)
                  .map(r => {
                    const reviewer = reviewerMap.get(r.reviewerId);
                    const name     = reviewer ? `${reviewer.firstName} ${reviewer.lastName}` : 'Client';
                    const initials = reviewer
                      ? (reviewer.firstName[0] + reviewer.lastName[0]).toUpperCase()
                      : 'CL';
                    return {
                      clientName:    name,
                      clientInitials: initials,
                      clientColor:   '#667eea',
                      projectName:   `Project #${r.projectId}`,
                      rating:        r.rating,
                      review:        r.comment!,
                    };
                  });

                const memberSince = user.createdAt
                  ? user.createdAt.substring(0, 4)
                  : new Date().getFullYear().toString();

                return {
                  userId:             user.id,
                  firstName:          user.firstName,
                  lastName:           user.lastName,
                  title:              experiences.length ? experiences[0].title : titleFromSkills(skills),
                  location:           'Remote',
                  memberSince,
                  avatarUrl:          user.avatarUrl ?? undefined,
                  rating:             Math.round((stats?.averageRating ?? 0) * 10) / 10,
                  totalReviews:       stats?.totalCount ?? 0,
                  projectsCompleted:  reviews.length,
                  responseRate:       100,
                  hourlyRate:         0,
                  bio:                '',
                  yearsExperience:    calcYearsExperience(experiences),
                  skills:             skills.map(s => skillFromApi(s, evalMap)),
                  experiences:        experiences.map(expFromApi),
                  recentProjects:     [],
                  testimonials,
                } as FreelancerProfile;
              }),
            );
          }),
          catchError(() => of(null)),
        );
      }),
      catchError(() => of(null)),
    );
  }
}

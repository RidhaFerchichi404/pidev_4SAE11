import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, tap, timeout } from 'rxjs';

// Mirrors the Java Domain enum — keep in sync with Domain.java
export type Domain =
  // Technology
  | 'WEB_DEVELOPMENT'
  | 'MOBILE_DEV'
  | 'DATA_SCIENCE'
  | 'ARTIFICIAL_INTELLIGENCE'
  | 'DEVOPS'
  | 'CLOUD_COMPUTING'
  | 'CYBERSECURITY'
  | 'BLOCKCHAIN'
  | 'GAME_DEVELOPMENT'
  | 'EMBEDDED_SYSTEMS'
  | 'DATABASE_ADMINISTRATION'
  // Creative & Media
  | 'UI_UX_DESIGN'
  | 'GRAPHIC_DESIGN'
  | 'PHOTOGRAPHY'
  | 'VIDEO_MAKING'
  | 'CONTENT_CREATION'
  | 'MUSIC_PRODUCTION'
  | 'ANIMATION'
  | 'ILLUSTRATION'
  // Business & Other
  | 'MARKETING'
  | 'COPYWRITING'
  | 'TRANSLATION'
  | 'ARCHITECTURE'
  | 'FINANCE'
  | 'OTHER';

/** Human-readable labels for each Domain enum value */
export const DOMAIN_LABELS: Record<Domain, string> = {
  WEB_DEVELOPMENT:        'Web Development',
  MOBILE_DEV:             'Mobile Development',
  DATA_SCIENCE:           'Data Science',
  ARTIFICIAL_INTELLIGENCE:'Artificial Intelligence',
  DEVOPS:                 'DevOps',
  CLOUD_COMPUTING:        'Cloud Computing',
  CYBERSECURITY:          'Cybersecurity',
  BLOCKCHAIN:             'Blockchain',
  GAME_DEVELOPMENT:       'Game Development',
  EMBEDDED_SYSTEMS:       'Embedded Systems',
  DATABASE_ADMINISTRATION:'Database Administration',
  UI_UX_DESIGN:           'UI / UX Design',
  GRAPHIC_DESIGN:         'Graphic Design',
  PHOTOGRAPHY:            'Photography',
  VIDEO_MAKING:           'Video Making',
  CONTENT_CREATION:       'Content Creation',
  MUSIC_PRODUCTION:       'Music Production',
  ANIMATION:              'Animation',
  ILLUSTRATION:           'Illustration',
  MARKETING:              'Marketing',
  COPYWRITING:            'Copywriting',
  TRANSLATION:            'Translation',
  ARCHITECTURE:           'Architecture',
  FINANCE:                'Finance',
  OTHER:                  'Other',
};

export interface SkillDomainStat {
  domain: Domain;
  count: number;
}

export interface ExperienceDomainStat {
  domain: Domain;
  count: number;
}

export interface Skill {
  id?: number;
  name: string;
  domains: Domain[];   // multi-select — matches the @ElementCollection in the backend
  description: string;
  userId?: number;
  verified?: boolean;
  score?: number;
}

export interface Experience {
  id?: number;
  userId: number;
  title: string;
  type: 'JOB' | 'PROJECT';
  domain?: Domain;      // single optional domain for the experience
  description: string;
  startDate: string;
  endDate: string;
  companyOrClientName: string;
  keyTasks: string[];
  skills?: Skill[];
  skillNames?: string[]; // For creation/update
}

export interface TestSubmission {
  testId: number;
  freelancerId: number;
  answers: { questionIndex: number; selectedOption: string }[];
}

export interface EvaluationTest {
  id: number;
  title: string;
  questions: { questionText: string; options: string; correctOption: string; points: number }[];
  passingScore: number;
  durationMinutes: number;
}

export interface EvaluationResult {
  id: number;
  score: number;
  passed: boolean;
  testResult: string;
  skill?: { id: number; name: string; domains: Domain[] };
  evaluatedAt?: string;
  freelancerId?: number;
}

export interface SkillUsageStat {
  skillName: string;
  userCount: number;
  percentage: number;
}

export interface SkillSuccessStat {
  skillName: string;
  totalAttempts: number;
  passedCount: number;
  successRate: number;
}

@Injectable({
  providedIn: 'root'
})
export class PortfolioService {
  private apiUrl = 'http://localhost:8078/portfolio/api'; // Gateway URL

  public skillsUpdated$ = new Subject<void>();

  constructor(private http: HttpClient) {}

  notifySkillsUpdated() {
    this.skillsUpdated$.next();
  }

  // --- Skills ---
  getAllSkills(): Observable<Skill[]> {
    return this.http.get<Skill[]>(`${this.apiUrl}/skills`);
  }

  getUserSkills(userId: number): Observable<Skill[]> {
    const url = `${this.apiUrl}/skills/user/${userId}`;
    console.log('Fetching skills from:', url);
    return this.http.get<Skill[]>(url).pipe(
      tap((response: Skill[]) => console.log('HTTP Response for getUserSkills:', response))
    );
  }

  // --- Domains ---

  /** Fetches all pre-defined domains from the backend enum. */
  getDomains(): Observable<Domain[]> {
    return this.http.get<Domain[]>(`${this.apiUrl}/skills/domains`);
  }

  // --- Admin statistics ---

  getSkillStatsByDomain(): Observable<SkillDomainStat[]> {
    return this.http.get<SkillDomainStat[]>(`${this.apiUrl}/skills/stats/by-domain`);
  }

  getExperienceStatsByDomain(): Observable<ExperienceDomainStat[]> {
    return this.http.get<ExperienceDomainStat[]>(`${this.apiUrl}/experiences/stats/by-domain`);
  }

  // --- Freelancer: replace checked domains for a skill ---

  /**
   * PATCH /api/skills/{id}/domains
   * Sends the full list of checked domain enum values.
   */
  updateSkillDomains(skillId: number, domains: Domain[]): Observable<Skill> {
    return this.http.patch<Skill>(`${this.apiUrl}/skills/${skillId}/domains`, domains);
  }

  // Helper for adding skill by name for a user
  addUserSkill(userId: number, skillName: string): Observable<Skill> {
    const skill: Skill = {
      name: skillName,
      domains: ['OTHER'],
      description: 'Added via Dashboard',
      userId: userId
    };
    return this.http.post<Skill>(`${this.apiUrl}/skills`, skill);
  }

  createSkill(skill: Skill): Observable<Skill> {
    return this.http.post<Skill>(`${this.apiUrl}/skills`, skill);
  }

  updateSkill(id: number, skill: Skill): Observable<Skill> {
    return this.http.put<Skill>(`${this.apiUrl}/skills/${id}`, skill);
  }

  deleteUserSkill(userId: number, skillId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/skills/${skillId}`);
  }

  deleteSkill(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/skills/${id}`);
  }

  // --- Experiences ---
  getExperiences(userId: number): Observable<Experience[]> {
    return this.http.get<Experience[]>(`${this.apiUrl}/experiences/user/${userId}`);
  }

  createExperience(experience: Experience): Observable<Experience> {
    return this.http.post<Experience>(`${this.apiUrl}/experiences`, experience);
  }

  updateExperience(id: number, experience: Experience): Observable<Experience> {
    return this.http.put<Experience>(`${this.apiUrl}/experiences/${id}`, experience);
  }

  deleteExperience(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/experiences/${id}`);
  }

  // --- Evaluation ---
  generateTest(skillId: number): Observable<EvaluationTest> {
    return this.http.post<EvaluationTest>(`${this.apiUrl}/evaluation-tests/generate/${skillId}`, {}).pipe(
      timeout(120000)
    );
  }

  submitTest(submission: TestSubmission): Observable<EvaluationResult> {
    return this.http.post<EvaluationResult>(`${this.apiUrl}/evaluations/submit`, submission);
  }

  getEvaluationResult(userId: number, skillId: number): Observable<EvaluationResult> {
    return this.http.get<EvaluationResult>(`${this.apiUrl}/evaluations/freelancer/${userId}/skill/${skillId}`);
  }

  getUserEvaluations(userId: number): Observable<EvaluationResult[]> {
    return this.http.get<EvaluationResult[]>(`${this.apiUrl}/evaluations/freelancer/${userId}`);
  }

  // --- Admin statistics ---

  getSkillUsageStats(): Observable<SkillUsageStat[]> {
    return this.http.get<SkillUsageStat[]>(`${this.apiUrl}/skills/stats/usage`);
  }

  getSkillSuccessStats(): Observable<SkillSuccessStat[]> {
    return this.http.get<SkillSuccessStat[]>(`${this.apiUrl}/skills/stats/success`);
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, tap, timeout } from 'rxjs';

export interface Skill {
  id?: number;
  name: string;
  domain: string;
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
  questions: { questionText: string; options: string; correctOption: string; points: number }[]; // options comma separated
  passingScore: number;
  durationMinutes: number;
}

export interface EvaluationResult {
  id: number;
  score: number;
  passed: boolean;
  testResult: string;
  skill?: { id: number; name: string; domain: string };
  evaluatedAt?: string;
  freelancerId?: number;
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
  getUserSkills(userId: number): Observable<Skill[]> {
    const url = `${this.apiUrl}/skills/user/${userId}`;
    console.log('Fetching skills from:', url);
    return this.http.get<Skill[]>(url).pipe(
      tap((response: Skill[]) => console.log('HTTP Response for getUserSkills:', response))
    );
  }

  // Helper for adding skill by name for a user
  addUserSkill(userId: number, skillName: string): Observable<Skill> {
      const skill: Skill = {
          name: skillName,
          domain: 'General',
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
      // Add timeout using RxJS operator (120 seconds for AI generation)
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
}

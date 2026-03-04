import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

export interface BriefData {
  appName: string;
  tagline: string;
  industry: string;
  style: string;
  primaryColor: string;
  secondaryColor: string;
  iconType: string;
  projectType: string;
  pages: string[];
  customPageInput: string;
  customPages: string[];
  budget: string;
  deadline: string;
  description: string;
}

@Component({
  selector: 'app-design-brief',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './design-brief.html',
  styleUrl: './design-brief.scss',
})
export class DesignBrief implements OnInit {
  step = 1;
  readonly totalSteps = 3;
  copied = false;
  briefSaved = false;

  static readonly STORAGE_KEY = 'client_design_brief';

  brief: BriefData = {
    appName: '',
    tagline: '',
    industry: '',
    style: '',
    primaryColor: '#6C63FF',
    secondaryColor: '#FF6584',
    iconType: 'letter',
    projectType: '',
    pages: [],
    customPageInput: '',
    customPages: [],
    budget: '',
    deadline: '',
    description: '',
  };

  readonly industries = [
    'Technologie', 'Santé', 'Finance', 'Éducation', 'E-commerce',
    'Divertissement', 'Restauration', 'Immobilier', 'Voyage', 'Mode',
    'Sport & Fitness', 'Autre',
  ];

  readonly styles = [
    { id: 'modern',   label: 'Moderne',    desc: 'Épuré, géométrique, contemporain' },
    { id: 'classic',  label: 'Classique',  desc: 'Élégant, professionnel, intemporel' },
    { id: 'bold',     label: 'Audacieux',  desc: 'Impactant, fort, énergique' },
    { id: 'playful',  label: 'Ludique',    desc: 'Fun, coloré, accessible' },
    { id: 'minimal',  label: 'Minimaliste',desc: 'Simple, discret, raffiné' },
    { id: 'tech',     label: 'Tech',       desc: 'Futuriste, numérique, innovant' },
  ];

  readonly palettes = [
    { name: 'Violet & Rose',     primary: '#6C63FF', secondary: '#FF6584' },
    { name: 'Bleu & Cyan',       primary: '#2196F3', secondary: '#00BCD4' },
    { name: 'Vert & Ambre',      primary: '#4CAF50', secondary: '#FFC107' },
    { name: 'Rouge & Orange',    primary: '#E53935', secondary: '#FF9800' },
    { name: 'Marine & Or',       primary: '#1A237E', secondary: '#FFD700' },
    { name: 'Sombre & Émeraude', primary: '#212121', secondary: '#00E676' },
    { name: 'Rose & Corail',     primary: '#E91E63', secondary: '#FF5722' },
    { name: 'Indigo & Vert',     primary: '#3F51B5', secondary: '#4CAF50' },
  ];

  readonly iconTypes = [
    { id: 'letter',   label: 'Initiales' },
    { id: 'abstract', label: 'Abstrait' },
    { id: 'badge',    label: 'Badge' },
    { id: 'circle',   label: 'Cercle' },
  ];

  readonly projectTypes = [
    'Site Web Vitrine', 'Application Mobile', 'Application Web',
    'E-commerce', 'Landing Page', 'Dashboard Admin',
    'Application Desktop', 'Portfolio',
  ];

  readonly availablePages = [
    'Accueil', 'À propos', 'Services', 'Portfolio', 'Blog',
    'Contact', 'Connexion / Inscription', 'Tableau de bord',
    'Profil', 'Paramètres', 'Tarifs', 'FAQ', 'Panier / Paiement',
    'Galerie', 'Témoignages',
  ];

  readonly budgetRanges = [
    '< 500 TND', '500–1 000 TND', '1 000–3 000 TND',
    '3 000–5 000 TND', '> 5 000 TND', 'Flexible / À discuter',
  ];

  // ── Computed ────────────────────────────────────────────────────

  get logoLetters(): string {
    if (!this.brief.appName.trim()) return 'AB';
    const words = this.brief.appName.trim().split(/\s+/);
    return words.length >= 2
      ? (words[0][0] + words[1][0]).toUpperCase()
      : this.brief.appName.substring(0, 2).toUpperCase();
  }

  get selectedStyle() {
    return this.styles.find(s => s.id === this.brief.style) ?? null;
  }

  get step1Valid(): boolean {
    return !!(this.brief.appName.trim() && this.brief.industry && this.brief.style);
  }

  get step2Valid(): boolean {
    return !!(this.brief.projectType && this.brief.pages.length > 0);
  }

  get progressPercent(): number {
    return ((this.step - 1) / (this.totalSteps - 1)) * 100;
  }

  // ── Navigation ──────────────────────────────────────────────────

  nextStep(): void {
    if (this.step < this.totalSteps) {
      this.step++;
      // Auto-save when reaching the summary step
      if (this.step === this.totalSteps) {
        this.saveBriefToStorage();
      }
    }
  }

  prevStep(): void {
    if (this.step > 1) this.step--;
  }

  goToStep(n: number): void {
    if (n < this.step) this.step = n;
  }

  // ── Actions ─────────────────────────────────────────────────────

  togglePage(page: string): void {
    const idx = this.brief.pages.indexOf(page);
    if (idx > -1) {
      this.brief.pages.splice(idx, 1);
    } else {
      this.brief.pages.push(page);
    }
  }

  isPageSelected(page: string): boolean {
    return this.brief.pages.includes(page);
  }

  setPalette(palette: { primary: string; secondary: string }): void {
    this.brief.primaryColor = palette.primary;
    this.brief.secondaryColor = palette.secondary;
  }

  addCustomPage(): void {
    const p = this.brief.customPageInput.trim();
    if (p && !this.brief.customPages.includes(p)) {
      this.brief.customPages.push(p);
      this.brief.pages.push(p);
    }
    this.brief.customPageInput = '';
  }

  removeCustomPage(page: string): void {
    this.brief.customPages = this.brief.customPages.filter(p => p !== page);
    this.brief.pages = this.brief.pages.filter(p => p !== page);
  }

  saveBriefToStorage(): void {
    const payload = {
      ...this.brief,
      savedAt: new Date().toISOString(),
      logoLetters: this.logoLetters,
      styleName: this.selectedStyle?.label ?? '',
    };
    localStorage.setItem(DesignBrief.STORAGE_KEY, JSON.stringify(payload));
    this.briefSaved = true;
  }

  copyBrief(): void {
    navigator.clipboard.writeText(this.generateBriefText()).then(() => {
      this.copied = true;
      setTimeout(() => (this.copied = false), 2500);
    });
  }

  generateBriefText(): string {
    const line = (label: string, value: string) =>
      value ? `${label}: ${value}` : '';
    return [
      '╔══════════════════════════════╗',
      '║       DESIGN BRIEF           ║',
      '╚══════════════════════════════╝',
      '',
      '── IDENTITÉ DE MARQUE ──────────',
      line('Nom du projet', this.brief.appName),
      line('Tagline',       this.brief.tagline),
      line('Secteur',       this.brief.industry),
      line('Style',         this.selectedStyle?.label ?? ''),
      line('Couleur principale',   this.brief.primaryColor),
      line('Couleur secondaire',   this.brief.secondaryColor),
      '',
      '── STRUCTURE APPLICATION ───────',
      line('Type de projet', this.brief.projectType),
      `Pages: ${[...this.brief.pages, ...this.brief.customPages].join(', ') || 'N/A'}`,
      '',
      '── DÉTAILS DU PROJET ───────────',
      line('Budget',      this.brief.budget),
      line('Délai',       this.brief.deadline),
      line('Description', this.brief.description),
      '',
      `Généré le ${new Date().toLocaleDateString('fr-FR')}`,
    ]
      .filter(l => l !== undefined)
      .join('\n');
  }

  ngOnInit(): void {}
}

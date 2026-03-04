// project-feed.ts — TypeScript interfaces for the Freelancia project feed + live search

export interface ProjectFeed {
  id: string;
  title: string;
  budget: number;
  client: ClientProfile;
  description: string;
  skills: string[];
  postedAgo: string;
  category?: string;
  isRemote?: boolean;
}

export interface ClientProfile {
  name: string;
  avatar: string;
  rating: number;        // e.g. 4.8
  reviewCount?: number;
}

export interface SearchResult {
  type: 'project' | 'category';
  id: string;
  title: string;
  // project-specific
  budget?: number;
  skills?: string[];
  thumbnail?: string;
  // category-specific
  icon?: string;
  projectCount?: number;
}

export interface FeedCategory {
  id: string;
  name: string;
  icon: string;
}

export const FEED_CATEGORIES: FeedCategory[] = [
  { id: 'web-dev',   name: 'Web Development',    icon: '💻' },
  { id: 'mobile',    name: 'Mobile Apps',         icon: '📱' },
  { id: 'design',    name: 'UI/UX Design',        icon: '🎨' },
  { id: 'data',      name: 'Data Science',        icon: '📊' },
  { id: 'devops',    name: 'DevOps & Cloud',      icon: '☁️' },
  { id: 'writing',   name: 'Content Writing',     icon: '✍️' },
  { id: 'marketing', name: 'Digital Marketing',   icon: '📢' },
  { id: 'backend',   name: 'Backend Engineering', icon: '⚙️' },
];

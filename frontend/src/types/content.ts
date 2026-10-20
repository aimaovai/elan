export interface MenuItem {
  id: string;
  name: string;
  description: string;
  price: number;
  imageUrl: string | null;
  vegetarian: boolean;
  vegan: boolean;
  glutenFree: boolean;
  featured: boolean;
  available: boolean;
  seasonal: boolean;
  allergens: string[];
}

export interface MenuCategory {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  items: MenuItem[];
}

export interface Menu {
  id: string;
  name: string;
  slug: string;
  description: string;
  menuType: string;
  validFrom: string | null;
  validUntil: string | null;
  categories: MenuCategory[];
}

export interface ExperienceCourse {
  id: string;
  courseNumber: number;
  name: string;
  description: string;
  imageUrl: string | null;
}

export interface DiningExperience {
  id: string;
  name: string;
  slug: string;
  shortDescription: string;
  fullDescription: string;
  heroImageUrl: string;
  price: number;
  winePairingPrice: number | null;
  courseCount: number;
  startDate: string;
  endDate: string;
  status: string;
  featured: boolean;
  courses: ExperienceCourse[];
}

export interface PrivateDiningSpace {
  id: string;
  name: string;
  slug: string;
  description: string;
  capacity: number;
  imageUrl: string;
}

export interface GalleryImage {
  id: string;
  title: string;
  altText: string;
  imageUrl: string;
  category: string;
}

export interface Restaurant {
  restaurantName: string;
  tagline: string;
  description: string;
  city: string;
  contactEmail: string;
  openingHours: string;
  reservationPolicy: string;
  dressCode: string;
}

export type ApiResult<T> =
  | { data: T; error: null }
  | { data: null; error: string };

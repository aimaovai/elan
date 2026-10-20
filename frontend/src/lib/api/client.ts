import type {
  ApiResult,
  DiningExperience,
  GalleryImage,
  Menu,
  MenuItem,
  PrivateDiningSpace,
  Restaurant,
} from "@/types/content";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

export async function apiGet<T>(path: string): Promise<ApiResult<T>> {
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      next: { revalidate: 300 },
      headers: { Accept: "application/json" },
    });
    if (!response.ok) {
      return { data: null, error: `The restaurant content service returned ${response.status}.` };
    }
    return { data: (await response.json()) as T, error: null };
  } catch {
    return { data: null, error: "Restaurant content is temporarily unavailable. Please try again shortly." };
  }
}

export const getMenus = () => apiGet<Menu[]>("/menus");
export const getMenu = (slug: string) => apiGet<Menu>(`/menus/${encodeURIComponent(slug)}`);
export const getFeaturedItems = () => apiGet<MenuItem[]>("/menus/featured-items");
export const getExperiences = () => apiGet<DiningExperience[]>("/experiences");
export const getExperience = (slug: string) =>
  apiGet<DiningExperience>(`/experiences/${encodeURIComponent(slug)}`);
export const getFeaturedExperience = () => apiGet<DiningExperience>("/experiences/featured");
export const getPrivateDiningSpaces = () => apiGet<PrivateDiningSpace[]>("/private-dining/spaces");
export const getGallery = (category?: string) =>
  apiGet<GalleryImage[]>(`/gallery${category ? `?category=${encodeURIComponent(category)}` : ""}`);
export const getRestaurant = () => apiGet<Restaurant>("/restaurant");

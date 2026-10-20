CREATE TABLE menus (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    menu_type VARCHAR(30) NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from DATE,
    valid_until DATE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_menus_published_order ON menus (is_published, display_order);

CREATE TABLE menu_categories (
    id UUID PRIMARY KEY,
    menu_id UUID NOT NULL REFERENCES menus(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    UNIQUE (menu_id, slug)
);
CREATE INDEX idx_categories_menu_order ON menu_categories (menu_id, display_order);

CREATE TABLE menu_items (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES menu_categories(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    price NUMERIC(8,2) NOT NULL CHECK (price >= 0),
    image_url TEXT,
    is_vegetarian BOOLEAN NOT NULL DEFAULT FALSE,
    is_vegan BOOLEAN NOT NULL DEFAULT FALSE,
    is_gluten_free BOOLEAN NOT NULL DEFAULT FALSE,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    is_seasonal BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_menu_items_category_order ON menu_items (category_id, display_order);
CREATE INDEX idx_menu_items_featured ON menu_items (is_featured, is_available);

CREATE TABLE allergens (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE menu_item_allergens (
    menu_item_id UUID NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    allergen_id UUID NOT NULL REFERENCES allergens(id) ON DELETE CASCADE,
    PRIMARY KEY (menu_item_id, allergen_id)
);

CREATE TABLE dining_experiences (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    short_description TEXT NOT NULL,
    full_description TEXT NOT NULL,
    hero_image_url TEXT NOT NULL,
    price NUMERIC(8,2) NOT NULL CHECK (price >= 0),
    wine_pairing_price NUMERIC(8,2) CHECK (wine_pairing_price >= 0),
    course_count INTEGER NOT NULL CHECK (course_count > 0),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL CHECK (end_date >= start_date),
    status VARCHAR(20) NOT NULL,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_experiences_status_order ON dining_experiences (status, display_order);
CREATE TABLE experience_courses (
    id UUID PRIMARY KEY,
    experience_id UUID NOT NULL REFERENCES dining_experiences(id) ON DELETE CASCADE,
    course_number INTEGER NOT NULL CHECK (course_number > 0),
    name VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    image_url TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    UNIQUE (experience_id, course_number)
);

CREATE TABLE private_dining_spaces (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    image_url TEXT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE gallery_images (
    id UUID PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    alt_text TEXT NOT NULL,
    image_url TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_gallery_category_order ON gallery_images (is_published, category, display_order);

CREATE TABLE restaurant_settings (
    id UUID PRIMARY KEY,
    restaurant_name VARCHAR(80) NOT NULL,
    tagline VARCHAR(240) NOT NULL,
    description TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    contact_email VARCHAR(160) NOT NULL,
    opening_hours TEXT NOT NULL,
    reservation_policy TEXT NOT NULL,
    dress_code TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

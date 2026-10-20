ALTER TABLE restaurant_settings
    ADD COLUMN restaurant_timezone VARCHAR(80) NOT NULL DEFAULT 'America/Los_Angeles',
    ADD COLUMN maximum_advance_booking_days INTEGER NOT NULL DEFAULT 90 CHECK (maximum_advance_booking_days BETWEEN 1 AND 365),
    ADD COLUMN minimum_advance_booking_hours INTEGER NOT NULL DEFAULT 2 CHECK (minimum_advance_booking_hours >= 0),
    ADD COLUMN minimum_party_size INTEGER NOT NULL DEFAULT 1 CHECK (minimum_party_size >= 1),
    ADD COLUMN maximum_online_party_size INTEGER NOT NULL DEFAULT 8 CHECK (maximum_online_party_size >= minimum_party_size),
    ADD COLUMN default_reservation_duration_minutes INTEGER NOT NULL DEFAULT 120 CHECK (default_reservation_duration_minutes > 0),
    ADD COLUMN reservation_interval_minutes INTEGER NOT NULL DEFAULT 30 CHECK (reservation_interval_minutes > 0),
    ADD COLUMN cancellation_cutoff_hours INTEGER NOT NULL DEFAULT 24 CHECK (cancellation_cutoff_hours >= 0),
    ADD COLUMN booking_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN private_dining_enabled BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE restaurant_settings
SET reservation_policy = 'Online reservations are available through this fictional demonstration system. Changes or cancellations must be made at least 24 hours before arrival. No real restaurant table is reserved.';

ALTER TABLE dining_experiences
    ADD COLUMN reservation_duration_minutes INTEGER NOT NULL DEFAULT 120 CHECK (reservation_duration_minutes > 0);

CREATE TABLE dining_tables (
    id UUID PRIMARY KEY,
    table_number VARCHAR(30) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    minimum_capacity INTEGER NOT NULL CHECK (minimum_capacity >= 1),
    maximum_capacity INTEGER NOT NULL CHECK (maximum_capacity >= minimum_capacity),
    location VARCHAR(40) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE restaurant_service_periods (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL CHECK (end_time > start_time),
    slot_interval_minutes INTEGER NOT NULL CHECK (slot_interval_minutes > 0),
    dining_duration_minutes INTEGER NOT NULL CHECK (dining_duration_minutes > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_service_periods_day_active ON restaurant_service_periods (day_of_week, is_active);

CREATE TABLE restaurant_blocked_dates (
    id UUID PRIMARY KEY,
    blocked_date DATE NOT NULL,
    reason VARCHAR(160) NOT NULL,
    is_full_day BOOLEAN NOT NULL DEFAULT TRUE,
    start_time TIME,
    end_time TIME,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_blocked_date_interval CHECK (
        (is_full_day AND start_time IS NULL AND end_time IS NULL)
        OR (NOT is_full_day AND start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time)
    )
);
CREATE INDEX idx_blocked_dates_date ON restaurant_blocked_dates (blocked_date);

CREATE TABLE reservation_guests (
    id UUID PRIMARY KEY,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_reservation_guests_email ON reservation_guests (lower(email));

CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    confirmation_code VARCHAR(16) NOT NULL UNIQUE,
    guest_id UUID NOT NULL REFERENCES reservation_guests(id),
    dining_table_id UUID NOT NULL REFERENCES dining_tables(id),
    experience_id UUID REFERENCES dining_experiences(id),
    party_size INTEGER NOT NULL CHECK (party_size >= 1),
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL CHECK (end_time > start_time),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','CONFIRMED','SEATED','COMPLETED','CANCELLED','NO_SHOW')),
    special_occasion VARCHAR(30),
    dietary_notes VARCHAR(1000),
    special_requests VARCHAR(1000),
    internal_notes VARCHAR(2000),
    management_token_hash VARCHAR(64) NOT NULL UNIQUE,
    management_token_expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_reservation_occasion CHECK (
        special_occasion IS NULL OR special_occasion IN ('BIRTHDAY','ANNIVERSARY','PROPOSAL','OTHER')
    )
);
CREATE INDEX idx_reservations_table_interval ON reservations (dining_table_id, reservation_date, start_time, end_time);
CREATE INDEX idx_reservations_date_status ON reservations (reservation_date, status);
CREATE INDEX idx_reservations_guest ON reservations (guest_id, reservation_date DESC);

CREATE TABLE reservation_idempotency_records (
    id UUID PRIMARY KEY,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    request_hash VARCHAR(64) NOT NULL,
    reservation_id UUID NOT NULL REFERENCES reservations(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE TABLE dining_experience_table_restrictions (
    experience_id UUID NOT NULL REFERENCES dining_experiences(id),
    dining_table_id UUID NOT NULL REFERENCES dining_tables(id),
    PRIMARY KEY (experience_id, dining_table_id)
);

CREATE TABLE reservation_status_history (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES reservations(id),
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    change_source VARCHAR(20) NOT NULL CHECK (change_source IN ('CUSTOMER','ADMINISTRATOR','SYSTEM')),
    reason VARCHAR(500),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_reservation_history_reservation ON reservation_status_history (reservation_id, changed_at);

CREATE TABLE private_dining_inquiries (
    id UUID PRIMARY KEY,
    reference_number VARCHAR(16) NOT NULL UNIQUE,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(32) NOT NULL,
    event_type VARCHAR(30) NOT NULL CHECK (event_type IN ('BIRTHDAY','ANNIVERSARY','CORPORATE','WEDDING_RELATED','CELEBRATION','OTHER')),
    preferred_date DATE NOT NULL,
    alternate_date DATE,
    guest_count INTEGER NOT NULL CHECK (guest_count >= 1),
    private_dining_space_id UUID REFERENCES private_dining_spaces(id),
    budget_per_guest NUMERIC(8,2) CHECK (budget_per_guest >= 0),
    dietary_requirements VARCHAR(1000),
    beverage_preferences VARCHAR(1000),
    event_description VARCHAR(2000) NOT NULL,
    special_requests VARCHAR(1000),
    preferred_contact_method VARCHAR(20) NOT NULL CHECK (preferred_contact_method IN ('EMAIL','PHONE')),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW','CONTACTED','PROPOSAL_SENT','BOOKED','DECLINED','CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_private_inquiries_status_created ON private_dining_inquiries (status, created_at DESC);

INSERT INTO dining_tables (id, table_number, display_name, minimum_capacity, maximum_capacity, location)
VALUES
    ('90000000-0000-0000-0000-000000000001', '01', 'Table 01', 1, 2, 'MAIN_DINING'),
    ('90000000-0000-0000-0000-000000000002', '02', 'Table 02', 1, 2, 'WINDOW'),
    ('90000000-0000-0000-0000-000000000003', '03', 'Table 03', 1, 2, 'MAIN_DINING'),
    ('90000000-0000-0000-0000-000000000004', '04', 'Table 04', 1, 4, 'MAIN_DINING'),
    ('90000000-0000-0000-0000-000000000005', '05', 'Table 05', 1, 4, 'WINDOW'),
    ('90000000-0000-0000-0000-000000000006', '06', 'Table 06', 1, 4, 'MAIN_DINING'),
    ('90000000-0000-0000-0000-000000000007', '07', 'Table 07', 1, 6, 'MAIN_DINING'),
    ('90000000-0000-0000-0000-000000000008', '08', 'Table 08', 1, 6, 'CHEFS_COUNTER');

INSERT INTO restaurant_service_periods
    (id, name, day_of_week, start_time, end_time, slot_interval_minutes, dining_duration_minutes)
VALUES
    ('91000000-0000-0000-0000-000000000002', 'Dinner', 2, '17:30', '22:00', 30, 120),
    ('91000000-0000-0000-0000-000000000003', 'Dinner', 3, '17:30', '22:00', 30, 120),
    ('91000000-0000-0000-0000-000000000004', 'Dinner', 4, '17:30', '22:00', 30, 120),
    ('91000000-0000-0000-0000-000000000005', 'Dinner', 5, '17:30', '22:00', 30, 120),
    ('91000000-0000-0000-0000-000000000006', 'Dinner', 6, '17:30', '22:00', 30, 120);

INSERT INTO dining_experience_table_restrictions (experience_id, dining_table_id)
VALUES ('50000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000008');

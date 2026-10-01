CREATE TABLE sites (
 id BIGSERIAL PRIMARY KEY, name TEXT NOT NULL, address TEXT NOT NULL,
 whatsapp VARCHAR(20) NOT NULL, image TEXT NOT NULL
);
CREATE TABLE physical_fields (
 id BIGSERIAL PRIMARY KEY, site_id BIGINT NOT NULL REFERENCES sites, name TEXT NOT NULL
);
CREATE TABLE sectors (
 id BIGSERIAL PRIMARY KEY, field_id BIGINT NOT NULL REFERENCES physical_fields, name TEXT NOT NULL,
 UNIQUE(field_id, name)
);
CREATE TABLE spaces (
 id BIGSERIAL PRIMARY KEY, field_id BIGINT NOT NULL REFERENCES physical_fields,
 name TEXT NOT NULL, modality INTEGER NOT NULL CHECK(modality IN (5,7,9,11)), active BOOLEAN NOT NULL DEFAULT true
);
CREATE TABLE space_sectors (
 space_id BIGINT NOT NULL REFERENCES spaces, sector_id BIGINT NOT NULL REFERENCES sectors,
 PRIMARY KEY(space_id,sector_id)
);
CREATE TABLE operating_hours (
 id BIGSERIAL PRIMARY KEY, site_id BIGINT NOT NULL REFERENCES sites,
 day_of_week INTEGER NOT NULL CHECK(day_of_week BETWEEN 1 AND 7),
 open_hour INTEGER NOT NULL, close_hour INTEGER NOT NULL,
 CHECK(open_hour >= 0 AND close_hour <= 24 AND open_hour < close_hour), UNIQUE(site_id,day_of_week)
);
CREATE TABLE rates (
 id BIGSERIAL PRIMARY KEY, space_id BIGINT NOT NULL REFERENCES spaces,
 day_of_week INTEGER NOT NULL CHECK(day_of_week BETWEEN 1 AND 7),
 start_hour INTEGER NOT NULL, end_hour INTEGER NOT NULL, amount NUMERIC(10,2) NOT NULL CHECK(amount >= 0),
 CHECK(start_hour >= 0 AND end_hour <= 24 AND start_hour < end_hour)
);
CREATE TABLE special_rates (
 id BIGSERIAL PRIMARY KEY, space_id BIGINT NOT NULL REFERENCES spaces,
 date DATE NOT NULL, hour INTEGER NOT NULL CHECK(hour BETWEEN 0 AND 23),
 amount NUMERIC(10,2) NOT NULL CHECK(amount >= 0), UNIQUE(space_id,date,hour)
);
CREATE TABLE customers (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(120) NOT NULL, phone VARCHAR(20) NOT NULL UNIQUE, email VARCHAR(180)
);
CREATE TABLE app_users (
 id BIGSERIAL PRIMARY KEY, username VARCHAR(60) NOT NULL UNIQUE, name TEXT NOT NULL,
 password_hash TEXT NOT NULL, role VARCHAR(20) NOT NULL CHECK(role IN ('ADMIN','OPERATOR')), active BOOLEAN NOT NULL DEFAULT true
);
CREATE TABLE user_sites (
 user_id BIGINT NOT NULL REFERENCES app_users, site_id BIGINT NOT NULL REFERENCES sites,
 PRIMARY KEY(user_id,site_id)
);
CREATE TABLE requests (
 id BIGSERIAL PRIMARY KEY, code VARCHAR(30) NOT NULL UNIQUE, customer_id BIGINT NOT NULL REFERENCES customers,
 space_id BIGINT NOT NULL REFERENCES spaces, date DATE NOT NULL,
 start_hour INTEGER NOT NULL, end_hour INTEGER NOT NULL,
 quoted_price NUMERIC(10,2) NOT NULL, agreed_price NUMERIC(10,2) NOT NULL CHECK(agreed_price >= 0),
 status VARCHAR(30) NOT NULL DEFAULT 'NEW' CHECK(status IN ('NEW','CONTACTED','AWAITING_PAYMENT','RESERVED','UNAVAILABLE','WITHDRAWN')),
 notes VARCHAR(2000) NOT NULL DEFAULT '', source VARCHAR(20) NOT NULL DEFAULT 'WEB',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK(start_hour >= 0 AND end_hour <= 24 AND start_hour < end_hour)
);
CREATE TABLE request_history (
 id BIGSERIAL PRIMARY KEY, request_id BIGINT NOT NULL REFERENCES requests,
 actor TEXT NOT NULL, action TEXT NOT NULL, details TEXT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE reservations (
 id BIGSERIAL PRIMARY KEY, request_id BIGINT NOT NULL UNIQUE REFERENCES requests,
 space_id BIGINT NOT NULL REFERENCES spaces, date DATE NOT NULL, start_hour INTEGER NOT NULL, end_hour INTEGER NOT NULL,
 total NUMERIC(10,2) NOT NULL, confirmed_by BIGINT NOT NULL REFERENCES app_users,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE payments (
 id BIGSERIAL PRIMARY KEY, reservation_id BIGINT NOT NULL UNIQUE REFERENCES reservations,
 amount NUMERIC(10,2) NOT NULL CHECK(amount >= 0), method VARCHAR(30) NOT NULL,
 reference VARCHAR(120), verified_by BIGINT NOT NULL REFERENCES app_users, verified_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX reservation_schedule_idx ON reservations(date,space_id,start_hour,end_hour);
CREATE INDEX request_schedule_idx ON requests(date,space_id,status);
CREATE INDEX history_request_idx ON request_history(request_id,created_at);

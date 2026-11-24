-- Поля и таблицы, которых не было в первой контрольной: UUID для API,
-- архив, приоритет, теги, заметки, пользователи, сессии и аудит.

ALTER TABLE passengers ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE passengers ADD CONSTRAINT uq_passengers_public_id UNIQUE (public_id);
ALTER TABLE passengers ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE bookings ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE bookings ADD CONSTRAINT uq_bookings_public_id UNIQUE (public_id);
ALTER TABLE bookings ADD COLUMN booking_code VARCHAR(20);
UPDATE bookings SET booking_code = 'BK-' || LPAD(id::text, 6, '0');
ALTER TABLE bookings ALTER COLUMN booking_code SET NOT NULL;
ALTER TABLE bookings ADD CONSTRAINT uq_bookings_code UNIQUE (booking_code);
ALTER TABLE bookings ADD COLUMN refundable BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bookings ADD COLUMN priority VARCHAR(10) NOT NULL DEFAULT 'LOW';
ALTER TABLE bookings ADD CONSTRAINT chk_bookings_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));
ALTER TABLE bookings ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bookings ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE bookings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE bookings SET priority = CASE
    WHEN status IN ('COMPLETED', 'CANCELLED') THEN 'LOW'
    WHEN price >= 10000 OR departure_date <= NOW() + INTERVAL '48 hours' THEN 'HIGH'
    WHEN price >= 7000 THEN 'MEDIUM'
    ELSE 'LOW'
END;

UPDATE bookings SET refundable = TRUE WHERE flight_number IN ('SU-1234', 'S7-4502');
UPDATE bookings SET archived = TRUE WHERE flight_number = 'N4-112';

CREATE INDEX idx_bookings_public_id ON bookings (public_id);
CREATE INDEX idx_bookings_code ON bookings (booking_code);
CREATE INDEX idx_bookings_priority ON bookings (priority);
CREATE INDEX idx_bookings_archived ON bookings (archived);
CREATE INDEX idx_passengers_public_id ON passengers (public_id);
CREATE INDEX idx_passengers_email ON passengers (email);

CREATE TABLE roles (
    name VARCHAR(32) PRIMARY KEY
);

INSERT INTO roles (name) VALUES ('VIEWER'), ('EDITOR'), ('ADMIN');

CREATE TABLE user_accounts (
    id                   UUID         PRIMARY KEY,
    email                VARCHAR(150) NOT NULL UNIQUE,
    full_name            VARCHAR(150) NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,
    active               BOOLEAN      NOT NULL,
    must_change_password BOOLEAN      NOT NULL,
    failed_attempts      INT          NOT NULL DEFAULT 0,
    locked_until         TIMESTAMP,
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP    NOT NULL
);

CREATE TABLE user_roles (
    user_id   UUID        NOT NULL REFERENCES user_accounts (id) ON DELETE CASCADE,
    role_name VARCHAR(32) NOT NULL REFERENCES roles (name),
    PRIMARY KEY (user_id, role_name)
);

CREATE TABLE refresh_sessions (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES user_accounts (id) ON DELETE CASCADE,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_refresh_user ON refresh_sessions (user_id);

CREATE TABLE tags (
    id   BIGSERIAL   PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE booking_tags (
    booking_id BIGINT NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    tag_id     BIGINT NOT NULL REFERENCES tags (id),
    PRIMARY KEY (booking_id, tag_id)
);

INSERT INTO tags (name) VALUES ('vip'), ('срочный');

INSERT INTO booking_tags (booking_id, tag_id)
SELECT b.id, t.id
FROM bookings b
JOIN tags t ON t.name = 'vip'
WHERE b.price >= 10000;

INSERT INTO booking_tags (booking_id, tag_id)
SELECT b.id, t.id
FROM bookings b
JOIN tags t ON t.name = 'срочный'
WHERE b.flight_number = 'SU-1234';

CREATE TABLE booking_notes (
    id         UUID          PRIMARY KEY,
    booking_id BIGINT        NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    author_id  UUID          REFERENCES user_accounts (id),
    text       VARCHAR(1000) NOT NULL,
    pinned     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP     NOT NULL
);

CREATE INDEX idx_notes_booking ON booking_notes (booking_id);

CREATE TABLE audit_events (
    id             UUID         PRIMARY KEY,
    user_id        UUID         REFERENCES user_accounts (id),
    event_type     VARCHAR(40)  NOT NULL,
    entity_type    VARCHAR(40),
    entity_id      UUID,
    description    VARCHAR(300) NOT NULL,
    correlation_id VARCHAR(40),
    created_at     TIMESTAMP    NOT NULL
);

CREATE INDEX idx_audit_created ON audit_events (created_at);
CREATE INDEX idx_audit_event_type ON audit_events (event_type);

-- Схема и данные контрольной работы №1.
-- DROP здесь нет: если таблицы уже существуют, миграция остановится и ничего не сотрет.

CREATE TABLE passengers (
    id              BIGSERIAL       PRIMARY KEY,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    NOT NULL UNIQUE,
    phone           VARCHAR(20)     NOT NULL,
    passport_number VARCHAR(20)     NOT NULL UNIQUE
);

CREATE TABLE bookings (
    id              BIGSERIAL       PRIMARY KEY,
    passenger_id    BIGINT          NOT NULL,
    flight_number   VARCHAR(10)     NOT NULL,
    departure_city  VARCHAR(100)    NOT NULL,
    arrival_city    VARCHAR(100)    NOT NULL,
    departure_date  TIMESTAMP       NOT NULL,
    price           DECIMAL(10, 2)  NOT NULL CHECK (price > 0),
    status          VARCHAR(20)     NOT NULL CHECK (status IN ('CREATED', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_booking_passenger
        FOREIGN KEY (passenger_id) REFERENCES passengers (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_bookings_passenger_id   ON bookings (passenger_id);
CREATE INDEX idx_bookings_status         ON bookings (status);
CREATE INDEX idx_bookings_flight_number  ON bookings (flight_number);
CREATE INDEX idx_bookings_departure_date ON bookings (departure_date);

INSERT INTO passengers (first_name, last_name, email, phone, passport_number) VALUES
    ('Иван',    'Петров',   'ivan.petrov@mail.ru', '+7-900-111-2233', '4510 123456'),
    ('Мария',   'Сидорова', 'maria.sid@gmail.com', '+7-900-222-3344', '4511 234567'),
    ('Алексей', 'Козлов',   'kozlov@yandex.ru',    '+7-900-333-4455', '4512 345678'),
    ('Елена',   'Новикова', 'novikova@mail.ru',    '+7-900-444-5566', '4513 456789'),
    ('Дмитрий', 'Волков',   'volkov.d@gmail.com',  '+7-900-555-6677', '4514 567890');

INSERT INTO bookings (passenger_id, flight_number, departure_city, arrival_city, departure_date, price, status) VALUES
    (1, 'SU-1234', 'Москва',          'Санкт-Петербург', '2026-10-15 08:30:00', 5500.00,  'CREATED'),
    (2, 'S7-4501', 'Новосибирск',     'Москва',          '2026-10-18 12:00:00', 12300.00, 'CONFIRMED'),
    (3, 'DP-820',  'Санкт-Петербург', 'Сочи',            '2026-09-25 06:45:00', 8900.00,  'COMPLETED'),
    (1, 'SU-5678', 'Москва',          'Казань',          '2026-11-01 14:20:00', 6700.00,  'CREATED'),
    (4, 'UT-302',  'Екатеринбург',    'Москва',          '2026-10-05 09:00:00', 9500.00,  'CANCELLED'),
    (2, 'S7-4502', 'Москва',          'Новосибирск',     '2026-10-22 16:30:00', 11800.00, 'CONFIRMED'),
    (5, 'SU-9012', 'Москва',          'Сочи',            '2026-09-20 07:15:00', 7200.00,  'COMPLETED'),
    (3, 'DP-821',  'Сочи',            'Санкт-Петербург', '2026-10-30 18:45:00', 9100.00,  'CREATED'),
    (4, 'UT-303',  'Москва',          'Екатеринбург',    '2026-11-10 11:00:00', 9800.00,  'CONFIRMED'),
    (5, 'N4-112',  'Краснодар',       'Москва',          '2026-10-08 13:30:00', 6200.00,  'CANCELLED');

# База данных проекта Airline Booking

## 1. Состояние окружения

- PostgreSQL: 18.0, служба `postgresql-x64-18`, порт `5432`, состояние: запущена.
- База данных: `airline_booking`.
- Java: JDK 17 найден в `C:\Program Files (x86)\Android\openjdk\jdk-17.0.8.101-hotspot`.
- Maven: отдельная команда `mvn` не установлена. В проекте Maven-конфигурация присутствует; IntelliJ IDEA содержит Maven-интеграцию.

## 2. Данные подключения

```text
DBMS       PostgreSQL
Host       localhost
Port       5432
Database   airline_booking
User       postgres
Password   postgres
JDBC URL   jdbc:postgresql://localhost:5432/airline_booking
Encoding   UTF-8
```

Пароль хранится в исходном `DatabaseManager.java` для учебного проекта. Для реального приложения пароль нужно вынести в переменные окружения или файл конфигурации, не добавляемый в Git.

## 3. Создание базы

Выполнять в базе `postgres`:

```sql
CREATE DATABASE airline_booking;
```

## 4. Полный скрипт инициализации (`init.sql`)

```sql
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS passengers;

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

CREATE INDEX idx_bookings_passenger_id    ON bookings (passenger_id);
CREATE INDEX idx_bookings_status          ON bookings (status);
CREATE INDEX idx_bookings_flight_number   ON bookings (flight_number);
CREATE INDEX idx_bookings_departure_date  ON bookings (departure_date);

INSERT INTO passengers (first_name, last_name, email, phone, passport_number) VALUES
    ('Иван', 'Петров', 'ivan.petrov@mail.ru', '+7-900-111-2233', '4510 123456'),
    ('Мария', 'Сидорова', 'maria.sid@gmail.com', '+7-900-222-3344', '4511 234567'),
    ('Алексей', 'Козлов', 'kozlov@yandex.ru', '+7-900-333-4455', '4512 345678'),
    ('Елена', 'Новикова', 'novikova@mail.ru', '+7-900-444-5566', '4513 456789'),
    ('Дмитрий', 'Волков', 'volkov.d@gmail.com', '+7-900-555-6677', '4514 567890');

INSERT INTO bookings (passenger_id, flight_number, departure_city, arrival_city, departure_date, price, status) VALUES
    (1, 'SU-1234', 'Москва', 'Санкт-Петербург', '2026-10-15 08:30:00', 5500.00, 'CREATED'),
    (2, 'S7-4501', 'Новосибирск', 'Москва', '2026-10-18 12:00:00', 12300.00, 'CONFIRMED'),
    (3, 'DP-820', 'Санкт-Петербург', 'Сочи', '2026-09-25 06:45:00', 8900.00, 'COMPLETED'),
    (1, 'SU-5678', 'Москва', 'Казань', '2026-11-01 14:20:00', 6700.00, 'CREATED'),
    (4, 'UT-302', 'Екатеринбург', 'Москва', '2026-10-05 09:00:00', 9500.00, 'CANCELLED'),
    (2, 'S7-4502', 'Москва', 'Новосибирск', '2026-10-22 16:30:00', 11800.00, 'CONFIRMED'),
    (5, 'SU-9012', 'Москва', 'Сочи', '2026-09-20 07:15:00', 7200.00, 'COMPLETED'),
    (3, 'DP-821', 'Сочи', 'Санкт-Петербург', '2026-10-30 18:45:00', 9100.00, 'CREATED'),
    (4, 'UT-303', 'Москва', 'Екатеринбург', '2026-11-10 11:00:00', 9800.00, 'CONFIRMED'),
    (5, 'N4-112', 'Краснодар', 'Москва', '2026-10-08 13:30:00', 6200.00, 'CANCELLED');
```

## 5. Запуск скриптов и проверки

```powershell
$env:PGPASSWORD = 'postgres'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
& $psql -h localhost -U postgres -d airline_booking -f init.sql
& $psql -h localhost -U postgres -d airline_booking -c "SELECT * FROM passengers;"
& $psql -h localhost -U postgres -d airline_booking -c "SELECT * FROM bookings;"
```

Ожидаемый результат: `passengers` — 5 строк, `bookings` — 10 строк.

## 6. Построчный разбор SQL

```text
1  Комментарий-раздел.
2  Комментарий с названием скрипта.
3  Комментарий с названием СУБД.
4  Комментарий-раздел.
5  Пустая строка.
6  Комментарий о порядке удаления таблиц.
7  DROP TABLE bookings; удаление таблицы бронирований при наличии.
8  DROP TABLE passengers; удаление таблицы пассажиров при наличии.
9  Пустая строка.
10 Комментарий-раздел.
11 Комментарий: таблица пассажиров.
12 Комментарий-раздел.
13 CREATE TABLE passengers; начало создания таблицы.
14 id BIGSERIAL; автоинкрементный идентификатор, PRIMARY KEY; первичный ключ.
15 first_name VARCHAR(100); имя до 100 символов, NOT NULL; обязательное поле.
16 last_name VARCHAR(100); фамилия до 100 символов, обязательное поле.
17 email VARCHAR(150); электронная почта, NOT NULL и UNIQUE; обязательное уникальное значение.
18 phone VARCHAR(20); телефон до 20 символов, обязательное поле.
19 passport_number VARCHAR(20); паспорт до 20 символов, обязательное уникальное значение.
20 ); завершение определения таблицы.
21 Пустая строка.
22 Комментарий-раздел.
23 Комментарий: таблица бронирований.
24 Комментарий-раздел.
25 CREATE TABLE bookings; начало создания таблицы.
26 id BIGSERIAL; автоинкрементный идентификатор и первичный ключ.
27 passenger_id BIGINT; идентификатор пассажира, обязательный.
28 flight_number VARCHAR(10); номер рейса до 10 символов.
29 departure_city VARCHAR(100); город отправления.
30 arrival_city VARCHAR(100); город прибытия.
31 departure_date TIMESTAMP; дата и время отправления.
32 price DECIMAL(10,2); цена, максимум 10 цифр и 2 после запятой, больше нуля.
33 status VARCHAR(20); статус из четырёх разрешённых значений.
34 created_at TIMESTAMP; время создания, по умолчанию NOW().
35 Пустая строка.
36 CONSTRAINT fk_booking_passenger; имя ограничения внешнего ключа.
37 FOREIGN KEY; связь bookings.passenger_id с passengers.id.
38 ON DELETE CASCADE; при удалении пассажира база сама удаляет его бронирования.
39 ); завершение определения таблицы.
40 Пустая строка.
41 Комментарий об индексах.
42 Индекс поиска по passenger_id.
43 Индекс фильтрации по status.
44 Индекс поиска по flight_number.
45 Индекс сортировки и фильтрации по departure_date.
46 Пустая строка.
47 Комментарий-раздел.
48 Комментарий о тестовых данных пассажиров.
49 Комментарий-раздел.
50 INSERT INTO passengers; начало вставки пассажиров.
51 Данные пассажира 1.
52 Данные пассажира 2.
53 Данные пассажира 3.
54 Данные пассажира 4.
55 Данные пассажира 5 и завершение INSERT.
56 Пустая строка.
57 Комментарий-раздел.
58 Комментарий о тестовых бронированиях.
59 Комментарий с распределением статусов.
60 Комментарий-раздел.
61 INSERT INTO bookings; начало вставки бронирований.
62 Бронирование 1.
63 Бронирование 2.
64 Бронирование 3.
65 Бронирование 4.
66 Бронирование 5.
67 Бронирование 6.
68 Бронирование 7.
69 Бронирование 8.
70 Бронирование 9.
71 Бронирование 10 и завершение INSERT.
```

## 7. Что улучшить дальше

1. Исправить кодировку `walkthrough.md` и комментариев `init.sql` на UTF-8.
2. Добавить `README.md` с быстрым запуском и `.env.example` без настоящего пароля.
3. Вынести пароль из `DatabaseManager` в переменные окружения.
4. Заменить `BIGSERIAL` на `GENERATED BY DEFAULT AS IDENTITY`.
5. Использовать `TIMESTAMP WITH TIME ZONE` для дат рейсов.
6. Вынести статусы в PostgreSQL `ENUM` или отдельную таблицу-справочник.
7. Добавить тесты репозиториев и сервисов, миграции Flyway и Docker Compose.
8. Добавить SQL-файл с учебными запросами `SELECT`, `JOIN`, `GROUP BY`, `UPDATE`, `DELETE`.
9. Разделить учебный консольный проект и HTTP REST API (например, Spring Boot) на этапы.
10. Создать карточки для запоминания: типы данных, ограничения, JOIN, JDBC и слои архитектуры.

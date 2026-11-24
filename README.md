# Бронирование авиабилетов

Учебный проект: пассажиры и бронирования авиабилетов в PostgreSQL.

- КР №1 — консольное меню и база данных. Этот код сохранен.
- КР №2 — настольное окно на JavaFX. Оно работает с базой на порту 5432 через JDBC.
- КР №3 — сервер Spring Boot в каталоге `backend`. Своя база в Docker на порту 56080, REST API и страница http://localhost:8080/. Окно JavaFX к этому серверу не подключается.

Как устроен сервер и что говорить на защите: `BACKEND_GUIDE.md`.

## Запуск сервера КР №3

Нужны Docker Desktop, файл `.env` в корне проекта и свободные порты 8080 и 56080. Порт 55432 из примера преподавателя на этом компьютере занят системным диапазоном Windows, поэтому наружу база смотрит на 56080. Отдельно ставить PostgreSQL и JDK 21 не нужно: их поднимает контейнер. JavaFX по-прежнему собирается локальным JDK 17 и свою базу на порту 5432 не трогает.

```powershell
docker desktop start
.\backend\start-compose.ps1
```

Тот же запуск вручную:

```powershell
docker compose --env-file .env -f backend/docker-compose.yml up -d --build --wait
```

| Что | Адрес |
| --- | --- |
| Страница | http://localhost:8080/ |
| Swagger | http://localhost:8080/swagger-ui.html |
| API | http://localhost:8080/api |
| Health | http://localhost:8080/actuator/health |
| PostgreSQL | `jdbc:postgresql://localhost:56080/airline_booking` |

Первый вход: `admin@airline.local` / `Adm1!Airline26`. Сервер попросит сменить пароль. Пример CSV для импорта: `backend/sample-import.csv`.

Остановка без удаления данных: `docker compose --env-file .env -f backend/docker-compose.yml down`.

Окно JavaFX не ходит в интернет и не использует Spring. Кнопка вызывает контроллер, контроллер вызывает сервис, сервис вызывает репозиторий, репозиторий выполняет SQL.

## Что нужно установить

- Java 17 (на этом компьютере стоит Temurin 17; JavaFX 21 на ней запускается);
- Maven 3.9 или новее;
- PostgreSQL с базой `airline_booking` из КР №1.

Проверка:

```powershell
java -version
mvn -version
```

## База данных

Если база еще не создана, в PostgreSQL:

```sql
CREATE DATABASE airline_booking;
```

Затем из корня проекта:

```powershell
$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
$env:PGPASSWORD = "postgres"
& $psql -h localhost -U postgres -d airline_booking -f init.sql
```

`init.sql` удаляет таблицы `passengers` и `bookings` и создает их заново вместе с тестовыми данными: 5 пассажиров и 10 бронирований. Повторный запуск стирает текущие данные этих таблиц.

Параметры подключения лежат в [src/main/resources/database.properties](src/main/resources/database.properties):

```properties
db.url=jdbc:postgresql://localhost:5432/airline_booking
db.user=postgres
db.password=postgres
```

Если пользователь, пароль или порт другие, меняется этот файл. В Java-классах пароля нет.

## Запуск окна (КР №2)

Из корня проекта:

```powershell
mvn javafx:run
```

То же самое делает [run.ps1](run.ps1):

```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

В IntelliJ IDEA можно запустить класс `com.airline.fx.AirlineFxApp`.

## Запуск консоли (КР №1)

```powershell
mvn clean package
java -jar target/airline-booking-1.0-SNAPSHOT.jar
```

Консольная точка входа — `com.airline.Main`. Окно и консоль используют одни и те же сервисы и одну базу.

## Проверка

```powershell
mvn test
```

Тесты проверяют поиск, фильтры, сортировку, запись CSV и реальный цикл «добавить бронирование — изменить — удалить» в PostgreSQL. Последний тест сам удаляет созданные им записи.

## Структура

```text
src/main/java/com/airline/
├── fx/                 окно JavaFX: запуск, диалоги, контроллеры
├── ui/                 консольное меню КР №1
├── service/            правила и проверки
├── repository/         SQL через JDBC
├── model/              Passenger, Booking, BookingStatus
├── exception/          понятные ошибки
└── util/               подключение к базе и экспорт
src/main/resources/
├── fxml/               разметка окон
├── css/app.css         оформление
└── database.properties параметры PostgreSQL
```

Подробный разбор решения — в [TECH_GUIDE.md](TECH_GUIDE.md). Сценарий демонстрации — в [walkthrough.md](walkthrough.md). Описание таблиц — в [DATABASE_GUIDE.md](DATABASE_GUIDE.md).

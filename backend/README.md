# Olimp Avto Backend

Backend на Spring Boot для статического фронта Olimp Avto.

## Что реализовано

- API-контракт в формате OpenAPI: `src/main/resources/openapi.yaml`
- Генерация API-интерфейсов, контроллеров и DTO через OpenAPI Generator
- Валидация форм на фронте и на backend
- Отправка формы консультации на почту: `POST /api/leads`
- Отправка формы отзыва на почту с фотографиями: `POST /api/reviews`
- Модерация отзывов: администратор принимает или отклоняет отзыв по ссылке из письма
- Таблица каталога в PostgreSQL для ручного заполнения
- API каталога: `GET /api/cars`, `GET /api/cars/{id}`, `POST /api/cars`
- Basic Auth для административных эндпоинтов и Swagger
- Docker Compose для backend, PostgreSQL и Mailpit

## Быстрый запуск

Запустить frontend, backend, PostgreSQL и локальный тестовый SMTP:

```powershell
cd C:\Users\tooor\Downloads\avto_project\backend
docker compose up -d --build
```

После запуска:

```text
Сайт:       http://localhost
Backend:    http://localhost:8080
Swagger UI: http://localhost/swagger-ui.html
Mailpit:    http://localhost:8025
```

Frontend лежит в папке:

```text
C:\Users\tooor\Downloads\avto_project\frontend
```

В Docker frontend раздаётся через nginx. Nginx также проксирует `/api`, `/swagger-ui` и `/v3/api-docs` на backend.

Если нужно запустить frontend без Docker:

```powershell
cd C:\Users\tooor\Downloads\avto_project\frontend
python -m http.server 8000
```

При локальном запуске без Docker нужно указать адрес backend в `frontend/config.js`:

```js
window.OLIMP_AVTO_CONFIG = {
    apiBaseUrl: 'http://localhost:8080'
};
```

При запуске через Docker значение должно быть пустым, потому что nginx проксирует `/api` на backend:

```js
window.OLIMP_AVTO_CONFIG = {
    apiBaseUrl: ''
};
```

## Авторизация

Basic Auth используется только для административных частей.

Защищены:

```text
POST /api/cars
/swagger-ui.html
/swagger-ui/**
/v3/api-docs/**
```

Публичные эндпоинты сайта:

```text
POST /api/leads
POST /api/reviews
GET /api/cars
GET /api/cars/{id}
```

Доступ по умолчанию:

```text
Логин: admin
Пароль: admin123
```

Для production обязательно переопределить:

```powershell
$env:APP_ADMIN_USERNAME="admin"
$env:APP_ADMIN_PASSWORD="strong-password"
```

В Docker Compose эти значения задаются в `docker-compose.yml` у сервиса `backend`.

## Конфигурация приложения

Основной файл конфигурации:

```text
src/main/resources/application.yml
```

Важные переменные окружения:

| Переменная | Для чего нужна | Значение по умолчанию |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL PostgreSQL | `jdbc:postgresql://localhost:5432/olimp_avto` |
| `SPRING_DATASOURCE_USERNAME` | пользователь PostgreSQL | `olimp_avto` |
| `SPRING_DATASOURCE_PASSWORD` | пароль PostgreSQL | `olimp_avto` |
| `MAIL_HOST` | SMTP-сервер отправителя | `localhost` |
| `MAIL_PORT` | SMTP-порт | `1025` |
| `MAIL_USERNAME` | логин SMTP | пусто |
| `MAIL_PASSWORD` | пароль SMTP или пароль приложения | пусто |
| `MAIL_SMTP_AUTH` | включить SMTP-авторизацию | `false` |
| `MAIL_SMTP_STARTTLS` | включить STARTTLS | `false` |
| `MAIL_SMTP_SSL` | включить SSL для SMTP, обычно порт `465` | `false` |
| `APP_MAIL_FROM` | адрес отправителя | `noreply@olimp-avto.local` |
| `APP_MAIL_TO` | адрес получателя заявок | `olimpautovl125@gmail.com` |
| `APP_ADMIN_USERNAME` | логин администратора | `admin` |
| `APP_ADMIN_PASSWORD` | пароль администратора | `admin123` |
| `APP_PUBLIC_BASE_URL` | публичный адрес backend для ссылок модерации в письмах | `http://localhost:8080` |
| `AVTOJP_API_URL` | адрес внешнего API аукционов | `http://78.46.90.228/gzip/` |
| `AVTOJP_API_CODE` | код доступа к API аукционов | пусто |
| `AVTOJP_TIMEOUT` | таймаут внешнего API | `5s` |
| `AVTOJP_CACHE_TTL` | время кеширования поиска аукционов | `10m` |

## Аукционы

Аукционная интеграция не использует готовый внешний поддомен. Frontend обращается в наш backend, а backend ходит во внешний API avto.jp/ajes и возвращает безопасные DTO.

Публичные endpoint'ы:

```text
GET  /api/auctions/search
GET  /api/auctions/{id}
POST /api/auctions/leads
```

Код доступа к внешнему API нельзя хранить во frontend. Для production задайте его только на сервере:

```text
AVTOJP_API_CODE=код-от-поставщика
```

Если `AVTOJP_API_CODE` не задан, страница аукционов откроется, но поиск вернёт сообщение `API-код аукционов не настроен`.

Backend не принимает SQL от браузера. SQL собирается на сервере из разрешённых полей поиска, ограничивает выдачу максимум 50 лотами и кеширует результаты.

## Модерация отзывов

Когда пользователь отправляет отзыв на сайте:

1. backend валидирует поля формы;
2. отзыв сохраняется в PostgreSQL со статусом `PENDING`;
3. владельцу бизнеса приходит письмо с текстом отзыва и фотографиями;
4. в письме есть две ссылки: `Принять отзыв` и `Отклонить отзыв`;
5. если администратор нажимает `Принять`, статус меняется на `APPROVED`;
6. только отзывы со статусом `APPROVED` отдаются на сайт через `GET /api/reviews`;
7. если администратор нажимает `Отклонить`, статус меняется на `REJECTED`, и отзыв не отображается на сайте.

Ссылки модерации формируются на основе переменной:

```text
APP_PUBLIC_BASE_URL
```

Для локального запуска:

```text
APP_PUBLIC_BASE_URL=http://localhost:8080
```

Для production нужно указать реальный публичный адрес backend, например:

```text
APP_PUBLIC_BASE_URL=https://api.olimpavtovl.ru
```

## Настройка SMTP

SMTP нужен, чтобы заявки и отзывы реально уходили на почту владельца бизнеса.

Автоматически отправляются:

- заявка с формы консультации
- отзыв с оценкой, текстом, данными автомобиля и фотографиями

### Режим разработки

По умолчанию в Docker запускается Mailpit. Это локальная тестовая почта.

Адрес:

```text
http://localhost:8025
```

В этом режиме реальные письма наружу не уходят. Они попадают в Mailpit.

Настройки для Mailpit:

```text
MAIL_HOST=mailpit
MAIL_PORT=1025
MAIL_SMTP_AUTH=false
MAIL_SMTP_STARTTLS=false
```

### Реальная доменная почта

Если у бизнеса есть домен, например:

```text
olimp-avto.ru
```

можно отправлять письма с адреса:

```text
noreply@olimp-avto.ru
info@olimp-avto.ru
sales@olimp-avto.ru
```

Для этого владелец бизнеса должен подключить почту для домена у провайдера: Яндекс 360, VK WorkMail, Google Workspace, Zoho, Timeweb, Beget, REG.RU или другого сервиса.

После подключения почты нужно получить SMTP-настройки:

```text
SMTP host
SMTP port
логин
пароль или пароль приложения
TLS/STARTTLS
```

Пример для доменной почты:

```powershell
$env:MAIL_HOST="smtp.yandex.ru"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="noreply@olimp-avto.ru"
$env:MAIL_PASSWORD="password-or-app-password"
$env:MAIL_SMTP_AUTH="true"
$env:MAIL_SMTP_STARTTLS="true"
$env:APP_MAIL_FROM="noreply@olimp-avto.ru"
$env:APP_MAIL_TO="owner@olimp-avto.ru"
```

После этого запустить backend:

```powershell
.\gradlew.bat bootRun
```

Для Docker эти переменные нужно прописать в `docker-compose.yml` в блоке `environment` сервиса `backend`.

### Пример SMTP для Beget

Из старой PHP-конфигурации видно, что использовался SMTP Beget:

```text
SMTP host: smtp.beget.com
SMTP port: 465
SSL:       true
Логин:    mail@olimpavtovl.ru
From:     mail@olimpavtovl.ru
To:       olimpautovl125@gmail.com
```

Для запуска backend без Docker:

```powershell
$env:MAIL_HOST="smtp.beget.com"
$env:MAIL_PORT="465"
$env:MAIL_USERNAME="mail@olimpavtovl.ru"
$env:MAIL_PASSWORD="новый-пароль-от-почты"
$env:MAIL_SMTP_AUTH="true"
$env:MAIL_SMTP_SSL="true"
$env:MAIL_SMTP_STARTTLS="false"
$env:APP_MAIL_FROM="mail@olimpavtovl.ru"
$env:APP_MAIL_TO="olimpautovl125@gmail.com"
.\gradlew.bat bootRun
```

Для Docker:

```yaml
backend:
  environment:
    MAIL_HOST: smtp.beget.com
    MAIL_PORT: 465
    MAIL_USERNAME: mail@olimpavtovl.ru
    MAIL_PASSWORD: новый-пароль-от-почты
    MAIL_SMTP_AUTH: "true"
    MAIL_SMTP_SSL: "true"
    MAIL_SMTP_STARTTLS: "false"
    APP_MAIL_FROM: mail@olimpavtovl.ru
    APP_MAIL_TO: olimpautovl125@gmail.com
```

Пароль не нужно хранить в репозитории. Для production лучше передавать его через переменные окружения на сервере или через `.env`-файл, который не коммитится.

### Gmail как SMTP

Для Gmail обычно нужны такие настройки:

```powershell
$env:MAIL_HOST="smtp.gmail.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="your-gmail@gmail.com"
$env:MAIL_PASSWORD="app-password"
$env:MAIL_SMTP_AUTH="true"
$env:MAIL_SMTP_STARTTLS="true"
$env:APP_MAIL_FROM="your-gmail@gmail.com"
$env:APP_MAIL_TO="owner@gmail.com"
```

Для `MAIL_PASSWORD` нужен не обычный пароль от Gmail, а App Password приложения. Обычно он доступен, если включена двухфакторная аутентификация.

## Что должен сделать владелец бизнеса для доменной почты

1. Купить или иметь домен.
2. Подключить почту для домена.
3. Создать почтовый ящик отправителя, например `noreply@domain.ru`.
4. Получить SMTP host, port, login, password.
5. Настроить DNS-записи домена:

```text
MX
SPF
DKIM
DMARC
```

Без SPF, DKIM и DMARC письма могут попадать в спам или отклоняться почтовыми сервисами.

## PostgreSQL

В Docker поднимается PostgreSQL:

```text
DB:       olimp_avto
User:     olimp_avto
Password: olimp_avto
Port:     5432
```

Таблица каталога создаётся Flyway-миграцией:

```text
src/main/resources/db/migration/V1__create_cars.sql
```

Ручное заполнение каталога можно делать через Swagger:

```text
POST /api/cars
```

Этот эндпоинт защищён Basic Auth.

Пример JSON:

```json
{
  "title": "Toyota Camry",
  "country": "Япония",
  "price": 2100000,
  "year": 2020,
  "engine": "2.5 л, бензин",
  "description": "Седан в хорошем состоянии, проверен перед покупкой.",
  "imageUrl": "images/camry.jpg"
}
```

## Запуск без Docker

Если PostgreSQL и SMTP уже запущены отдельно:

```powershell
cd C:\Users\tooor\Downloads\avto_project\backend
.\gradlew.bat bootRun
```

## Тесты и сборка

Запустить тесты:

```powershell
.\gradlew.bat test
```

Полная сборка:

```powershell
.\gradlew.bat clean build
```

Тесты запускаются перед сборкой `bootJar`. Если тесты падают, backend-артефакт не собирается.

Сборка Docker-образа backend:

```powershell
docker compose build backend
```

В Docker-сборке тесты тоже выполняются перед упаковкой приложения.

Сборка Docker-образа frontend:

```powershell
docker compose build frontend
```

## Проверка API

Проверить чтение каталога:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/cars -Method Get
```

Проверить отправку заявки:

```powershell
Invoke-RestMethod `
  -Uri http://localhost:8080/api/leads `
  -Method Post `
  -ContentType "application/json; charset=utf-8" `
  -Body '{"name":"Иван","phone":"+7 999 111-22-33","comment":"Тест","policyAccepted":true}'
```

# Placement Management System

Placement Management System is a full-stack student placement platform with a Spring Boot backend, a React frontend, JWT-based authentication, MySQL persistence, Swagger documentation, and production deployment support for Vercel, Render, and Clever Cloud.

This repository now supports:

- Local development with Spring Boot, React, and Docker Compose
- Frontend deployment on Vercel
- Backend deployment on Render
- Managed MySQL deployment on Clever Cloud
- Safer backend testing without depending on a local MySQL instance

## What This Project Does

The system supports two main user roles:

- `ADMIN`
  - manage students
  - manage companies
  - manage applications
  - view dashboard statistics
- `STUDENT`
  - register and log in
  - update profile
  - browse companies
  - apply for openings
  - track application status

## Tech Stack

- Backend: Spring Boot 3, Spring Security, Spring Data JPA, Hibernate
- Frontend: React, Vite
- Database: MySQL
- Auth: JWT access token + refresh token flow
- Documentation: Swagger / OpenAPI
- Local orchestration: Docker, Docker Compose
- Production hosting:
  - Frontend: Vercel
  - Backend: Render
  - Database: Clever Cloud MySQL

## Project Structure

```text
.
|-- frontend/                    React + Vite frontend
|-- src/main/java/               Spring Boot application code
|-- src/main/resources/          Backend configuration
|-- src/test/java/               Backend tests
|-- src/test/resources/          Test-only configuration
|-- docker-compose.yml           Local full-stack setup
|-- Dockerfile                   Backend container build
|-- render.yaml                  Render deployment blueprint
|-- .env.example                 Local environment reference
```

## Main Features

- JWT-based login and registration
- Refresh-token support
- Role-based authorization
- Student profile management
- Company posting and editing
- Application workflows and status tracking
- Search and filtering
- Swagger UI for backend API testing
- Rate limiting for request protection

## Local Development

### Backend

Run the backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

### Frontend

Run the frontend:

```powershell
cd frontend
npm install
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

### Full Stack With Docker

Run frontend, backend, and MySQL together:

```powershell
docker compose up --build
```

Services:

- Frontend: `http://localhost`
- Backend: `http://localhost:8080`
- MySQL: `localhost:3307` by default

## Environment Variables

Use [.env.example](/C:/placement-system/.env.example) as the base reference.

Important backend variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`
- `SPRING_JPA_SHOW_SQL`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `JWT_REFRESH_EXPIRATION_MS`
- `APP_CORS_ALLOWED_ORIGINS`

Important frontend variable:

- `VITE_API_BASE_URL`

## How The Deployment Is Split

The production setup used for this project is:

- Frontend on Vercel
- Backend on Render
- MySQL on Clever Cloud

Why this split works well:

- Vercel is a strong fit for React and Vite
- Render is a better fit for Spring Boot and Docker-based deployment
- Clever Cloud offers a managed MySQL service with simple credential access

## Frontend Deployment On Vercel

Use these Vercel project settings:

- Framework preset: `Vite`
- Root directory: `frontend`
- Build command: `npm run build`
- Output directory: `dist`

Set this Vercel environment variable:

```text
VITE_API_BASE_URL=https://your-render-backend-url.onrender.com
```

The SPA rewrite is already configured in [frontend/vercel.json](/C:/placement-system/frontend/vercel.json).

## Backend Deployment On Render

Recommended runtime:

- Docker

You can use [render.yaml](/C:/placement-system/render.yaml) as the Render starting point.

Required Render environment variables:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://HOST:3306/DATABASE_NAME
SPRING_DATASOURCE_USERNAME=YOUR_DB_USER
SPRING_DATASOURCE_PASSWORD=YOUR_DB_PASSWORD
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
JWT_SECRET=YOUR_STRONG_SECRET
APP_CORS_ALLOWED_ORIGINS=https://your-vercel-project.vercel.app
```

If you also want local frontend access while testing:

```text
APP_CORS_ALLOWED_ORIGINS=https://your-vercel-project.vercel.app,http://localhost:5173,http://127.0.0.1:5173
```

Important CORS rule:

- do not add a trailing slash to the frontend origin

## MySQL Deployment On Clever Cloud

This backend already supports external MySQL through environment variables, so no backend code rewrite is needed to switch from local Docker MySQL to Clever Cloud.

Clever Cloud gives credentials like:

- Host
- Database name
- User
- Password
- Port

Spring Boot should use them in JDBC format:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://HOST:3306/DATABASE_NAME
SPRING_DATASOURCE_USERNAME=USER
SPRING_DATASOURCE_PASSWORD=PASSWORD
```

Do not use the raw `mysql://...` URI directly for `SPRING_DATASOURCE_URL`.

Example format:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://briadr7ltppe2by73two-mysql.services.clever-cloud.com:3306/briadr7ltppe2by73two
```

Deployment steps:

1. Create the MySQL add-on in Clever Cloud.
2. Copy the database host, name, user, password, and port.
3. Open the backend service in Render.
4. Go to Environment.
5. Set the Spring datasource variables.
6. Save and redeploy.

## Connection Limit Fix For Clever Cloud Dev Plan

During deployment, the database connection itself succeeded, but the Clever Cloud dev MySQL plan hit its connection cap:

```text
max_user_connections = 5
```

To keep the backend stable on a low-connection MySQL plan, configure a small Hikari pool in Render:

```text
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=2
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=0
SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT=10000
SPRING_DATASOURCE_HIKARI_MAX_LIFETIME=30000
```

If you hit connection errors:

1. Open Clever Cloud MySQL add-on page.
2. Click `Kill all connections`.
3. Close `phpMyAdmin`, local MySQL CLI sessions, and DB tools.
4. Redeploy the backend once.

## Problems We Encountered And How We Solved Them

### 1. Dialect / JDBC Metadata Error

Error pattern:

```text
Unable to determine Dialect without JDBC metadata
```

Meaning:

- Hibernate could not read database metadata because the app was not successfully connected to MySQL.

Root causes encountered:

- incomplete or incorrect datasource URL
- wrong database password
- too many active DB connections on Clever Cloud

### 2. Wrong Password

The backend initially failed because the Render password did not match the Clever Cloud MySQL password.

Fix:

- update `SPRING_DATASOURCE_PASSWORD` in Render
- redeploy the backend

### 3. Too Many MySQL Connections

The Clever Cloud dev plan only allowed 5 active connections for the DB user.

Fix:

- kill open connections in Clever Cloud
- reduce Hikari pool size
- redeploy

### 4. Render Restart During Port Detection

Render restarted once after detecting the open port:

```text
New primary port detected: 8080
```

This is normal behavior during deployment and was not the root problem.

## Safer Test Setup Added To This Repository

Originally, the backend `contextLoads` test depended on a real local MySQL connection. That made tests fail whenever the local DB password or DB state differed.

This repository was updated so tests no longer depend on local MySQL:

- `H2` test dependency added in [pom.xml](/C:/placement-system/pom.xml)
- test-only datasource config added in [application.yaml](/C:/placement-system/src/test/resources/application.yaml)
- the MySQL-specific legacy migration runner is disabled during tests in [LegacyStudentMigrationRunner.java](/C:/placement-system/src/main/java/com/rameshkumar/placementsystem/config/LegacyStudentMigrationRunner.java)

This allows the backend test suite to run in isolation.

## Files Updated During This Deployment Work

- [README.md](/C:/placement-system/README.md)
- [pom.xml](/C:/placement-system/pom.xml)
- [application.yaml](/C:/placement-system/src/main/resources/application.yaml)
- [application.yaml](/C:/placement-system/src/test/resources/application.yaml)
- [LegacyStudentMigrationRunner.java](/C:/placement-system/src/main/java/com/rameshkumar/placementsystem/config/LegacyStudentMigrationRunner.java)

## Verification Commands

Backend tests:

```powershell
.\mvnw.cmd test
```

Frontend build:

```powershell
cd frontend
npm run build
```

## Post-Deployment Checklist

After backend deployment:

1. Open the Render backend URL.
2. Open Swagger UI.
3. Open the Vercel frontend.
4. Test registration.
5. Test login.
6. Test creating a student, company, or application.
7. Confirm records persist in MySQL.

## Security Notes

- Do not commit production DB credentials into source files.
- Keep production DB credentials in Render environment variables only.
- Use a strong `JWT_SECRET`.
- If a database password was exposed during debugging, rotate it in Clever Cloud and then update Render.

## Official References

- Clever Cloud MySQL docs: https://www.clever.cloud/developers/doc/addons/mysql/
- Clever Cloud MySQL product page: https://www.clever-cloud.com/product/mysql/
- Render web services docs: https://render.com/docs/web-services
- Vercel docs: https://vercel.com/docs

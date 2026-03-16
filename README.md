# Placement Management System

Spring Boot placement backend with JWT authentication, role-based access, student/company/application workflows, Swagger docs, a React frontend, and Docker deployment support.

## Stack

- Backend: Spring Boot 3, Spring Security, Spring Data JPA, MySQL
- Frontend: React + Vite
- Auth: JWT
- Docs: Swagger / OpenAPI
- Deployment: Docker, Docker Compose, Render, Vercel

## Run Locally

### Backend

```powershell
.\mvnw.cmd spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

### Frontend

```powershell
cd frontend
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

## Run With Docker

Build and run the full stack:

```powershell
docker compose up --build
```

Services:

- Frontend: `http://localhost`
- Backend: `http://localhost:8080`
- MySQL: `localhost:3306`

## Environment Variables

Use [.env.example](/C:/placement-system/.env.example) as the reference set for backend configuration.

Important variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `APP_CORS_ALLOWED_ORIGINS`

## Deploy Frontend On Vercel

Recommended project settings:

- Framework preset: `Vite`
- Root directory: `frontend`
- Build command: `npm run build`
- Output directory: `dist`

Set this environment variable in Vercel:

- `VITE_API_BASE_URL=https://your-render-backend-url.onrender.com`

The frontend SPA rewrite is configured in [frontend/vercel.json](/C:/placement-system/frontend/vercel.json).

## Deploy Backend On Render

Recommended runtime:

- Docker

You can use [render.yaml](/C:/placement-system/render.yaml) as the starting blueprint for the backend service.

Set these Render environment variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `APP_CORS_ALLOWED_ORIGINS`

Recommended CORS value after frontend deployment:

```text
https://your-vercel-project.vercel.app
```

You can also include local origins during testing:

```text
https://your-vercel-project.vercel.app,http://localhost:5173,http://127.0.0.1:5173
```

## Database For Production

This backend currently uses MySQL.

For Render deployment, use one of these:

1. A dedicated MySQL service on Render using their MySQL Docker deployment approach
2. An external managed MySQL provider

Once the database is ready, wire its internal or external connection string into:

```text
SPRING_DATASOURCE_URL
```

## Recommended Production Setup

- Frontend: Vercel
- Backend: Render
- Database: MySQL service on Render or external managed MySQL

This split is the best fit for the current project:

- Vercel is strong for React/Vite frontend delivery
- Render is a better fit for a Spring Boot backend and Docker-based deployment

## Verification

Before pushing or deploying:

```powershell
.\mvnw.cmd test
```

```powershell
cd frontend
npm run build
```

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
- MySQL: `localhost:3307` by default, configurable with `MYSQL_HOST_PORT`

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

## Deploy MySQL On Clever Cloud

Your current backend already supports external MySQL through environment variables, so no code change is required to switch from local Docker MySQL to Clever Cloud.

Use your Clever Cloud database values to set these backend variables on the backend host:

- `SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/<database>`
- `SPRING_DATASOURCE_USERNAME=<user>`
- `SPRING_DATASOURCE_PASSWORD=<password>`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
- `SPRING_JPA_SHOW_SQL=false`

Example using Clever Cloud credentials:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://briadr7ltppe2by73two-mysql.services.clever-cloud.com:3306/briadr7ltppe2by73two
SPRING_DATASOURCE_USERNAME=ubgjff7h4mns672y
SPRING_DATASOURCE_PASSWORD=<your-clever-cloud-password>
```

If your backend is deployed on Render:

1. Open the Render service for the backend.
2. Go to Environment.
3. Replace the database variables with the Clever Cloud values above.
4. Save changes and redeploy.

If your backend is deployed on Clever Cloud instead:

1. Open the application in Clever Cloud.
2. Open Service Dependencies.
3. Link the MySQL add-on to the app.
4. Map the injected add-on variables to the Spring Boot variables if needed.

After redeploy, verify:

1. Backend logs show the datasource started successfully.
2. Login and registration still work.
3. Student, company, and application data can be created and read.

Official references:

- Clever Cloud MySQL add-on docs: https://www.clever-cloud.com/developers/doc/addons/mysql/
- Clever Cloud app/add-on linking example: https://www.clever-cloud.com/developers/guides/tutorial-drupal/

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

# E-Commerce Backend

Spring Boot REST API for the GUVI E-Commerce application with JWT auth, product/cart/order management, and Razorpay payments.

## Tech Stack
- Spring Boot 3, Spring Security (JWT), Spring Data JPA
- MySQL
- Razorpay Java SDK
- Swagger / OpenAPI (`/swagger-ui.html`)
- JUnit 5 + Mockito

## Configuration
All secrets are read from environment variables (with local fallbacks). Set these before running:

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/ecommerce_db?...` |
| `DB_USERNAME` | DB user | `root` |
| `DB_PASSWORD` | DB password | _(empty)_ |
| `JWT_SECRET` | JWT signing secret | dev default |
| `RAZORPAY_KEY_ID` | Razorpay key id | placeholder |
| `RAZORPAY_KEY_SECRET` | Razorpay key secret | placeholder |
| `PORT` | Server port | `8080` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origin | `http://localhost:3000` |
| `ADMIN_EMAIL` | Seeded admin email | `admin@guvi.com` |
| `ADMIN_PASSWORD` | Seeded admin password | `Admin@123` |
| `ADMIN_SEED_ENABLED` | Seed an admin on startup | `true` |

## Run Locally
```bash
# Windows (PowerShell)
$env:DB_PASSWORD="yourpassword"
mvn spring-boot:run
```

```bash
# Linux/macOS
DB_PASSWORD=yourpassword mvn spring-boot:run
```

API runs at `http://localhost:8080`. Swagger UI at `http://localhost:8080/swagger-ui.html`.

## Run Tests
```bash
mvn test
```

The suite covers all three layers the brief asks for — controllers
(`@WebMvcTest` slices), services (JUnit 5 + Mockito), and repositories
(`@DataJpaTest`) — plus `EcommerceWorkflowEndToEndTest`, which drives the real
security filter chain over an in-memory H2 database to cover registration,
login, role-based access, the cart workflow, and profile updates. No MySQL or
Razorpay credentials are needed to run it.

## Key Endpoints
| Method | Path | Access |
|--------|------|--------|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| GET | `/api/products` | Public |
| GET | `/api/products/search?query=` | Public |
| POST/PUT/DELETE | `/api/products/**` | ADMIN |
| GET/PUT | `/api/users/me` | Authenticated |
| GET/POST/PUT/DELETE | `/api/cart/**` | Authenticated |
| POST | `/api/orders/checkout` | Authenticated |
| POST | `/api/orders/verify-payment` | Authenticated |
| GET | `/api/orders/admin/all` | ADMIN |

New users register as `CUSTOMER`. On first startup the app seeds an `ADMIN`
account (default `admin@guvi.com` / `Admin@123`) so the admin module is usable
immediately. Override it with the `ADMIN_EMAIL` / `ADMIN_PASSWORD` environment
variables, or set `ADMIN_SEED_ENABLED=false` to disable seeding entirely.

> **Change the seeded password before deploying publicly.**

You can also promote an existing user by hand:
```sql
UPDATE users SET role='ADMIN' WHERE email='you@example.com';
```

## Deployment (Render)

This directory includes the `Dockerfile`; the `render.yaml` Blueprint lives at the
repository root, since Render only reads Blueprints from there.

> **Database note:** Render's free managed databases are **PostgreSQL only**. Since this app uses MySQL, provision a free external MySQL first (e.g. [Aiven](https://aiven.io), [Railway](https://railway.app), or [Clever Cloud](https://clever-cloud.com)) and grab its JDBC URL, username, and password.

### Steps
1. Push the repository to GitHub (already done).
2. On [Render](https://render.com): **New → Web Service** → connect the repo.
3. **Set Root Directory to `backend`.** The backend is one half of a monorepo, so
   without this Render looks for the `Dockerfile` at the repo root and the build
   fails. (Deploying via **New → Blueprint** with the root `render.yaml` sets
   this for you.) Render then auto-detects the `Dockerfile` (Runtime: Docker).
4. Add the environment variables (Dashboard → Environment):
   | Key | Example value |
   |-----|---------------|
   | `DB_URL` | `jdbc:mysql://host:port/dbname?useSSL=true&serverTimezone=UTC` |
   | `DB_USERNAME` | your MySQL user |
   | `DB_PASSWORD` | your MySQL password |
   | `JWT_SECRET` | any long random string |
   | `RAZORPAY_KEY_ID` | your Razorpay key id |
   | `RAZORPAY_KEY_SECRET` | your Razorpay key secret |
   | `CORS_ALLOWED_ORIGINS` | your Vercel URL, e.g. `https://your-app.vercel.app` |
5. Deploy. The service listens on the `PORT` Render provides (defaults to 8080).

You can also use **New → Blueprint** and point it at the root `render.yaml` to
pre-create the service with these env var slots and the correct root directory.

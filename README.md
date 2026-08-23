# GUVI Ecommerce Application

Spring Boot + React ecommerce app with Razorpay payments.

## Tech Stack
- **Backend**: Spring Boot 3, Spring Security (JWT), Spring Data JPA
- **Database**: MySQL
- **Frontend**: React 18, React Router, Axios
- **Payment**: Razorpay
- **Docs**: Swagger UI (`/swagger-ui.html`)

## Setup

### Prerequisites
- Java 17+, Maven, Node 18+, MySQL 8

### Backend
```bash
cd backend
# Edit src/main/resources/application.properties — set DB credentials & Razorpay keys
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm start
```

### First admin login
On first startup the backend seeds an admin account (default `admin@guvi.com` /
`Admin@123`). Override it with the `ADMIN_EMAIL` / `ADMIN_PASSWORD` environment
variables, and change the password before deploying publicly.

## API Endpoints
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/auth/register | Public | Register |
| POST | /api/auth/login | Public | Login |
| GET | /api/products | Public | List products |
| GET | /api/products/search?query= | Public | Search |
| POST | /api/products | ADMIN | Create product |
| PUT | /api/products/{id} | ADMIN | Update product |
| DELETE | /api/products/{id} | ADMIN | Delete product |
| GET | /api/users/me | USER | Get own profile |
| PUT | /api/users/me | USER | Update own profile |
| GET | /api/cart | USER | Get cart |
| POST | /api/cart | USER | Add to cart |
| PUT | /api/cart/{itemId}?quantity= | USER | Change quantity |
| DELETE | /api/cart/{itemId} | USER | Remove item |
| GET | /api/orders | USER | Own order history |
| POST | /api/orders/checkout | USER | Create Razorpay order |
| POST | /api/orders/verify-payment | USER | Verify payment |
| GET | /api/orders/admin/all | ADMIN | All orders |
| PUT | /api/orders/admin/{id}/status?status= | ADMIN | Update order status |

## Running Tests
```bash
cd backend
mvn test
```

Covers the controller, service, and repository layers plus an end-to-end
workflow suite that exercises registration, login, role-based access, the cart
workflow, and profile updates against an in-memory database.

## Repository layout
```
.
├── backend/     Spring Boot API (Maven)
├── frontend/    React SPA (Create React App)
└── render.yaml  Render Blueprint for the backend
```

## Deployment

Both services deploy from this single repository, so each one must be told which
subdirectory it owns.

### Backend — Render
Deploy via **New → Blueprint** pointing at `render.yaml`, which already sets
`rootDir: backend`. If you instead create the service by hand, set **Root
Directory** to `backend` or the Docker build will fail looking for a Dockerfile
at the repo root. Set the env vars listed in [backend/README.md](backend/README.md).

### Frontend — Vercel
In **Project Settings → General**, set **Root Directory** to `frontend`. Vercel
then auto-detects Create React App. Add `REACT_APP_API_URL` pointing at your
Render URL and ending in `/api`, then add the resulting Vercel URL to the
backend's `CORS_ALLOWED_ORIGINS`.

> Both projects were originally deployed from separate repositories. After
> consolidating, update each project's Root Directory setting and its connected
> repository, otherwise they will keep building from the old repos.

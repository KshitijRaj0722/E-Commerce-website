# E-Commerce Frontend

React single-page app for the GUVI E-Commerce application — product browsing, cart, checkout (Razorpay), order history, and an admin panel.

## Tech Stack
- React 18, React Router 6
- Axios
- Razorpay Checkout

## Configuration
The API base URL is proxied to `http://localhost:8080` during development via the `proxy` field in `package.json`. For production, set the API URL through your hosting provider (e.g. an `REACT_APP_API_URL` env var) and update `src/services/api.js` accordingly.

## Run Locally
```bash
npm install
npm start
```
App runs at `http://localhost:3000`. The backend must be running on port 8080.

## Build for Production
```bash
npm run build
```
Outputs static assets to `build/`.

## Deployment (Vercel)

This repo includes a `vercel.json` (SPA routing rewrite) and `.env.example`.

### Steps
1. Push the repository to GitHub (already done).
2. On [Vercel](https://vercel.com): **Add New → Project** → import the repo.
3. **Set Root Directory to `frontend`** (Project Settings → General). The frontend
   is one half of a monorepo, so without this Vercel builds from the repo root and
   finds no `package.json`. Vercel then auto-detects Create React App
   (Build: `npm run build`, Output: `build`).
4. Add an environment variable:
   | Key | Value |
   |-----|-------|
   | `REACT_APP_API_URL` | your Render backend URL **ending in `/api`**, e.g. `https://ecommerce-backend.onrender.com/api` |
5. Deploy. Copy the resulting Vercel URL and add it to the backend's `CORS_ALLOWED_ORIGINS` env var on Render.

> The API URL is read from `REACT_APP_API_URL` (see `src/services/api.js`). Locally, leave it unset and the dev proxy in `package.json` forwards `/api` to `localhost:8080`.

## Pages
- `/` — Home
- `/login`, `/register` — Auth
- `/products` — Browse + search, add to cart
- `/cart` — Cart management + Razorpay checkout
- `/orders` — Order history
- `/admin` — Product CRUD + order management (ADMIN only)

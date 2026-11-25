# Frontend (React + Vite)

## Run

1. Start the backend (port 2000) the way you normally do (IntelliJ or `docker compose up`).
2. In this folder:
   ```
   copy .env.example .env.local      (Windows)   or   cp .env.example .env.local
   ```
   Put the same API key you use for the backend in `.env.local` (leave it as is if auth is disabled).
3. Install and start:
   ```
   npm install
   npm run dev
   ```
4. Open http://localhost:5173

The Vite dev server forwards `/api/*` to the backend and adds the `X-API-Key` header itself, so the key is never
shipped to the browser and there are no CORS problems.

## Build for production

```
npm run build      # output in dist/
```

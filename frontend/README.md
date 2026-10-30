# Notification Service Admin

Админка сервиса уведомлений на [react-admin](https://github.com/marmelab/react-admin) 5 (React 19, MUI 7, Vite, TypeScript).
Полное описание — [docs/frontend.md](../docs/frontend.md).

```bash
npm ci
cp .env.example .env      # необязательно: настройки VITE_*
npm run dev               # http://localhost:5173, прокси на бэкенд http://localhost:8080
npm test                  # vitest
npm run build             # typecheck + сборка в dist/
```

Бэкенд для `npm run dev` удобно поднять так: `docker compose up --build notification-service` из корня репозитория.

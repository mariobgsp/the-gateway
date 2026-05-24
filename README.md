# The Gateway
Gateway services example with a React TypeScript frontend and Spring Boot backend.

## Design Reference
[Figma Link](https://www.figma.com/design/8IKh4NrzxsXJajEt8jL32x/the-gateway?node-id=1-6344&t=RuiBqghCN5cc1GHp-1)

## Running with Docker
You can run the entire stack (PostgreSQL database, Spring Boot Backend, and React Frontend) through Docker Compose.

1. Ensure you have Docker and Docker Compose installed.
2. Navigate to the `project` directory:
   ```bash
   cd project
   ```
3. Build and start the containers:
   ```bash
   docker compose up -d --build
   ```

### Services
- **Frontend**: Accessible at `http://localhost:3000`
- **Backend (Gateway Service)**: Accessible at `http://localhost:8080`
- **Database (PostgreSQL)**: Port `5432`

## Testing the Frontend
To run the End-to-End Playwright tests for the frontend:
```bash
cd frontend
npm install
npx playwright test
```

# auth-service: login, registration and token refresh. Issues the JWTs the trading app checks.
# Built and run by docker-compose (the auth-service service); settings come from the root .env through compose.

# build: compile the TypeScript to dist/
FROM node:26.12-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY tsconfig.json ./
COPY src ./src
RUN npm run build

# run: only the compiled code and the production dependencies
FROM node:26.12-slim
WORKDIR /app
ENV NODE_ENV=production
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist
USER node
EXPOSE 3000
CMD ["node", "dist/main.js"]

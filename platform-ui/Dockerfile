FROM node:22-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

FROM nginx:alpine
COPY --from=build /app/dist/platform-ui/browser /usr/share/nginx/html
# a template: the image fills in ${LOGO_DEV_CRED_KEY} when the container starts (see nginx.conf)
# empty unless compose passes the key, so nginx still starts without it (logos then fall back to a letter)
ENV LOGO_DEV_CRED_KEY=""
COPY nginx.conf /etc/nginx/templates/default.conf.template

EXPOSE 80
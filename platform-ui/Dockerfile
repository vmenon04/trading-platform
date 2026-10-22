FROM node:22-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

FROM nginx:alpine
COPY --from=build /app/dist/platform-ui/browser /usr/share/nginx/html
# a template: the image fills in ${LOGO_DEV_CRED_KEY} when the container starts (see nginx.conf)
COPY nginx.conf /etc/nginx/templates/default.conf.template

EXPOSE 80
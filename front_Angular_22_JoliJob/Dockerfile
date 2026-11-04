FROM node:24-alpine AS build

WORKDIR /app
RUN npm install --global npm@11
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:stable-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/agora-jobs/browser/ /usr/share/nginx/html/
EXPOSE 80

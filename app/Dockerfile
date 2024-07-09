FROM node:18
ARG PUBLIC_URL=/

COPY package.json /app/package.json
COPY package-lock.json /app/package-lock.json
WORKDIR /app
RUN npm ci

COPY . /app

ENV PUBLIC_URL=$PUBLIC_URL
RUN npm run build

FROM nginx:stable

COPY nginx.conf /etc/nginx/nginx.conf

COPY --from=0 /app/build /usr/share/nginx/html

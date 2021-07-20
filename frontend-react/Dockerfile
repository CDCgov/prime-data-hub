FROM node:alpine

WORKDIR /app

# Directories
COPY ./public /app/public
COPY ./src /app/src

# Root files
COPY ./package.json /app/package.json
COPY ./package-lock.json /app/package-lock.json
COPY ./tsconfig.json /app/tsconfig.json

RUN [ "npm", "install", "--silent", "--only=production" ]
RUN [ "npm", "run", "build", "--production" ]

ENV NODE_ENV=production
EXPOSE 3000
ENTRYPOINT [ "npm", "run", "start" ]
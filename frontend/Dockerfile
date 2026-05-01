# 🎨 Dockerfile for Frontend (React)
# Location: /frontend/Dockerfile

# Build stage
FROM node:18-alpine AS build
WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./

# Install dependencies
RUN npm ci

# Copy source
COPY . .

# Build React app
RUN npm run build

# Production stage
FROM node:18-alpine
WORKDIR /app

# Install serve to run the build
RUN npm install -g serve

# Copy built app from build stage
COPY --from=build /app/build ./build

# Expose port
EXPOSE 3000

# Serve the app
CMD ["serve", "-s", "build", "-l", "3000"]


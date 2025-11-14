# GEMINI.md - AI Travel Assistant

## Project Overview

This is a full-stack web application that acts as an AI-powered travel assistant. It allows users to generate personalized travel plans based on their destination, budget, and preferences. The application is built with a Vue.js frontend and a Node.js backend.

**Key Technologies:**

*   **Frontend:**
    *   Vue.js 3 with Composition API
    *   Ant Design Vue for UI components
    *   Pinia for state management
    *   Vue Router for client-side routing
    *   Axios for HTTP requests
    *   Vite for the build tool
*   **Backend:**
    *   Node.js with Express.js
    *   SQLite for the database
    *   JWT for user authentication
    *   DeepSeek API for AI-powered travel plan generation

**Architecture:**

The application is divided into two main parts:

*   `front`: A single-page application (SPA) built with Vue.js that provides the user interface.
*   `backend`: A RESTful API built with Node.js and Express.js that handles business logic, user authentication, and data storage.

The frontend communicates with the backend via HTTP requests to the API endpoints.

## Building and Running

### Frontend

1.  **Navigate to the frontend directory:**
    ```bash
    cd front
    ```

2.  **Install dependencies:**
    ```bash
    npm install
    ```

3.  **Configure environment variables:**
    Create a `.env` file by copying `.env.example` and add your DeepSeek API key and the backend API URL.

4.  **Run the development server:**
    ```bash
    npm run dev
    ```
    The frontend will be available at `http://localhost:3000`.

### Backend

1.  **Navigate to the backend directory:**
    ```bash
    cd backend
    ```

2.  **Install dependencies:**
    ```bash
    npm install
    ```

3.  **Configure environment variables:**
    Create a `.env` file by copying `.env.example` and add a JWT secret and your DeepSeek API key.

4.  **Run the development server:**
    ```bash
    npm run dev
    ```
    The backend will be available at `http://localhost:8080`.

## Development Conventions

*   **Frontend:**
    *   Follows the Vue 3 Composition API style.
    *   State management is handled by Pinia, with separate stores for `auth` and `travel`.
    *   Components are well-organized into `components` and `views` directories.
    *   Routing is managed by Vue Router, with route guards for authentication.
*   **Backend:**
    *   Uses a standard Node.js/Express.js project structure.
    *   Authentication is handled via JWTs.
    *   Passwords are encrypted using bcrypt.
    *   The database schema is defined in `backend/database/init.js`.
    *   API responses follow a consistent JSON format.

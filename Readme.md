# 🎬 Online Theater Booking System

A full-stack, smart movie ticket booking platform built with **Java (Spring Boot)** and **React**. This project goes beyond basic CRUD operations by introducing real-time seat locking, intelligent theater recommendations, and WebSocket-driven live updates.

> **College Project:** Developed as part of a 4-week sprint, focusing on concurrency control, algorithm design, and real-time web technologies.

---

## ✨ Key Features

### 1. ⏱️ 10-Minute Seat Locking (Concurrency Control)
* When a user selects a seat, it is temporarily locked for **10 minutes**.
* Uses **atomic database updates** (`WHERE status='AVAILABLE'`) to prevent double-booking when multiple users click the same seat simultaneously.
* A background **Spring Scheduler** runs every minute to automatically release seats whose lock has expired.

### 2. 🧠 Smart Theater Recommendation
* Recommends the best theaters for a specific movie based on:
  * **Screen Size** (Larger screens score higher).
  * **Aspect Ratio Match** (Compares the movie's aspect ratio to the theater screen's aspect ratio).
* Calculates a weighted **Recommendation Score** and highlights the best option with a "★ Recommended" badge.

### 3. 🔔 Real-Time Seat Release Notification
* Utilizes **WebSockets (STOMP)** to push live updates to all users viewing a specific show.
* If a seat lock expires or a user cancels a booking, the UI instantly updates the seat color from "Locked/Booked" to "Available" without requiring a page refresh.

### 4. 🎟️ Booking Cancellation
* Users can cancel upcoming bookings from their "My Bookings" dashboard.
* Cancellation triggers an atomic database transaction to release the seats and broadcasts a WebSocket event so other users can grab them immediately.

---

## 🛠️ Technology Stack

### Backend
* **Language:** Java 21
* **Framework:** Spring Boot 3 (Spring Web, Spring Data JPA, Spring Security)
* **Real-Time:** Spring WebSocket (STOMP)
* **Authentication:** JWT (JSON Web Tokens)
* **Database:** MySQL 8

### Frontend
* **Framework:** React.js (Vite)
* **Styling:** Tailwind CSS
* **Routing:** React Router DOM
* **State Management:** React Context API
* **HTTP Client:** Axios
* **Real-Time Client:** SockJS + STOMP.js
* **Notifications:** React-Toastify

---

## 🎨 UI Development Techniques

* **Component-Based Architecture:** Reusable components (e.g., `MovieCard`, `SeatGrid`, `Navbar`) for consistency.
* **Responsive Design:** Mobile-first approach using CSS Grid and Flexbox to ensure the seat map works on all screen sizes.
* **Optimistic UI Updates:** The seat color changes to "Selected" immediately upon click. If the backend returns a `409 Conflict` (seat already taken), the UI reverts the color and shows an error toast.
* **Conditional Rendering:** Navbar dynamically changes based on authentication state.
* **Real-Time UI Sync:** The `SeatGrid` component subscribes to a WebSocket topic and updates specific seat colors instantly when a broadcast is received.

---

## 🗄️ Database Schema (Core Tables)

| Table | Key Columns |
| :--- | :--- |
| **Users** | `user_id`, `name`, `email`, `password_hash`, `role` |
| **Movies** | `movie_id`, `title`, `aspect_ratio`, `duration`, `poster_url` |
| **Theaters** | `theater_id`, `name`, `location`, `screen_size_inches`, `screen_aspect_ratio` |
| **Shows** | `show_id`, `movie_id`, `theater_id`, `start_time` |
| **Seats** | `seat_id`, `show_id`, `seat_number`, `status` (AVAILABLE/LOCKED/BOOKED), `locked_until`, `user_id` |
| **Bookings** | `booking_id`, `user_id`, `show_id`, `booking_time`, `status` (CONFIRMED/CANCELLED) |

---

## 🚀 Getting Started

### Prerequisites
* Java 21 (or Java 17+)
* Node.js (v18+)
* MySQL Server
* Maven

### 1. Database Setup
1. Create a MySQL database named `theater_booking_db`.
2. Update the `application.properties` in the backend `src/main/resources` folder with your MySQL credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/theater_booking_db
   spring.datasource.username=root
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=update
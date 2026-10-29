# Online Theater Booking System

Full-stack Spring Boot 3 + Java 21 + React application for online movie ticket and seat reservation with pessimistic 10-minute seat locking.

---

## 🛠 Tech Stack

- **Backend**:
  - **Java**: 21
  - **Framework**: Spring Boot 3.2.0
  - **Dependencies**: Spring Web, Spring Data JPA, Spring Security, Validation, MySQL Connector/J
  - **Authentication**: JWT (HMAC-SHA256, RFC 7519) + BCrypt Password Encoding
  - **Concurrency Control**: JPA Pessimistic Write Lock (`PESSIMISTIC_WRITE`) for zero race-condition seat reservations
- **Frontend**:
  - **Framework**: React 19 + Vite
  - **Icons**: Lucide React
  - **Styling**: Modern Cinematic Dark Design System (Responsive CSS Grid, Glassmorphism, Screen curvature arc)
  - **Integration**: REST client with JWT storage, live 10-minute countdown lock timer, and auto-proxy to `:8080`
- **Database**:
  - **RDBMS**: MySQL 8.x

---

## 🏛 Database Schema & Relationships

```
+--------------------+        +---------------------+
|       users        |        |       theaters      |
+--------------------+        +---------------------+
| user_id (PK)       |        | theater_id (PK)     |
| name               |        | name                |
| email (UQ)         |        | location            |
| password_hash      |        | screen_size_inches  |
| role               |        | screen_aspect_ratio |
+---------+----------+        +----------+----------+
          |                              |
          | 1:N                          | 1:N
          v                              v
+---------+----------+ 1:N    +----------+----------+
|       seats        |<-------|        shows        |
+--------------------+        +---------------------+
| seat_id (PK)       |        | show_id (PK)        |
| show_id (FK)       |        | movie_id (FK) ----+ |
| seat_number        |        | theater_id (FK)   | |
| status             |        | start_time        | |
| locked_until       |        +-------------------+ |
| user_id (FK)       |                              |
+--------------------+                              |
                                                    |
+--------------------+                              |
|       movies       |<-----------------------------+
+--------------------+ 1:N
| movie_id (PK)      |
| title              |
| aspect_ratio       |
| duration           |
| poster_url         |
| rating             |
+--------------------+
```

---

## 🖥 Frontend UI Pages & Features

1. **Navigation Bar with Conditional Rendering**:
   - **Guest State (Not Logged In)**: Displays **"Login"** and **"Sign Up"** action buttons.
   - **Authenticated State**: Displays **"My Bookings"** navigation link, user profile badge (name & role), and **"Logout"** button.
2. **Home Page**:
   - Cinematic Hero Banner with real-time movie search by title.
   - Responsive Grid of **Movie Cards**:
     - Poster image with fallback error handling and hover zoom.
     - Title, rating badge (e.g. ⭐ 8.8), duration pill (e.g. 🕒 148 min), aspect ratio tag.
     - Clickable cards route to the showtime selection page for that specific movie.
3. **Showtime & Seat Selection Page**:
   - Movie hero summary (poster, title, rating, duration, screen aspect ratio).
   - Showtime picker grouped by theater with screen size in inches and location.
   - **Interactive Seat Map**:
     - Visual curved cinema screen with ambient glow ("ALL EYES ON SCREEN").
     - Available seats, booked seats, and locked seats.
     - **10-Minute Pessimistic Seat Hold**: Locking a seat triggers a live `10:00` countdown timer.
     - One-click booking confirmation with instant ticket receipt.
4. **Login & Signup Modal/Forms**:
   - Connects to backend `/api/auth/login` and `/api/auth/signup`.
   - Validates form inputs and displays clear error/success messages.
   - Demo test account quick-fill buttons (`admin@example.com` / `john@example.com`).
5. **My Bookings Page**:
   - Lists all confirmed and currently held tickets for the authenticated user.
   - Shows movie poster, theater name, location, showtime, and seat reference.

---

## 🔒 10-Minute Seat Locking Mechanism

1. When a user selects an available seat, `SeatLockService.lockSeat(showId, seatNumber, user)` acquires a database pessimistic lock (`PESSIMISTIC_WRITE`).
2. The seat status changes to `LOCKED`, assigned to `user`, and `locked_until` is set to `now() + 10 minutes`.
3. If checkout is completed before expiration, status transitions to `BOOKED` and `locked_until` is cleared.
4. A background `@Scheduled` job executes every minute calling `releaseExpiredLocks()`, automatically resetting any seat where `status = 'LOCKED' AND locked_until < now()` back to `AVAILABLE`.

---

## 📡 REST API Endpoints

| Category | Method | Endpoint | Description |
|---|---|---|---|
| **Auth** | `POST` | `/api/auth/signup` | Register new user, returns JWT token |
| **Auth** | `POST` | `/api/auth/login` | Authenticate with email/password, returns JWT token |
| **Auth** | `GET` | `/api/auth/me` | Get current user from Bearer JWT |
| **Movies** | `GET` | `/api/movies` | List movies (optional `?search=title`) |
| **Movies** | `GET` | `/api/movies/{id}` | Get movie details |
| **Movies** | `GET` | `/api/movies/{movieId}/recommended-theaters` | Get sorted list of best theaters for movie based on screen size and aspect ratio match |
| **Shows** | `GET` | `/api/shows` | List all shows |
| **Shows** | `GET` | `/api/shows/movie/{movieId}` | List shows for a specific movie |
| **Seats** | `GET` | `/api/shows/{showId}/seats` | List seats and statuses for a show |
| **Seats** | `POST` | `/api/seats/{seatId}/lock` | Atomically lock seat by ID for 10 minutes (validates user) |
| **Seats** | `POST` | `/api/shows/{showId}/seats/{seatNumber}/lock?userId={id}` | Lock seat by show ID and seat number |
| **Seats** | `POST` | `/api/shows/{showId}/seats/{seatNumber}/book?userId={id}` | Confirm booking of locked seat |
| **Bookings**| `GET` | `/api/bookings/my-bookings?userId={id}` | List user's bookings |

---

## 🎯 Smart Theater Recommendation Algorithm

The `TheaterRecommendationService` ranks auditoriums for a specific film based on two core cinematic factors:
1. **Screen Size (50% weight)**: Larger screen sizes in inches score higher (normalized against a 1000" benchmark screen).
2. **Aspect Ratio Match (50% weight)**: The movie's aspect ratio (e.g., `2.39:1`, `1.85:1`, `1.90:1`, `16:9`) is parsed numerically and compared against the auditorium's `screen_aspect_ratio`. Closer matches minimize letterboxing/pillarboxing and score higher:
   $$\text{diff} = |\text{movieRatio} - \text{theaterRatio}|$$
   $$\text{Aspect Score} = \max(0, 100 - (\text{diff} \times 100))$$
3. **Composite Recommendation Score**:
   $$\text{Total Score} = (0.5 \times \text{Size Score}) + (0.5 \times \text{Aspect Score})$$
4. **Utility Class**: `com.example.onlinebooking.util.TheaterScoreCalculator` encapsulates parsing, component scores, and rounding logic.


---

## 🔔 Real-Time Seat Release Notifications (WebSockets & STOMP)

The system delivers real-time notifications when a held seat expires and is unlocked, instantly updating all active viewers' browsers without requiring page refreshes:

1. **WebSocket Configuration (`WebSocketConfig`)**:
   - STOMP message broker configured with endpoint `/ws` (with native WebSocket support and SockJS fallback).
   - In-memory message broker with destination prefix `/topic`.
2. **WebSocket Handler (`SeatWebSocketHandler`)**:
   - Broadcasts real-time events to show-specific topics: `/topic/show/{showId}`.
   - Sends structured `SeatReleaseMessage` payloads (`showId`, `seatId`, `seatNumber`, `status: "AVAILABLE"`, `action: "SEAT_RELEASED"`, `timestamp`).
3. **Scheduled Expiration & Trigger (`SeatLockService`)**:
   - `@Scheduled(fixedRate = 60000)` background task runs every minute.
   - Queries all expired held seats via `seatRepository.findByStatusAndLockedUntilBefore(SeatStatus.LOCKED, now)`.
   - Atomically updates status to `AVAILABLE` and clears lock fields.
   - Iterates through the released seats and invokes `seatWebSocketHandler.broadcastSeatReleased(showId, seatId, seatNumber)` for each show topic.
4. **Frontend Real-time Subscription (`ShowtimePage.jsx`)**:
   - Subscribes to `/topic/show/${selectedShow.showId}` using `@stomp/stompjs` `Client`.
   - Shows a green **"Live Sync"** connection badge in the UI.
   - When a release notification is received, the seat's status transitions from `'LOCKED'` to `'AVAILABLE'`.
   - Automatically shifts the seat button's CSS color from **Locked (Yellow)** to **Available (Grey)** with an emerald pulse animation and notification banner.

---

## 🚀 How to Run

### Option 1: Run Full App via Spring Boot (Recommended)
The React frontend is built into Spring Boot's `src/main/resources/static`.
```powershell
cd online-theater-booking
.\mvnw.cmd spring-boot:run
```
Open **`http://localhost:8080`** in your browser.

### Option 2: Run Frontend Development Server (Hot Reload)
```powershell
cd frontend
npm run dev
```
Open **`http://localhost:5173`** in your browser (proxied to `:8080`).

### Running Backend Unit Tests
```powershell
cd online-theater-booking
.\mvnw.cmd test
```

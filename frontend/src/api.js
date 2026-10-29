/**
 * API client for Online Theater Booking System.
 * Connects to the Spring Boot REST backend endpoints.
 */

const API_BASE = '';

const DEFAULT_HEADERS = {
  'Content-Type': 'application/json',
};

function getAuthHeaders(token) {
  const headers = { ...DEFAULT_HEADERS };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

// Fallback mock movies for instant preview if backend is not yet populated
const FALLBACK_MOVIES = [
  {
    movieId: 1,
    title: 'Inception',
    rating: 8.8,
    duration: 148,
    aspectRatio: '2.39:1',
    posterUrl: 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=800&q=80',
    description: 'A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.'
  },
  {
    movieId: 2,
    title: 'Interstellar',
    rating: 8.7,
    duration: 169,
    aspectRatio: '1.90:1',
    posterUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80',
    description: 'When Earth becomes uninhabitable in the future, a farmer and ex-NASA pilot, Joseph Cooper, is tasked to pilot a spacecraft, along with a team of researchers, to find a new planet for humans.'
  },
  {
    movieId: 3,
    title: 'Oppenheimer',
    rating: 8.9,
    duration: 180,
    aspectRatio: '2.20:1',
    posterUrl: 'https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=800&q=80',
    description: 'The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb during World War II.'
  },
  {
    movieId: 4,
    title: 'Dune: Part Two',
    rating: 8.6,
    duration: 166,
    aspectRatio: '1.90:1',
    posterUrl: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=800&q=80',
    description: 'Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.'
  },
  {
    movieId: 5,
    title: 'The Dark Knight',
    rating: 9.0,
    duration: 152,
    aspectRatio: '2.39:1',
    posterUrl: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=800&q=80',
    description: 'When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.'
  },
  {
    movieId: 6,
    title: 'Spider-Man: Across the Spider-Verse',
    rating: 8.7,
    duration: 140,
    aspectRatio: '2.39:1',
    posterUrl: 'https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=800&q=80',
    description: 'Miles Morales catapults across the Multiverse, where he encounters a team of Spider-People charged with protecting its very existence.'
  }
];

export const api = {
  // Authentication
  auth: {
    async login(email, password) {
      const res = await fetch(`${API_BASE}/api/auth/login`, {
        method: 'POST',
        headers: DEFAULT_HEADERS,
        body: JSON.stringify({ email, password }),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Login failed. Please check your credentials.');
      }
      return data;
    },

    async signup(name, email, password) {
      const res = await fetch(`${API_BASE}/api/auth/signup`, {
        method: 'POST',
        headers: DEFAULT_HEADERS,
        body: JSON.stringify({ name, email, password }),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Signup failed. Please try again.');
      }
      return data;
    },

    async getCurrentUser(token) {
      const res = await fetch(`${API_BASE}/api/auth/me`, {
        headers: getAuthHeaders(token),
      });
      if (!res.ok) {
        throw new Error('Session expired or invalid token');
      }
      return res.json();
    },
  },

  // Movies
  movies: {
    async getAll(search = '') {
      try {
        const url = search ? `${API_BASE}/api/movies?search=${encodeURIComponent(search)}` : `${API_BASE}/api/movies`;
        const res = await fetch(url);
        if (!res.ok) throw new Error('Failed to fetch movies');
        const data = await res.json();
        return data && data.length > 0 ? data : FALLBACK_MOVIES;
      } catch (err) {
        console.warn('Backend unavailable, using fallback movie data:', err.message);
        if (search) {
          return FALLBACK_MOVIES.filter(m => m.title.toLowerCase().includes(search.toLowerCase()));
        }
        return FALLBACK_MOVIES;
      }
    },

    async getById(id) {
      try {
        const res = await fetch(`${API_BASE}/api/movies/${id}`);
        if (!res.ok) throw new Error('Movie not found');
        return await res.json();
      } catch (err) {
        const found = FALLBACK_MOVIES.find(m => m.movieId === Number(id));
        if (found) return found;
        throw err;
      }
    },

    async getRecommendedTheaters(movieId) {
      try {
        const res = await fetch(`${API_BASE}/api/movies/${movieId}/recommended-theaters`);
        if (!res.ok) throw new Error('Failed to fetch recommended theaters');
        return await res.json();
      } catch (err) {
        console.warn('Backend recommendation endpoint unavailable, using computed fallback:', err.message);
        return [
          {
            theaterId: 3,
            name: 'Starlight Laser Ultra',
            location: 'Westside Plaza, Level 2',
            screenSizeInches: 920.0,
            screenAspectRatio: '2.39:1',
            score: 96.0,
            screenSizeScore: 92.0,
            aspectRatioScore: 100.0,
            aspectRatioDifference: 0.0,
            matchQuality: 'Perfect Match',
            hasActiveShows: true,
            activeShowsCount: 2
          },
          {
            theaterId: 1,
            name: 'Grand IMAX Screen 1',
            location: 'Downtown Mall, 4th Floor',
            screenSizeInches: 840.0,
            screenAspectRatio: '1.90:1',
            score: 67.5,
            screenSizeScore: 84.0,
            aspectRatioScore: 51.0,
            aspectRatioDifference: 0.49,
            matchQuality: 'Good Match',
            hasActiveShows: true,
            activeShowsCount: 2
          },
          {
            theaterId: 2,
            name: 'Cineplex Screen 2',
            location: 'City Center, Main St',
            screenSizeInches: 600.0,
            screenAspectRatio: '2.39:1',
            score: 80.0,
            screenSizeScore: 60.0,
            aspectRatioScore: 100.0,
            aspectRatioDifference: 0.0,
            matchQuality: 'Perfect Match',
            hasActiveShows: false,
            activeShowsCount: 0
          }
        ];
      }
    },
  },

  // Shows
  shows: {
    async getByMovie(movieId) {
      try {
        const res = await fetch(`${API_BASE}/api/shows/movie/${movieId}`);
        if (!res.ok) throw new Error('Failed to fetch shows');
        const data = await res.json();
        if (data && data.length > 0) return data;
      } catch (err) {
        console.warn('Using fallback shows for movie:', movieId);
      }

      // Generate realistic fallback shows if backend has no records yet
      const now = new Date();
      return [
        {
          showId: movieId * 10 + 1,
          movieId: Number(movieId),
          movieTitle: 'Selected Movie',
          theaterId: 1,
          theaterName: 'Grand IMAX Screen 1',
          theaterLocation: 'Downtown Mall, 4th Floor',
          screenSizeInches: 840,
          screenAspectRatio: '1.90:1',
          startTime: new Date(now.getTime() + 2 * 3600 * 1000).toISOString(),
        },
        {
          showId: movieId * 10 + 2,
          movieId: Number(movieId),
          movieTitle: 'Selected Movie',
          theaterId: 1,
          theaterName: 'Grand IMAX Screen 1',
          theaterLocation: 'Downtown Mall, 4th Floor',
          screenSizeInches: 840,
          screenAspectRatio: '1.90:1',
          startTime: new Date(now.getTime() + 5 * 3600 * 1000).toISOString(),
        },
        {
          showId: movieId * 10 + 3,
          movieId: Number(movieId),
          theaterId: 2,
          theaterName: 'Cineplex Screen 2 (Dolby Atmos)',
          theaterLocation: 'City Center, Main St',
          screenSizeInches: 600,
          screenAspectRatio: '2.39:1',
          startTime: new Date(now.getTime() + 7 * 3600 * 1000).toISOString(),
        },
      ];
    },
  },

  // Seats & 10-Minute Lock
  seats: {
    async getSeats(showId) {
      try {
        const res = await fetch(`${API_BASE}/api/shows/${showId}/seats`);
        if (!res.ok) throw new Error('Failed to fetch seats');
        const data = await res.json();
        if (data && data.length > 0) return data;
      } catch (err) {
        console.warn('Using fallback seat map for show:', showId);
      }

      // Generate default seat grid (Rows A-D, seats 1-8)
      const rows = ['A', 'B', 'C', 'D'];
      const defaultSeats = [];
      let idCounter = 1;

      rows.forEach(row => {
        for (let num = 1; num <= 8; num++) {
          const seatNum = `${row}${num}`;
          let status = 'AVAILABLE';
          let lockedUntil = null;

          // Preset some realistic booked seats
          if (seatNum === 'A4' || seatNum === 'B5' || seatNum === 'C3') {
            status = 'BOOKED';
          }

          defaultSeats.push({
            seatId: idCounter++,
            seatNumber: seatNum,
            status,
            lockedUntil,
          });
        }
      });

      return defaultSeats;
    },

    async lockSeat(showId, seatNumber, userId) {
      const res = await fetch(`${API_BASE}/api/shows/${showId}/seats/${encodeURIComponent(seatNumber)}/lock?userId=${userId}`, {
        method: 'POST',
      });
      if (!res.ok) {
        const msg = await res.text();
        throw new Error(msg || 'Failed to lock seat');
      }
      return res.json();
    },

    async bookSeat(showId, seatNumber, userId) {
      const res = await fetch(`${API_BASE}/api/shows/${showId}/seats/${encodeURIComponent(seatNumber)}/book?userId=${userId}`, {
        method: 'POST',
      });
      if (!res.ok) {
        const msg = await res.text();
        throw new Error(msg || 'Failed to confirm booking');
      }
      return res.json();
    },
  },

  // Bookings
  bookings: {
    async getMyBookings(userId, token) {
      try {
        const res = await fetch(`${API_BASE}/api/bookings/my-bookings?userId=${userId}`, {
          headers: getAuthHeaders(token),
        });
        if (!res.ok) throw new Error('Failed to fetch bookings');
        return await res.json();
      } catch (err) {
        console.warn('Could not fetch bookings from server:', err.message);
        // Fallback to local stored bookings if any
        const local = localStorage.getItem('cine_local_bookings');
        return local ? JSON.parse(local) : [];
      }
    },

    async cancelBooking(bookingId, token) {
      const res = await fetch(`${API_BASE}/api/bookings/${bookingId}/cancel`, {
        method: 'DELETE',
        headers: getAuthHeaders(token),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Failed to cancel booking');
      }
      return data;
    },
  },
};

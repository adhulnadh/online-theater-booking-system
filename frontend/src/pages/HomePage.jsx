import React, { useState, useEffect } from 'react';
import { api } from '../api';
import { MovieCard } from '../components/MovieCard';
import { Search, Clapperboard, Sparkles, TrendingUp, AlertTriangle } from 'lucide-react';

export function HomePage({ onSelectMovie, onOpenAuth }) {
  const [movies, setMovies] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let isMounted = true;
    async function loadMovies() {
      setLoading(true);
      setError(null);
      try {
        const data = await api.movies.getAll(searchQuery);
        if (isMounted) setMovies(data);
      } catch (err) {
        if (isMounted) setError(err.message);
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    const timer = setTimeout(() => {
      loadMovies();
    }, 250);

    return () => {
      isMounted = false;
      clearTimeout(timer);
    };
  }, [searchQuery]);

  return (
    <div className="home-page">
      {/* Hero Section */}
      <section className="hero-banner">
        <div className="hero-content">
          <div className="hero-badge">
            <Sparkles size={16} className="sparkle-icon" />
            <span>Next-Gen Cinema Ticketing with 10-Min Live Seat Hold</span>
          </div>
          <h1 className="hero-title">
            Experience Cinema on the <span className="gradient-text">Biggest Screens</span>
          </h1>
          <p className="hero-description">
            Explore premier IMAX and widescreen releases. Lock your preferred seats instantly
            with our guaranteed 10-minute pessimistic lock before checkout.
          </p>

          {/* Search Box */}
          <div className="search-bar-wrapper">
            <Search className="search-icon" size={20} />
            <input
              type="text"
              className="search-input"
              placeholder="Search movies by title (e.g. Inception, Oppenheimer, Dune)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              aria-label="Search movies"
            />
            {searchQuery && (
              <button
                className="clear-search-btn"
                onClick={() => setSearchQuery('')}
                aria-label="Clear search"
              >
                ✕
              </button>
            )}
          </div>
        </div>
      </section>

      {/* Main Movie Catalog */}
      <section className="catalog-section">
        <div className="catalog-header">
          <div className="catalog-title-group">
            <div className="section-title-icon">
              <TrendingUp size={22} />
            </div>
            <div>
              <h2 className="section-title">Now Showing in Theaters</h2>
              <p className="section-subtitle">
                Click any movie to select theater, showtime, and reserve your seats
              </p>
            </div>
          </div>
          <div className="catalog-count-pill">
            <span>{movies.length} {movies.length === 1 ? 'Movie' : 'Movies'} Available</span>
          </div>
        </div>

        {/* Loading State */}
        {loading && (
          <div className="loading-grid">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="skeleton-card">
                <div className="skeleton-poster" />
                <div className="skeleton-text title" />
                <div className="skeleton-text meta" />
              </div>
            ))}
          </div>
        )}

        {/* Error State */}
        {error && !loading && (
          <div className="catalog-alert">
            <AlertTriangle size={24} />
            <div>
              <h4>Could not load live movies</h4>
              <p>{error}</p>
            </div>
          </div>
        )}

        {/* Empty Search Results */}
        {!loading && !error && movies.length === 0 && (
          <div className="empty-catalog-state">
            <Clapperboard size={48} className="empty-icon" />
            <h3>No movies found</h3>
            <p>We couldn't find any title matching "{searchQuery}". Try a different keyword!</p>
            <button className="btn btn-secondary" onClick={() => setSearchQuery('')}>
              Clear Search
            </button>
          </div>
        )}

        {/* Movies Grid */}
        {!loading && movies.length > 0 && (
          <div className="movie-grid">
            {movies.map((movie) => (
              <MovieCard
                key={movie.movieId}
                movie={movie}
                onSelectMovie={onSelectMovie}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

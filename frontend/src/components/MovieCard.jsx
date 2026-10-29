import React from 'react';
import { Star, Clock, Ratio, Ticket } from 'lucide-react';

export function MovieCard({ movie, onSelectMovie }) {
  const fallbackPoster = 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=600&q=80';

  const handleImageError = (e) => {
    e.target.src = fallbackPoster;
  };

  return (
    <article
      className="movie-card"
      onClick={() => onSelectMovie(movie)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onSelectMovie(movie);
        }
      }}
      aria-label={`Select showtimes for ${movie.title}`}
    >
      <div className="poster-container">
        <img
          src={movie.posterUrl || fallbackPoster}
          alt={movie.title}
          className="movie-poster"
          loading="lazy"
          onError={handleImageError}
        />
        <div className="poster-gradient-overlay" />

        {/* Rating Badge */}
        <div className="badge-rating" title="Audience Rating">
          <Star size={14} className="star-icon" fill="currentColor" />
          <span>{movie.rating ? movie.rating.toFixed(1) : '8.5'}</span>
        </div>

        {/* Aspect Ratio Badge */}
        {movie.aspectRatio && (
          <div className="badge-aspect" title="Cinema Aspect Ratio">
            <Ratio size={12} className="ratio-icon" />
            <span>{movie.aspectRatio}</span>
          </div>
        )}

        <div className="hover-cta-overlay">
          <span className="btn-select-showtime">
            <Ticket size={16} />
            <span>Select Showtimes</span>
          </span>
        </div>
      </div>

      <div className="movie-card-content">
        <h3 className="movie-title" title={movie.title}>
          {movie.title}
        </h3>

        <div className="movie-meta-row">
          <div className="meta-item duration-pill" title="Duration">
            <Clock size={13} className="meta-icon" />
            <span>{movie.duration} min</span>
          </div>
          <span className="meta-divider">•</span>
          <span className="meta-genre">In Theaters</span>
        </div>
      </div>
    </article>
  );
}

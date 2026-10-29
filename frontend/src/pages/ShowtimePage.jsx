import React, { useState, useEffect } from 'react';
import { api } from '../api';
import { useAuth } from '../context/AuthContext';
import { Client } from '@stomp/stompjs';
import {
  ArrowLeft,
  Calendar,
  Clock,
  Star,
  Tv,
  MapPin,
  ShieldAlert,
  CheckCircle2,
  Lock,
  Ticket,
  Maximize2,
  Sparkles
} from 'lucide-react';

export function ShowtimePage({ movie, onBack, onOpenAuth, onNavigate }) {
  const { user, isAuthenticated } = useAuth();

  const [shows, setShows] = useState([]);
  const [selectedShow, setSelectedShow] = useState(null);
  const [seats, setSeats] = useState([]);
  const [selectedSeat, setSelectedSeat] = useState(null);
  const [recommendedTheaters, setRecommendedTheaters] = useState([]);
  const [loadingShows, setLoadingShows] = useState(true);
  const [loadingSeats, setLoadingSeats] = useState(false);
  const [lockTimeRemaining, setLockTimeRemaining] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [bookingSuccess, setBookingSuccess] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [wsConnected, setWsConnected] = useState(false);
  const [recentReleasedSeatId, setRecentReleasedSeatId] = useState(null);
  const [releaseNotification, setReleaseNotification] = useState(null);

  // Load shows and recommended theaters for selected movie
  useEffect(() => {
    let isMounted = true;
    async function fetchShowsAndRecommendations() {
      setLoadingShows(true);
      try {
        const [showsData, recsData] = await Promise.all([
          api.shows.getByMovie(movie.movieId),
          api.movies.getRecommendedTheaters(movie.movieId)
        ]);

        if (isMounted) {
          setShows(showsData);
          setRecommendedTheaters(recsData);
          if (showsData && showsData.length > 0) {
            setSelectedShow(showsData[0]);
          }
        }
      } catch (err) {
        console.error('Error loading shows or recommendations:', err);
      } finally {
        if (isMounted) setLoadingShows(false);
      }
    }
    fetchShowsAndRecommendations();
    return () => {
      isMounted = false;
    };
  }, [movie.movieId]);

  // Load seats when selectedShow changes
  useEffect(() => {
    let isMounted = true;
    async function fetchSeats() {
      if (!selectedShow) return;
      setLoadingSeats(true);
      setActionError(null);
      setSelectedSeat(null);
      setLockTimeRemaining(null);

      try {
        const data = await api.seats.getSeats(selectedShow.showId);
        if (isMounted) setSeats(data);
      } catch (err) {
        console.error('Error loading seats:', err);
      } finally {
        if (isMounted) setLoadingSeats(false);
      }
    }
    fetchSeats();
    return () => {
      isMounted = false;
    };
  }, [selectedShow]);

  // Requirement 2 & 4: Subscribe to WebSocket topic specific to this Show ID (/topic/show/{showId})
  useEffect(() => {
    if (!selectedShow?.showId) return;

    const showId = selectedShow.showId;
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const brokerURL = `${protocol}//${window.location.host}/ws`;

    const stompClient = new Client({
      brokerURL,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setWsConnected(true);
        console.log(`[WebSocket] Connected. Subscribed to topic: /topic/show/${showId}`);

        stompClient.subscribe(`/topic/show/${showId}`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('[WebSocket] Received update:', data);

            // Requirement 4: When a message arrives (seat released),
            // update seat UI status from LOCKED to AVAILABLE (changes color from Yellow to Grey)
            if (data.action === 'SEAT_RELEASED' || data.status === 'AVAILABLE') {
              const releasedSeatId = data.seatId != null ? Number(data.seatId) : null;
              const releasedSeatNum = data.seatNumber;

              setSeats((prev) =>
                prev.map((s) => {
                  const isMatch =
                    (releasedSeatId != null && s.seatId === releasedSeatId) ||
                    (releasedSeatNum && s.seatNumber === releasedSeatNum);

                  if (isMatch) {
                    return {
                      ...s,
                      status: 'AVAILABLE',
                      lockedUntil: null,
                      user: null,
                    };
                  }
                  return s;
                })
              );

              // Flash highlight on newly available seat
              if (releasedSeatId != null) {
                setRecentReleasedSeatId(releasedSeatId);
                setTimeout(() => setRecentReleasedSeatId(null), 3500);
              }

              // Show friendly notification toast
              setReleaseNotification(
                `Seat ${releasedSeatNum || ''} was released from hold and is now available!`
              );
              setTimeout(() => setReleaseNotification(null), 5000);

              // If current user had this expired seat selected, clear selection
              setSelectedSeat((prev) => {
                if (
                  prev &&
                  ((releasedSeatId != null && prev.seatId === releasedSeatId) ||
                    (releasedSeatNum && prev.seatNumber === releasedSeatNum))
                ) {
                  setLockTimeRemaining(null);
                  return null;
                }
                return prev;
              });
            } else if (data.action === 'SEAT_LOCKED' || data.status === 'LOCKED') {
              // Real-time lock broadcast from other sessions
              const lockedSeatId = data.seatId != null ? Number(data.seatId) : null;
              const lockedSeatNum = data.seatNumber;

              setSeats((prev) =>
                prev.map((s) => {
                  const isMatch =
                    (lockedSeatId != null && s.seatId === lockedSeatId) ||
                    (lockedSeatNum && s.seatNumber === lockedSeatNum);

                  if (isMatch) {
                    return {
                      ...s,
                      status: 'LOCKED',
                      lockedUntil: data.lockedUntil || null,
                    };
                  }
                  return s;
                })
              );
            }
          } catch (err) {
            console.error('[WebSocket] Error parsing message payload:', err);
          }
        });
      },
      onDisconnect: () => {
        setWsConnected(false);
      },
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP broker error:', frame.headers['message'], frame.body);
      },
      onWebSocketClose: () => {
        setWsConnected(false);
      },
    });

    stompClient.activate();

    return () => {
      stompClient.deactivate();
      setWsConnected(false);
    };
  }, [selectedShow?.showId]);

  // Live countdown timer for locked seat
  useEffect(() => {
    if (lockTimeRemaining === null || lockTimeRemaining <= 0) return;

    const interval = setInterval(() => {
      setLockTimeRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          // Lock expired - refresh seats
          if (selectedShow) {
            api.seats.getSeats(selectedShow.showId).then(setSeats);
          }
          setSelectedSeat(null);
          setActionError('Your 10-minute seat lock has expired. Please select a seat again.');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [lockTimeRemaining, selectedShow]);

  // Lock seat action
  const handleSeatClick = async (seat) => {
    setActionError(null);

    if (seat.status === 'BOOKED') {
      setActionError(`Seat ${seat.seatNumber} is already booked.`);
      return;
    }

    if (seat.status === 'LOCKED' && (!selectedSeat || selectedSeat.seatNumber !== seat.seatNumber)) {
      setActionError(`Seat ${seat.seatNumber} is currently on a 10-minute hold by another guest.`);
      return;
    }

    // Require authentication to lock a seat
    if (!isAuthenticated) {
      setActionError('Please sign in to lock and reserve this seat.');
      onOpenAuth('login');
      return;
    }

    setIsProcessing(true);
    try {
      // Connect to backend seat lock endpoint
      const lockedSeatData = await api.seats.lockSeat(
        selectedShow.showId,
        seat.seatNumber,
        user.userId
      );

      setSelectedSeat(lockedSeatData);
      setLockTimeRemaining(10 * 60); // 10 minutes in seconds

      // Update seat in local grid
      setSeats((prev) =>
        prev.map((s) =>
          s.seatNumber === seat.seatNumber
            ? { ...s, status: 'LOCKED', user: user }
            : s
        )
      );
    } catch (err) {
      setActionError(err.message || 'Failed to lock seat');
    } finally {
      setIsProcessing(false);
    }
  };

  // Confirm booking action
  const handleConfirmBooking = async () => {
    if (!selectedSeat || !selectedShow || !user) return;

    setIsProcessing(true);
    setActionError(null);

    try {
      const bookedData = await api.seats.bookSeat(
        selectedShow.showId,
        selectedSeat.seatNumber,
        user.userId
      );

      // Save to local bookings list as client fallback
      const currentLocal = JSON.parse(localStorage.getItem('cine_local_bookings') || '[]');
      currentLocal.unshift({
        seatId: bookedData.seatId || Date.now(),
        seatNumber: selectedSeat.seatNumber,
        status: 'BOOKED',
        showId: selectedShow.showId,
        movieTitle: movie.title,
        posterUrl: movie.posterUrl,
        theaterName: selectedShow.theaterName,
        theaterLocation: selectedShow.theaterLocation,
        showStartTime: selectedShow.startTime,
      });
      localStorage.setItem('cine_local_bookings', JSON.stringify(currentLocal));

      setBookingSuccess({
        seatNumber: selectedSeat.seatNumber,
        movieTitle: movie.title,
        theaterName: selectedShow.theaterName,
        showTime: selectedShow.startTime,
      });

      // Update grid
      setSeats((prev) =>
        prev.map((s) =>
          s.seatNumber === selectedSeat.seatNumber ? { ...s, status: 'BOOKED' } : s
        )
      );
      setSelectedSeat(null);
      setLockTimeRemaining(null);
    } catch (err) {
      setActionError(err.message || 'Booking confirmation failed');
    } finally {
      setIsProcessing(false);
    }
  };

  const formatShowTime = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const formatShowDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString([], { weekday: 'short', month: 'short', day: 'numeric' });
  };

  const formatSeconds = (sec) => {
    const mins = Math.floor(sec / 60);
    const remainingSecs = sec % 60;
    return `${mins.toString().padStart(2, '0')}:${remainingSecs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="showtime-page">
      {/* Back button */}
      <div className="page-nav-bar">
        <button className="btn btn-ghost back-btn" onClick={onBack}>
          <ArrowLeft size={18} />
          <span>Back to Movies</span>
        </button>
      </div>

      {/* Movie Details Header Banner */}
      <section className="movie-hero-banner">
        <div className="movie-hero-container">
          <div className="movie-hero-poster-box">
            <img
              src={movie.posterUrl}
              alt={movie.title}
              className="movie-hero-poster"
            />
          </div>

          <div className="movie-hero-info">
            <div className="hero-tag-pills">
              <span className="badge-rating-lg">
                <Star size={16} fill="currentColor" />
                <span>{movie.rating ? movie.rating.toFixed(1) : '8.5'} / 10</span>
              </span>
              <span className="badge-pill">
                <Clock size={14} />
                <span>{movie.duration} minutes</span>
              </span>
              {movie.aspectRatio && (
                <span className="badge-pill">
                  <Maximize2 size={14} />
                  <span>Aspect: {movie.aspectRatio}</span>
                </span>
              )}
            </div>

            <h1 className="movie-hero-title">{movie.title}</h1>
            <p className="movie-hero-desc">
              {movie.description ||
                'Experience this blockbuster with immersive audio and crystal-clear laser projection. Select a theater and showtime below to begin seat selection.'}
            </p>
          </div>
        </div>
      </section>

      {/* Showtime Selection Section */}
      <div className="booking-layout-container">
        {/* Step 1: Choose Showtime */}
        <div className="showtime-picker-panel card-panel">
          <div className="panel-header">
            <Calendar size={20} className="panel-icon" />
            <h2 className="panel-title">1. Select Theater & Showtime</h2>
          </div>

          {loadingShows ? (
            <div className="panel-loading">Loading available showtimes...</div>
          ) : shows.length === 0 ? (
            <div className="panel-empty">No shows currently scheduled for this title.</div>
          ) : (
            <div className="shows-list">
              {shows.map((show) => {
                const isSelected = selectedShow?.showId === show.showId;
                const rec = recommendedTheaters.find((r) => r.theaterId === show.theaterId);

                return (
                  <div
                    key={show.showId}
                    className={`show-card-item ${isSelected ? 'selected' : ''}`}
                    onClick={() => setSelectedShow(show)}
                  >
                    <div className="show-header">
                      <div className="theater-name-row">
                        <Tv size={16} className="theater-icon" />
                        <span className="theater-name">{show.theaterName}</span>
                      </div>
                      <span className="show-time-badge">
                        {formatShowTime(show.startTime)}
                      </span>
                    </div>

                    <div className="theater-details-row">
                      <div className="theater-location">
                        <MapPin size={13} />
                        <span>{show.theaterLocation}</span>
                      </div>
                      {show.screenSizeInches && (
                        <span className="screen-spec-tag">
                          {show.screenSizeInches}" Screen • {show.screenAspectRatio}
                        </span>
                      )}
                    </div>

                    {rec && (
                      <div className="theater-rec-pill" title={`Screen Size Score: ${rec.screenSizeScore}%, Aspect Match Score: ${rec.aspectRatioScore}%`}>
                        <Sparkles size={12} />
                        <span>Recommendation Score: <strong>{rec.score}%</strong> • {rec.matchQuality}</span>
                      </div>
                    )}

                    <div className="show-date-tag">
                      <Calendar size={13} />
                      <span>{formatShowDate(show.startTime)}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Step 2: Interactive Seat Map */}
        <div className="seat-selection-panel card-panel">
          <div className="panel-header">
            <Ticket size={20} className="panel-icon" />
            <div className="panel-header-titles">
              <div className="panel-title-row">
                <h2 className="panel-title">2. Select Your Seat</h2>
                {selectedShow && (
                  <span
                    className={`ws-status-badge ${wsConnected ? 'connected' : 'connecting'}`}
                    title={
                      wsConnected
                        ? `Live seat release notifications active for show #${selectedShow.showId}`
                        : 'Connecting to live seat notifications...'
                    }
                  >
                    <span className={`ws-dot ${wsConnected ? 'live' : 'connecting'}`} />
                    <span>{wsConnected ? 'Live Sync' : 'Connecting'}</span>
                  </span>
                )}
              </div>
              <span className="panel-subtitle">
                {selectedShow
                  ? `${selectedShow.theaterName} • ${formatShowTime(selectedShow.startTime)}`
                  : 'Choose a showtime first'}
              </span>
            </div>
          </div>

          {/* Real-time Seat Release Notification Toast */}
          {releaseNotification && (
            <div className="seat-alert release-notification" role="status">
              <Sparkles size={18} className="notification-icon" />
              <span>{releaseNotification}</span>
            </div>
          )}

          {/* Alert / Notifications */}
          {actionError && (
            <div className="seat-alert error" role="alert">
              <ShieldAlert size={18} />
              <span>{actionError}</span>
            </div>
          )}

          {/* 10-Minute Lock Notification with Live Timer */}
          {lockTimeRemaining !== null && lockTimeRemaining > 0 && selectedSeat && (
            <div className="seat-lock-banner">
              <div className="lock-banner-left">
                <Lock size={18} className="lock-pulse-icon" />
                <div>
                  <strong>Seat {selectedSeat.seatNumber} is temporarily locked for you!</strong>
                  <p>Complete booking before the 10-minute reservation expires.</p>
                </div>
              </div>
              <div className="lock-timer-display">
                <Clock size={16} />
                <span className="countdown-text">{formatSeconds(lockTimeRemaining)}</span>
              </div>
            </div>
          )}

          {/* Cinema Screen Curve */}
          <div className="cinema-screen-area">
            <div className="screen-curve" />
            <span className="screen-label">ALL EYES ON SCREEN</span>
          </div>

          {/* Seat Grid */}
          {loadingSeats ? (
            <div className="seat-map-loading">Loading seat configuration...</div>
          ) : (
            <div className="seat-grid-container">
              <div className="seat-grid">
                {seats.map((seat) => {
                  const isSelected = selectedSeat?.seatNumber === seat.seatNumber;
                  const isLocked = seat.status === 'LOCKED';
                  const isBooked = seat.status === 'BOOKED';
                  const isAvailable = seat.status === 'AVAILABLE';
                  const isRecentlyReleased = recentReleasedSeatId === seat.seatId;

                  let seatClass = 'seat-btn available';
                  if (isSelected) seatClass = 'seat-btn selected';
                  else if (isBooked) seatClass = 'seat-btn booked';
                  else if (isLocked) seatClass = 'seat-btn locked';

                  if (isRecentlyReleased && isAvailable) {
                    seatClass += ' just-released';
                  }

                  return (
                    <button
                      key={seat.seatId}
                      className={seatClass}
                      onClick={() => handleSeatClick(seat)}
                      disabled={isBooked || isProcessing}
                      title={`Seat ${seat.seatNumber} (${seat.status})`}
                      aria-label={`Seat ${seat.seatNumber}, ${seat.status}`}
                    >
                      <span className="seat-number-label">{seat.seatNumber}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Seat Legend */}
          <div className="seat-legend">
            <div className="legend-item">
              <span className="legend-sample available" />
              <span>Available</span>
            </div>
            <div className="legend-item">
              <span className="legend-sample selected" />
              <span>Your Selection</span>
            </div>
            <div className="legend-item">
              <span className="legend-sample locked" />
              <span>Locked (10-Min Hold)</span>
            </div>
            <div className="legend-item">
              <span className="legend-sample booked" />
              <span>Booked</span>
            </div>
          </div>

          {/* Action Footer: Lock / Book Buttons */}
          <div className="seat-checkout-footer">
            <div className="selected-summary">
              {selectedSeat ? (
                <>
                  <span className="summary-label">Seat Selected:</span>
                  <span className="summary-value badge-seat">{selectedSeat.seatNumber}</span>
                  <span className="summary-price">$14.50</span>
                </>
              ) : (
                <span className="summary-placeholder">
                  Click on an available seat to lock it for 10 minutes.
                </span>
              )}
            </div>

            <div className="checkout-btn-group">
              {selectedSeat && (
                <button
                  className="btn btn-primary confirm-booking-btn"
                  onClick={handleConfirmBooking}
                  disabled={isProcessing || lockTimeRemaining === 0}
                >
                  <CheckCircle2 size={18} />
                  <span>Confirm & Book Seat {selectedSeat.seatNumber}</span>
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Booking Success Modal */}
      {bookingSuccess && (
        <div className="modal-backdrop">
          <div className="modal-container success-modal">
            <div className="success-icon-badge">
              <CheckCircle2 size={48} className="success-icon" />
            </div>
            <h2 className="modal-title">Booking Confirmed!</h2>
            <p className="modal-subtitle">
              Your ticket for <strong>{bookingSuccess.movieTitle}</strong> is secured.
            </p>

            <div className="ticket-receipt">
              <div className="receipt-row">
                <span>Movie:</span>
                <strong>{bookingSuccess.movieTitle}</strong>
              </div>
              <div className="receipt-row">
                <span>Theater:</span>
                <span>{bookingSuccess.theaterName}</span>
              </div>
              <div className="receipt-row">
                <span>Seat:</span>
                <span className="receipt-seat-badge">{bookingSuccess.seatNumber}</span>
              </div>
              <div className="receipt-row">
                <span>Showtime:</span>
                <span>{formatShowTime(bookingSuccess.showTime)} • {formatShowDate(bookingSuccess.showTime)}</span>
              </div>
            </div>

            <div className="receipt-actions">
              <button
                className="btn btn-primary"
                onClick={() => {
                  setBookingSuccess(null);
                  onNavigate('bookings');
                }}
              >
                <Ticket size={16} />
                <span>View My Bookings</span>
              </button>
              <button
                className="btn btn-outline"
                onClick={() => {
                  setBookingSuccess(null);
                  onBack();
                }}
              >
                Browse More Movies
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

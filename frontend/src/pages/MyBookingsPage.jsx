import React, { useState, useEffect } from 'react';
import { api } from '../api';
import { useAuth } from '../context/AuthContext';
import {
  Ticket,
  Calendar,
  MapPin,
  Tv,
  Film,
  ArrowLeft,
  XCircle,
  AlertTriangle,
  CheckCircle2,
  AlertCircle,
  Info
} from 'lucide-react';

export function MyBookingsPage({ onNavigate }) {
  const { user, token } = useAuth();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);

  // Cancellation modal state
  const [bookingToCancel, setBookingToCancel] = useState(null);
  const [isCancelling, setIsCancelling] = useState(false);
  const [cancelError, setCancelError] = useState(null);

  // Success toast notification
  const [toastMessage, setToastMessage] = useState(null);

  useEffect(() => {
    let isMounted = true;
    async function loadBookings() {
      if (!user) return;
      setLoading(true);
      try {
        const data = await api.bookings.getMyBookings(user.userId, token);
        if (isMounted) setBookings(data || []);
      } catch (err) {
        console.error('Failed to load user bookings:', err);
      } finally {
        if (isMounted) setLoading(false);
      }
    }
    loadBookings();
    return () => {
      isMounted = false;
    };
  }, [user, token]);

  const formatShowTime = (dateString) => {
    if (!dateString) return 'Upcoming';
    const date = new Date(dateString);
    return date.toLocaleString([], {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const handleOpenCancelModal = (booking) => {
    setCancelError(null);
    setBookingToCancel(booking);
  };

  const handleConfirmCancel = async () => {
    if (!bookingToCancel) return;

    setIsCancelling(true);
    setCancelError(null);

    try {
      const idToCancel = bookingToCancel.bookingId || bookingToCancel.seatId;
      await api.bookings.cancelBooking(idToCancel, token);

      // Remove from UI list
      setBookings((prev) =>
        prev.filter((b) => {
          if (bookingToCancel.bookingId && b.bookingId) {
            return b.bookingId !== bookingToCancel.bookingId;
          }
          return b.seatId !== bookingToCancel.seatId;
        })
      );

      // Also clean up local storage fallback if present
      const currentLocal = JSON.parse(localStorage.getItem('cine_local_bookings') || '[]');
      const updatedLocal = currentLocal.filter(
        (b) => b.seatNumber !== bookingToCancel.seatNumber
      );
      localStorage.setItem('cine_local_bookings', JSON.stringify(updatedLocal));

      const seatLabel = bookingToCancel.seatNumber || bookingToCancel.seatNumbers?.join(', ') || '';
      setToastMessage(`Booking for "${bookingToCancel.movieTitle}" (Seat ${seatLabel}) was cancelled successfully. Seat is now available!`);
      setBookingToCancel(null);

      setTimeout(() => {
        setToastMessage(null);
      }, 6000);

    } catch (err) {
      setCancelError(err.message || 'Failed to cancel booking. Please try again.');
    } finally {
      setIsCancelling(false);
    }
  };

  return (
    <div className="bookings-page">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="toast-notification success" role="status">
          <CheckCircle2 size={18} />
          <span>{toastMessage}</span>
          <button
            className="toast-close"
            onClick={() => setToastMessage(null)}
            aria-label="Close notification"
          >
            ×
          </button>
        </div>
      )}

      {/* Header */}
      <div className="bookings-header">
        <div className="bookings-title-group">
          <div className="icon-badge">
            <Ticket size={24} />
          </div>
          <div>
            <h1 className="page-title">My Bookings & Reservations</h1>
            <p className="page-subtitle">
              Manage your confirmed movie tickets and active seat holds for {user?.name || 'your account'}
            </p>
          </div>
        </div>

        <button className="btn btn-secondary" onClick={() => onNavigate('home')}>
          <ArrowLeft size={16} />
          <span>Browse Movies</span>
        </button>
      </div>

      {/* Content */}
      {loading ? (
        <div className="loading-state">
          <div className="spinner" />
          <p>Retrieving your reservations from CineReserve...</p>
        </div>
      ) : bookings.length === 0 ? (
        <div className="empty-bookings-card">
          <div className="empty-icon-wrapper">
            <Film size={48} className="empty-film-icon" />
          </div>
          <h3>No bookings found</h3>
          <p>You haven't reserved any theater seats yet. Explore currently showing movies to book tickets!</p>
          <button className="btn btn-primary" onClick={() => onNavigate('home')}>
            Explore Movies Now
          </button>
        </div>
      ) : (
        <div className="bookings-grid">
          {bookings.map((booking, idx) => {
            const isUpcoming =
              booking.showStartTime && new Date(booking.showStartTime) > new Date();
            const isCancelled = booking.status === 'CANCELLED';
            const canCancel =
              booking.canCancel !== false && isUpcoming && !isCancelled;
            const seatDisplay =
              booking.seatNumber ||
              (booking.seatNumbers && booking.seatNumbers.join(', ')) ||
              'N/A';

            return (
              <div
                key={booking.bookingId || booking.seatId || idx}
                className={`booking-ticket-card ${isCancelled ? 'cancelled-card' : ''}`}
              >
                <div className="ticket-poster-side">
                  <img
                    src={
                      booking.posterUrl ||
                      'https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=400&q=80'
                    }
                    alt={booking.movieTitle || 'Movie'}
                    className="ticket-poster-img"
                  />
                </div>

                <div className="ticket-body">
                  <div className="ticket-top-row">
                    <span
                      className={`status-badge ${
                        isCancelled
                          ? 'cancelled'
                          : booking.status?.toLowerCase() || 'confirmed'
                      }`}
                    >
                      {booking.status || 'CONFIRMED'}
                    </span>
                    <div className="ticket-seat-pill">
                      Seat: <strong>{seatDisplay}</strong>
                    </div>
                  </div>

                  <h3 className="ticket-movie-title">
                    {booking.movieTitle || 'Movie Reservation'}
                  </h3>

                  <div className="ticket-details-list">
                    <div className="ticket-detail-item">
                      <Tv size={14} className="detail-icon" />
                      <span>{booking.theaterName || 'Grand Cinema Screen 1'}</span>
                    </div>

                    <div className="ticket-detail-item">
                      <MapPin size={14} className="detail-icon" />
                      <span>{booking.theaterLocation || 'Downtown Cineplex'}</span>
                    </div>

                    <div className="ticket-detail-item">
                      <Calendar size={14} className="detail-icon" />
                      <span>{formatShowTime(booking.showStartTime)}</span>
                    </div>
                  </div>

                  {/* Actions & Stub Footer */}
                  <div className="ticket-actions-row">
                    <span className="booking-ref">
                      {booking.bookingId
                        ? `BOOKING-#${booking.bookingId}`
                        : `REF-SEAT-${seatDisplay}`}
                    </span>

                    {/* Requirement 3: Cancel Booking button next to each upcoming booking */}
                    {canCancel && (
                      <button
                        className="btn-outline-danger btn-sm cancel-booking-btn"
                        onClick={() => handleOpenCancelModal(booking)}
                        title="Cancel this booking and release seats"
                      >
                        <XCircle size={15} />
                        <span>Cancel Booking</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Requirement 4: Confirmation Modal */}
      {bookingToCancel && (
        <div
          className="modal-backdrop"
          onClick={() => !isCancelling && setBookingToCancel(null)}
          role="dialog"
          aria-modal="true"
        >
          <div className="modal-container cancel-modal" onClick={(e) => e.stopPropagation()}>
            <div className="cancel-modal-header">
              <div className="alert-badge-warning">
                <AlertTriangle size={32} />
              </div>
              <h2>Cancel Reservation?</h2>
              <p>
                Are you sure you want to cancel your reservation for{' '}
                <strong>{bookingToCancel.movieTitle}</strong>?
              </p>
            </div>

            <div className="cancel-booking-summary">
              <div className="summary-row">
                <span>Theater:</span>
                <strong>{bookingToCancel.theaterName}</strong>
              </div>
              <div className="summary-row">
                <span>Showtime:</span>
                <span>{formatShowTime(bookingToCancel.showStartTime)}</span>
              </div>
              <div className="summary-row">
                <span>Seat(s):</span>
                <strong className="badge-seat-highlight">
                  {bookingToCancel.seatNumber ||
                    bookingToCancel.seatNumbers?.join(', ')}
                </strong>
              </div>
            </div>

            <div className="cancel-notice">
              <Info size={16} className="notice-icon" />
              <span>
                Your seats will be immediately released back to <strong>AVAILABLE</strong> for other
                guests in real time via live WebSocket broadcast.
              </span>
            </div>

            {cancelError && (
              <div className="auth-alert error-alert" role="alert">
                <AlertCircle size={16} />
                <span>{cancelError}</span>
              </div>
            )}

            <div className="modal-actions-group">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setBookingToCancel(null)}
                disabled={isCancelling}
              >
                Keep Booking
              </button>
              <button
                type="button"
                className="btn btn-danger confirm-cancel-btn"
                onClick={handleConfirmCancel}
                disabled={isCancelling}
              >
                {isCancelling ? 'Cancelling...' : 'Yes, Cancel Booking'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

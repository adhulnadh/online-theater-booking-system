import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Film, Ticket, User, LogOut, LogIn, Sparkles } from 'lucide-react';

export function Navbar({ onNavigate, currentPage, onOpenAuth }) {
  const { user, isAuthenticated, logout } = useAuth();

  return (
    <header className="navbar">
      <div className="navbar-container">
        {/* Brand Logo */}
        <button
          className="brand-logo-btn"
          onClick={() => onNavigate('home')}
          aria-label="CineReserve Home"
        >
          <div className="brand-icon-wrapper">
            <Film className="brand-icon" size={24} />
          </div>
          <span className="brand-name">
            Cine<span className="brand-accent">Reserve</span>
          </span>
        </button>

        {/* Navigation Links */}
        <nav className="nav-links">
          <button
            className={`nav-link-btn ${currentPage === 'home' ? 'active' : ''}`}
            onClick={() => onNavigate('home')}
          >
            Movies
          </button>

          {/* Conditional Rendering: Show "My Bookings" ONLY if authenticated */}
          {isAuthenticated && (
            <button
              className={`nav-link-btn ${currentPage === 'bookings' ? 'active' : ''}`}
              onClick={() => onNavigate('bookings')}
            >
              <Ticket size={18} className="btn-icon" />
              <span>My Bookings</span>
            </button>
          )}
        </nav>

        {/* Right Action: Conditional Auth rendering */}
        <div className="navbar-actions">
          {isAuthenticated ? (
            <div className="user-profile-menu">
              <div className="user-badge" title={user?.email}>
                <div className="user-avatar">
                  {user?.name ? user.name.charAt(0).toUpperCase() : <User size={16} />}
                </div>
                <div className="user-info">
                  <span className="user-name">{user?.name || 'User'}</span>
                  <span className="user-role">{user?.role || 'USER'}</span>
                </div>
              </div>
              <button
                className="btn btn-secondary btn-sm logout-btn"
                onClick={logout}
                title="Log out"
              >
                <LogOut size={16} />
                <span className="logout-text">Logout</span>
              </button>
            </div>
          ) : (
            /* Conditional Rendering: Show "Login" button if NOT authenticated */
            <div className="auth-actions">
              <button
                className="btn btn-primary btn-sm login-nav-btn"
                onClick={() => onOpenAuth('login')}
              >
                <LogIn size={16} className="btn-icon" />
                <span>Login</span>
              </button>
              <button
                className="btn btn-outline btn-sm signup-nav-btn"
                onClick={() => onOpenAuth('signup')}
              >
                <Sparkles size={16} className="btn-icon" />
                <span>Sign Up</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

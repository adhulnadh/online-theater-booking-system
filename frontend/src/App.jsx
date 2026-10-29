import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { AuthModal } from './components/AuthModal';
import { HomePage } from './pages/HomePage';
import { ShowtimePage } from './pages/ShowtimePage';
import { MyBookingsPage } from './pages/MyBookingsPage';
import { Film, Shield, Database, Cpu } from 'lucide-react';

function MainApp() {
  const { isAuthenticated } = useAuth();

  const getInitialPage = () => {
    if (typeof window !== 'undefined' && window.location.pathname === '/my-bookings') {
      return 'bookings';
    }
    return 'home';
  };

  const [currentPage, setCurrentPage] = useState(getInitialPage); // 'home' | 'showtime' | 'bookings'
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authModalMode, setAuthModalMode] = useState('login');

  useEffect(() => {
    const handlePopState = () => {
      if (window.location.pathname === '/my-bookings') {
        setCurrentPage('bookings');
      } else {
        setCurrentPage('home');
      }
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const handleOpenAuth = (mode = 'login') => {
    setAuthModalMode(mode);
    setAuthModalOpen(true);
  };

  const handleSelectMovie = (movie) => {
    setSelectedMovie(movie);
    setCurrentPage('showtime');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleNavigate = (page) => {
    if (page === 'bookings') {
      if (!isAuthenticated) {
        handleOpenAuth('login');
        return;
      }
      if (window.location.pathname !== '/my-bookings') {
        window.history.pushState(null, '', '/my-bookings');
      }
    } else if (page === 'home') {
      if (window.location.pathname !== '/') {
        window.history.pushState(null, '', '/');
      }
    }
    setCurrentPage(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="app-shell">
      {/* Top Navbar with Conditional Rendering */}
      <Navbar
        currentPage={currentPage}
        onNavigate={handleNavigate}
        onOpenAuth={handleOpenAuth}
      />

      {/* Main Content Area */}
      <main className="main-content">
        {currentPage === 'home' && (
          <HomePage
            onSelectMovie={handleSelectMovie}
            onOpenAuth={handleOpenAuth}
          />
        )}

        {currentPage === 'showtime' && selectedMovie && (
          <ShowtimePage
            movie={selectedMovie}
            onBack={() => setCurrentPage('home')}
            onOpenAuth={handleOpenAuth}
            onNavigate={handleNavigate}
          />
        )}

        {currentPage === 'bookings' && (
          <MyBookingsPage onNavigate={handleNavigate} />
        )}
      </main>

      {/* Authentication Modal */}
      <AuthModal
        isOpen={authModalOpen}
        initialMode={authModalMode}
        onClose={() => setAuthModalOpen(false)}
      />

      {/* Footer */}
      <footer className="app-footer">
        <div className="footer-container">
          <div className="footer-brand-side">
            <div className="footer-logo">
              <Film size={20} className="brand-icon" />
              <span>Cine<strong className="brand-accent">Reserve</strong></span>
            </div>
            <p className="footer-tagline">
              Online Theater Booking System • Java 21 & Spring Boot REST Backend • MySQL Persistence • Pessimistic 10-Minute Seat Locking
            </p>
          </div>

          <div className="footer-stack-pills">
            <span className="stack-pill"><Cpu size={14} /> Java 21</span>
            <span className="stack-pill"><Shield size={14} /> Spring Security (JWT)</span>
            <span className="stack-pill"><Database size={14} /> MySQL Data JPA</span>
            <span className="stack-pill">React + Vite</span>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}

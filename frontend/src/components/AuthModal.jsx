import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { X, Lock, Mail, User, AlertCircle, CheckCircle, ArrowRight, Sparkles } from 'lucide-react';

export function AuthModal({ isOpen, onClose, initialMode = 'login', onSuccess }) {
  const [mode, setMode] = useState(initialMode); // 'login' or 'signup'
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMsg, setSuccessMsg] = useState(null);

  const { login, signup } = useAuth();

  useEffect(() => {
    setMode(initialMode);
    setError(null);
    setSuccessMsg(null);
  }, [initialMode, isOpen]);

  // Handle escape key
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);
    setIsSubmitting(true);

    try {
      if (mode === 'signup') {
        if (!name.trim()) throw new Error('Please enter your full name');
        if (!email.trim()) throw new Error('Please enter your email address');
        if (password.length < 6) throw new Error('Password must be at least 6 characters');

        await signup(name, email, password);
        setSuccessMsg('Account created successfully! Welcome to CineReserve.');
      } else {
        if (!email.trim() || !password) throw new Error('Please enter both email and password');
        await login(email, password);
        setSuccessMsg('Signed in successfully!');
      }

      setTimeout(() => {
        setIsSubmitting(false);
        if (onSuccess) onSuccess();
        onClose();
      }, 700);
    } catch (err) {
      setIsSubmitting(false);
      setError(err.message || 'Authentication request failed');
    }
  };

  const handleDemoFill = (demoEmail, demoPass) => {
    setEmail(demoEmail);
    setPassword(demoPass);
    setError(null);
  };

  return (
    <div className="modal-backdrop" onClick={onClose} role="dialog" aria-modal="true">
      <div className="modal-container" onClick={(e) => e.stopPropagation()}>
        {/* Close Button */}
        <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
          <X size={20} />
        </button>

        {/* Modal Header */}
        <div className="modal-header">
          <div className="modal-icon-badge">
            <Lock size={22} className="modal-icon" />
          </div>
          <h2 className="modal-title">
            {mode === 'login' ? 'Welcome Back' : 'Create an Account'}
          </h2>
          <p className="modal-subtitle">
            {mode === 'login'
              ? 'Sign in to manage your tickets, book seats, and track showtimes.'
              : 'Join CineReserve to lock seats for 10 minutes and enjoy priority booking.'}
          </p>
        </div>

        {/* Mode Switch Tabs */}
        <div className="tab-pill-group">
          <button
            type="button"
            className={`tab-pill ${mode === 'login' ? 'active' : ''}`}
            onClick={() => {
              setMode('login');
              setError(null);
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            className={`tab-pill ${mode === 'signup' ? 'active' : ''}`}
            onClick={() => {
              setMode('signup');
              setError(null);
            }}
          >
            Sign Up
          </button>
        </div>

        {/* Status Messages */}
        {error && (
          <div className="auth-alert error-alert" role="alert">
            <AlertCircle size={18} className="alert-icon" />
            <span>{error}</span>
          </div>
        )}

        {successMsg && (
          <div className="auth-alert success-alert" role="status">
            <CheckCircle size={18} className="alert-icon" />
            <span>{successMsg}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          {mode === 'signup' && (
            <div className="form-group">
              <label htmlFor="auth-name">Full Name</label>
              <div className="input-wrapper">
                <User size={18} className="input-icon" />
                <input
                  id="auth-name"
                  type="text"
                  placeholder="e.g. John Doe"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  autoComplete="name"
                  disabled={isSubmitting}
                />
              </div>
            </div>
          )}

          <div className="form-group">
            <label htmlFor="auth-email">Email Address</label>
            <div className="input-wrapper">
              <Mail size={18} className="input-icon" />
              <input
                id="auth-email"
                type="email"
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                disabled={isSubmitting}
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="auth-password">Password</label>
            <div className="input-wrapper">
              <Lock size={18} className="input-icon" />
              <input
                id="auth-password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                disabled={isSubmitting}
              />
            </div>
            {mode === 'signup' && (
              <span className="field-hint">Must be at least 6 characters long</span>
            )}
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-block submit-auth-btn"
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <span className="spinner-text">Processing...</span>
            ) : (
              <>
                <span>{mode === 'login' ? 'Sign In to Account' : 'Create Account'}</span>
                <ArrowRight size={18} className="btn-icon-right" />
              </>
            )}
          </button>
        </form>

        {/* Demo Fast-Fill helper */}
        <div className="demo-credentials-box">
          <div className="demo-label">
            <Sparkles size={14} />
            <span>Demo Test Accounts (Backend Seed Data):</span>
          </div>
          <div className="demo-buttons">
            <button
              type="button"
              className="demo-btn"
              onClick={() => handleDemoFill('admin@example.com', 'admin123')}
            >
              Fill Admin
            </button>
            <button
              type="button"
              className="demo-btn"
              onClick={() => handleDemoFill('john@example.com', 'john123')}
            >
              Fill User
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

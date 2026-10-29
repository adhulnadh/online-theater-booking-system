import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('cine_user');
    return saved ? JSON.parse(saved) : null;
  });

  const [token, setToken] = useState(() => {
    return localStorage.getItem('cine_token') || null;
  });

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function verifySession() {
      if (token) {
        try {
          const res = await api.auth.getCurrentUser(token);
          if (res && res.user) {
            setUser(res.user);
            localStorage.setItem('cine_user', JSON.stringify(res.user));
          }
        } catch (err) {
          console.warn('Session verification notice:', err.message);
          // Keep existing cached user if offline, or logout if explicit unauthorized
          if (err.message.includes('expired') || err.message.includes('invalid')) {
            logout();
          }
        }
      }
      setLoading(false);
    }
    verifySession();
  }, [token]);

  const login = async (email, password) => {
    const res = await api.auth.login(email, password);
    setToken(res.token);
    setUser(res.user);
    localStorage.setItem('cine_token', res.token);
    localStorage.setItem('cine_user', JSON.stringify(res.user));
    return res;
  };

  const signup = async (name, email, password) => {
    const res = await api.auth.signup(name, email, password);
    setToken(res.token);
    setUser(res.user);
    localStorage.setItem('cine_token', res.token);
    localStorage.setItem('cine_user', JSON.stringify(res.user));
    return res;
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('cine_token');
    localStorage.removeItem('cine_user');
  };

  const value = {
    user,
    token,
    isAuthenticated: Boolean(user && token),
    loading,
    login,
    signup,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

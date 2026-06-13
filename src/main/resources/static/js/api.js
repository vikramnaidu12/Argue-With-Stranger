/**
 * api.js — Centralized HTTP client for all REST API calls.
 *
 * All fetch calls go through this module.
 * Automatically attaches the JWT Bearer token to every request.
 * Handles 401 responses by redirecting to login.
 */

const API_BASE = window.location.origin;

// ── Auth helpers ───────────────────────────────────────────────

function getToken()            { return localStorage.getItem('token'); }
function getUser()             { return JSON.parse(localStorage.getItem('user') || 'null'); }
function isLoggedIn()          { return !!getToken(); }

function saveAuth(data) {
  localStorage.setItem('token', data.token);
  localStorage.setItem('user', JSON.stringify({
    id:       data.userId,
    username: data.username,
    email:    data.email,
    role:     data.role
  }));
}

function clearAuth() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
}

function logout() {
  clearAuth();
  window.location.href = '/login.html';
}

// ── Core fetch wrapper ─────────────────────────────────────────

async function apiFetch(path, options = {}) {
  const token = getToken();

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...(options.headers || {})
  };

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  });

  // Auto-logout on 401
  if (response.status === 401) {
    clearAuth();
    window.location.href = '/login.html';
    return;
  }

  // Parse JSON for all responses
  const data = response.ok
    ? await response.json()
    : await response.json().catch(() => ({ message: 'Unknown error' }));

  if (!response.ok) {
    const error = new Error(data.message || 'Request failed');
    error.data   = data;
    error.status = response.status;
    throw error;
  }

  return data;
}

// ── Auth API ───────────────────────────────────────────────────

const AuthAPI = {
  register: (body) => apiFetch('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  login:    (body) => apiFetch('/auth/login',    { method: 'POST', body: JSON.stringify(body) })
};

// ── Debate API ─────────────────────────────────────────────────

const DebateAPI = {
  getAll:    (page = 0, size = 10) => apiFetch(`/debates?page=${page}&size=${size}`),
  getById:   (id)                  => apiFetch(`/debates/${id}`),
  getByStatus:(status, page = 0, size = 10) =>
    apiFetch(`/debates/status/${status}?page=${page}&size=${size}`),
  search:    (keyword, page = 0, size = 10) =>
    apiFetch(`/debates/search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`),
  create:    (body) => apiFetch('/debates',         { method: 'POST', body: JSON.stringify(body) }),
  join:      (id, side) => apiFetch(`/debates/${id}/join`, {
    method: 'POST',
    body: JSON.stringify({ side })
  }),
  end:       (id) => apiFetch(`/debates/${id}/end`, { method: 'POST' }),
  myDebates: ()   => apiFetch('/debates/my')
};

// ── Message API ────────────────────────────────────────────────

const MessageAPI = {
  getHistory: (debateId) => apiFetch(`/debates/${debateId}/messages`)
};

// ── Vote API ───────────────────────────────────────────────────

const VoteAPI = {
  castVote:  (debateId, selectedSide) => apiFetch(`/debates/${debateId}/vote`, {
    method: 'POST',
    body: JSON.stringify({ selectedSide })
  }),
  getResult: (debateId) => apiFetch(`/debates/${debateId}/result`)
};

// ── User API ───────────────────────────────────────────────────

const UserAPI = {
  getProfile:     ()         => apiFetch('/users/profile'),
  getLeaderboard: ()         => apiFetch('/users/leaderboard'),
  getUserProfile: (username) => apiFetch(`/users/${username}`)
};

// ── Guard: redirect to login if not authenticated ──────────────

function requireAuth() {
  if (!isLoggedIn()) {
    window.location.href = '/login.html';
  }
}
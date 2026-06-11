/**
 * auth.js — Handles login and registration form logic.
 * Used by both login.html and register.html.
 */

// Redirect to home if already logged in
if (isLoggedIn()) {
  window.location.href = '/index.html';
}

// ── Shared helpers ─────────────────────────────────────────────

function showError(msg) {
  const banner = document.getElementById('error-banner');
  if (banner) {
    banner.textContent = msg;
    banner.classList.remove('hidden');
  }
}

function hideError() {
  const banner = document.getElementById('error-banner');
  if (banner) banner.classList.add('hidden');
}

function setFieldError(fieldId, msg) {
  const el = document.getElementById(`${fieldId}-error`);
  const input = document.getElementById(fieldId);
  if (el)    el.textContent = msg;
  if (input) input.classList.toggle('invalid', !!msg);
}

function clearFieldErrors() {
  document.querySelectorAll('.field-error').forEach(el => el.textContent = '');
  document.querySelectorAll('input').forEach(el => el.classList.remove('invalid'));
  hideError();
}

function setLoading(btnId, loading) {
  const btn = document.getElementById(btnId);
  if (btn) {
    btn.disabled    = loading;
    btn.textContent = loading ? 'Please wait...' : btn.dataset.label;
  }
}

// Store original button labels
document.querySelectorAll('button[id$="-btn"]').forEach(btn => {
  btn.dataset.label = btn.textContent;
});

// ── Login form ─────────────────────────────────────────────────

const loginForm = document.getElementById('login-form');
if (loginForm) {
  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearFieldErrors();

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    let valid = true;
    if (!username) { setFieldError('username', 'Username is required'); valid = false; }
    if (!password) { setFieldError('password', 'Password is required'); valid = false; }
    if (!valid) return;

    setLoading('login-btn', true);

    try {
      const data = await AuthAPI.login({ username, password });
      saveAuth(data);
      window.location.href = '/index.html';
    } catch (err) {
      showError(err.message || 'Login failed. Please try again.');

      // Show field-level errors from server validation
      if (err.data?.validationErrors) {
        Object.entries(err.data.validationErrors).forEach(([field, msg]) => {
          setFieldError(field, msg);
        });
      }
    } finally {
      setLoading('login-btn', false);
    }
  });
}

// ── Register form ──────────────────────────────────────────────

const registerForm = document.getElementById('register-form');
if (registerForm) {
  registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearFieldErrors();

    const username = document.getElementById('username').value.trim();
    const email    = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    let valid = true;
    if (!username) { setFieldError('username', 'Username is required'); valid = false; }
    if (!email)    { setFieldError('email',    'Email is required');    valid = false; }
    if (!password) { setFieldError('password', 'Password is required'); valid = false; }
    if (password && password.length < 6) {
      setFieldError('password', 'Password must be at least 6 characters');
      valid = false;
    }
    if (!valid) return;

    setLoading('register-btn', true);

    try {
      const data = await AuthAPI.register({ username, email, password });
      saveAuth(data);
      window.location.href = '/index.html';
    } catch (err) {
      showError(err.message || 'Registration failed. Please try again.');
      if (err.data?.validationErrors) {
        Object.entries(err.data.validationErrors).forEach(([field, msg]) => {
          setFieldError(field, msg);
        });
      }
    } finally {
      setLoading('register-btn', false);
    }
  });
}
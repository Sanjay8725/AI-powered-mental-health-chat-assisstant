const express = require('express');
const router = express.Router();
const { supabase } = require('../config/supabase');
const { requireAuth } = require('../middleware/authMiddleware');

/**
 * 1. POST /api/auth/register
 * User registration with Supabase Auth & email confirmation trigger
 */
router.post('/register', async (req, res) => {
  try {
    const { email, password, name } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        error: 'Email and password are required.'
      });
    }

    if (password.length < 6) {
      return res.status(400).json({
        success: false,
        error: 'Password must be at least 6 characters.'
      });
    }

    const { data, error } = await supabase.auth.signUp({
      email: email.trim(),
      password: password.trim(),
      options: {
        data: {
          display_name: name?.trim() || email.split('@')[0]
        }
      }
    });

    if (error) {
      return res.status(400).json({ success: false, error: error.message });
    }

    return res.status(201).json({
      success: true,
      message: 'User registered successfully. Check email for verification if enabled.',
      user: {
        id: data.user?.id,
        email: data.user?.email,
        name: data.user?.user_metadata?.display_name
      },
      session: data.session
    });
  } catch (err) {
    return res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * 2. POST /api/auth/login
 * User login using Supabase Auth credentials (returns JWT token and session)
 */
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        error: 'Email and password are required.'
      });
    }

    const { data, error } = await supabase.auth.signInWithPassword({
      email: email.trim(),
      password: password.trim()
    });

    if (error) {
      return res.status(401).json({ success: false, error: error.message });
    }

    return res.json({
      success: true,
      token: data.session.access_token,
      refreshToken: data.session.refresh_token,
      expiresAt: data.session.expires_at,
      user: {
        id: data.user.id,
        email: data.user.email,
        name: data.user.user_metadata?.display_name || data.user.email.split('@')[0]
      }
    });
  } catch (err) {
    return res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * 3. POST /api/auth/logout
 * Invalidate session token
 */
router.post('/logout', requireAuth, async (req, res) => {
  try {
    const { error } = await supabase.auth.admin.signOut(req.token);
    return res.json({
      success: true,
      message: 'Signed out successfully'
    });
  } catch (err) {
    return res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * 4. POST /api/auth/password-reset
 * Initiates Supabase password recovery email
 */
router.post('/password-reset', async (req, res) => {
  try {
    const { email } = req.body;
    if (!email) {
      return res.status(400).json({ success: false, error: 'Email is required.' });
    }

    const { data, error } = await supabase.auth.resetPasswordForEmail(email.trim(), {
      redirectTo: process.env.CLIENT_URL || 'https://mindcare.ai/reset-password'
    });

    if (error) {
      return res.status(400).json({ success: false, error: error.message });
    }

    return res.json({
      success: true,
      message: `Password reset instructions sent to ${email}`
    });
  } catch (err) {
    return res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * 5. GET /api/auth/verify-email
 * Verifies email confirmation token or OTP
 */
router.get('/verify-email', async (req, res) => {
  try {
    const { token_hash, type } = req.query;

    if (!token_hash || !type) {
      return res.status(400).json({
        success: false,
        error: 'Missing token_hash or verification type query parameter'
      });
    }

    const { data, error } = await supabase.auth.verifyOtp({
      token_hash,
      type
    });

    if (error) {
      return res.status(400).json({ success: false, error: error.message });
    }

    return res.json({
      success: true,
      message: 'Email verified successfully',
      user: data.user
    });
  } catch (err) {
    return res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * 6. GET /api/auth/me
 * Current authenticated user profile
 */
router.get('/me', requireAuth, async (req, res) => {
  return res.json({
    success: true,
    user: {
      id: req.user.id,
      email: req.user.email,
      name: req.user.user_metadata?.display_name,
      createdAt: req.user.created_at
    }
  });
});

module.exports = router;

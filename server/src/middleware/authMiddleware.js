const { supabase } = require('../config/supabase');

/**
 * Middleware to authenticate requests using Supabase Auth JWT token
 */
async function requireAuth(req, res, next) {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        error: 'Authorization header missing or invalid. Format: Bearer <token>'
      });
    }

    const token = authHeader.split(' ')[1];

    // Verify token with Supabase Auth
    const { data: { user }, error } = await supabase.auth.getUser(token);

    if (error || !user) {
      return res.status(401).json({
        success: false,
        error: error?.message || 'Invalid or expired authentication session'
      });
    }

    // Attach user payload to request
    req.user = user;
    req.token = token;
    next();
  } catch (err) {
    return res.status(500).json({
      success: false,
      error: 'Authentication verification failure: ' + err.message
    });
  }
}

module.exports = { requireAuth };

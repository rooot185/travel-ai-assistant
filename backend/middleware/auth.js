const jwt = require('jsonwebtoken');
const db = require('../database/init');

const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

  if (!token) {
    return res.status(401).json({
      error: 'Access token required',
      message: 'Please provide a valid authentication token'
    });
  }

  jwt.verify(token, process.env.JWT_SECRET, (err, decoded) => {
    if (err) {
      return res.status(403).json({
        error: 'Invalid token',
        message: 'Token is invalid or expired'
      });
    }

    // Verify user still exists in database
    db.get('SELECT id, username, email FROM users WHERE id = ?', [decoded.userId], (err, user) => {
      if (err || !user) {
        return res.status(403).json({
          error: 'User not found',
          message: 'User associated with this token no longer exists'
        });
      }

      req.user = user;
      next();
    });
  });
};

const optionalAuth = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return next();
  }

  jwt.verify(token, process.env.JWT_SECRET, (err, decoded) => {
    if (err) {
      return next();
    }

    db.get('SELECT id, username, email FROM users WHERE id = ?', [decoded.userId], (err, user) => {
      if (!err && user) {
        req.user = user;
      }
      next();
    });
  });
};

module.exports = {
  authenticateToken,
  optionalAuth
};
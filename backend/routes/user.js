const express = require('express');
const db = require('../database/init');
const { authenticateToken } = require('../middleware/auth');

const router = express.Router();

// Get user profile
router.get('/profile', authenticateToken, (req, res) => {
  try {
    const userId = req.user.id;

    db.get(
      `SELECT id, username, email, created_at, updated_at
       FROM users WHERE id = ?`,
      [userId],
      (err, user) => {
        if (err) {
          console.error('Error fetching user profile:', err);
          return res.status(500).json({
            error: 'Failed to fetch user profile',
            message: 'Could not retrieve your profile information'
          });
        }

        if (!user) {
          return res.status(404).json({
            error: 'User not found',
            message: 'User profile not found'
          });
        }

        res.json({
          id: user.id,
          username: user.username,
          email: user.email,
          createdAt: user.created_at,
          updatedAt: user.updated_at
        });
      }
    );
  } catch (error) {
    console.error('Get user profile error:', error);
    res.status(500).json({
      error: 'Failed to fetch user profile',
      message: 'An error occurred while retrieving your profile'
    });
  }
});

// Update user profile
router.put('/profile', authenticateToken, (req, res) => {
  try {
    const userId = req.user.id;
    const { username, email } = req.body;

    // Validate input
    if (!username && !email) {
      return res.status(400).json({
        error: 'Invalid input',
        message: 'At least one field (username or email) is required'
      });
    }

    // Check if username or email already exists
    if (username || email) {
      db.get(
        `SELECT id FROM users WHERE (username = ? OR email = ?) AND id != ?`,
        [username || '', email || '', userId],
        (err, existingUser) => {
          if (err) {
            console.error('Database error:', err);
            return res.status(500).json({
              error: 'Database error',
              message: 'An error occurred while checking user data'
            });
          }

          if (existingUser) {
            return res.status(409).json({
              error: 'Update failed',
              message: 'Username or email is already in use by another account'
            });
          }

          // Proceed with update
          updateProfile();
        }
      );
    } else {
      updateProfile();
    }

    function updateProfile() {
      const updateFields = [];
      const updateValues = [];

      if (username) {
        updateFields.push('username = ?');
        updateValues.push(username);
      }

      if (email) {
        updateFields.push('email = ?');
        updateValues.push(email);
      }

      updateFields.push('updated_at = CURRENT_TIMESTAMP');
      updateValues.push(userId);

      const query = `UPDATE users SET ${updateFields.join(', ')} WHERE id = ?`;

      db.run(query, updateValues, function(err) {
        if (err) {
          console.error('Error updating user profile:', err);
          return res.status(500).json({
            error: 'Failed to update profile',
            message: 'Could not update your profile information'
          });
        }

        if (this.changes === 0) {
          return res.status(404).json({
            error: 'User not found',
            message: 'User profile not found'
          });
        }

        // Get updated user data
        db.get(
          `SELECT id, username, email, created_at, updated_at
           FROM users WHERE id = ?`,
          [userId],
          (err, updatedUser) => {
            if (err || !updatedUser) {
              return res.status(500).json({
                error: 'Failed to fetch updated profile',
                message: 'Profile updated but could not retrieve new data'
              });
            }

            res.json({
              success: true,
              user: {
                id: updatedUser.id,
                username: updatedUser.username,
                email: updatedUser.email,
                createdAt: updatedUser.created_at,
                updatedAt: updatedUser.updated_at
              },
              message: 'Profile updated successfully'
            });
          }
        );
      });
    }
  } catch (error) {
    console.error('Update user profile error:', error);
    res.status(500).json({
      error: 'Failed to update profile',
      message: 'An error occurred while updating your profile'
    });
  }
});

// Get user statistics
router.get('/stats', authenticateToken, (req, res) => {
  try {
    const userId = req.user.id;

    // Get total travel plans count
    db.get(
      `SELECT COUNT(*) as totalPlans FROM travel_plans WHERE user_id = ?`,
      [userId],
      (err, planCount) => {
        if (err) {
          console.error('Error fetching user stats:', err);
          return res.status(500).json({
            error: 'Failed to fetch user statistics',
            message: 'Could not retrieve your statistics'
          });
        }

        // Get total destinations visited
        db.get(
          `SELECT COUNT(DISTINCT destination) as uniqueDestinations FROM travel_plans WHERE user_id = ?`,
          [userId],
          (err, destinationCount) => {
            if (err) {
              console.error('Error fetching destination stats:', err);
              return res.status(500).json({
                error: 'Failed to fetch user statistics',
                message: 'Could not retrieve your statistics'
              });
            }

            // Get total travel days
            db.get(
              `SELECT SUM(days) as totalDays FROM travel_plans WHERE user_id = ?`,
              [userId],
              (err, dayCount) => {
                if (err) {
                  console.error('Error fetching day stats:', err);
                  return res.status(500).json({
                    error: 'Failed to fetch user statistics',
                    message: 'Could not retrieve your statistics'
                  });
                }

                res.json({
                  totalPlans: planCount.totalPlans || 0,
                  uniqueDestinations: destinationCount.uniqueDestinations || 0,
                  totalDays: dayCount.totalDays || 0,
                  memberSince: req.user.createdAt
                });
              }
            );
          }
        );
      }
    );
  } catch (error) {
    console.error('Get user stats error:', error);
    res.status(500).json({
      error: 'Failed to fetch user statistics',
      message: 'An error occurred while retrieving your statistics'
    });
  }
});

module.exports = router;
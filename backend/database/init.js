const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const bcrypt = require('bcryptjs');

const dbPath = process.env.DB_PATH || path.join(__dirname, 'travel_assistant.db');

// Create database directory if it doesn't exist
const fs = require('fs');
const dbDir = path.dirname(dbPath);
if (!fs.existsSync(dbDir)) {
  fs.mkdirSync(dbDir, { recursive: true });
}

const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error('Error opening database:', err.message);
  } else {
    console.log('Connected to SQLite database');
  }
});

// Initialize database tables
const initDatabase = () => {
  return new Promise((resolve, reject) => {
    // Users table
    db.run(`
      CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE NOT NULL,
        email TEXT UNIQUE NOT NULL,
        password TEXT NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
      )
    `, (err) => {
      if (err) {
        reject(err);
        return;
      }
      console.log('Users table created/verified');
    });

    // Travel plans table
    db.run(`
      CREATE TABLE IF NOT EXISTS travel_plans (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL,
        destination TEXT NOT NULL,
        start_date TEXT NOT NULL,
        days INTEGER NOT NULL,
        travelers INTEGER NOT NULL,
        budget REAL NOT NULL,
        preferences TEXT, -- JSON array
        additional_requirements TEXT,
        plan_data TEXT NOT NULL, -- JSON string of complete plan
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id)
      )
    `, (err) => {
      if (err) {
        reject(err);
        return;
      }
      console.log('Travel plans table created/verified');
    });

    // Create indexes for better performance
    db.run(`CREATE INDEX IF NOT EXISTS idx_user_id ON travel_plans(user_id)`);
    db.run(`CREATE INDEX IF NOT EXISTS idx_username ON users(username)`);
    db.run(`CREATE INDEX IF NOT EXISTS idx_email ON users(email)`);

    console.log('Database initialization completed');
    resolve();
  });
};

// Create a demo user for testing
const createDemoUser = async () => {
  const hashedPassword = await bcrypt.hash('demo123', 10);

  return new Promise((resolve, reject) => {
    db.run(
      `INSERT OR IGNORE INTO users (username, email, password) VALUES (?, ?, ?)`,
      ['demo', 'demo@example.com', hashedPassword],
      function(err) {
        if (err) {
          console.error('Error creating demo user:', err);
          reject(err);
        } else {
          if (this.changes > 0) {
            console.log('Demo user created: demo / demo123');
          }
          resolve();
        }
      }
    );
  });
};

// Initialize database when this module is loaded
initDatabase()
  .then(() => createDemoUser())
  .then(() => console.log('Database setup completed successfully'))
  .catch(err => console.error('Database setup failed:', err));

module.exports = db;
-- Add full_name column to users table
ALTER TABLE users ADD COLUMN full_name VARCHAR(255) NOT NULL DEFAULT '';

-- Update existing records to populate full_name from first_name and last_name
UPDATE users SET full_name = CONCAT(COALESCE(first_name, ''), ' ', COALESCE(last_name, '')) WHERE full_name = '';


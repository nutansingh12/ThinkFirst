-- Add total_time_spent_minutes column to children table
ALTER TABLE children ADD COLUMN total_time_spent_minutes INTEGER DEFAULT 0;

-- Update existing records to have 0 time spent
UPDATE children SET total_time_spent_minutes = 0 WHERE total_time_spent_minutes IS NULL;


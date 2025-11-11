-- Increase field lengths for learning_paths table to handle longer AI-generated content

ALTER TABLE learning_paths 
    ALTER COLUMN topic TYPE VARCHAR(1000),
    ALTER COLUMN original_query TYPE TEXT;

-- Note: motivational_message is already TEXT, no change needed


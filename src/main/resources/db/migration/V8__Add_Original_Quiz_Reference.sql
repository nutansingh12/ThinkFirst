-- Add original_quiz_id column to quizzes table for retake quiz functionality
ALTER TABLE quizzes
ADD COLUMN original_quiz_id BIGINT;

-- Add foreign key constraint
ALTER TABLE quizzes
ADD CONSTRAINT fk_quizzes_original_quiz
FOREIGN KEY (original_quiz_id) REFERENCES quizzes(id);

-- Add index for better query performance
CREATE INDEX idx_quizzes_original_quiz_id ON quizzes(original_quiz_id);


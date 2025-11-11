-- Create learning_paths table
CREATE TABLE learning_paths (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    quiz_id BIGINT NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    topic VARCHAR(255) NOT NULL,
    original_query VARCHAR(500) NOT NULL,
    motivational_message TEXT,
    total_lessons INTEGER NOT NULL DEFAULT 0,
    completed_lessons INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT unique_child_quiz UNIQUE (child_id, quiz_id)
);

-- Create lessons table
CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,
    learning_path_id BIGINT NOT NULL REFERENCES learning_paths(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    content TEXT,
    display_order INTEGER NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create lesson_resources table
CREATE TABLE lesson_resources (
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    url VARCHAR(500),
    description TEXT,
    PRIMARY KEY (lesson_id, type, title)
);

-- Create indexes for better performance
CREATE INDEX idx_learning_paths_child ON learning_paths(child_id);
CREATE INDEX idx_learning_paths_quiz ON learning_paths(quiz_id);
CREATE INDEX idx_learning_paths_active ON learning_paths(active);
CREATE INDEX idx_lessons_learning_path ON lessons(learning_path_id);
CREATE INDEX idx_lessons_order ON lessons(learning_path_id, display_order);
CREATE INDEX idx_lesson_resources_lesson ON lesson_resources(lesson_id);


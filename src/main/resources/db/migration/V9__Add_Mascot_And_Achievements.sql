-- Add mascot messages table
CREATE TABLE mascot_messages (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    context VARCHAR(50),
    message TEXT NOT NULL,
    subject_id BIGINT REFERENCES subjects(id),
    min_score INTEGER,
    max_score INTEGER,
    priority INTEGER DEFAULT 1,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add subject statistics table
CREATE TABLE subject_statistics (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children(id),
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    total_questions INTEGER NOT NULL DEFAULT 0,
    total_quizzes INTEGER NOT NULL DEFAULT 0,
    correct_answers INTEGER NOT NULL DEFAULT 0,
    time_spent_minutes INTEGER NOT NULL DEFAULT 0,
    current_streak INTEGER NOT NULL DEFAULT 0,
    best_streak INTEGER NOT NULL DEFAULT 0,
    proficiency_level INTEGER DEFAULT 0,
    category_title VARCHAR(100),
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(child_id, subject_id)
);

-- Add badges table
CREATE TABLE badges (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    icon VARCHAR(10) NOT NULL,
    category VARCHAR(50),
    rarity VARCHAR(50),
    subject_id BIGINT REFERENCES subjects(id),
    criteria_type VARCHAR(50) NOT NULL,
    criteria_value INTEGER NOT NULL,
    display_order INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add child_badges junction table
CREATE TABLE child_badges (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children(id),
    badge_id BIGINT NOT NULL REFERENCES badges(id),
    earned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notification_seen BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(child_id, badge_id)
);

-- Create indexes for better performance
CREATE INDEX idx_subject_statistics_child ON subject_statistics(child_id);
CREATE INDEX idx_subject_statistics_subject ON subject_statistics(subject_id);
CREATE INDEX idx_child_badges_child ON child_badges(child_id);
CREATE INDEX idx_child_badges_badge ON child_badges(badge_id);
CREATE INDEX idx_mascot_messages_type ON mascot_messages(type);


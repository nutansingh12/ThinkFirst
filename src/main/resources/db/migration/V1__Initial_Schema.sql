-- ThinkFirst Database Schema
-- Initial migration

-- Users table (Parents/Educators)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Children table (Learners)
CREATE TABLE children (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    age INTEGER,
    grade_level VARCHAR(50),
    parent_id BIGINT NOT NULL,
    current_streak INTEGER DEFAULT 0,
    last_active_date TIMESTAMP,
    total_questions_answered INTEGER DEFAULT 0,
    total_quizzes_completed INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Subjects table
CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    age_group VARCHAR(50),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Subject prerequisites (self-referencing many-to-many)
CREATE TABLE subject_prerequisites (
    subject_id BIGINT NOT NULL,
    prerequisite_id BIGINT NOT NULL,
    PRIMARY KEY (subject_id, prerequisite_id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (prerequisite_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Skill levels (tracks child's proficiency per subject)
CREATE TABLE skill_levels (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    proficiency_score INTEGER DEFAULT 0,
    current_level VARCHAR(50) DEFAULT 'BEGINNER',
    quizzes_completed INTEGER DEFAULT 0,
    average_score INTEGER DEFAULT 0,
    last_assessed TIMESTAMP,
    prerequisite_met BOOLEAN DEFAULT FALSE,
    UNIQUE (child_id, subject_id),
    FOREIGN KEY (child_id) REFERENCES children(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Quizzes table
CREATE TABLE quizzes (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    difficulty VARCHAR(50),
    type VARCHAR(50) NOT NULL,
    passing_score INTEGER DEFAULT 70,
    time_limit INTEGER,
    title VARCHAR(255),
    description TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Questions table
CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    correct_answer TEXT,
    correct_option_index INTEGER,
    explanation TEXT,
    display_order INTEGER,
    FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

-- Question options (for multiple choice)
CREATE TABLE question_options (
    question_id BIGINT NOT NULL,
    option_text VARCHAR(500) NOT NULL,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- Quiz attempts
CREATE TABLE quiz_attempts (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL,
    quiz_id BIGINT NOT NULL,
    score INTEGER,
    passed BOOLEAN,
    time_spent_seconds INTEGER,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    feedback_message TEXT,
    FOREIGN KEY (child_id) REFERENCES children(id) ON DELETE CASCADE,
    FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

-- Quiz attempt answers
CREATE TABLE quiz_attempt_answers (
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_text TEXT,
    PRIMARY KEY (attempt_id, question_id),
    FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE
);

-- Chat sessions
CREATE TABLE chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL,
    subject_id BIGINT,
    title VARCHAR(255),
    message_count INTEGER DEFAULT 0,
    archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (child_id) REFERENCES children(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL
);

-- Chat messages
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    chat_session_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    quiz_id BIGINT,
    requires_quiz_completion BOOLEAN DEFAULT FALSE,
    content_moderation VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE SET NULL
);

-- Achievements/Badges
CREATE TABLE achievements (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL,
    badge_name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500),
    type VARCHAR(50) NOT NULL,
    points INTEGER DEFAULT 0,
    earned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (child_id) REFERENCES children(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_children_parent ON children(parent_id);
CREATE INDEX idx_skill_levels_child ON skill_levels(child_id);
CREATE INDEX idx_skill_levels_subject ON skill_levels(subject_id);
CREATE INDEX idx_quiz_attempts_child ON quiz_attempts(child_id);
CREATE INDEX idx_quiz_attempts_quiz ON quiz_attempts(quiz_id);
CREATE INDEX idx_chat_sessions_child ON chat_sessions(child_id);
CREATE INDEX idx_chat_messages_session ON chat_messages(chat_session_id);
CREATE INDEX idx_achievements_child ON achievements(child_id);
CREATE INDEX idx_questions_quiz ON questions(quiz_id);


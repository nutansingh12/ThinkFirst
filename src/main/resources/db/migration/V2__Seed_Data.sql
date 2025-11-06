-- Seed initial subjects
INSERT INTO subjects (name, description, age_group) VALUES
('Mathematics', 'Numbers, arithmetic, algebra, geometry', 'GENERAL'),
('Science', 'Biology, chemistry, physics, earth science', 'GENERAL'),
('English', 'Reading, writing, grammar, literature', 'GENERAL'),
('History', 'World history, geography, social studies', 'GENERAL'),
('Computer Science', 'Programming, logic, technology', 'GENERAL');

-- Elementary level subjects
INSERT INTO subjects (name, description, age_group) VALUES
('Basic Math', 'Addition, subtraction, multiplication, division', 'ELEMENTARY'),
('Basic Science', 'Plants, animals, weather, simple experiments', 'ELEMENTARY'),
('Reading & Writing', 'Phonics, spelling, simple sentences', 'ELEMENTARY');

-- Set up prerequisites (Basic Math is prerequisite for Mathematics)
INSERT INTO subject_prerequisites (subject_id, prerequisite_id)
SELECT s1.id, s2.id
FROM subjects s1, subjects s2
WHERE s1.name = 'Mathematics' AND s2.name = 'Basic Math';

-- Create a demo parent user (password: Demo123!)
-- Note: In production, this should be hashed properly
INSERT INTO users (email, password, first_name, last_name, role, email_verified, active)
VALUES ('demo@thinkfirst.com', '$2a$10$XYZ...', 'Demo', 'Parent', 'PARENT', true, true);


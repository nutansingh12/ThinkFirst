-- Seed Mascot Messages (Quizzy the Owl)

-- Welcome messages
INSERT INTO mascot_messages (type, context, message, priority) VALUES
('WELCOME', 'GENERAL', 'Hi! I''m Quizzy the Owl 🦉. I''m here to help you learn and have fun! Ask me anything!', 5),
('WELCOME', 'GENERAL', 'Hello there! I''m Quizzy, your learning buddy! Ready to explore new things together?', 3);

-- Quiz start messages
INSERT INTO mascot_messages (type, context, message, priority) VALUES
('QUIZ_START', 'GENERAL', 'Let''s test your knowledge! Take your time and think carefully. You''ve got this! 🦉', 5),
('QUIZ_START', 'MATHEMATICS', 'Time for some math magic! Remember, every problem has a solution. Let''s find it together!', 4),
('QUIZ_START', 'SCIENCE', 'Science time! Get ready to discover amazing things about our world! 🔬', 4);

-- Quiz success messages (70%+)
INSERT INTO mascot_messages (type, message, min_score, max_score, priority) VALUES
('QUIZ_SUCCESS', 'Excellent work! You scored {score}%! I knew you could do it! 🎉', 90, 100, 5),
('QUIZ_SUCCESS', 'Great job! You scored {score}%! You''re really getting the hang of this! ⭐', 70, 89, 5),
('QUIZ_SUCCESS', 'Fantastic! You passed with {score}%! Keep up the amazing work! 🌟', 70, 100, 3);

-- Quiz partial messages (40-69%)
INSERT INTO mascot_messages (type, message, min_score, max_score, priority) VALUES
('QUIZ_PARTIAL', 'Good effort! You scored {score}%. Let me give you some hints to help you improve! 💡', 40, 69, 5),
('QUIZ_PARTIAL', 'You''re on the right track with {score}%! Let''s work on those tricky questions together!', 40, 69, 4);

-- Quiz fail messages (<40%)
INSERT INTO mascot_messages (type, message, min_score, max_score, priority) VALUES
('QUIZ_FAIL', 'Don''t worry! Learning takes practice. Let''s review some lessons together! 📚', 0, 39, 5),
('QUIZ_FAIL', 'That''s okay! Every expert was once a beginner. Let me help you understand this better!', 0, 39, 4);

-- Hint messages
INSERT INTO mascot_messages (type, context, message, priority) VALUES
('HINT_GIVEN', 'GENERAL', 'Here''s a hint to help you think about it differently! 💡', 5),
('HINT_GIVEN', 'GENERAL', 'Let me give you a clue that might help! Think about it step by step.', 3);

-- Achievement unlock messages
INSERT INTO mascot_messages (type, context, message, priority) VALUES
('ACHIEVEMENT_UNLOCK', 'GENERAL', 'Congratulations! You''ve earned the ''{badge}'' badge! 🏆', 5),
('ACHIEVEMENT_UNLOCK', 'GENERAL', 'Wow! You unlocked ''{badge}''! You''re doing amazing! 🎖️', 4);

-- Encouragement messages
INSERT INTO mascot_messages (type, context, message, priority) VALUES
('ENCOURAGEMENT', 'GENERAL', 'Keep up the great work! Every question you ask makes you smarter! 🌟', 5),
('ENCOURAGEMENT', 'GENERAL', 'You''re doing wonderfully! I love your curiosity! 🦉', 4),
('ENCOURAGEMENT', 'GENERAL', 'Remember: mistakes are just learning opportunities in disguise! Keep going!', 3);

-- Seed Achievement Badges

-- Science badges
INSERT INTO badges (code, name, description, icon, category, rarity, subject_id, criteria_type, criteria_value, display_order) VALUES
('SCIENCE_BEGINNER', 'Science Explorer', 'Asked 10 science questions', '🔬', 'SUBJECT_MASTERY', 'COMMON', 
    (SELECT id FROM subjects WHERE name = 'Science'), 'QUESTIONS_ASKED', 10, 1),
('SCIENCE_ENTHUSIAST', 'Science Enthusiast', 'Asked 25 science questions', '🧪', 'SUBJECT_MASTERY', 'UNCOMMON',
    (SELECT id FROM subjects WHERE name = 'Science'), 'QUESTIONS_ASKED', 25, 2),
('YOUNG_EINSTEIN', 'Young Einstein', 'Asked 50 science questions', '⚗️', 'SUBJECT_MASTERY', 'RARE',
    (SELECT id FROM subjects WHERE name = 'Science'), 'QUESTIONS_ASKED', 50, 3),
('SCIENCE_MASTER', 'Science Master', 'Asked 100 science questions', '🏆', 'SUBJECT_MASTERY', 'EPIC',
    (SELECT id FROM subjects WHERE name = 'Science'), 'QUESTIONS_ASKED', 100, 4);

-- Math badges
INSERT INTO badges (code, name, description, icon, category, rarity, subject_id, criteria_type, criteria_value, display_order) VALUES
('MATH_BEGINNER', 'Number Explorer', 'Asked 10 math questions', '🔢', 'SUBJECT_MASTERY', 'COMMON',
    (SELECT id FROM subjects WHERE name = 'Mathematics'), 'QUESTIONS_ASKED', 10, 5),
('MATH_SOLVER', 'Math Solver', 'Asked 25 math questions', '➕', 'SUBJECT_MASTERY', 'UNCOMMON',
    (SELECT id FROM subjects WHERE name = 'Mathematics'), 'QUESTIONS_ASKED', 25, 6),
('MATH_WHIZ', 'Math Whiz', 'Asked 50 math questions', '➗', 'SUBJECT_MASTERY', 'RARE',
    (SELECT id FROM subjects WHERE name = 'Mathematics'), 'QUESTIONS_ASKED', 50, 7),
('MATH_WIZARD', 'Math Wizard', 'Solved 100 math problems', '🧙', 'SUBJECT_MASTERY', 'EPIC',
    (SELECT id FROM subjects WHERE name = 'Mathematics'), 'QUESTIONS_ASKED', 100, 8);

-- History badges
INSERT INTO badges (code, name, description, icon, category, rarity, subject_id, criteria_type, criteria_value, display_order) VALUES
('HISTORY_EXPLORER', 'History Explorer', 'Asked 10 history questions', '📜', 'SUBJECT_MASTERY', 'COMMON',
    (SELECT id FROM subjects WHERE name = 'History'), 'QUESTIONS_ASKED', 10, 9),
('HISTORY_BUFF', 'History Buff', 'Asked 25 history questions', '🏛️', 'SUBJECT_MASTERY', 'UNCOMMON',
    (SELECT id FROM subjects WHERE name = 'History'), 'QUESTIONS_ASKED', 25, 10),
('TIME_TRAVELER', 'Time Traveler', 'Asked 50 history questions', '⏳', 'SUBJECT_MASTERY', 'RARE',
    (SELECT id FROM subjects WHERE name = 'History'), 'QUESTIONS_ASKED', 50, 11);

-- Computer Science badges
INSERT INTO badges (code, name, description, icon, category, rarity, subject_id, criteria_type, criteria_value, display_order) VALUES
('TECH_BEGINNER', 'Tech Explorer', 'Asked 10 tech questions', '💻', 'SUBJECT_MASTERY', 'COMMON',
    (SELECT id FROM subjects WHERE name = 'Computer Science'), 'QUESTIONS_ASKED', 10, 12),
('TECH_CURIOUS', 'Tech Curious', 'Asked 25 tech questions', '🖥️', 'SUBJECT_MASTERY', 'UNCOMMON',
    (SELECT id FROM subjects WHERE name = 'Computer Science'), 'QUESTIONS_ASKED', 25, 13),
('TECH_WIZARD', 'Tech Wizard', 'Asked 50 tech questions', '⚡', 'SUBJECT_MASTERY', 'RARE',
    (SELECT id FROM subjects WHERE name = 'Computer Science'), 'QUESTIONS_ASKED', 50, 14);

-- Quiz performance badges
INSERT INTO badges (code, name, description, icon, category, rarity, criteria_type, criteria_value, display_order) VALUES
('QUIZ_STARTER', 'Quiz Starter', 'Completed 5 quizzes', '📝', 'QUIZ_PERFORMANCE', 'COMMON', NULL, 'QUIZZES_COMPLETED', 5, 20),
('QUIZ_MASTER', 'Quiz Master', 'Completed 25 quizzes', '📋', 'QUIZ_PERFORMANCE', 'UNCOMMON', NULL, 'QUIZZES_COMPLETED', 25, 21),
('QUIZ_LEGEND', 'Quiz Legend', 'Completed 50 quizzes', '🎯', 'QUIZ_PERFORMANCE', 'RARE', NULL, 'QUIZZES_COMPLETED', 50, 22),
('QUIZ_CHAMPION', 'Quiz Champion', 'Completed 100 quizzes', '👑', 'QUIZ_PERFORMANCE', 'EPIC', NULL, 'QUIZZES_COMPLETED', 100, 23);

-- General learning badges
INSERT INTO badges (code, name, description, icon, category, rarity, criteria_type, criteria_value, display_order) VALUES
('CURIOUS_MIND', 'Curious Mind', 'Asked 50 questions total', '🤔', 'SPECIAL', 'UNCOMMON', NULL, 'QUESTIONS_ASKED', 50, 30),
('KNOWLEDGE_SEEKER', 'Knowledge Seeker', 'Asked 100 questions total', '📚', 'SPECIAL', 'RARE', NULL, 'QUESTIONS_ASKED', 100, 31),
('WISDOM_COLLECTOR', 'Wisdom Collector', 'Asked 250 questions total', '🎓', 'SPECIAL', 'EPIC', NULL, 'QUESTIONS_ASKED', 250, 32),
('MASTER_LEARNER', 'Master Learner', 'Asked 500 questions total', '🌟', 'SPECIAL', 'LEGENDARY', NULL, 'QUESTIONS_ASKED', 500, 33);


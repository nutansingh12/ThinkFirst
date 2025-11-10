-- Add General subject as fallback for unclassified queries
INSERT INTO subjects (name, description, age_group) 
VALUES ('General', 'General knowledge and miscellaneous topics', 'GENERAL')
ON CONFLICT (name) DO NOTHING;


-- MDCAT Prep - seed data (import after schema.sql)
-- EVERYTHING in this file marked DEMO is sample content so the system can be
-- tested end to end. Real MCQ / PMDC syllabus content is provided by the
-- client and loaded through the admin panel; delete the demo questions there.
SET NAMES utf8mb4;

-- ---------------------------------------------------------------- settings
-- All changeable values live here and are edited from Admin > Settings.
INSERT INTO settings (`key`, `value`) VALUES
('app_name', 'MDCAT Prep'),
('tagline', 'Your Path to Medical College'),
('support_email', 'support@example.com'),
('social_facebook', ''),
('social_youtube', ''),
('social_instagram', ''),
('daily_goal', '50'),
('mock_question_count', '200'),
('timer_seconds_per_question', '60'),
('weak_threshold_percent', '60'),
('weak_min_attempts', '5'),
('default_design', '1'),
('google_client_id', ''),
('privacy_url', 'privacy.php'),
('daily_reward_coins', '20'),
('rewards_enabled', '1'),
('whatsapp_channel_url', '')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

-- ------------------------------------------------------------------- admin
-- DEMO ADMIN (change this password immediately after first login):
--   email: admin@mdcatprep.local   password: (demo seed, changed at go-live)
INSERT INTO admins (name, email, password_hash, role) VALUES
('Admin', 'admin@mdcatprep.local', '$2y$10$aVZMYjFELMVgjEPzHwkM7ez6IJKSHL6lbCtoo2EPXrq/gZHU66Kke', 'admin')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ------------------------------------------------------------ demo student
-- DEMO STUDENT (for testing only, delete from the database before launch):
--   email: student@demo.local   password: (demo seed, removed at go-live)
INSERT INTO users (name, email, phone, password_hash) VALUES
('Demo Student', 'student@demo.local', '03000000000', '$2y$10$hCfQA6ktifeNoH0f.7VorefSR.iHlqotKLKp83sRuL7QtgGozxagC')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ---------------------------------------------------------------- subjects
INSERT INTO subjects (id, name, tagline, icon, color, sort_order) VALUES
(1, 'Biology', 'Cell Biology', 'leaf', '#22a06b', 1),
(2, 'Chemistry', 'Organic Basics', 'flask', '#3e7cb1', 2),
(3, 'Physics', 'Motion and Force', 'atom', '#e8833a', 3),
(4, 'English & Logic', 'Grammar Rules', 'book', '#7c5cbf', 4)
ON DUPLICATE KEY UPDATE name = VALUES(name), tagline = VALUES(tagline);

-- ---------------------------------------------------------------- chapters
INSERT INTO chapters (id, subject_id, title, subtopics, sort_order) VALUES
(1, 1, 'Cell Biology', 'Cell structure, Organelles, Cell membrane', 1),
(2, 1, 'Biological Molecules', 'Carbohydrates, Proteins, Lipids, Nucleic acids', 2),
(3, 1, 'Enzymes', 'Properties, Mechanism, Factors affecting enzymes', 3),
(4, 2, 'Atomic Structure', 'Subatomic particles, Isotopes, Orbitals', 1),
(5, 3, 'Measurements', 'SI units, Vernier calipers, Errors', 1),
(6, 4, 'Grammar and Logic', 'Tenses, Synonyms, Antonyms, Series', 1)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- ------------------------------------------------- DEMO questions: Biology
-- Chapter 1: Cell Biology (10)
INSERT INTO questions (chapter_id, subtopic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation) VALUES
(1, 'Organelles', 'Which organelle is known as the powerhouse of the cell?', 'Nucleus', 'Mitochondria', 'Ribosome', 'Golgi Apparatus', 'B', 'Mitochondria produce ATP, the energy currency of the cell, through cellular respiration.'),
(1, 'Organelles', 'Which organelle is the site of protein synthesis?', 'Lysosome', 'Vacuole', 'Ribosome', 'Nucleus', 'C', 'Ribosomes assemble amino acids into proteins.'),
(1, 'Organelles', 'Which structure controls all activities of the cell?', 'Nucleus', 'Cytoplasm', 'Ribosome', 'Mitochondria', 'A', 'The nucleus contains DNA and controls the activities of the cell.'),
(1, 'Cell membrane', 'Which structure forms a selectively permeable barrier around the cell?', 'Cell wall', 'Plasma membrane', 'Nuclear membrane', 'Capsule', 'B', 'The plasma membrane controls the entry and exit of substances.'),
(1, 'Organelles', 'Photosynthesis takes place in which organelle?', 'Mitochondria', 'Golgi body', 'Chloroplast', 'Lysosome', 'C', 'Chloroplasts contain chlorophyll and carry out photosynthesis.'),
(1, 'Organelles', 'Which organelle modifies, sorts and packages proteins?', 'Ribosome', 'Golgi apparatus', 'Lysosome', 'Vacuole', 'B', 'The Golgi apparatus modifies and packages proteins for transport.'),
(1, 'Organelles', 'Lysosomes are commonly called:', 'Suicide bags', 'Power houses', 'Protein factories', 'Control centres', 'A', 'Lysosomes contain digestive enzymes and can digest the cell itself, so they are called suicide bags.'),
(1, 'Cell structure', 'The cell wall of plants is mainly made of:', 'Chitin', 'Peptidoglycan', 'Cellulose', 'Glycogen', 'C', 'Plant cell walls are mainly composed of cellulose.'),
(1, 'Organelles', 'ATP stands for:', 'Adenine triphosphate', 'Adenosine triphosphate', 'Adenosine tetraphosphate', 'Adenine tetraphosphate', 'B', 'ATP is adenosine triphosphate, the energy currency of the cell.'),
(1, 'Organelles', 'Lipids are synthesised mainly by the:', 'Smooth endoplasmic reticulum', 'Rough endoplasmic reticulum', 'Golgi apparatus', 'Nucleus', 'A', 'The smooth endoplasmic reticulum is the main site of lipid synthesis.');

-- Chapter 2: Biological Molecules (10)
INSERT INTO questions (chapter_id, subtopic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation) VALUES
(2, 'Proteins', 'The monomers of proteins are:', 'Fatty acids', 'Amino acids', 'Monosaccharides', 'Nucleotides', 'B', 'Proteins are polymers of amino acids joined by peptide bonds.'),
(2, 'Carbohydrates', 'Sucrose is formed by the combination of:', 'Glucose and fructose', 'Glucose and glucose', 'Glucose and galactose', 'Fructose and fructose', 'A', 'Sucrose is a disaccharide of glucose and fructose.'),
(2, 'Nucleic acids', 'The double helix model of DNA was proposed by:', 'Darwin and Wallace', 'Schleiden and Schwann', 'Watson and Crick', 'Hershey and Chase', 'C', 'Watson and Crick proposed the double helix model of DNA in 1953.'),
(2, 'Proteins', 'Most enzymes are chemically:', 'Carbohydrates', 'Proteins', 'Lipids', 'Nucleic acids', 'B', 'Almost all enzymes are proteins.'),
(2, 'Carbohydrates', 'The main storage carbohydrate in animals is:', 'Starch', 'Glycogen', 'Cellulose', 'Sucrose', 'B', 'Animals store carbohydrates as glycogen in the liver and muscles.'),
(2, 'Proteins', 'A peptide bond is formed between two:', 'Fatty acids', 'Amino acids', 'Sugars', 'Nucleotides', 'B', 'A peptide bond links the carboxyl group of one amino acid with the amino group of another.'),
(2, 'Carbohydrates', 'Which of the following is a reducing sugar?', 'Sucrose', 'Starch', 'Glucose', 'Cellulose', 'C', 'Glucose is a monosaccharide and a reducing sugar.'),
(2, 'Lipids', 'Lipids are made up of:', 'Amino acids and glycerol', 'Fatty acids and glycerol', 'Sugars and fatty acids', 'Nucleotides and glycerol', 'B', 'Fats are esters of fatty acids with glycerol.'),
(2, 'Nucleic acids', 'A nucleotide is made up of:', 'Sugar, phosphate and a nitrogen base', 'Sugar and a nitrogen base only', 'Phosphate and amino acids', 'Fatty acid and sugar', 'A', 'Each nucleotide consists of a pentose sugar, a phosphate group and a nitrogenous base.'),
(2, 'Proteins', 'Denaturation of a protein affects its:', 'Amino acid sequence', 'Structure', 'Elemental composition', 'Number of peptide bonds', 'B', 'Denaturation changes the folded structure of a protein without breaking its peptide bonds.');

-- Chapter 3: Enzymes (10)
INSERT INTO questions (chapter_id, subtopic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation) VALUES
(3, 'Mechanism', 'Enzymes speed up a reaction by:', 'Increasing activation energy', 'Lowering activation energy', 'Raising the temperature', 'Changing the products', 'B', 'Enzymes lower the activation energy needed for a reaction.'),
(3, 'Mechanism', 'The lock and key model of enzyme action was proposed by:', 'Emil Fischer', 'Koshland', 'Watson', 'Darwin', 'A', 'Emil Fischer proposed the lock and key model in 1894.'),
(3, 'Factors', 'The optimum pH of pepsin is around:', '2', '7', '9', '12', 'A', 'Pepsin works best in the strongly acidic medium of the stomach, around pH 2.'),
(3, 'Properties', 'The enzyme present in saliva that digests starch is:', 'Pepsin', 'Amylase', 'Lipase', 'Trypsin', 'B', 'Salivary amylase begins the digestion of starch in the mouth.'),
(3, 'Mechanism', 'The non-protein part of an enzyme is called a:', 'Substrate', 'Apoenzyme', 'Cofactor', 'Inhibitor', 'C', 'Cofactors are non-protein helpers needed by some enzymes for activity.'),
(3, 'Mechanism', 'The substrate binds to the enzyme at the:', 'Active site', 'Binding groove', 'Allosteric site', 'Terminal end', 'A', 'The substrate binds at the active site of the enzyme.'),
(3, 'Factors', 'Very high temperature usually causes an enzyme to:', 'Work faster forever', 'Denature', 'Become a substrate', 'Change into a lipid', 'B', 'High temperature breaks the bonds holding the enzyme shape, causing denaturation.'),
(3, 'Inhibition', 'A competitive inhibitor competes with the substrate because it resembles the:', 'Enzyme', 'Product', 'Substrate', 'Cofactor', 'C', 'A competitive inhibitor has a shape similar to the substrate and competes for the active site.'),
(3, 'Mechanism', 'The induced fit model was proposed by:', 'Fischer', 'Koshland', 'Crick', 'Bohr', 'B', 'Koshland proposed the induced fit model, in which the active site adjusts its shape.'),
(3, 'Properties', 'Enzymes are best described as:', 'Inorganic catalysts', 'Biological catalysts', 'Reactants', 'Products', 'B', 'Enzymes are biological catalysts made by living cells.');

-- ----------------------------------------------- DEMO questions: Chemistry
-- Chapter 4: Atomic Structure (6)
INSERT INTO questions (chapter_id, subtopic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation) VALUES
(4, 'Subatomic particles', 'The proton was discovered by:', 'J.J. Thomson', 'Goldstein', 'Chadwick', 'Rutherford', 'B', 'Goldstein discovered canal rays, which led to the discovery of the proton.'),
(4, 'Atomic number', 'The atomic number of an element is equal to the number of:', 'Protons', 'Neutrons', 'Electrons and neutrons', 'Nucleons only', 'A', 'Atomic number equals the number of protons in the nucleus.'),
(4, 'Isotopes', 'Isotopes of an element differ from each other in the number of:', 'Protons', 'Electrons', 'Neutrons', 'Shells', 'C', 'Isotopes have the same number of protons but different numbers of neutrons.'),
(4, 'Subatomic particles', 'The charge on an electron is:', 'Positive', 'Negative', 'Neutral', 'Variable', 'B', 'An electron carries a unit negative charge.'),
(4, 'Orbitals', 'The number of orbitals in a p subshell is:', '1', '3', '5', '7', 'B', 'A p subshell contains three orbitals.'),
(4, 'Subatomic particles', 'The neutron was discovered by:', 'Thomson', 'Goldstein', 'Chadwick', 'Bohr', 'C', 'Chadwick discovered the neutron in 1932.');

-- ------------------------------------------------- DEMO questions: Physics
-- Chapter 5: Measurements (6)
INSERT INTO questions (chapter_id, subtopic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation) VALUES
(5, 'SI units', 'The SI unit of force is:', 'Joule', 'Newton', 'Pascal', 'Watt', 'B', 'Force is measured in newtons in the SI system.'),
(5, 'SI units', 'Which of the following is a base quantity?', 'Length', 'Speed', 'Force', 'Energy', 'A', 'Length is one of the seven base quantities in SI.'),
(5, 'SI units', 'The SI unit of energy is the:', 'Newton', 'Watt', 'Joule', 'Ampere', 'C', 'Energy is measured in joules.'),
(5, 'SI units', 'One kilometre is equal to:', '100 m', '1000 m', '10,000 m', '10 m', 'B', 'One kilometre equals 1000 metres.'),
(5, 'Instruments', 'A vernier caliper is used to measure:', 'Small lengths accurately', 'Mass of liquids', 'Temperature', 'Electric current', 'A', 'A vernier caliper measures small lengths with high accuracy.'),
(5, 'Dimensions', 'The dimensional formula of velocity is:', 'LT', 'L T-1', 'L-1 T', 'M L T-1', 'B', 'Velocity is displacement divided by time, so its dimensions are L T-1.');

-- ------------------------------------------- DEMO questions: English & Logic
-- Chapter 6: Grammar and Logic (6)
INSERT INTO questions (chapter_id, subtopic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation) VALUES
(6, 'Tenses', 'She ___ to school every day.', 'go', 'goes', 'going', 'gone', 'B', 'With a third person singular subject in the present simple tense, the verb takes s or es: goes.'),
(6, 'Synonyms', 'Choose the word closest in meaning to RAPID:', 'Slow', 'Fast', 'Late', 'Weak', 'B', 'Rapid means fast or quick.'),
(6, 'Tenses', 'Choose the grammatically correct sentence:', 'He has completed his work.', 'He have completed his work.', 'He completed his work yesterday daily.', 'He completing his work.', 'A', 'Has is used with he, she or it in the present perfect tense.'),
(6, 'Antonyms', 'Choose the word opposite in meaning to ANCIENT:', 'Old', 'Modern', 'Antique', 'Historic', 'B', 'Modern is the antonym of ancient.'),
(6, 'Logic', 'All cats are animals. Tom is a cat. Therefore:', 'Tom is an animal', 'Tom is not an animal', 'All animals are cats', 'Tom is a dog', 'A', 'If all cats are animals and Tom is a cat, then Tom must be an animal.'),
(6, 'Series', 'What comes next in the series: 2, 4, 8, 16, ?', '20', '24', '32', '64', 'C', 'Each term is double the previous one, so the next term is 32.');

-- ------------------------------------------------------------- notifications
INSERT INTO notifications (title, body, is_active) VALUES
('Welcome to MDCAT Prep', 'Demo notification: real announcements are posted from the admin panel.', 1),
('Demo content loaded', 'Demo notification: sample MCQs are loaded for testing. Real content will replace them.', 1);

-- ------------------------------------------------------------------ banners
-- Demo banner ships INACTIVE. Activate it from Admin > Banners after the
-- demo image is replaced, or delete the row. Image: uploads/banners/demo-banner.png
INSERT INTO banners (title, image_path, target_link, placement, sort_order, is_active) VALUES
('Demo Banner', 'uploads/banners/demo-banner.png', '', 'dashboard', 1, 0);

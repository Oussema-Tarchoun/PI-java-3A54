-- ====================================================
-- BASE DE DONNÉES AIVA - Script d'initialisation
-- ====================================================

CREATE DATABASE IF NOT EXISTS aiva;
USE aiva;

-- ====================================================
-- TABLE USER (Authentification avec rôles)
-- ====================================================
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    roles VARCHAR(50) NOT NULL DEFAULT 'user', -- 'admin' ou 'user'
    is_blocked INT DEFAULT 0,
    totp_secret VARCHAR(255),
    reset_password_attempts INT DEFAULT 0,
    known_ips TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    verification_token VARCHAR(255),
    token_expires_at DATETIME,
    experience_points INT DEFAULT 0,
    last_points_awarded_at DATETIME,
    is2fa_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_roles (roles)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================
-- TABLE ALIMENTATION
-- ====================================================
CREATE TABLE IF NOT EXISTS alimentation (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    type VARCHAR(100) NOT NULL, -- petit-déjeuner, déjeuner, dîner, snack
    description VARCHAR(500),
    calories INT,
    date_consommation DATE NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, date_consommation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================
-- TABLE FINANCE
-- ====================================================
CREATE TABLE IF NOT EXISTS finance (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    categorie VARCHAR(100) NOT NULL, -- revenu, dépense
    description VARCHAR(500),
    montant DECIMAL(10, 2) NOT NULL,
    date_transaction DATE NOT NULL,
    type_transaction VARCHAR(100), -- salaire, nourriture, transport, etc
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, date_transaction),
    INDEX idx_categorie (categorie)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================
-- TABLE ENERGIE
-- ====================================================
CREATE TABLE IF NOT EXISTS energie (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    type VARCHAR(100) NOT NULL, -- electricité, gaz, eau
    consommation DECIMAL(10, 2), -- en kWh ou m3
    cout DECIMAL(10, 2),
    date_consommation DATE NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, date_consommation),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================
-- TABLE ACTIVITE (Sport/Exercice)
-- ====================================================
CREATE TABLE IF NOT EXISTS activite (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    type VARCHAR(100) NOT NULL, -- running, cycling, swimming, etc
    distance DECIMAL(10, 2), -- en km
    duree TIME,
    date_activite DATE NOT NULL,
    calories_brulees INT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, date_activite),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================
-- TABLE APPRENTISSAGE
-- ====================================================
CREATE TABLE IF NOT EXISTS apprentissage (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    sujet VARCHAR(255), -- cours, livre, vidéo, projet
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    heures_etudiees INT DEFAULT 0,
    niveau VARCHAR(100), -- débutant, intermédiaire, avancé
    date_debut DATE NOT NULL,
    date_fin_estimee DATE,
    statut VARCHAR(100) DEFAULT 'en cours', -- en cours, complété, en attente
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, date_debut),
    INDEX idx_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================
-- TABLE OBJECTIF
-- ====================================================
CREATE TABLE IF NOT EXISTS objectif (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    description VARCHAR(500),
    type VARCHAR(100),
    valeur_cible INT,
    date_debut DATE,
    date_fin DATE,
    statut VARCHAR(100) DEFAULT 'en cours', -- en cours, complété, abandonné
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, date_debut),
    INDEX idx_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================
-- UTILISATEURS PAR DÉFAUT (test)
-- ====================================================

-- Admin: admin@aiva.tn / admin123
INSERT INTO user (email, password, name, roles, is_verified, experience_points) 
VALUES ('admin@aiva.tn', 'admin123', 'Admin AIVA', 'admin', TRUE, 1000);

-- User normal: user@aiva.tn / user123
INSERT INTO user (email, password, name, roles, is_verified, experience_points) 
VALUES ('user@aiva.tn', 'user123', 'User Normal', 'user', TRUE, 500);

-- ====================================================
-- DONNÉES DE TEST
-- ====================================================

-- Alimentation pour user 2
INSERT INTO alimentation (user_id, type, description, calories, date_consommation, notes)
VALUES 
(2, 'petit-déjeuner', 'Oeufs et pain', 350, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'Bon matin'),
(2, 'déjeuner', 'Poulet avec riz', 650, CURDATE(), 'Déjeuner équilibré'),
(2, 'dîner', 'Salade', 280, CURDATE(), 'Léger le soir');

-- Finance pour user 2
INSERT INTO finance (user_id, categorie, description, montant, date_transaction, type_transaction, notes)
VALUES 
(2, 'revenu', 'Salaire mensuel', 2500.00, CURDATE(), 'salaire', 'Salaire régulier'),
(2, 'dépense', 'Courses supermarché', 150.00, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'nourriture', 'Épicerie hebdomadaire'),
(2, 'dépense', 'Essence voiture', 60.00, CURDATE(), 'transport', 'Plein d''essence');

-- Energie pour user 2
INSERT INTO energie (user_id, type, consommation, cout, date_consommation, notes)
VALUES 
(2, 'electricité', 250.50, 45.00, CURDATE(), 'Consommation normale'),
(2, 'eau', 45.00, 25.00, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'Facture d''eau');

-- Activité pour user 2
INSERT INTO activite (user_id, type, distance, duree, date_activite, calories_brulees, notes)
VALUES 
(2, 'running', 5.5, '00:45:00', CURDATE(), 450, 'Course matinale'),
(2, 'cycling', 15.0, '01:30:00', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 600, 'Balade à vélo');

-- Apprentissage pour user 2
INSERT INTO apprentissage (user_id, sujet, titre, description, heures_etudiees, niveau, date_debut, date_fin_estimee, statut, notes)
VALUES 
(2, 'cours', 'Java Avancé', 'Cours complet sur Java', 30, 'intermédiaire', DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'en cours', 'Bien progressé'),
(2, 'projet', 'Application JavaFX', 'Créer une app desktop', 15, 'avancé', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 'en cours', 'En cours de développement');

-- Objectif pour user 2
INSERT INTO objectif (user_id, description, type, valeur_cible, date_debut, date_fin, statut, notes)
VALUES 
(2, 'Perdre du poids', 'santé', 5, DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'en cours', 'Perdre 5 kg'),
(2, 'Apprendre Java', 'éducation', 100, DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 60 DAY), 'en cours', 'Maîtriser Java');

-- ====================================================
-- FIN DU SCRIPT
-- ====================================================

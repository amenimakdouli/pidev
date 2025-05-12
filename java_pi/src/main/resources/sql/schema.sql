-- Drop tables if they exist (in correct order due to foreign key constraints)
DROP TABLE IF EXISTS partnership;
DROP TABLE IF EXISTS partenaire;

-- Create the partenaire table
CREATE TABLE IF NOT EXISTS partenaire (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    website VARCHAR(255),
    CONSTRAINT uc_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create the partnership table with foreign key reference
CREATE TABLE IF NOT EXISTS partnership (
    id INT PRIMARY KEY AUTO_INCREMENT,
    partner_id INT NOT NULL,
    type VARCHAR(255) NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (partner_id) REFERENCES partenaire(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add indexes for better performance
CREATE INDEX idx_name ON partenaire(name);
CREATE INDEX idx_email ON partenaire(email);
CREATE INDEX idx_partner_id ON partnership(partner_id);
CREATE INDEX idx_type ON partnership(type);
CREATE INDEX idx_created_at ON partnership(created_at);


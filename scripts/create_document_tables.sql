-- 등기부등본 정보 테이블
CREATE TABLE IF NOT EXISTS registry_document (
    registry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    riskck_id BIGINT NOT NULL,
    region_address VARCHAR(255),
    road_address VARCHAR(255),
    owner_name VARCHAR(100),
    owner_birth_date DATE,
    debtor VARCHAR(100),
    has_seizure BOOLEAN DEFAULT FALSE,
    has_auction BOOLEAN DEFAULT FALSE,
    has_litigation BOOLEAN DEFAULT FALSE,
    has_attachment BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (riskck_id) REFERENCES risk_check(riskck_id) ON DELETE CASCADE,
    INDEX idx_riskck_id (riskck_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 근저당권 정보 테이블
CREATE TABLE IF NOT EXISTS mortgage_info (
    mortgage_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    registry_id BIGINT NOT NULL,
    priority_number INT,
    max_claim_amount BIGINT,
    debtor VARCHAR(100),
    mortgagee VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (registry_id) REFERENCES registry_document(registry_id) ON DELETE CASCADE,
    INDEX idx_registry_id (registry_id),
    INDEX idx_priority_number (priority_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 건축물대장 정보 테이블
CREATE TABLE IF NOT EXISTS building_document (
    building_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    riskck_id BIGINT NOT NULL,
    site_location VARCHAR(255),
    road_address VARCHAR(255),
    total_floor_area DECIMAL(10, 2),
    purpose VARCHAR(100),
    floor_number INT,
    approval_date DATE,
    is_violation_building BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (riskck_id) REFERENCES risk_check(riskck_id) ON DELETE CASCADE,
    INDEX idx_riskck_id (riskck_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
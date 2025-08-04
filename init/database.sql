CREATE TABLE user
(
    user_id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    nickname        VARCHAR(50),
    email           VARCHAR(100) UNIQUE,
    password        VARCHAR(255),
    birth_date      DATE,
    gender          ENUM ('MALE', 'FEMALE'),
    profile_img_url VARCHAR(255),
    created_at      DATETIME,
    updated_at      DATETIME,
    role            ENUM ('ROLE_USER', 'ROLE_ADMIN')
);

CREATE TABLE social_account (
                                social_id VARCHAR(255) PRIMARY KEY,
                                social_type ENUM('KAKAO', 'GOOGLE', 'NAVER'),
                                user_id BIGINT NOT NULL,
                                FOREIGN KEY (user_id) REFERENCES user(user_id)
);
CREATE TABLE home (
                      home_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      user_id BIGINT,
                      user_name VARCHAR(50),
                      addr1 VARCHAR(255),
                      addr2 VARCHAR(255),
                      lease_type ENUM('JEONSE', 'WOLSE'),
                      deposit_price INT,
                      monthly_rent INT,
                      maintenance_fee INT,
                      home_status ENUM('AVAILABLE', 'RESERVED', 'SOLD'),
                      view_cnt INT DEFAULT 0,
                      like_cnt INT DEFAULT 0,
                      chat_cnt INT DEFAULT 0,
                      report_cnt INT DEFAULT 0,
                      room_cnt INT,
                      supply_area FLOAT,
                      created_at DATETIME,
                      updated_at DATETIME,
                      FOREIGN KEY (user_id) REFERENCES user(user_id)
);

CREATE TABLE home_detail (
                             home_detail_id BIGINT PRIMARY KEY,
                             home_id BIGINT,
                             building_type ENUM('OPEN_ONEROOM', 'SEPERATE_ONEROOM', 'TWOROOM', 'APT', 'OFFICETEL', 'VILLA'),
                             home_direction ENUM('N', 'S', 'E', 'W', 'NE', 'NW', 'SE', 'SW'),
                             move_in_date DATE,
                             build_date DATE,
                             exclusive_area FLOAT,
                             room_count INT,
                             bathroom_count INT,
                             building_total_floors INT,
                             home_floor INT,
                             report_count INT DEFAULT 0,
                             FOREIGN KEY (home_id) REFERENCES home(home_id)
);
CREATE TABLE home_image (
                            image_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            home_id BIGINT,
                            image_url VARCHAR(255),
                            FOREIGN KEY (home_id) REFERENCES home(home_id)
);
CREATE TABLE facility_category (
                                   category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                   category_type VARCHAR(100)
);

CREATE TABLE facility_item (
                               item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               category_id BIGINT,
                               item_name VARCHAR(100),
                               FOREIGN KEY (category_id) REFERENCES facility_category(category_id)
);

CREATE TABLE home_facility (
                               home_detail_id BIGINT,
                               item_id BIGINT,
                               PRIMARY KEY (home_detail_id, item_id),
                               FOREIGN KEY (home_detail_id) REFERENCES home_detail(home_detail_id),
                               FOREIGN KEY (item_id) REFERENCES facility_item(item_id)
);
CREATE TABLE maintenance_item (
                                  maintenance_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                  item_name VARCHAR(100)
);

CREATE TABLE home_maintenance_fee (
                                      home_id BIGINT,
                                      maintenance_id BIGINT,
                                      fee INT,
                                      PRIMARY KEY (home_id, maintenance_id),
                                      FOREIGN KEY (home_id) REFERENCES home(home_id),
                                      FOREIGN KEY (maintenance_id) REFERENCES maintenance_item(maintenance_id)
);
CREATE TABLE home_like (
                           user_id BIGINT,
                           home_id BIGINT,
                           liked_at DATETIME,
                           PRIMARY KEY (user_id, home_id),
                           FOREIGN KEY (user_id) REFERENCES user(user_id),
                           FOREIGN KEY (home_id) REFERENCES home(home_id)
);
CREATE TABLE chatroom (
                          chatroom_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          owner_id BIGINT,
                          buyer_id BIGINT,
                          home_id BIGINT,
                          created_at DATETIME,
                          last_message_at DATETIME,
                          last_message VARCHAR(255),
                          unread_message_count INT,
                          FOREIGN KEY (owner_id) REFERENCES user(user_id),
                          FOREIGN KEY (buyer_id) REFERENCES user(user_id),
                          FOREIGN KEY (home_id) REFERENCES home(home_id)
);
CREATE TABLE risk_check (
                            riskck_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            user_id BIGINT,
                            home_id BIGINT,
                            risk_type ENUM('DANGER','WARN', 'SAFE'),
                            checked_at DATETIME,
                            registry_file_url VARCHAR(255),
                            building_file_url VARCHAR(255),
                            registry_file_date DATETIME,
                            building_file_date DATETIME,
                            FOREIGN KEY (user_id) REFERENCES user(user_id),
                            FOREIGN KEY (home_id) REFERENCES home(home_id)
);
CREATE TABLE risk_check_detail (
                                   riskck_id BIGINT,
                                   title1 VARCHAR(255),
                                   title2 VARCHAR(255),
                                   content VARCHAR(255),
                                   FOREIGN KEY (riskck_id) REFERENCES risk_check(riskck_id)
);
CREATE TABLE precontract_check
(
    precheck_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_role   ENUM ('OWNER', 'BUYER'),
    checked_at  DATETIME
);
CREATE TABLE contract_chat (
                               contract_chat_id BIGINT PRIMARY KEY,
                               home_id BIGINT,
                               owner_id BIGINT,
                               buyer_id BIGINT,
                               owner_precheck_id BIGINT,
                               buyer_precheck_id BIGINT,
                               cotract_start_at DATETIME,
                               last_message VARCHAR(255),
                               start_point VARCHAR(255),
                               end_point VARCHAR(255),
                               FOREIGN KEY (home_id) REFERENCES home(home_id),
                               FOREIGN KEY (owner_id) REFERENCES user(user_id),
                               FOREIGN KEY (buyer_id) REFERENCES user(user_id),
                               FOREIGN KEY (owner_precheck_id) REFERENCES precontract_check(precheck_id),
                               FOREIGN KEY (buyer_precheck_id) REFERENCES precontract_check(precheck_id)
);

CREATE TABLE final_contract (
                                contract_id BIGINT PRIMARY KEY,
                                home_id BIGINT,
                                owner_id BIGINT,
                                buyer_id BIGINT,
                                contract_pdf_url VARCHAR(255),
                                contract_date DATETIME,
                                contract_expire_date DATETIME,
                                FOREIGN KEY (contract_id) REFERENCES contract_chat(contract_chat_id),
                                FOREIGN KEY (home_id) REFERENCES home(home_id),
                                FOREIGN KEY (owner_id) REFERENCES user(user_id),
                                FOREIGN KEY (buyer_id) REFERENCES user(user_id)
);
CREATE TABLE home_report (
                             report_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             user_id BIGINT,
                             home_id BIGINT,
                             report_reason TEXT,
                             report_at DATETIME,
                             report_status ENUM('WAITING', 'PROCESSING', 'DONE'),
                             FOREIGN KEY (user_id) REFERENCES user(user_id),
                             FOREIGN KEY (home_id) REFERENCES home(home_id)
);

alter table home drop column user_name
-- 건축물대장 테이블 수정
ALTER TABLE building_document 
ADD COLUMN land_area DECIMAL(10, 2) COMMENT '대지면적' AFTER road_address,
ADD COLUMN issue_date DATE COMMENT '발급일' AFTER is_violation_building;

-- 등기부등본 테이블 수정  
ALTER TABLE registry_document
ADD COLUMN building_number VARCHAR(100) COMMENT '건물번호' AFTER road_address,
ADD COLUMN building_detail VARCHAR(255) COMMENT '건물구조' AFTER building_number,
ADD COLUMN issue_date DATE COMMENT '발급일' AFTER has_attachment;

-- 기존 컬럼 타입 변경 (필요시)
-- 건축물대장의 totalFloorArea가 기존에 이미 DECIMAL(10,2)로 되어있어 변경 불필요
-- 등기부등본의 mortgageeList는 별도 테이블(mortgage_info)로 관리이중이므로 변경 불필요
-- Modify risk_check table to allow NULL home_id for external risk analysis
-- This allows risk checks to be performed on properties not in our system

-- First, drop the existing foreign key constraint
ALTER TABLE risk_check 
DROP FOREIGN KEY risk_check_ibfk_2;

-- Modify the home_id column to allow NULL values
ALTER TABLE risk_check 
MODIFY COLUMN home_id BIGINT NULL;

-- Re-add the foreign key constraint with NULL allowed
ALTER TABLE risk_check 
ADD CONSTRAINT risk_check_ibfk_2 
FOREIGN KEY (home_id) REFERENCES home(home_id) 
ON DELETE CASCADE;

-- Add an index for better query performance
CREATE INDEX idx_risk_check_user_home ON risk_check(user_id, home_id);
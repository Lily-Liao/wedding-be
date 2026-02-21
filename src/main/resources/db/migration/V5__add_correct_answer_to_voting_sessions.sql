-- V5: Add correct_answer column to voting_sessions for lucky draw filtering
ALTER TABLE voting_sessions
    ADD COLUMN correct_answer CHAR(1) NULL COMMENT '正確答案 (A/B/C/D)',
    ADD CONSTRAINT chk_correct_answer CHECK (correct_answer IN ('A', 'B', 'C', 'D'));
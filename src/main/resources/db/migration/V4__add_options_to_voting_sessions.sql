-- V4: Add customizable options JSON column to voting_sessions
ALTER TABLE voting_sessions
    ADD COLUMN options JSON NULL COMMENT '投票選項 [{key, label, color}]';

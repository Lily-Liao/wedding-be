-- V2: Add AWAITING_SEAT_QUERY to line_user_states check constraint
ALTER TABLE line_user_states DROP CHECK chk_user_state;
ALTER TABLE line_user_states
    ADD CONSTRAINT chk_user_state
    CHECK (current_state IN ('IDLE', 'AWAITING_MESSAGE', 'AWAITING_VOTE', 'AWAITING_SEAT_QUERY'));

ALTER TABLE actions
    ALTER COLUMN source TYPE VARCHAR(100);

ALTER TABLE actions
    DROP CONSTRAINT ck_action_score_nonnegative;

ALTER TABLE actions
    ADD CONSTRAINT ck_action_score_range
        CHECK (action_score BETWEEN 0 AND 8);

ALTER TABLE actions
    DROP COLUMN helpfulness_score;

ALTER TABLE care_card
    ALTER COLUMN source TYPE VARCHAR(100);

ALTER TABLE care_card
    ADD COLUMN created_date DATE NOT NULL DEFAULT CURRENT_DATE;

ALTER TABLE care_card
    ALTER COLUMN created_date DROP DEFAULT;

CREATE TABLE user_action_feedback (
    user_id BIGINT NOT NULL,
    action_id BIGINT NOT NULL,
    helpfulness_score SMALLINT NOT NULL,
    PRIMARY KEY (user_id, action_id),
    CONSTRAINT fk_user_action_feedback_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_action_feedback_action
        FOREIGN KEY (action_id)
        REFERENCES actions(action_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_user_action_feedback_score
        CHECK (helpfulness_score BETWEEN 1 AND 5)
);

ALTER TABLE check_in
    ADD CONSTRAINT uq_check_in_user_date UNIQUE (user_id, checkin_date);

ALTER TABLE check_in
    ADD CONSTRAINT ck_check_in_emotion CHECK (emotion BETWEEN 1 AND 4);

ALTER TABLE images
    ADD COLUMN user_id BIGINT;

UPDATE images image
SET user_id = (
    SELECT check_in.user_id
    FROM check_in
    WHERE check_in.checkin_id = image.checkin_id
);

ALTER TABLE images
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE images
    ADD CONSTRAINT fk_images_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE;

ALTER TABLE images
    ADD COLUMN object_key VARCHAR(1024);

ALTER TABLE images
    ADD COLUMN content_type VARCHAR(255);

ALTER TABLE images
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;

ALTER TABLE images
    ALTER COLUMN checkin_id DROP NOT NULL;

ALTER TABLE images
    DROP CONSTRAINT ck_images_max_1mib;

ALTER TABLE images
    ALTER COLUMN image_data DROP NOT NULL;

ALTER TABLE images_analysis
    DROP CONSTRAINT ck_images_analysis_score_nonnegative;

ALTER TABLE images_analysis
    ADD CONSTRAINT ck_images_analysis_score CHECK (score BETWEEN 1 AND 8);

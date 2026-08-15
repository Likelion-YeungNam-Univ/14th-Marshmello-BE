ALTER TABLE report
    ADD COLUMN report_month DATE;

UPDATE report
SET report_month = CAST(DATE_TRUNC('MONTH', period_start) AS DATE);

ALTER TABLE report
    ALTER COLUMN report_month SET NOT NULL;

DROP INDEX idx_report_user_period_start;

ALTER TABLE report
    DROP CONSTRAINT uq_report_user_period;

ALTER TABLE report
    DROP CONSTRAINT ck_report_period;

ALTER TABLE report
    DROP COLUMN period_start;

ALTER TABLE report
    DROP COLUMN period_end;

ALTER TABLE report
    ADD CONSTRAINT ck_report_month_first_day
        CHECK (EXTRACT(DAY FROM report_month) = 1);

ALTER TABLE report
    ADD CONSTRAINT uq_report_user_month
        UNIQUE (user_id, report_month);

CREATE INDEX idx_report_user_month
    ON report(user_id, report_month DESC);

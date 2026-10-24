-- Slow delivery: a letter flies from the sender's country to the recipient's and only becomes
-- readable at deliver_at. The countries are copied onto the letter so moving later doesn't
-- rewrite where an old letter came from.

ALTER TABLE letters
    ADD COLUMN origin_country       CHAR(2),
    ADD COLUMN destination_country  CHAR(2),
    ADD COLUMN distance_km          INT,           -- null when a country's location is unknown
    ADD COLUMN deliver_at           TIMESTAMPTZ,
    ADD COLUMN arrival_notified_at  TIMESTAMPTZ;   -- set once the arrival has been processed

-- Letters sent before this migration count as already delivered.
UPDATE letters l
SET origin_country      = s.country,
    destination_country = r.country,
    deliver_at          = l.sent_at,
    arrival_notified_at = l.sent_at
FROM users s, users r
WHERE s.id = l.sender_id AND r.id = l.recipient_id;

ALTER TABLE letters
    ALTER COLUMN origin_country SET NOT NULL,
    ALTER COLUMN destination_country SET NOT NULL,
    ALTER COLUMN deliver_at SET NOT NULL,
    ADD CONSTRAINT ck_letters_deliver_after_send CHECK (deliver_at >= sent_at),
    ADD CONSTRAINT ck_letters_distance CHECK (distance_km IS NULL OR distance_km >= 0);

-- The arrival dispatcher only ever looks at letters still in flight; keep that index tiny.
CREATE INDEX ix_letters_awaiting_arrival ON letters (deliver_at) WHERE arrival_notified_at IS NULL;

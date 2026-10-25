-- Emoji avatars: one emoji from the app's fixed set (validated in code).
ALTER TABLE users ADD COLUMN avatar VARCHAR(16);

-- "Send to a random penpal" is limited per user per day. The counter row is claimed with a single
-- INSERT ... ON CONFLICT DO UPDATE ... WHERE used < limit, so concurrent sends can't exceed it.
CREATE TABLE random_letter_quota (
    user_id  BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    day      DATE   NOT NULL,
    used     INT    NOT NULL,
    PRIMARY KEY (user_id, day),
    CONSTRAINT ck_random_letter_quota_used CHECK (used > 0)
);

-- KREN v2 initial schema

CREATE TABLE users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    display_name    VARCHAR(50)  NOT NULL,
    country         CHAR(2)      NOT NULL,           -- ISO 3166-1 alpha-2
    native_lang     VARCHAR(8)   NOT NULL,           -- ISO 639-1 / BCP 47
    learning_lang   VARCHAR(8)   NOT NULL,
    bio             VARCHAR(500),
    stamp_balance   INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_stamp_balance CHECK (stamp_balance >= 0),
    CONSTRAINT ck_users_langs_differ CHECK (native_lang <> learning_lang)
);

-- Penpal matching: "people whose native language is what I'm learning, and vice versa"
CREATE INDEX ix_users_lang_pair ON users (native_lang, learning_lang);

CREATE TABLE interests (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name    VARCHAR(50) NOT NULL,
    CONSTRAINT uq_interests_name UNIQUE (name)
);

CREATE TABLE user_interests (
    user_id      BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    interest_id  BIGINT NOT NULL REFERENCES interests (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, interest_id)
);

CREATE INDEX ix_user_interests_interest ON user_interests (interest_id);

CREATE TABLE letters (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sender_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipient_id  BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    parent_id     BIGINT        REFERENCES letters (id) ON DELETE SET NULL,  -- set when this is a reply
    body          VARCHAR(5000) NOT NULL,
    sent_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    read_at       TIMESTAMPTZ,
    CONSTRAINT ck_letters_not_self CHECK (sender_id <> recipient_id)
);

CREATE INDEX ix_letters_inbox  ON letters (recipient_id, sent_at DESC);
CREATE INDEX ix_letters_outbox ON letters (sender_id, sent_at DESC);

-- Append-only record of every stamp change; users.stamp_balance is the running total.
CREATE TABLE stamp_ledger (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount      INT         NOT NULL,
    reason      VARCHAR(20) NOT NULL,
    letter_id   BIGINT      REFERENCES letters (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_stamp_ledger_reason CHECK (reason IN ('SIGNUP', 'CHECKIN', 'SEND_LETTER')),
    CONSTRAINT ck_stamp_ledger_amount CHECK (amount <> 0)
);

CREATE INDEX ix_stamp_ledger_user ON stamp_ledger (user_id, created_at DESC);

-- The primary key enforces one check-in per user per day at the database level.
CREATE TABLE daily_checkins (
    user_id       BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    checkin_date  DATE   NOT NULL,
    PRIMARY KEY (user_id, checkin_date)
);

CREATE TABLE blocks (
    blocker_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (blocker_id, blocked_id),
    CONSTRAINT ck_blocks_not_self CHECK (blocker_id <> blocked_id)
);

CREATE INDEX ix_blocks_blocked ON blocks (blocked_id);

INSERT INTO interests (name) VALUES
    ('Music'), ('Movies'), ('Books'), ('Travel'), ('Food'), ('Sports'),
    ('Gaming'), ('Art'), ('Photography'), ('Technology'), ('K-Culture'), ('Language Learning');

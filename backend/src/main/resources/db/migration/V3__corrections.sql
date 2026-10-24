-- Sentence-by-sentence corrections: the recipient of a letter marks up the sender's sentences.

CREATE TABLE corrections (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    letter_id     BIGINT        NOT NULL REFERENCES letters (id) ON DELETE CASCADE,
    corrector_id  BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    comment       VARCHAR(1000),                 -- optional overall note ("Great letter!")
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- One correction per letter. This is also what makes the stamp reward pay out only once, even
    -- if the same correction is submitted twice at the same moment.
    CONSTRAINT uq_corrections_letter UNIQUE (letter_id)
);

CREATE TABLE correction_lines (
    correction_id  BIGINT        NOT NULL REFERENCES corrections (id) ON DELETE CASCADE,
    line_no        INT           NOT NULL,
    original       VARCHAR(1000) NOT NULL,
    corrected      VARCHAR(1000) NOT NULL,
    note           VARCHAR(300),                 -- why: "sounds more natural", "particle 은 vs 는"
    PRIMARY KEY (correction_id, line_no),
    CONSTRAINT ck_correction_lines_changed CHECK (original <> corrected)
);

-- Correcting earns a stamp.
ALTER TABLE stamp_ledger DROP CONSTRAINT ck_stamp_ledger_reason;
ALTER TABLE stamp_ledger ADD CONSTRAINT ck_stamp_ledger_reason
    CHECK (reason IN ('SIGNUP', 'CHECKIN', 'SEND_LETTER', 'CORRECTION'));

-- Small key/value store for the app's own bookkeeping, e.g. which version of the demo data is loaded.
CREATE TABLE app_settings (
    key    VARCHAR(50)  PRIMARY KEY,
    value  VARCHAR(200) NOT NULL
);

-- Demo accounts get their stamps topped up by the demo letter generator.
ALTER TABLE stamp_ledger DROP CONSTRAINT ck_stamp_ledger_reason;
ALTER TABLE stamp_ledger ADD CONSTRAINT ck_stamp_ledger_reason
    CHECK (reason IN ('SIGNUP', 'CHECKIN', 'SEND_LETTER', 'CORRECTION', 'DEMO_TOPUP'));

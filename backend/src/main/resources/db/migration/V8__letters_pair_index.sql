-- One conversation = the letters between two people, both directions. The thread query is
-- "(sender = A AND recipient = B) OR (sender = B AND recipient = A) ORDER BY sent_at DESC", which
-- Postgres answers with two scans of this index (one per direction) merged together.
CREATE INDEX ix_letters_pair ON letters (sender_id, recipient_id, sent_at DESC, id DESC);

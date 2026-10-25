-- The home dashboard asks "which letters I received have I not answered yet?", i.e. NOT EXISTS a
-- letter whose parent_id is that letter. Without this index that's a scan of every letter per row.
CREATE INDEX ix_letters_parent ON letters (parent_id) WHERE parent_id IS NOT NULL;

-- Avatars become illustrated characters (presets m01..m40, drawn client-side) instead of emoji.
-- Existing rows get a preset derived from their id.
UPDATE users SET avatar = 'm' || lpad(((id % 40) + 1)::text, 2, '0');

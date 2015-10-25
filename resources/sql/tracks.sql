-- name: create-tracks!
-- Create tracks table
CREATE TABLE IF NOT EXISTS tracks (
  postId      VARCHAR(255) NOT NULL PRIMARY KEY,
  time        TIMESTAMP    NOT NULL,
  mediaUrl    TEXT         NOT NULL,
  artist      VARCHAR(255) NOT NULL,
  track       VARCHAR(255) NOT NULL,
  parseFailed BOOL         NOT NULL
);

-- name: drop-tracks!
-- Drop tracks table
DROP TABLE IF EXISTS tracks;

-- name: insert-track<!
-- Insert track record
INSERT INTO tracks (
  postId,
  time,
  mediaUrl,
  artist,
  track,
  parseFailed
) VALUES (
  :postId,
  :time,
  :mediaUrl,
  :artist,
  :track,
  :parseFailed
);

-- name: track-exists
-- Check if track with given postId exists
SELECT exists(SELECT 1 FROM tracks where postId=:postId);
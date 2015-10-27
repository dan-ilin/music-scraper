-- name: create-tracks!
-- Create tracks table
CREATE TABLE IF NOT EXISTS tracks (
  postId      VARCHAR   NOT NULL PRIMARY KEY,
  time        TIMESTAMP NOT NULL,
  mediaUrl    TEXT      NOT NULL,
  artist      VARCHAR   NOT NULL,
  track       VARCHAR   NOT NULL,
  spotifyUri  VARCHAR
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
  track
) VALUES (
  :postId,
  :time,
  :mediaUrl,
  :artist,
  :track
);

-- name: track-exists
-- Check if track with given postId exists
SELECT exists(SELECT 1
              FROM tracks
              WHERE postId = :postId);

-- name: update-spotify-uri
-- Update spotifyUri field for track with given postId
UPDATE tracks SET spotifyUri = :spotifyUri WHERE postId = :postId;
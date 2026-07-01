-- Initial login password for xiaxinyu (BCrypt hash of "123456", cost 10).
-- Change after first login in production.

UPDATE fs_user
SET password   = '$2a$10$iC43Pz5/CGBxB.t/eZpK.eKpngTwP5WnKLBtYMARgt0kU0/4HQCqe',
    updated_at = NOW()
WHERE username = 'xiaxinyu';

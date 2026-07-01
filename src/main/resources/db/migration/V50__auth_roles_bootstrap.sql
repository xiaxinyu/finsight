-- Bootstrap ADMIN / USER roles and grant ADMIN to existing users (upgrade-safe).
INSERT INTO fs_role (code, name, description, created_at, updated_at)
SELECT 'ADMIN', 'Administrator', 'Full system administration', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM fs_role WHERE code = 'ADMIN');

INSERT INTO fs_role (code, name, description, created_at, updated_at)
SELECT 'USER', 'Standard user', 'Standard authenticated access', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM fs_role WHERE code = 'USER');

INSERT INTO fs_user_role (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, NOW(), NOW()
FROM fs_user u
CROSS JOIN fs_role r
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM fs_user_role ur
      INNER JOIN fs_role rr ON rr.id = ur.role_id
      WHERE ur.user_id = u.id AND rr.code = 'ADMIN'
  );

INSERT INTO fs_user_role (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, NOW(), NOW()
FROM fs_user u
CROSS JOIN fs_role r
WHERE r.code = 'USER'
  AND NOT EXISTS (
      SELECT 1 FROM fs_user_role ur
      WHERE ur.user_id = u.id
  );

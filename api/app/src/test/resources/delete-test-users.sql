-- delete all setup test users and roles after each test
DELETE
FROM "user"
WHERE username ILIKE '%test%';

DELETE
FROM "role"
WHERE name ILIKE '%test%'
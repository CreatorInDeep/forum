INSERT INTO users (username, email, role, password_hash, created_at)
VALUES (
	'admin',
	'admin@example.com',
	'ADMIN',
	'$2a$10$nWlXA5gcZSOyyUYXlFm34.9OXnoivKplnXtEmXdOCY90YQ7GdJu.6',
	CURRENT_TIMESTAMP
);

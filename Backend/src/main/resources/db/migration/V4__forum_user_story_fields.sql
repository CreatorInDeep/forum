ALTER TABLE posts
	ADD COLUMN created_by BIGINT,
	ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE,
	ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;

ALTER TABLE replies
	ADD COLUMN created_by BIGINT,
	ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

UPDATE posts
SET created_by = (SELECT id FROM users WHERE username = 'admin'),
	updated_at = created_at
WHERE created_by IS NULL;

UPDATE replies
SET created_by = (SELECT id FROM users WHERE username = 'admin'),
	updated_at = created_at
WHERE created_by IS NULL;

ALTER TABLE posts
	ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE replies
	ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE posts
	ADD CONSTRAINT uk_posts_title UNIQUE (title),
	ADD CONSTRAINT fk_posts_created_by
		FOREIGN KEY (created_by)
		REFERENCES users (id)
		ON DELETE SET NULL;

ALTER TABLE replies
	ADD CONSTRAINT fk_replies_created_by
		FOREIGN KEY (created_by)
		REFERENCES users (id)
		ON DELETE SET NULL;

CREATE INDEX idx_posts_created_by ON posts (created_by);
CREATE INDEX idx_replies_created_by ON replies (created_by);

ALTER TABLE posts
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN published_at TIMESTAMPTZ,
    ADD COLUMN scheduled_at TIMESTAMPTZ;

CREATE INDEX idx_posts_status ON posts(status);

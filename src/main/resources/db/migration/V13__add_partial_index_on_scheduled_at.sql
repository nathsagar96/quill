CREATE INDEX idx_posts_scheduled_at ON posts (scheduled_at) WHERE status = 'SCHEDULED';

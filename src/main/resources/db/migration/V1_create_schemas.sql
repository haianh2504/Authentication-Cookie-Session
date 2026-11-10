CREATE TABLE images(
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       image_url TEXT NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE admin(
                      email VARCHAR(250) PRIMARY KEY,
                      password_hash VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_images_created_at ON images (created_at);
-- gen_random_uuid() es core desde PostgreSQL 13; pgcrypto por compatibilidad.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- Módulo de auth: users, roles, user_roles
-- ============================================================

CREATE TABLE IF NOT EXISTS roles (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        varchar(64) NOT NULL UNIQUE,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users (
    id             uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    email          varchar(255) NOT NULL UNIQUE,
    password_hash  varchar(255) NOT NULL,
    full_name      varchar(255),
    enabled        boolean      NOT NULL DEFAULT true,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id  uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id  uuid NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles (name) VALUES ('ROLE_MEMBER'), ('ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;

CREATE TABLE IF NOT EXISTS memberships (
    id              uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid          NOT NULL,
    plan            varchar(64)   NOT NULL,
    price           numeric(10,2) NOT NULL,
    currency        varchar(3)    NOT NULL DEFAULT 'EUR',
    renewal_months  int           NOT NULL DEFAULT 1,
    auto_renew      boolean       NOT NULL DEFAULT false,
    status          varchar(16)   NOT NULL,
    start_date      timestamptz   NOT NULL,
    end_date        timestamptz   NOT NULL,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_memberships_user_id ON memberships (user_id);
CREATE INDEX IF NOT EXISTS idx_memberships_auto_renew ON memberships (auto_renew, end_date);

CREATE TABLE IF NOT EXISTS payments (
    id            uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    membership_id uuid          NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
    amount        numeric(10,2) NOT NULL,
    currency      varchar(3)    NOT NULL,
    months        int           NOT NULL,
    renewal_type  varchar(16)   NOT NULL,
    paid_at       timestamptz   NOT NULL,
    created_at    timestamptz   NOT NULL DEFAULT now(),
    updated_at    timestamptz   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payments_membership_id ON payments (membership_id);

-- Membresía de demo para probar los endpoints (id fijo).
INSERT INTO memberships (id, user_id, plan, price, currency, renewal_months, auto_renew, status, start_date, end_date)
VALUES ('11111111-1111-1111-1111-111111111111',
        '22222222-2222-2222-2222-222222222222',
        'PRO', 9.99, 'EUR', 1, true, 'ACTIVE', now(), now() + interval '30 days')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Módulo de comunidad: posts, comments, likes, notifications
-- ============================================================

CREATE TABLE IF NOT EXISTS posts (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id   uuid         NOT NULL,
    title       varchar(200) NOT NULL,
    content     text         NOT NULL,
    status      varchar(16)  NOT NULL DEFAULT 'VISIBLE',
    like_count  bigint       NOT NULL DEFAULT 0,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_posts_status ON posts (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_author ON posts (author_id);

CREATE TABLE IF NOT EXISTS comments (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     uuid         NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author_id   uuid         NOT NULL,
    content     text         NOT NULL,
    status      varchar(16)  NOT NULL DEFAULT 'VISIBLE',
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_comments_post ON comments (post_id, created_at);

CREATE TABLE IF NOT EXISTS likes (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     uuid         NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id     uuid         NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_like UNIQUE (post_id, user_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id            uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id  uuid         NOT NULL,
    type          varchar(32)  NOT NULL,
    message       varchar(500) NOT NULL,
    post_id       uuid,
    is_read       boolean      NOT NULL DEFAULT false,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications (recipient_id, is_read, created_at DESC);

-- ============================================================
-- Módulo de directos WebRTC: live_events
-- ============================================================

CREATE TABLE IF NOT EXISTS live_events (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    title       varchar(200) NOT NULL,
    description text,
    starts_at   timestamptz  NOT NULL,
    status      varchar(16)  NOT NULL DEFAULT 'SCHEDULED',
    host_id     uuid         NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_live_events_status ON live_events (status, starts_at);
CREATE INDEX IF NOT EXISTS idx_live_events_host ON live_events (host_id);

-- ============================================================
-- Módulo de contenido: content_modules, content_items, content_progress
-- ============================================================

CREATE TABLE IF NOT EXISTS content_modules (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    title       varchar(200) NOT NULL,
    description text,
    position    int          NOT NULL DEFAULT 0,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS content_items (
    id               uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id        uuid          NOT NULL REFERENCES content_modules(id) ON DELETE CASCADE,
    title            varchar(200)  NOT NULL,
    type             varchar(16)   NOT NULL,
    url              varchar(1000) NOT NULL,
    duration_seconds int           NOT NULL DEFAULT 0,
    position         int           NOT NULL DEFAULT 0,
    created_at       timestamptz   NOT NULL DEFAULT now(),
    updated_at       timestamptz   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_content_items_module ON content_items (module_id, position);

CREATE TABLE IF NOT EXISTS content_progress (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid        NOT NULL,
    item_id     uuid        NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    progress    int         NOT NULL DEFAULT 0,
    completed   boolean     NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_progress UNIQUE (user_id, item_id)
);

-- ============================================================
-- Seeds: usuario admin + membresía activa + contenido demo
-- ============================================================

-- Admin demo: admin@demo.com / admin1234 (BCrypt vía pgcrypto, compatible con Spring)
INSERT INTO users (email, password_hash, full_name)
VALUES ('admin@demo.com', crypt('admin1234', gen_salt('bf')), 'Administrador')
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE u.email = 'admin@demo.com' AND r.name IN ('ROLE_ADMIN', 'ROLE_MEMBER')
ON CONFLICT DO NOTHING;

-- Membresía ACTIVE para el admin (idempotente: solo si no tiene ya una)
INSERT INTO memberships (user_id, plan, price, currency, renewal_months, auto_renew, status, start_date, end_date)
SELECT u.id, 'PRO', 9.99, 'EUR', 1, true, 'ACTIVE', now(), now() + interval '30 days'
FROM users u
WHERE u.email = 'admin@demo.com'
  AND NOT EXISTS (SELECT 1 FROM memberships m WHERE m.user_id = u.id);

-- Contenido demo
INSERT INTO content_modules (id, title, description, position) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Onboarding', 'Primeros pasos en la plataforma', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_items (id, module_id, title, type, url, duration_seconds, position) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Vídeo de bienvenida', 'VIDEO', 'https://example.com/welcome.mp4', 300, 1),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Guía en PDF', 'PDF', 'https://example.com/guide.pdf', 0, 2),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Podcast introductorio', 'AUDIO', 'https://example.com/intro.mp3', 600, 3)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Módulo de reservas
-- ============================================================

CREATE TABLE IF NOT EXISTS reservations (
    id                uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           uuid         NOT NULL,
    fecha_hora        timestamptz  NOT NULL,
    duracion_minutos  int          NOT NULL,
    estado            varchar(20)  NOT NULL DEFAULT 'PENDIENTE_PAGO',
    payment_order     varchar(12)  UNIQUE,
    amount_cents      int          NOT NULL DEFAULT 0,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_reservations_user ON reservations (user_id, fecha_hora DESC);

-- Таблица тарифных планов
CREATE TABLE IF NOT EXISTS subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    storage_limit_bytes BIGINT NOT NULL,

    price_per_month DECIMAL(7,2) NOT NULL DEFAULT 0, -- макс. цена за месяц XXXXX.XX
    price_per_year DECIMAL(8,2) NOT NULL DEFAULT 0,  -- макс. цена за год  XXXXXX.XX

    max_file_size_bytes BIGINT,
    max_files_count INT,

    can_share_files BOOLEAN DEFAULT TRUE,
    can_create_folders BOOLEAN DEFAULT TRUE,

    priority INT DEFAULT 0,  -- уровень доступа
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица подписок пользователей
CREATE TABLE IF NOT EXISTS user_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,

    -- Данные об оплате
    billing_period VARCHAR(20) NOT NULL CHECK (billing_period IN ('MONTHLY', 'YEARLY')),
    auto_renew BOOLEAN DEFAULT FALSE,
    last_payment_at TIMESTAMP,
    next_payment_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, plan_id)
);

-- Индексы
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_user_id ON user_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_expires_at ON user_subscriptions(expires_at);
CREATE INDEX IF NOT EXISTS idx_subscription_plans_name ON subscription_plans(name);
CREATE INDEX IF NOT EXISTS idx_subscription_plans_is_active ON subscription_plans(is_active);

-- Дефолтный тариф
ALTER TABLE users ADD COLUMN IF NOT EXISTS storage_limit BIGINT DEFAULT 1073741824;
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_id UUID REFERENCES subscription_plans(id);
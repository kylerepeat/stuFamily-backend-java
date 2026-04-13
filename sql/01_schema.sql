SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

BEGIN;

CREATE SCHEMA IF NOT EXISTS stufamily;
SET search_path TO stufamily, public;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_type') THEN
        CREATE TYPE user_type AS ENUM ('ADMIN', 'WECHAT', 'HYBRID');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_status') THEN
        CREATE TYPE user_status AS ENUM ('ACTIVE', 'DISABLED', 'LOCKED');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'product_type') THEN
        CREATE TYPE product_type AS ENUM ('FAMILY_CARD', 'VALUE_ADDED_SERVICE');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'publish_status') THEN
        CREATE TYPE publish_status AS ENUM ('DRAFT', 'ON_SHELF', 'OFF_SHELF');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'duration_type') THEN
        CREATE TYPE duration_type AS ENUM ('MONTH', 'SEMESTER', 'YEAR');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_type') THEN
        CREATE TYPE order_type AS ENUM ('FAMILY_CARD', 'VALUE_ADDED_SERVICE');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_status') THEN
        CREATE TYPE order_status AS ENUM ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'EXPIRED', 'REFUNDED');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status') THEN
        CREATE TYPE payment_status AS ENUM ('INITIATED', 'SUCCESS', 'FAILED', 'CLOSED', 'REFUNDED');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'member_status') THEN
        CREATE TYPE member_status AS ENUM ('ACTIVE', 'EXPIRED', 'CANCELLED');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'group_status') THEN
        CREATE TYPE group_status AS ENUM ('ACTIVE', 'CLOSED');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'message_status') THEN
        CREATE TYPE message_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'entitlement_status') THEN
        CREATE TYPE entitlement_status AS ENUM ('ACTIVE', 'EXPIRED', 'USED_UP', 'CANCELLED');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    user_no VARCHAR(32) NOT NULL UNIQUE,
    user_type user_type NOT NULL DEFAULT 'WECHAT',
    status user_status NOT NULL DEFAULT 'ACTIVE',
    token_version BIGINT NOT NULL DEFAULT 0,
    username VARCHAR(64),
    password_hash VARCHAR(255),
    openid VARCHAR(64),
    unionid VARCHAR(64),
    nickname VARCHAR(100),
    avatar_url TEXT,
    phone VARCHAR(32),
    email VARCHAR(128),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sys_user_username UNIQUE (username),
    CONSTRAINT uq_sys_user_openid UNIQUE (openid),
    CONSTRAINT chk_sys_user_login_source CHECK (
        (user_type IN ('ADMIN', 'HYBRID') AND username IS NOT NULL) OR
        (user_type IN ('WECHAT', 'HYBRID') AND openid IS NOT NULL)
    )
);


CREATE INDEX IF NOT EXISTS idx_sys_user_type_status ON sys_user (user_type, status);
CREATE INDEX IF NOT EXISTS idx_sys_user_phone ON sys_user (phone);

CREATE TABLE IF NOT EXISTS sys_admin_user (
    id BIGSERIAL PRIMARY KEY,
    user_no VARCHAR(32) NOT NULL UNIQUE,
    status user_status NOT NULL DEFAULT 'ACTIVE',
    token_version BIGINT NOT NULL DEFAULT 0,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    avatar_url TEXT,
    phone VARCHAR(32),
    email VARCHAR(128),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sys_admin_user_status ON sys_admin_user (status);
CREATE INDEX IF NOT EXISTS idx_sys_admin_user_phone ON sys_admin_user (phone);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_builtin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS auth_refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token_jti VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_user ON auth_refresh_token (user_id, expires_at DESC);

CREATE TABLE IF NOT EXISTS visibility_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(80) NOT NULL UNIQUE,
    allow_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    require_login BOOLEAN NOT NULL DEFAULT FALSE,
    require_family_registered BOOLEAN NOT NULL DEFAULT FALSE,
    require_active_family_card BOOLEAN NOT NULL DEFAULT FALSE,
    require_value_added_entitlement BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_visibility_rule_anonymous CHECK (NOT (allow_anonymous AND require_login))
);

CREATE TABLE IF NOT EXISTS service_category (
    id BIGSERIAL PRIMARY KEY,
    category_code VARCHAR(40) NOT NULL UNIQUE,
    category_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS product (
    id BIGSERIAL PRIMARY KEY,
    product_no VARCHAR(40) NOT NULL UNIQUE,
    product_type product_type NOT NULL,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(255),
    detail_content TEXT NOT NULL,
    image_urls JSONB NOT NULL DEFAULT '[]'::JSONB,
    contact_name VARCHAR(100),
    contact_phone VARCHAR(32),
    service_start_at TIMESTAMPTZ,
    service_end_at TIMESTAMPTZ,
    sale_start_at TIMESTAMPTZ,
    sale_end_at TIMESTAMPTZ,
    publish_status publish_status NOT NULL DEFAULT 'DRAFT',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    is_top BOOLEAN NOT NULL DEFAULT FALSE,
    display_priority INT NOT NULL DEFAULT 0,
    list_visibility_rule_id BIGINT REFERENCES visibility_rule(id),
    detail_visibility_rule_id BIGINT REFERENCES visibility_rule(id),
    category_id BIGINT REFERENCES service_category(id),
    created_by BIGINT REFERENCES sys_user(id),
    updated_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_product_service_time CHECK (service_end_at IS NULL OR service_start_at IS NULL OR service_end_at >= service_start_at),
    CONSTRAINT chk_product_sale_time CHECK (sale_end_at IS NULL OR sale_start_at IS NULL OR sale_end_at >= sale_start_at)
);

CREATE INDEX IF NOT EXISTS idx_product_list ON product (product_type, publish_status, is_top, display_priority DESC);

CREATE TABLE IF NOT EXISTS product_family_card_plan (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    duration_type duration_type NOT NULL,
    duration_months INT NOT NULL CHECK (duration_months > 0),
    price_cents BIGINT NOT NULL CHECK (price_cents >= 0),
    max_family_members INT NOT NULL CHECK (max_family_members > 0),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (product_id, duration_type)
);

CREATE TABLE IF NOT EXISTS product_value_added_sku (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    sku_no VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    unit_name VARCHAR(40) NOT NULL DEFAULT 'item',
    price_cents BIGINT NOT NULL CHECK (price_cents >= 0),
    max_purchase_qty INT NOT NULL DEFAULT 1 CHECK (max_purchase_qty > 0),
    service_notice TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_main (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    buyer_user_id BIGINT NOT NULL REFERENCES sys_user(id),
    order_type order_type NOT NULL,
    order_status order_status NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount_cents BIGINT NOT NULL CHECK (total_amount_cents >= 0),
    discount_amount_cents BIGINT NOT NULL DEFAULT 0 CHECK (discount_amount_cents >= 0),
    payable_amount_cents BIGINT NOT NULL CHECK (payable_amount_cents >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    source_channel VARCHAR(32) NOT NULL DEFAULT 'WEIXIN_MINIAPP',
    client_ip INET,
    expire_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancel_reason VARCHAR(255),
    remark VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_order_amount_math CHECK (payable_amount_cents = total_amount_cents - discount_amount_cents)
);

CREATE INDEX IF NOT EXISTS idx_order_main_user_status ON order_main (buyer_user_id, order_status, created_at DESC);

CREATE TABLE IF NOT EXISTS order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES order_main(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES product(id),
    product_type_snapshot product_type NOT NULL,
    product_title_snapshot VARCHAR(200) NOT NULL,
    product_brief_snapshot VARCHAR(255),
    product_detail_snapshot JSONB NOT NULL DEFAULT '{}'::JSONB,
    selected_duration_type duration_type,
    selected_duration_months INT,
    service_start_at TIMESTAMPTZ,
    service_end_at TIMESTAMPTZ,
    unit_price_cents BIGINT NOT NULL CHECK (unit_price_cents >= 0),
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    total_price_cents BIGINT NOT NULL CHECK (total_price_cents >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_order_item_total CHECK (total_price_cents = unit_price_cents * quantity)
);

CREATE TABLE IF NOT EXISTS payment_transaction (
    id BIGSERIAL PRIMARY KEY,
    payment_no VARCHAR(50) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL UNIQUE REFERENCES order_main(id),
    payment_status payment_status NOT NULL DEFAULT 'INITIATED',
    channel VARCHAR(32) NOT NULL DEFAULT 'WECHAT_PAY',
    appid VARCHAR(64),
    mchid VARCHAR(64),
    out_trade_no VARCHAR(64) NOT NULL UNIQUE,
    transaction_id VARCHAR(64) UNIQUE,
    trade_type VARCHAR(32),
    payer_openid VARCHAR(64),
    bank_type VARCHAR(32),
    total_amount_cents BIGINT NOT NULL CHECK (total_amount_cents >= 0),
    payer_total_cents BIGINT CHECK (payer_total_cents IS NULL OR payer_total_cents >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    notify_payload JSONB,
    notify_signature TEXT,
    notify_sign_type VARCHAR(16),
    success_time TIMESTAMPTZ,
    fail_reason VARCHAR(255),
    product_type_snapshot product_type,
    product_title_snapshot VARCHAR(200),
    product_meta_snapshot JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_status_created ON payment_transaction (payment_status, created_at DESC);

CREATE TABLE IF NOT EXISTS payment_refund (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payment_transaction(id),
    refund_no VARCHAR(50) NOT NULL UNIQUE,
    wechat_refund_id VARCHAR(64) UNIQUE,
    refund_status VARCHAR(32) NOT NULL,
    refund_amount_cents BIGINT NOT NULL CHECK (refund_amount_cents >= 0),
    reason VARCHAR(255),
    refund_payload JSONB,
    success_time TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS service_review (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES order_main(id) ON DELETE CASCADE,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    buyer_user_id BIGINT NOT NULL REFERENCES sys_user(id),
    product_id BIGINT REFERENCES product(id),
    product_type product_type,
    stars INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_service_review_buyer_created ON service_review (buyer_user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS family_group (
    id BIGSERIAL PRIMARY KEY,
    group_no VARCHAR(40) NOT NULL UNIQUE,
    source_order_id BIGINT NOT NULL UNIQUE REFERENCES order_main(id),
    owner_user_id BIGINT NOT NULL REFERENCES sys_user(id),
    family_card_product_id BIGINT NOT NULL REFERENCES product(id),
    family_card_plan_id BIGINT REFERENCES product_family_card_plan(id),
    max_members INT NOT NULL CHECK (max_members > 0),
    current_members INT NOT NULL DEFAULT 0 CHECK (current_members >= 0),
    status group_status NOT NULL DEFAULT 'ACTIVE',
    activated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expire_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_group_member_limit CHECK (current_members <= max_members)
);

CREATE TABLE IF NOT EXISTS family_member_card (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES family_group(id) ON DELETE CASCADE,
    member_no VARCHAR(40) NOT NULL UNIQUE,
    bound_user_id BIGINT REFERENCES sys_user(id),
    member_name VARCHAR(100) NOT NULL,
    student_or_card_no VARCHAR(64) NOT NULL,
    phone VARCHAR(32),
    card_received_date DATE NOT NULL,
    relation_to_owner VARCHAR(32),
    added_by_user_id BIGINT NOT NULL REFERENCES sys_user(id),
    status member_status NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expired_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancel_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (group_id, student_or_card_no)
);


CREATE INDEX IF NOT EXISTS idx_family_member_group_status ON family_member_card (group_id, status);
CREATE INDEX IF NOT EXISTS idx_family_member_name ON family_member_card (member_name);

CREATE TABLE IF NOT EXISTS family_checkin (
    id BIGSERIAL PRIMARY KEY,
    checkin_no VARCHAR(40) NOT NULL UNIQUE,
    group_id BIGINT NOT NULL REFERENCES family_group(id) ON DELETE CASCADE,
    owner_user_id BIGINT NOT NULL REFERENCES sys_user(id),
    family_member_id BIGINT REFERENCES family_member_card(id) ON DELETE SET NULL,
    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    address_text VARCHAR(255) NOT NULL,
    checked_in_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_family_checkin_latitude CHECK (latitude >= -90 AND latitude <= 90),
    CONSTRAINT chk_family_checkin_longitude CHECK (longitude >= -180 AND longitude <= 180)
);

CREATE INDEX IF NOT EXISTS idx_family_checkin_owner_checked ON family_checkin (owner_user_id, checked_in_at DESC);
CREATE INDEX IF NOT EXISTS idx_family_checkin_group_checked ON family_checkin (group_id, checked_in_at DESC);
CREATE INDEX IF NOT EXISTS idx_family_checkin_member_checked ON family_checkin (family_member_id, checked_in_at DESC);

CREATE TABLE IF NOT EXISTS family_member_audit (
    id BIGSERIAL PRIMARY KEY,
    member_card_id BIGINT NOT NULL REFERENCES family_member_card(id) ON DELETE CASCADE,
    action VARCHAR(32) NOT NULL,
    operator_user_id BIGINT REFERENCES sys_user(id),
    note VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_service_entitlement (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    order_id BIGINT NOT NULL REFERENCES order_main(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    sku_id BIGINT REFERENCES product_value_added_sku(id),
    status entitlement_status NOT NULL DEFAULT 'ACTIVE',
    remaining_uses INT CHECK (remaining_uses IS NULL OR remaining_uses >= 0),
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    ext JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS homepage_banner (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    image_url TEXT NOT NULL,
    link_type VARCHAR(30) NOT NULL DEFAULT 'NONE',
    link_target TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    created_by BIGINT REFERENCES sys_user(id),
    updated_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_banner_time CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at)
);

CREATE TABLE IF NOT EXISTS site_profile (
    id BIGSERIAL PRIMARY KEY,
    community_name VARCHAR(150) NOT NULL,
    banner_slogan VARCHAR(200),
    intro_text TEXT,
    contact_person VARCHAR(64),
    contact_phone VARCHAR(32),
    contact_wechat VARCHAR(64),
    contact_wechat_qr_url TEXT,
    address_text VARCHAR(255),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    ext JSONB NOT NULL DEFAULT '{}'::JSONB,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE UNIQUE INDEX IF NOT EXISTS uq_site_profile_active ON site_profile (active) WHERE active = TRUE;

CREATE TABLE IF NOT EXISTS home_notice (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(50) NOT NULL,
    content TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_home_notice_time CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at)
);

CREATE INDEX IF NOT EXISTS idx_home_notice_enabled_time ON home_notice (enabled, sort_order DESC, id DESC);

CREATE TABLE IF NOT EXISTS parent_message (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES sys_user(id),
    parent_id BIGINT,
    root_id BIGINT,
    sender_type VARCHAR(20) NOT NULL DEFAULT 'USER',
    nickname_snapshot VARCHAR(100),
    avatar_snapshot TEXT,
    content TEXT NOT NULL,
    status message_status NOT NULL DEFAULT 'PENDING',
    viewed BOOLEAN NOT NULL DEFAULT FALSE,
    viewed_at TIMESTAMPTZ,
    replied_at TIMESTAMPTZ,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    review_note VARCHAR(255),
    reviewed_by BIGINT REFERENCES sys_user(id),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE INDEX IF NOT EXISTS idx_parent_message_root ON parent_message(root_id, id);
CREATE INDEX IF NOT EXISTS idx_parent_message_user ON parent_message(user_id, id);
CREATE INDEX IF NOT EXISTS idx_parent_message_admin_list ON parent_message(sender_type, deleted, viewed, replied_at, created_at DESC);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sys_user_updated_at ON sys_user;
CREATE TRIGGER trg_sys_user_updated_at BEFORE UPDATE ON sys_user FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_sys_role_updated_at ON sys_role;
CREATE TRIGGER trg_sys_role_updated_at BEFORE UPDATE ON sys_role FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_sys_admin_user_updated_at ON sys_admin_user;
CREATE TRIGGER trg_sys_admin_user_updated_at BEFORE UPDATE ON sys_admin_user FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_visibility_rule_updated_at ON visibility_rule;
CREATE TRIGGER trg_visibility_rule_updated_at BEFORE UPDATE ON visibility_rule FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_service_category_updated_at ON service_category;
CREATE TRIGGER trg_service_category_updated_at BEFORE UPDATE ON service_category FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_product_updated_at ON product;
CREATE TRIGGER trg_product_updated_at BEFORE UPDATE ON product FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_product_family_card_plan_updated_at ON product_family_card_plan;
CREATE TRIGGER trg_product_family_card_plan_updated_at BEFORE UPDATE ON product_family_card_plan FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_product_value_added_sku_updated_at ON product_value_added_sku;
CREATE TRIGGER trg_product_value_added_sku_updated_at BEFORE UPDATE ON product_value_added_sku FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_order_main_updated_at ON order_main;
CREATE TRIGGER trg_order_main_updated_at BEFORE UPDATE ON order_main FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_payment_transaction_updated_at ON payment_transaction;
CREATE TRIGGER trg_payment_transaction_updated_at BEFORE UPDATE ON payment_transaction FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_payment_refund_updated_at ON payment_refund;
CREATE TRIGGER trg_payment_refund_updated_at BEFORE UPDATE ON payment_refund FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_service_review_updated_at ON service_review;
CREATE TRIGGER trg_service_review_updated_at BEFORE UPDATE ON service_review FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_family_group_updated_at ON family_group;
CREATE TRIGGER trg_family_group_updated_at BEFORE UPDATE ON family_group FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_family_member_card_updated_at ON family_member_card;
CREATE TRIGGER trg_family_member_card_updated_at BEFORE UPDATE ON family_member_card FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_family_checkin_updated_at ON family_checkin;
CREATE TRIGGER trg_family_checkin_updated_at BEFORE UPDATE ON family_checkin FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_user_service_entitlement_updated_at ON user_service_entitlement;
CREATE TRIGGER trg_user_service_entitlement_updated_at BEFORE UPDATE ON user_service_entitlement FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_homepage_banner_updated_at ON homepage_banner;
CREATE TRIGGER trg_homepage_banner_updated_at BEFORE UPDATE ON homepage_banner FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_site_profile_updated_at ON site_profile;
CREATE TRIGGER trg_site_profile_updated_at BEFORE UPDATE ON site_profile FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_home_notice_updated_at ON home_notice;
CREATE TRIGGER trg_home_notice_updated_at BEFORE UPDATE ON home_notice FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_parent_message_updated_at ON parent_message;
CREATE TRIGGER trg_parent_message_updated_at BEFORE UPDATE ON parent_message FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMIT;

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

BEGIN;

SET search_path TO stufamily, public;

-- Postman test account (hybrid admin + wechat-openid for local API testing)
INSERT INTO sys_user (
    user_no,
    user_type,
    status,
    username,
    password_hash,
    openid,
    nickname,
    avatar_url
) VALUES (
    'UPOSTMANADMIN001',
    'HYBRID',
    'ACTIVE',
    'postman_admin',
    '$2a$10$l9Mmm/.XRxO7VMvUoHv8d.6zNYxy6W24dIXNJRIspp0ZHH3OwinGS',
    'openid_postman_admin_001',
    'Postman Admin',
    'https://example.com/avatar/postman-admin.png'
)
ON CONFLICT (username) DO UPDATE SET
    user_type = EXCLUDED.user_type,
    status = EXCLUDED.status,
    password_hash = EXCLUDED.password_hash,
    openid = EXCLUDED.openid,
    nickname = EXCLUDED.nickname,
    avatar_url = EXCLUDED.avatar_url,
    updated_at = NOW();

-- Postman admin account for admin-api authentication (sys_admin_user)
INSERT INTO sys_admin_user (
    user_no,
    status,
    username,
    password_hash,
    nickname,
    avatar_url
) VALUES (
    'APOSTMANADMIN001',
    'ACTIVE',
    'postman_admin',
    '$2a$10$l9Mmm/.XRxO7VMvUoHv8d.6zNYxy6W24dIXNJRIspp0ZHH3OwinGS',
    'Postman Admin',
    'https://example.com/avatar/postman-admin.png'
)
ON CONFLICT (username) DO UPDATE SET
    status = EXCLUDED.status,
    password_hash = EXCLUDED.password_hash,
    nickname = EXCLUDED.nickname,
    avatar_url = EXCLUDED.avatar_url,
    updated_at = NOW();

-- Visibility rules used by seed products
INSERT INTO visibility_rule (
    rule_name,
    allow_anonymous,
    require_login,
    require_family_registered,
    require_active_family_card,
    require_value_added_entitlement
) VALUES
    ('POSTMAN_PUBLIC', TRUE, FALSE, FALSE, FALSE, FALSE),
    ('POSTMAN_LOGIN', FALSE, TRUE, FALSE, FALSE, FALSE)
ON CONFLICT (rule_name) DO UPDATE SET
    allow_anonymous = EXCLUDED.allow_anonymous,
    require_login = EXCLUDED.require_login,
    require_family_registered = EXCLUDED.require_family_registered,
    require_active_family_card = EXCLUDED.require_active_family_card,
    require_value_added_entitlement = EXCLUDED.require_value_added_entitlement,
    updated_at = NOW();

-- Family card product
INSERT INTO product (
    product_no,
    product_type,
    title,
    subtitle,
    detail_content,
    image_urls,
    contact_name,
    contact_phone,
    service_start_at,
    service_end_at,
    sale_start_at,
    sale_end_at,
    publish_status,
    is_deleted,
    is_top,
    display_priority,
    list_visibility_rule_id,
    detail_visibility_rule_id
) VALUES (
    'P-FAMILY-POSTMAN-001',
    'FAMILY_CARD',
    'Family Card (Postman Seed)',
    'Monthly/Semester/Year plans',
    'Seed family card product for manual API testing.',
    '["https://example.com/images/family-card.png"]'::jsonb,
    'Service Desk',
    '13800001111',
    NOW(),
    NOW() + INTERVAL '365 day',
    NOW() - INTERVAL '7 day',
    NOW() + INTERVAL '365 day',
    'ON_SHELF',
    FALSE,
    TRUE,
    100,
    (SELECT id FROM visibility_rule WHERE rule_name = 'POSTMAN_PUBLIC'),
    (SELECT id FROM visibility_rule WHERE rule_name = 'POSTMAN_LOGIN')
)
ON CONFLICT (product_no) DO UPDATE SET
    title = EXCLUDED.title,
    subtitle = EXCLUDED.subtitle,
    detail_content = EXCLUDED.detail_content,
    image_urls = EXCLUDED.image_urls,
    contact_name = EXCLUDED.contact_name,
    contact_phone = EXCLUDED.contact_phone,
    service_start_at = EXCLUDED.service_start_at,
    service_end_at = EXCLUDED.service_end_at,
    sale_start_at = EXCLUDED.sale_start_at,
    sale_end_at = EXCLUDED.sale_end_at,
    publish_status = EXCLUDED.publish_status,
    is_deleted = EXCLUDED.is_deleted,
    is_top = EXCLUDED.is_top,
    display_priority = EXCLUDED.display_priority,
    list_visibility_rule_id = EXCLUDED.list_visibility_rule_id,
    detail_visibility_rule_id = EXCLUDED.detail_visibility_rule_id,
    updated_at = NOW();

INSERT INTO product_family_card_plan (
    product_id,
    duration_type,
    duration_months,
    price_cents,
    max_family_members,
    enabled,
    sort_order
) VALUES
    ((SELECT id FROM product WHERE product_no = 'P-FAMILY-POSTMAN-001'), 'MONTH', 1, 19900, 3, TRUE, 1),
    ((SELECT id FROM product WHERE product_no = 'P-FAMILY-POSTMAN-001'), 'SEMESTER', 6, 99900, 5, TRUE, 2),
    ((SELECT id FROM product WHERE product_no = 'P-FAMILY-POSTMAN-001'), 'YEAR', 12, 179900, 8, TRUE, 3)
ON CONFLICT (product_id, duration_type) DO UPDATE SET
    duration_months = EXCLUDED.duration_months,
    price_cents = EXCLUDED.price_cents,
    max_family_members = EXCLUDED.max_family_members,
    enabled = EXCLUDED.enabled,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

-- Value-added service product
INSERT INTO product (
    product_no,
    product_type,
    title,
    subtitle,
    detail_content,
    image_urls,
    contact_name,
    contact_phone,
    service_start_at,
    service_end_at,
    sale_start_at,
    sale_end_at,
    publish_status,
    is_deleted,
    is_top,
    display_priority,
    list_visibility_rule_id,
    detail_visibility_rule_id
) VALUES (
    'P-VAS-POSTMAN-001',
    'VALUE_ADDED_SERVICE',
    'Station Pickup (Postman Seed)',
    'One-way station pickup',
    'Seed value-added service product for manual API testing.',
    '["https://example.com/images/station-pickup.png"]'::jsonb,
    'Service Desk',
    '13800002222',
    NOW(),
    NOW() + INTERVAL '365 day',
    NOW() - INTERVAL '7 day',
    NOW() + INTERVAL '365 day',
    'ON_SHELF',
    FALSE,
    FALSE,
    50,
    (SELECT id FROM visibility_rule WHERE rule_name = 'POSTMAN_PUBLIC'),
    (SELECT id FROM visibility_rule WHERE rule_name = 'POSTMAN_LOGIN')
)
ON CONFLICT (product_no) DO UPDATE SET
    title = EXCLUDED.title,
    subtitle = EXCLUDED.subtitle,
    detail_content = EXCLUDED.detail_content,
    image_urls = EXCLUDED.image_urls,
    contact_name = EXCLUDED.contact_name,
    contact_phone = EXCLUDED.contact_phone,
    service_start_at = EXCLUDED.service_start_at,
    service_end_at = EXCLUDED.service_end_at,
    sale_start_at = EXCLUDED.sale_start_at,
    sale_end_at = EXCLUDED.sale_end_at,
    publish_status = EXCLUDED.publish_status,
    is_deleted = EXCLUDED.is_deleted,
    is_top = EXCLUDED.is_top,
    display_priority = EXCLUDED.display_priority,
    list_visibility_rule_id = EXCLUDED.list_visibility_rule_id,
    detail_visibility_rule_id = EXCLUDED.detail_visibility_rule_id,
    updated_at = NOW();

INSERT INTO product_value_added_sku (
    product_id,
    sku_no,
    title,
    unit_name,
    price_cents,
    max_purchase_qty,
    service_notice,
    enabled
) VALUES (
    (SELECT id FROM product WHERE product_no = 'P-VAS-POSTMAN-001'),
    'SKU-VAS-POSTMAN-001',
    'Station Pickup x1',
    'trip',
    12900,
    3,
    'Please book at least 24h in advance.',
    TRUE
)
ON CONFLICT (sku_no) DO UPDATE SET
    title = EXCLUDED.title,
    unit_name = EXCLUDED.unit_name,
    price_cents = EXCLUDED.price_cents,
    max_purchase_qty = EXCLUDED.max_purchase_qty,
    service_notice = EXCLUDED.service_notice,
    enabled = EXCLUDED.enabled,
    updated_at = NOW();

-- Home page seed data
UPDATE site_profile
SET active = FALSE, updated_at = NOW()
WHERE active = TRUE;

INSERT INTO site_profile (
    community_name,
    banner_slogan,
    intro_text,
    contact_person,
    contact_phone,
    contact_wechat,
    contact_wechat_qr_url,
    address_text,
    latitude,
    longitude,
    ext,
    active
) VALUES (
    'stuFamily Community',
    '让每一次离家都更安心',
    'Postman seed profile for manual testing.',
    'Service Teacher',
    '021-12345678',
    'stufamily-service',
    'https://example.com/images/stufamily-wechat-qr.png',
    'No. 88 Campus Road',
    31.2304160,
    121.4737010,
    '{}'::jsonb,
    TRUE
);

DELETE FROM homepage_banner
WHERE title IN ('Postman Banner 1', 'Postman Banner 2');

INSERT INTO homepage_banner (
    title,
    image_url,
    link_type,
    link_target,
    sort_order,
    enabled,
    start_at,
    end_at
) VALUES
    ('Postman Banner 1', 'https://example.com/images/banner-1.png', 'NONE', NULL, 1, TRUE, NOW() - INTERVAL '1 day', NOW() + INTERVAL '365 day'),
    ('Postman Banner 2', 'https://example.com/images/banner-2.png', 'NONE', NULL, 2, TRUE, NOW() - INTERVAL '1 day', NOW() + INTERVAL '365 day');

DELETE FROM home_notice
WHERE title = 'Postman 首页通知';

INSERT INTO home_notice (
    title,
    content,
    enabled,
    sort_order,
    start_at,
    end_at
) VALUES (
    'Postman 首页通知',
    '欢迎使用 stuFamily 小程序，购买家庭卡后可添加家人并享受服务。',
    TRUE,
    100,
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '365 day'
);

-- Seed a paid order and family group for family/search/cancel API testing without depending on real WeChat pay.
INSERT INTO order_main (
    order_no,
    buyer_user_id,
    order_type,
    order_status,
    total_amount_cents,
    discount_amount_cents,
    payable_amount_cents,
    currency,
    source_channel,
    client_ip,
    expire_at,
    paid_at,
    created_at,
    updated_at
) VALUES (
    'ORDPOSTMANSEED000001',
    (SELECT id FROM sys_user WHERE username = 'postman_admin'),
    'FAMILY_CARD',
    'PAID',
    19900,
    0,
    19900,
    'CNY',
    'WEIXIN_MINIAPP',
    '127.0.0.1'::inet,
    NOW() + INTERVAL '30 day',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day',
    NOW()
)
ON CONFLICT (order_no) DO UPDATE SET
    buyer_user_id = EXCLUDED.buyer_user_id,
    order_type = EXCLUDED.order_type,
    order_status = EXCLUDED.order_status,
    total_amount_cents = EXCLUDED.total_amount_cents,
    discount_amount_cents = EXCLUDED.discount_amount_cents,
    payable_amount_cents = EXCLUDED.payable_amount_cents,
    currency = EXCLUDED.currency,
    source_channel = EXCLUDED.source_channel,
    client_ip = EXCLUDED.client_ip,
    expire_at = EXCLUDED.expire_at,
    paid_at = EXCLUDED.paid_at,
    updated_at = NOW();

DELETE FROM order_item
WHERE order_id = (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000001');

INSERT INTO order_item (
    order_id,
    product_id,
    product_type_snapshot,
    product_title_snapshot,
    product_brief_snapshot,
    product_detail_snapshot,
    selected_duration_type,
    selected_duration_months,
    service_start_at,
    service_end_at,
    unit_price_cents,
    quantity,
    total_price_cents
) VALUES (
    (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000001'),
    (SELECT id FROM product WHERE product_no = 'P-FAMILY-POSTMAN-001'),
    'FAMILY_CARD',
    'Family Card (Postman Seed)',
    'Seed order item',
    '{}'::jsonb,
    'MONTH',
    3,
    NOW(),
    NOW() + INTERVAL '30 day',
    19900,
    1,
    19900
);

INSERT INTO payment_transaction (
    payment_no,
    order_id,
    payment_status,
    channel,
    out_trade_no,
    transaction_id,
    trade_type,
    payer_openid,
    total_amount_cents,
    currency,
    success_time,
    product_type_snapshot,
    product_title_snapshot,
    product_meta_snapshot
) VALUES (
    'PAYPOSTMANSEED000001',
    (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000001'),
    'SUCCESS',
    'WECHAT_PAY',
    'ORDPOSTMANSEED000001',
    'TXNPOSTMANSEED000001',
    'JSAPI',
    'openid_postman_admin_001',
    19900,
    'CNY',
    NOW() - INTERVAL '1 day',
    'FAMILY_CARD',
    'Family Card (Postman Seed)',
    jsonb_build_object(
        'productId', (SELECT id FROM product WHERE product_no = 'P-FAMILY-POSTMAN-001'),
        'durationType', 'MONTH',
        'planId', (SELECT id FROM product_family_card_plan WHERE product_id = (SELECT id FROM product WHERE product_no = 'P-FAMILY-POSTMAN-001') AND duration_type = 'MONTH'),
        'maxMembers', 3
    )
)
ON CONFLICT (out_trade_no) DO UPDATE SET
    order_id = EXCLUDED.order_id,
    payment_status = EXCLUDED.payment_status,
    channel = EXCLUDED.channel,
    transaction_id = EXCLUDED.transaction_id,
    trade_type = EXCLUDED.trade_type,
    payer_openid = EXCLUDED.payer_openid,
    total_amount_cents = EXCLUDED.total_amount_cents,
    currency = EXCLUDED.currency,
    success_time = EXCLUDED.success_time,
    product_type_snapshot = EXCLUDED.product_type_snapshot,
    product_title_snapshot = EXCLUDED.product_title_snapshot,
    product_meta_snapshot = EXCLUDED.product_meta_snapshot,
    updated_at = NOW();

-- Additional paid orders for current seed user (postman_admin), used by purchased list pagination testing.
INSERT INTO order_main (
    order_no,
    buyer_user_id,
    order_type,
    order_status,
    total_amount_cents,
    discount_amount_cents,
    payable_amount_cents,
    currency,
    source_channel,
    client_ip,
    expire_at,
    paid_at,
    created_at,
    updated_at
) VALUES (
    'ORDPOSTMANSEED000002',
    (SELECT id FROM sys_user WHERE username = 'postman_admin'),
    'VALUE_ADDED_SERVICE',
    'PAID',
    12900,
    0,
    12900,
    'CNY',
    'WEIXIN_MINIAPP',
    '127.0.0.1'::inet,
    NOW() + INTERVAL '30 day',
    NOW() - INTERVAL '12 hour',
    NOW() - INTERVAL '12 hour',
    NOW()
)
ON CONFLICT (order_no) DO UPDATE SET
    buyer_user_id = EXCLUDED.buyer_user_id,
    order_type = EXCLUDED.order_type,
    order_status = EXCLUDED.order_status,
    total_amount_cents = EXCLUDED.total_amount_cents,
    discount_amount_cents = EXCLUDED.discount_amount_cents,
    payable_amount_cents = EXCLUDED.payable_amount_cents,
    currency = EXCLUDED.currency,
    source_channel = EXCLUDED.source_channel,
    client_ip = EXCLUDED.client_ip,
    expire_at = EXCLUDED.expire_at,
    paid_at = EXCLUDED.paid_at,
    updated_at = NOW();

DELETE FROM order_item
WHERE order_id = (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000002');

INSERT INTO order_item (
    order_id,
    product_id,
    product_type_snapshot,
    product_title_snapshot,
    product_brief_snapshot,
    product_detail_snapshot,
    selected_duration_type,
    selected_duration_months,
    service_start_at,
    service_end_at,
    unit_price_cents,
    quantity,
    total_price_cents
) VALUES (
    (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000002'),
    (SELECT id FROM product WHERE product_no = 'P-VAS-POSTMAN-001'),
    'VALUE_ADDED_SERVICE',
    'Station Pickup (Postman Seed)',
    'Seed order item',
    '{}'::jsonb,
    NULL,
    NULL,
    NOW(),
    NOW() + INTERVAL '30 day',
    12900,
    1,
    12900
);

INSERT INTO payment_transaction (
    payment_no,
    order_id,
    payment_status,
    channel,
    out_trade_no,
    transaction_id,
    trade_type,
    payer_openid,
    total_amount_cents,
    currency,
    success_time,
    product_type_snapshot,
    product_title_snapshot,
    product_meta_snapshot
) VALUES (
    'PAYPOSTMANSEED000002',
    (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000002'),
    'SUCCESS',
    'WECHAT_PAY',
    'ORDPOSTMANSEED000002',
    'TXNPOSTMANSEED000002',
    'JSAPI',
    'openid_postman_admin_001',
    12900,
    'CNY',
    NOW() - INTERVAL '12 hour',
    'VALUE_ADDED_SERVICE',
    'Station Pickup (Postman Seed)',
    jsonb_build_object(
        'productId', (SELECT id FROM product WHERE product_no = 'P-VAS-POSTMAN-001'),
        'durationType', 'NONE',
        'planId', 0,
        'maxMembers', 0
    )
)
ON CONFLICT (out_trade_no) DO UPDATE SET
    order_id = EXCLUDED.order_id,
    payment_status = EXCLUDED.payment_status,
    channel = EXCLUDED.channel,
    transaction_id = EXCLUDED.transaction_id,
    trade_type = EXCLUDED.trade_type,
    payer_openid = EXCLUDED.payer_openid,
    total_amount_cents = EXCLUDED.total_amount_cents,
    currency = EXCLUDED.currency,
    success_time = EXCLUDED.success_time,
    product_type_snapshot = EXCLUDED.product_type_snapshot,
    product_title_snapshot = EXCLUDED.product_title_snapshot,
    product_meta_snapshot = EXCLUDED.product_meta_snapshot,
    updated_at = NOW();

INSERT INTO order_main (
    order_no,
    buyer_user_id,
    order_type,
    order_status,
    total_amount_cents,
    discount_amount_cents,
    payable_amount_cents,
    currency,
    source_channel,
    client_ip,
    expire_at,
    paid_at,
    created_at,
    updated_at
) VALUES (
    'ORDPOSTMANSEED000003',
    (SELECT id FROM sys_user WHERE username = 'postman_admin'),
    'FAMILY_CARD',
    'PAID',
    99900,
    0,
    99900,
    'CNY',
    'WEIXIN_MINIAPP',
    '127.0.0.1'::inet,
    NOW() + INTERVAL '30 day',
    NOW() - INTERVAL '6 hour',
    NOW() - INTERVAL '6 hour',
    NOW()
)
ON CONFLICT (order_no) DO UPDATE SET
    buyer_user_id = EXCLUDED.buyer_user_id,
    order_type = EXCLUDED.order_type,
    order_status = EXCLUDED.order_status,
    total_amount_cents = EXCLUDED.total_amount_cents,
    discount_amount_cents = EXCLUDED.discount_amount_cents,
    payable_amount_cents = EXCLUDED.payable_amount_cents,
    currency = EXCLUDED.currency,
    source_channel = EXCLUDED.source_channel,
    client_ip = EXCLUDED.client_ip,
    expire_at = EXCLUDED.expire_at,
    paid_at = EXCLUDED.paid_at,
    updated_at = NOW();

DELETE FROM order_item
WHERE order_id = (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000003');

INSERT INTO order_item (
    order_id,
    product_id,
    product_type_snapshot,
    product_title_snapshot,
    product_brief_snapshot,
    product_detail_snapshot,
    selected_duration_type,
    selected_duration_months,
    service_start_at,
    service_end_at,
    unit_price_cents,
    quantity,
    total_price_cents
) VALUES (
    (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000003'),
    (SELECT id FROM product WHERE product_no = 'P-FAMILY-POSTMAN-001'),
    'FAMILY_CARD',
    'Family Card (Postman Seed)',
    'Seed order item',
    '{}'::jsonb,
    'SEMESTER',
    6,
    NOW(),
    NOW() + INTERVAL '180 day',
    99900,
    1,
    99900
);

INSERT INTO payment_transaction (
    payment_no,
    order_id,
    payment_status,
    channel,
    out_trade_no,
    transaction_id,
    trade_type,
    payer_openid,
    total_amount_cents,
    currency,
    success_time,
    product_type_snapshot,
    product_title_snapshot,
    product_meta_snapshot
) VALUES (
    'PAYPOSTMANSEED000003',
    (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000003'),
    'SUCCESS',
    'WECHAT_PAY',
    'ORDPOSTMANSEED000003',
    'TXNPOSTMANSEED000003',
    'JSAPI',
    'openid_postman_admin_001',
    99900,
    'CNY',
    NOW() - INTERVAL '6 hour',
    'FAMILY_CARD',
    'Family Card (Postman Seed)',
    jsonb_build_object(
        'productId', (SELECT id FROM product WHERE product_no = 'P-FAMILY-POSTMAN-001'),
        'durationType', 'SEMESTER',
        'planId', (SELECT id FROM product_family_card_plan WHERE product_id = (SELECT id FROM product WHERE product_no = 'P-FAMILY-POSTMAN-001') AND duration_type = 'SEMESTER'),
        'maxMembers', 5
    )
)
ON CONFLICT (out_trade_no) DO UPDATE SET
    order_id = EXCLUDED.order_id,
    payment_status = EXCLUDED.payment_status,
    channel = EXCLUDED.channel,
    transaction_id = EXCLUDED.transaction_id,
    trade_type = EXCLUDED.trade_type,
    payer_openid = EXCLUDED.payer_openid,
    total_amount_cents = EXCLUDED.total_amount_cents,
    currency = EXCLUDED.currency,
    success_time = EXCLUDED.success_time,
    product_type_snapshot = EXCLUDED.product_type_snapshot,
    product_title_snapshot = EXCLUDED.product_title_snapshot,
    product_meta_snapshot = EXCLUDED.product_meta_snapshot,
    updated_at = NOW();

INSERT INTO service_review (
    order_id,
    order_no,
    buyer_user_id,
    product_id,
    product_type,
    stars,
    content,
    created_at,
    updated_at
) VALUES (
    (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000002'),
    'ORDPOSTMANSEED000002',
    (SELECT id FROM sys_user WHERE username = 'postman_admin'),
    (SELECT id FROM product WHERE product_no = 'P-VAS-POSTMAN-001'),
    'VALUE_ADDED_SERVICE',
    5,
    'Postman seed review: 服务很及时，体验很好。',
    NOW() - INTERVAL '11 hour',
    NOW() - INTERVAL '11 hour'
)
ON CONFLICT (order_no) DO UPDATE SET
    order_id = EXCLUDED.order_id,
    buyer_user_id = EXCLUDED.buyer_user_id,
    product_id = EXCLUDED.product_id,
    product_type = EXCLUDED.product_type,
    stars = EXCLUDED.stars,
    content = EXCLUDED.content,
    updated_at = NOW();

INSERT INTO family_group (
    group_no,
    source_order_id,
    owner_user_id,
    family_card_product_id,
    family_card_plan_id,
    max_members,
    current_members,
    status,
    activated_at,
    expire_at
) VALUES (
    'FGPOSTMANSEED0001',
    (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000001'),
    (SELECT id FROM sys_user WHERE username = 'postman_admin'),
    (SELECT id FROM product WHERE product_no = 'P-FAMILY-POSTMAN-001'),
    (SELECT id FROM product_family_card_plan WHERE product_id = (SELECT id FROM product WHERE product_no = 'P-FAMILY-POSTMAN-001') AND duration_type = 'MONTH'),
    3,
    3,
    'ACTIVE',
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '29 day'
)
ON CONFLICT (source_order_id) DO UPDATE SET
    owner_user_id = EXCLUDED.owner_user_id,
    family_card_product_id = EXCLUDED.family_card_product_id,
    family_card_plan_id = EXCLUDED.family_card_plan_id,
    max_members = EXCLUDED.max_members,
    current_members = EXCLUDED.current_members,
    status = EXCLUDED.status,
    activated_at = EXCLUDED.activated_at,
    expire_at = EXCLUDED.expire_at,
    updated_at = NOW();

INSERT INTO family_member_card (
    group_id,
    member_no,
    member_name,
    student_or_card_no,
    phone,
    card_received_date,
    added_by_user_id,
    status,
    joined_at
) VALUES (
    (SELECT id FROM family_group WHERE source_order_id = (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000001')),
    'MPOSTMANSEED001',
    'Seed Member',
    'STU-SEED-001',
    '13800009999',
    CURRENT_DATE - INTERVAL '10 day',
    (SELECT id FROM sys_user WHERE username = 'postman_admin'),
    'ACTIVE',
    NOW() - INTERVAL '10 day'
)
ON CONFLICT (member_no) DO UPDATE SET
    group_id = EXCLUDED.group_id,
    member_name = EXCLUDED.member_name,
    student_or_card_no = EXCLUDED.student_or_card_no,
    phone = EXCLUDED.phone,
    card_received_date = EXCLUDED.card_received_date,
    added_by_user_id = EXCLUDED.added_by_user_id,
    status = EXCLUDED.status,
    joined_at = EXCLUDED.joined_at,
    updated_at = NOW();

INSERT INTO family_member_card (
    group_id,
    member_no,
    member_name,
    student_or_card_no,
    phone,
    card_received_date,
    added_by_user_id,
    status,
    joined_at
) VALUES (
    (SELECT id FROM family_group WHERE source_order_id = (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000001')),
    'MPOSTMANSEED002',
    'Seed Member Two',
    'STU-SEED-002',
    '13800008888',
    CURRENT_DATE - INTERVAL '8 day',
    (SELECT id FROM sys_user WHERE username = 'postman_admin'),
    'ACTIVE',
    NOW() - INTERVAL '8 day'
)
ON CONFLICT (member_no) DO UPDATE SET
    group_id = EXCLUDED.group_id,
    member_name = EXCLUDED.member_name,
    student_or_card_no = EXCLUDED.student_or_card_no,
    phone = EXCLUDED.phone,
    card_received_date = EXCLUDED.card_received_date,
    added_by_user_id = EXCLUDED.added_by_user_id,
    status = EXCLUDED.status,
    joined_at = EXCLUDED.joined_at,
    updated_at = NOW();

INSERT INTO family_member_card (
    group_id,
    member_no,
    member_name,
    student_or_card_no,
    phone,
    card_received_date,
    added_by_user_id,
    status,
    joined_at
) VALUES (
    (SELECT id FROM family_group WHERE source_order_id = (SELECT id FROM order_main WHERE order_no = 'ORDPOSTMANSEED000001')),
    'MPOSTMANSEED003',
    'Seed Member Three',
    'STU-SEED-003',
    '13800007777',
    CURRENT_DATE - INTERVAL '5 day',
    (SELECT id FROM sys_user WHERE username = 'postman_admin'),
    'ACTIVE',
    NOW() - INTERVAL '5 day'
)
ON CONFLICT (member_no) DO UPDATE SET
    group_id = EXCLUDED.group_id,
    member_name = EXCLUDED.member_name,
    student_or_card_no = EXCLUDED.student_or_card_no,
    phone = EXCLUDED.phone,
    card_received_date = EXCLUDED.card_received_date,
    added_by_user_id = EXCLUDED.added_by_user_id,
    status = EXCLUDED.status,
    joined_at = EXCLUDED.joined_at,
    updated_at = NOW();

-- Parent message seed data (for admin message list/filter/reply testing)
DELETE FROM parent_message
WHERE content LIKE 'SeedParentMessage:%';

-- Case 1: unread + unreplied
WITH root AS (
    INSERT INTO parent_message (
        user_id,
        parent_id,
        root_id,
        sender_type,
        nickname_snapshot,
        avatar_snapshot,
        content,
        status,
        viewed,
        viewed_at,
        replied_at,
        closed,
        deleted
    ) VALUES (
        (SELECT id FROM sys_user WHERE username = 'postman_admin'),
        NULL,
        NULL,
        'USER',
        'Postman Parent',
        'https://example.com/avatar/postman-parent.png',
        'SeedParentMessage: unread and unreplied',
        'APPROVED',
        FALSE,
        NULL,
        NULL,
        FALSE,
        FALSE
    )
    RETURNING id
)
UPDATE parent_message pm
SET root_id = root.id
FROM root
WHERE pm.id = root.id;

-- Case 2: viewed + replied (chain)
WITH root AS (
    INSERT INTO parent_message (
        user_id,
        parent_id,
        root_id,
        sender_type,
        nickname_snapshot,
        avatar_snapshot,
        content,
        status,
        viewed,
        viewed_at,
        replied_at,
        closed,
        deleted
    ) VALUES (
        (SELECT id FROM sys_user WHERE username = 'postman_admin'),
        NULL,
        NULL,
        'USER',
        'Postman Parent',
        'https://example.com/avatar/postman-parent.png',
        'SeedParentMessage: viewed and replied root',
        'APPROVED',
        TRUE,
        NOW() - INTERVAL '1 day',
        NOW() - INTERVAL '20 hour',
        FALSE,
        FALSE
    )
    RETURNING id, user_id
),
root_fix AS (
    UPDATE parent_message pm
    SET root_id = root.id
    FROM root
    WHERE pm.id = root.id
    RETURNING root.id AS root_id, root.user_id
)
INSERT INTO parent_message (
    user_id,
    parent_id,
    root_id,
    sender_type,
    nickname_snapshot,
    avatar_snapshot,
    content,
    status,
    viewed,
    viewed_at,
    replied_at,
    closed,
    deleted
)
SELECT
    root_fix.user_id,
    root_fix.root_id,
    root_fix.root_id,
    'ADMIN',
    'ADMIN',
    NULL,
    'SeedParentMessage: admin reply node',
    'APPROVED',
    TRUE,
    NOW() - INTERVAL '20 hour',
    NULL,
    FALSE,
    FALSE
FROM root_fix;

-- Case 3: viewed + closed + unreplied
WITH root AS (
    INSERT INTO parent_message (
        user_id,
        parent_id,
        root_id,
        sender_type,
        nickname_snapshot,
        avatar_snapshot,
        content,
        status,
        viewed,
        viewed_at,
        replied_at,
        closed,
        deleted
    ) VALUES (
        (SELECT id FROM sys_user WHERE username = 'postman_admin'),
        NULL,
        NULL,
        'USER',
        'Postman Parent',
        'https://example.com/avatar/postman-parent.png',
        'SeedParentMessage: viewed and closed',
        'APPROVED',
        TRUE,
        NOW() - INTERVAL '2 day',
        NULL,
        TRUE,
        FALSE
    )
    RETURNING id
)
UPDATE parent_message pm
SET root_id = root.id
FROM root
WHERE pm.id = root.id;

COMMIT;


-- 插入测试角色
INSERT INTO sys_role (id, role_code, role_name, is_builtin) VALUES (1001, 'WECHAT', '微信用户', true);
INSERT INTO sys_role (id, role_code, role_name, is_builtin) VALUES (1002, 'ADMIN', '管理员', true);

-- 插入测试用户 (用于微信登录测试)
INSERT INTO sys_user (id, user_no, user_type, status, openid, nickname, avatar_url, phone, created_at, updated_at)
VALUES (1001, 'U000000000001001', 'WECHAT', 'ACTIVE', 'test_openid_1001', '测试用户1', 'https://example.com/avatar1.png', '13800138001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_user (id, user_no, user_type, status, openid, nickname, avatar_url, phone, created_at, updated_at)
VALUES (1002, 'U000000000001002', 'WECHAT', 'ACTIVE', 'test_openid_1002', '测试用户2', 'https://example.com/avatar2.png', '13800138002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入用户角色关联
INSERT INTO sys_user_role (user_id, role_id, created_at) VALUES (1001, 1001, CURRENT_TIMESTAMP);
INSERT INTO sys_user_role (user_id, role_id, created_at) VALUES (1002, 1001, CURRENT_TIMESTAMP);

-- 插入管理员
INSERT INTO sys_admin_user (id, user_no, status, username, password_hash, nickname, created_at, updated_at)
VALUES (1999, 'A000000000001999', 'ACTIVE', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '管理员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入站点信息
INSERT INTO site_profile (id, community_name, banner_slogan, intro_text, contact_person, contact_phone, contact_wechat, address_text, latitude, longitude, active, created_at, updated_at)
VALUES (1001, '测试社区', '欢迎来到测试社区', '这是一个测试社区的简介', '张老师', '13800138000', 'wechat_test', '测试地址', 31.230416, 121.473701, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入轮播图
INSERT INTO homepage_banner (id, title, image_url, link_type, sort_order, enabled, created_at, updated_at)
VALUES (1001, '轮播图1', 'https://example.com/banner1.jpg', 'NONE', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO homepage_banner (id, title, image_url, link_type, sort_order, enabled, created_at, updated_at)
VALUES (1002, '轮播图2', 'https://example.com/banner2.jpg', 'NONE', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入通知
INSERT INTO home_notice (id, title, content, enabled, sort_order, created_at, updated_at)
VALUES (1001, '系统通知', '欢迎使用系统', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入可见性规则
INSERT INTO visibility_rule (id, rule_name, allow_anonymous, require_login, created_at, updated_at)
VALUES (1001, '公开访问', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO visibility_rule (id, rule_name, allow_anonymous, require_login, created_at, updated_at)
VALUES (1002, '需要登录', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入服务分类
INSERT INTO service_category (id, category_code, category_name, sort_order, enabled, created_at, updated_at)
VALUES (1001, 'FAMILY', '家庭服务', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入家庭卡商品
INSERT INTO product (id, product_no, product_type, title, subtitle, detail_content, image_urls, contact_name, contact_phone, service_start_at, service_end_at, sale_start_at, sale_end_at, publish_status, is_deleted, is_top, display_priority, list_visibility_rule_id, detail_visibility_rule_id, category_id, created_at, updated_at)
VALUES (1001, 'P000000000001001', 'FAMILY_CARD', '家庭年卡', '适合全家使用的年卡', '家庭年卡详情内容', '["https://example.com/product1.jpg"]', '李老师', '13800138000', DATEADD('YEAR', -1, CURRENT_TIMESTAMP), DATEADD('YEAR', 1, CURRENT_TIMESTAMP), DATEADD('YEAR', -1, CURRENT_TIMESTAMP), DATEADD('YEAR', 1, CURRENT_TIMESTAMP), 'ON_SHELF', false, true, 100, 1001, 1001, 1001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product (id, product_no, product_type, title, subtitle, detail_content, image_urls, contact_name, contact_phone, service_start_at, service_end_at, sale_start_at, sale_end_at, publish_status, is_deleted, is_top, display_priority, list_visibility_rule_id, detail_visibility_rule_id, category_id, created_at, updated_at)
VALUES (1002, 'P000000000001002', 'VALUE_ADDED_SERVICE', '增值服务包', '额外的增值服务', '增值服务详情内容', '["https://example.com/product2.jpg"]', '王老师', '13800138001', DATEADD('YEAR', -1, CURRENT_TIMESTAMP), DATEADD('YEAR', 1, CURRENT_TIMESTAMP), DATEADD('YEAR', -1, CURRENT_TIMESTAMP), DATEADD('YEAR', 1, CURRENT_TIMESTAMP), 'ON_SHELF', false, false, 50, 1001, 1001, 1001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入家庭卡套餐
INSERT INTO product_family_card_plan (id, product_id, duration_type, duration_months, price_cents, max_family_members, enabled, sort_order, created_at, updated_at)
VALUES (1001, 1001, 'YEAR', 12, 19900, 5, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO product_family_card_plan (id, product_id, duration_type, duration_months, price_cents, max_family_members, enabled, sort_order, created_at, updated_at)
VALUES (1002, 1001, 'SEMESTER', 6, 9900, 3, true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入增值服务SKU
INSERT INTO product_value_added_sku (id, product_id, sku_no, title, unit_name, price_cents, max_purchase_qty, enabled, created_at, updated_at)
VALUES (1001, 1002, 'SKU0000000001001', '单次服务', '次', 9900, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

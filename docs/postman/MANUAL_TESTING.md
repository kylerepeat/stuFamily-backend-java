# stuFamily Backend Manual API Testing (Postman)

## 1. Files

- Collection: `docs/postman/stuFamily-backend.postman_collection.json`
- Environment: `docs/postman/stuFamily-local.postman_environment.json`
- Seed SQL: `sql/02_postman_seed.sql`

## 2. One-time Setup

1. Initialize schema:

```bash
psql -h localhost -p 5432 -U postgres -d stufamily -f sql/01_schema.sql
```

2. Insert deterministic test data:

```bash
psql -h localhost -p 5432 -U postgres -d stufamily -f sql/02_postman_seed.sql
```

2.1 (for existing old databases) apply admin-user split migration:

```bash
psql -h localhost -p 5432 -U postgres -d stufamily -f sql/03_admin_user_split.sql
```

3. Start backend:

```bash
mvn -Dmaven.repo.local=.m2 -pl stufamily-boot -am spring-boot:run
```

4. In Postman:
- Import `stuFamily-backend.postman_collection.json`
- Import `stuFamily-local.postman_environment.json`
- Select environment `stuFamily-local`

## 3. Seed Test Account

- Username: `postman_admin`
- Password: `Admin@123456`

This account is `HYBRID` (admin + wechat-openid) for local end-to-end API testing.

## 4. Recommended Run Order

1. `Admin API / Admin Login`
2. `Admin API / Admin Logout`
3. `Admin API / List Admin Accounts`
4. `Admin API / Validate Admin Password Strength`
5. `Admin API / Create Admin Account`
6. `Admin API / Change Admin Password`
7. `Admin API / Disable Admin Account`
8. `Admin API / List Admin Products`
8. `Admin API / Get Admin Product Detail`
9. `Admin API / Update Admin Product`
10. `Admin API / On Shelf Product`
11. `Admin API / Off Shelf Product`
12. `Admin API / List Admin Filter Options`
13. `Admin API / List Weixin Users`
14. `Admin API / List Orders With Weixin User`
15. `Admin API / Refund Order`
16. `Admin API / List Order Refunds`
17. `Admin API / Disable Family Group By Order`
18. `Admin API / List Family Cards With Weixin User`
19. `Weixin API / Home Index`
20. `Weixin API / List Home Products` (requires `sale_start_at` and `sale_end_at`)
21. `Weixin API / Get Product Detail`
22. `Weixin API / Get Order Status` (default seed order: `ORDPOSTMANSEED000001`)
23. `Weixin API / Pay Notify` (uses `order_no` variable)
24. `Weixin API / Search Family Members`
25. `Weixin API / Add Family Member`
26. `Weixin API / Cancel Family Member Card`
27. `Weixin API / Weixin Login` (requires real WeChat code)
28. `Weixin API / Create Family Card Order` (requires valid WeChat Pay config)
29. `Weixin API / Create Value Added Order` (requires valid WeChat Pay config)

## 5. Auth Notes

- `admin_token` is set automatically by `Admin Login`.
- `weixin_token` is set automatically by `Weixin Login`.
- Collection pre-request script falls back to `admin_token` when `weixin_token` is empty.
- Therefore, all authenticated weixin endpoints can still be tested locally even without real wechat login.
- Admin side all list APIs now support pagination query params: `page_no` and `page_size`.
- `GET /api/admin/products` supports optional `publish_status` filter (`DRAFT` / `ON_SHELF` / `OFF_SHELF`), mapped to env var `product_publish_status`.
- `GET /api/admin/weixin-users` supports optional `status` filter (`ACTIVE` / `DISABLED` / `LOCKED`), mapped to env var `weixin_user_status`.
- `GET /api/admin/orders` supports optional `order_status` and `order_type` for dropdown filtering.
- `GET /api/admin/family-cards` supports optional `status` for dropdown filtering.

## 6. APIs Covered

- `POST /api/admin/auth/login`
- `POST /api/admin/auth/logout`
- `GET /api/admin/accounts`
- `POST /api/admin/accounts`
- `POST /api/admin/accounts/password/validate`
- `POST /api/admin/accounts/{userId}/password`
- `POST /api/admin/accounts/{userId}/disable`
- `GET /api/admin/products`
- `GET /api/admin/products/{productId}`
- `POST /api/admin/products`
- `PUT /api/admin/products/{productId}`
- `POST /api/admin/products/{productId}/on-shelf`
- `POST /api/admin/products/{productId}/off-shelf`
- `GET /api/admin/filter-options`
- `GET /api/admin/weixin-users`
- `GET /api/admin/orders`
- `POST /api/admin/orders/{orderNo}/refund`
- `GET /api/admin/orders/{orderNo}/refunds`
- `POST /api/admin/orders/{orderNo}/disable-family-group`
- `GET /api/admin/family-cards`
- `GET /api/weixin/home/index`
- `GET /api/weixin/home/products`
- `GET /api/weixin/home/products/{productId}`
- `POST /api/weixin/auth/login`
- `POST /api/weixin/orders/create`
- `GET /api/weixin/orders/{orderNo}/status`
- `POST /api/weixin/pay/notify`
- `POST /api/weixin/family/members`
- `POST /api/weixin/family/check-ins`
- `GET /api/weixin/family/members`
- `DELETE /api/weixin/family/members/{memberNo}`

## 7. Known External Dependency Points

- `POST /api/weixin/auth/login` calls real WeChat `code2session`.
- `POST /api/weixin/orders/create` calls real WeChat Pay unified order.

If your local `app.wechat.*` config is not valid, these two endpoints are expected to fail, while all other endpoints can still be manually tested with seed data.

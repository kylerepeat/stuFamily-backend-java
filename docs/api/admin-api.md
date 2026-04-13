# stuFamily Admin API 文档

## 1. 通用说明

- 基础路径：`/api/admin`
- 认证方式：`Authorization: Bearer {admin_token}`
- 返回包裹：
  - `success`：是否成功
  - `code`：业务码（`OK` 表示成功）
  - `message`：提示信息
  - `data`：业务数据

### 1.1 分页返回结构（所有 list 接口统一）

`data` 字段结构：

- `items`：当前页数据列表
- `total`：总记录数
- `pageNo`：当前页码（从 1 开始）
- `pageSize`：每页条数
- `totalPages`：总页数

### 1.2 全局重放限制

- GET：同一请求键 200ms 内重复请求会被拒绝
- POST/PUT/DELETE：同一请求键 2s 内重复请求会被拒绝
- 拒绝时返回 HTTP `429`，`code=TOO_MANY_REQUESTS`

---

## 2. 认证接口

### 2.1 管理员登录

- 方法/路径：`POST /api/admin/auth/login`
- 接口说明：管理员账号密码登录，获取 JWT
- 请求字段：
  - `username`（string，必填）：管理员用户名
  - `password`（string，必填）：管理员密码
- 返回字段（`data`）：
  - `accessToken`（string）：访问令牌
  - `tokenType`（string）：令牌类型（通常为 `Bearer`）
  - `expiresIn`（long）：过期秒数
  - `userId`（long）：用户ID
  - `username`（string）：用户名
  - `roles`（string[]）：角色列表

### 2.2 管理员登出

- 方法/路径：`POST /api/admin/auth/logout`
- 接口说明：退出登录并使当前账号历史 token 失效（踢出）
- 请求字段：无
- 返回字段：无（`data=null`）

---

## 3. 管理员账号管理

### 3.1 分页查询管理员账号

- 方法/路径：`GET /api/admin/accounts`
- 接口说明：按关键词和状态分页查询 admin/hybrid 账号
- 查询参数：
  - `keyword`（string，非必填）：用户名/昵称/手机号/邮箱/用户编号模糊匹配
  - `status`（string，非必填，下拉建议）：
    - `ACTIVE`（正常）
    - `DISABLED`（已停用）
    - `LOCKED`（已锁定）
  - `page_no`（int，非必填，默认1）
  - `page_size`（int，非必填，默认20，最大200）
- 返回字段（`data.items[]`）：
  - `id`（long）：用户ID
  - `userNo`（string）：用户编号
  - `username`（string）：用户名
  - `userType`（string）：用户类型（`ADMIN`/`WECHAT`/`HYBRID`）
  - `status`（string）：账号状态（`ACTIVE`/`DISABLED`/`LOCKED`）
  - `nickname`（string）：昵称
  - `phone`（string）：手机号
  - `email`（string）：邮箱
  - `lastLoginAt`（string, datetime）：最后登录时间
  - `createdAt`（string, datetime）：创建时间

### 3.2 新增管理员账号

- 方法/路径：`POST /api/admin/accounts`
- 接口说明：新增管理员账号（包含密码强度校验）
- 请求字段：
  - `username`（string，必填）：用户名
  - `password`（string，必填）：密码
  - `nickname`（string，非必填）：昵称
  - `phone`（string，非必填）：手机号
  - `email`（string，非必填）：邮箱
- 返回字段：同 3.1 的单条账号字段

### 3.3 停用管理员账号

- 方法/路径：`POST /api/admin/accounts/{userId}/disable`
- 接口说明：停用指定管理员账号，并踢出登录状态
- 路径参数：
  - `userId`（long，必填）：管理员用户ID
- 返回字段：无

### 3.4 修改管理员密码

- 方法/路径：`POST /api/admin/accounts/{userId}/password`
- 接口说明：修改指定管理员密码
- 路径参数：
  - `userId`（long，必填）：管理员用户ID
- 请求字段：
  - `newPassword`（string，必填）：新密码
- 返回字段：无

### 3.5 校验密码强度

- 方法/路径：`POST /api/admin/accounts/password/validate`
- 接口说明：单独校验密码强度规则
- 请求字段：
  - `password`（string，必填）：待校验密码
  - `username`（string，非必填）：用户名（用于校验“密码不能包含用户名”）
- 返回字段：无

---

## 4. 商品管理

### 4.1 分页查询商品列表

- 方法/路径：`GET /api/admin/products`
- 接口说明：按销售时间窗口查询商品并分页
- 查询参数：
  - `sale_start_at`（string，必填，格式 `yyyy-MM-dd`）：销售窗口开始日
  - `sale_end_at`（string，必填，格式 `yyyy-MM-dd`）：销售窗口结束日
  - `publish_status`（string，非必填，下拉建议）：
    - `DRAFT`（草稿）
    - `ON_SHELF`（上架）
    - `OFF_SHELF`（下架）
  - `page_no`（int，非必填，默认1）
  - `page_size`（int，非必填，默认20，最大200）
- 返回字段（`data.items[]`）：
  - `id`（long）：商品ID
  - `type`（string）：商品类型（`FAMILY_CARD` / `VALUE_ADDED_SERVICE`）
  - `title`（string）：商品标题
  - `priceCents`（long）：价格（分）
  - `top`（boolean）：是否置顶
  - `publishStatus`（string）：发布状态（`DRAFT` / `ON_SHELF` / `OFF_SHELF`）

### 4.2 商品详情

- 方法/路径：`GET /api/admin/products/{productId}`
- 接口说明：查询商品完整详情（含套餐/SKU）
- 路径参数：
  - `productId`（long，必填）：商品ID
- 返回字段：`AdminProductDetailView`（完整字段如下）

#### 4.2.1 `AdminProductDetailView` 字段定义（返回）

| 字段名 | 类型 | 中文说明 | 是否必返 |
| --- | --- | --- | --- |
| `id` | long | 商品ID | 是 |
| `productNo` | string | 商品编号 | 是 |
| `productType` | string | 商品类型（`FAMILY_CARD`/`VALUE_ADDED_SERVICE`） | 是 |
| `title` | string | 商品标题 | 是 |
| `subtitle` | string | 商品副标题 | 否 |
| `detailContent` | string | 商品详情说明 | 是 |
| `imageUrls` | string[] | 商品图片URL列表 | 是（可为空数组） |
| `contactName` | string | 联系人 | 否 |
| `contactPhone` | string | 联系电话 | 否 |
| `serviceStartAt` | string(datetime) | 服务开始时间（ISO-8601） | 否 |
| `serviceEndAt` | string(datetime) | 服务结束时间（ISO-8601） | 否 |
| `saleStartAt` | string(datetime) | 销售开始时间（ISO-8601） | 否 |
| `saleEndAt` | string(datetime) | 销售结束时间（ISO-8601） | 否 |
| `publishStatus` | string | 发布状态（`DRAFT`/`ON_SHELF`/`OFF_SHELF`） | 是 |
| `deleted` | boolean | 是否删除 | 是 |
| `top` | boolean | 是否置顶 | 是 |
| `displayPriority` | int | 展示优先级 | 是 |
| `listVisibilityRuleId` | long | 列表展示权限规则ID | 否 |
| `detailVisibilityRuleId` | long | 详情展示权限规则ID | 否 |
| `categoryId` | long | 商品分类ID | 否 |
| `familyCardPlans` | `FamilyCardPlanView[]` | 家庭卡套餐列表 | 是（可为空数组） |
| `valueAddedSkus` | `ValueAddedSkuView[]` | 增值服务SKU列表 | 是（可为空数组） |

#### 4.2.2 `FamilyCardPlanView` 字段定义（`AdminProductDetailView.familyCardPlans[]`）

| 字段名 | 类型 | 中文说明 | 是否必返 |
| --- | --- | --- | --- |
| `id` | long | 套餐ID | 是 |
| `durationType` | string | 时长类型（`MONTH`/`SEMESTER`/`YEAR`） | 是 |
| `durationMonths` | int | 时长（月数） | 是 |
| `priceCents` | long | 价格（分） | 是 |
| `maxFamilyMembers` | int | 可添加家人上限 | 是 |
| `enabled` | boolean | 是否启用 | 是 |

#### 4.2.3 `ValueAddedSkuView` 字段定义（`AdminProductDetailView.valueAddedSkus[]`）

| 字段名 | 类型 | 中文说明 | 是否必返 |
| --- | --- | --- | --- |
| `id` | long | SKU ID | 是 |
| `title` | string | SKU标题 | 是 |
| `priceCents` | long | 价格（分） | 是 |
| `enabled` | boolean | 是否启用 | 是 |

### 4.3 新增商品

- 方法/路径：`POST /api/admin/products`
- 接口说明：新增家庭卡或增值服务商品
- 请求字段：`AdminProductUpdateRequest`（完整字段如下）
- 返回字段：`AdminProductDetailView`

#### 4.3.1 `AdminProductUpdateRequest` 字段定义（请求）

| 字段名 | 类型 | 中文说明 | 是否必填 |
| --- | --- | --- | --- |
| `productType` | string | 商品类型（`FAMILY_CARD`/`VALUE_ADDED_SERVICE`） | 是 |
| `title` | string | 商品标题 | 是 |
| `subtitle` | string | 商品副标题 | 否 |
| `detailContent` | string | 商品详情说明 | 是 |
| `imageUrls` | string[] | 商品图片URL列表 | 否 |
| `contactName` | string | 联系人 | 否 |
| `contactPhone` | string | 联系电话 | 否 |
| `serviceStartAt` | string(datetime) | 服务开始时间（ISO-8601） | 否 |
| `serviceEndAt` | string(datetime) | 服务结束时间（ISO-8601） | 否 |
| `saleStartAt` | string(datetime) | 销售开始时间（ISO-8601） | 否 |
| `saleEndAt` | string(datetime) | 销售结束时间（ISO-8601） | 否 |
| `publishStatus` | string | 发布状态（`DRAFT`/`ON_SHELF`/`OFF_SHELF`），不传默认 `DRAFT` | 否 |
| `top` | boolean | 是否置顶 | 否 |
| `displayPriority` | int | 展示优先级 | 否 |
| `familyCardPlans` | `FamilyCardPlanRequest[]` | 家庭卡套餐列表 | 条件必填（`productType=FAMILY_CARD` 新增时必填） |
| `valueAddedSkus` | `ValueAddedSkuRequest[]` | 增值服务SKU列表 | 条件必填（`productType=VALUE_ADDED_SERVICE` 新增时必填） |

#### 4.3.2 `FamilyCardPlanRequest` 字段定义（`AdminProductUpdateRequest.familyCardPlans[]`）

| 字段名 | 类型 | 中文说明 | 是否必填 |
| --- | --- | --- | --- |
| `id` | long | 套餐ID（编辑已有套餐时传） | 否 |
| `durationType` | string | 时长类型（`MONTH`/`SEMESTER`/`YEAR`） | 是 |
| `durationMonths` | int | 时长（月数） | 是 |
| `priceCents` | long | 价格（分） | 是 |
| `maxFamilyMembers` | int | 可添加家人上限 | 是 |
| `enabled` | boolean | 是否启用（不传按 `true` 处理） | 否 |

#### 4.3.3 `ValueAddedSkuRequest` 字段定义（`AdminProductUpdateRequest.valueAddedSkus[]`）

| 字段名 | 类型 | 中文说明 | 是否必填 |
| --- | --- | --- | --- |
| `id` | long | SKU ID（编辑已有SKU时传） | 否 |
| `title` | string | SKU标题 | 是 |
| `priceCents` | long | 价格（分） | 是 |
| `enabled` | boolean | 是否启用（不传按 `true` 处理） | 否 |

#### 4.3.4 业务必填规则补充

- 新增商品时：
  - `productType=FAMILY_CARD`：`familyCardPlans` 必须有值，`valueAddedSkus` 可空
  - `productType=VALUE_ADDED_SERVICE`：`valueAddedSkus` 必须有值，`familyCardPlans` 可空
- 编辑商品时：
  - 不允许变更 `productType`
  - `familyCardPlans` / `valueAddedSkus` 仅在传入时执行对应更新逻辑

### 4.4 编辑商品

- 方法/路径：`PUT /api/admin/products/{productId}`
- 接口说明：编辑已存在商品
- 路径参数：`productId`（long）
- 请求字段：同 4.3
- 返回字段：`AdminProductDetailView`

### 4.5 商品上架

- 方法/路径：`POST /api/admin/products/{productId}/on-shelf`
- 接口说明：将商品状态改为上架
- 返回字段：`AdminProductDetailView`

### 4.6 商品下架

- 方法/路径：`POST /api/admin/products/{productId}/off-shelf`
- 接口说明：将商品状态改为下架
- 返回字段：`AdminProductDetailView`

---

## 5. 用户与订单查询

### 5.0 查询筛选下拉选项（前端字典）

- 方法/路径：`GET /api/admin/filter-options`
- 接口说明：返回 admin 端查询页面所需的下拉选项（值+中文），用于前端筛选器渲染
- 查询参数：无
- 返回字段（`data`）：
  - `productPublishStatuses`（array）：商品上下架状态选项
  - `weixinUserStatuses`（array）：微信用户状态选项
  - `orderStatuses`（array）：订单状态选项
  - `orderTypes`（array）：订单类型选项
  - `familyCardStatuses`（array）：家庭卡状态选项
- 选项结构（每项）：
  - `value`（string）：实际传给筛选接口的值
  - `label`（string）：用于下拉显示的中文文案

### 5.1 分页查询微信用户

- 方法/路径：`GET /api/admin/weixin-users`
- 接口说明：查询微信用户（WECHAT/HYBRID）
- 查询参数：
  - `keyword`（string，非必填）
  - `status`（string，非必填，下拉建议）：
    - `ACTIVE`（正常）
    - `DISABLED`（已停用）
    - `LOCKED`（已锁定）
  - `page_no`（int）
  - `page_size`（int）
- 返回字段（`data.items[]`）：
  - `id`、`userNo`、`userType`、`status`、`openid`、`nickname`、`avatarUrl`、`phone`、`lastLoginAt`、`createdAt`

### 5.2 分页查询订单（关联微信用户）

- 方法/路径：`GET /api/admin/orders`
- 接口说明：查询订单并冗余买家微信信息
- 查询参数：
  - `order_status`（string，非必填，下拉建议）：
    - `PENDING_PAYMENT`（待支付）
    - `PAID`（已支付）
    - `CANCELLED`（已取消）
    - `EXPIRED`（已过期）
    - `REFUNDED`（已退款）
  - `order_type`（string，非必填，下拉建议）：
    - `FAMILY_CARD`（家庭卡订单）
    - `VALUE_ADDED_SERVICE`（增值服务订单）
  - `keyword`（string，非必填）
  - `page_no`（int）
  - `page_size`（int）
- 返回字段（`data.items[]`）：
  - `orderId`、`orderNo`、`buyerUserId`、`orderType`、`orderStatus`、`payableAmountCents`、`currency`、`createdAt`、`paidAt`
  - `buyerOpenid`、`buyerNickname`、`buyerAvatarUrl`

### 5.3 分页查询家庭卡（关联微信用户）

- 方法/路径：`GET /api/admin/family-cards`
- 接口说明：查询家庭组（家庭卡）并冗余归属微信用户
- 查询参数：
  - `status`（string，非必填，下拉建议）：
    - `ACTIVE`（有效）
    - `CLOSED`（已关闭）
  - `keyword`（string，非必填）
  - `page_no`（int）
  - `page_size`（int）
- 返回字段（`data.items[]`）：
  - `groupId`、`groupNo`、`sourceOrderId`、`ownerUserId`
  - `maxMembers`、`currentMembers`、`status`
  - `activatedAt`、`expireAt`、`createdAt`
  - `ownerOpenid`、`ownerNickname`、`ownerAvatarUrl`

### 5.4 分页查询打卡记录（支持成员ID/微信用户ID）

- 方法/路径：`GET /api/admin/family-checkins`
- 接口说明：分页查询家庭打卡记录，可按成员 ID 或微信用户 ID 过滤。
- 查询参数：
  - `family_member_id`（long，非必填）：家庭成员 ID
  - `wechat_user_id`（long，非必填）：微信用户 ID（`sys_user.id`）
  - `page_no`（int，非必填，默认1）
  - `page_size`（int，非必填，默认20，最大200）
- 返回字段（`data.items[]`）：
  - `id`（long）：打卡记录 ID
  - `checkinNo`（string）：打卡编号
  - `groupId`（long）：家庭组 ID
  - `groupNo`（string）：家庭组编号
  - `ownerUserId`（long）：微信用户 ID
  - `ownerOpenid`（string）：微信 openid
  - `ownerNickname`（string）：微信昵称
  - `familyMemberId`（long）：家庭成员 ID
  - `familyMemberNo`（string）：家庭成员编号
  - `familyMemberName`（string）：家庭成员姓名
  - `latitude`（number）：纬度
  - `longitude`（number）：经度
  - `addressText`（string）：打卡地址
  - `checkedInAt`（string, datetime）：打卡时间
  - `createdAt`（string, datetime）：记录创建时间

---

## 6. 订单退款与家庭组停用

### 6.1 订单退款

- 方法/路径：`POST /api/admin/orders/{orderNo}/refund`
- 接口说明：对已支付订单发起退款（支持部分退款）
- 路径参数：
  - `orderNo`（string，必填）：订单号
- 请求字段：
  - `refundAmountCents`（long，必填）：退款金额（分）
  - `reason`（string，非必填）：退款原因
- 返回字段（`data`）：
  - `orderNo`、`refundNo`、`refundStatus`、`wechatRefundId`
  - `refundAmountCents`、`refundedAmountCents`、`remainRefundableAmountCents`
  - `orderStatus`、`paymentStatus`、`refundAt`

### 6.2 分页查询订单退款记录

- 方法/路径：`GET /api/admin/orders/{orderNo}/refunds`
- 接口说明：查询指定订单退款明细（分页）
- 查询参数：
  - `page_no`（int）
  - `page_size`（int）
- 返回字段（`data.items[]`）：
  - `refundNo`、`refundStatus`、`wechatRefundId`
  - `refundAmountCents`、`reason`、`successTime`、`createdAt`

### 6.3 按订单停用家庭组（FAMILY_CARD）

- 方法/路径：`POST /api/admin/orders/{orderNo}/disable-family-group`
- 接口说明：停用家庭卡对应 `family_group`，并级联注销其 `family_member_card`
- 路径参数：
  - `orderNo`（string，必填）
- 返回字段（`data`）：
  - `orderNo`：订单号
  - `groupNo`：家庭组编号
  - `groupStatus`：停用后状态（`CLOSED`）
  - `totalMemberCount`：该组总成员数
  - `disabledMemberCount`：本次被停用的成员数
  - `operatedAt`：操作时间

### 6.4 根据 `orderId` 查询商品评价

- 方法/路径：`GET /api/admin/orders/{orderId}/product-review`
- 接口说明：根据订单 ID 查询该订单的商品服务评价（`service_review`）。
- 路径参数：
  - `orderId`（long，必填）：订单 ID
- 返回字段（`data`，类型：`AdminProductReviewView`）：
  - `reviewId`（long）：评价 ID
  - `orderId`（long）：订单 ID
  - `orderNo`（string）：订单号
  - `buyerUserId`（long）：评价用户 ID
  - `productId`（long）：商品 ID
  - `productType`（string）：商品类型（`FAMILY_CARD` / `VALUE_ADDED_SERVICE`）
  - `stars`（int）：评分（1-5）
  - `content`（string）：评价内容
  - `reviewedAt`（string, datetime）：评价更新时间
  - `createdAt`（string, datetime）：评价创建时间
- 结果说明：
  - 订单不存在：返回业务异常 `order not found`
  - 订单存在但无评价：`data = null`

---

## 7. 首页内容管理（Admin Home）

### 7.1 分页查询轮播图

- 方法/路径：`GET /api/admin/home/banners`
- 接口说明：查询首页轮播图列表
- 查询参数：`page_no`、`page_size`
- 返回字段（`data.items[]`）：
  - `id`、`title`、`imageUrl`、`linkType`、`linkTarget`
  - `sortOrder`、`enabled`、`startAt`、`endAt`

### 7.2 编辑轮播图

- 方法/路径：`PUT /api/admin/home/banners/{bannerId}`
- 接口说明：更新轮播图内容
- 请求字段：
  - `title`、`imageUrl`、`linkType`、`linkTarget`
  - `sortOrder`、`enabled`、`startAt`、`endAt`
- 返回字段：同 7.1 单条

### 7.3 删除轮播图

- 方法/路径：`DELETE /api/admin/home/banners/{bannerId}`
- 接口说明：删除轮播图
- 返回字段：无

### 7.4 分页查询站点档案

- 方法/路径：`GET /api/admin/home/site-profiles`
- 接口说明：查询站点档案列表
- 查询参数：`page_no`、`page_size`
- 返回字段（`data.items[]`）：
  - `id`、`communityName`、`bannerSlogan`、`introText`
  - `contactPerson`、`contactPhone`、`contactWechat`、`contactWechatQrUrl`、`addressText`、`active`

### 7.5 站点档案详情

- 方法/路径：`GET /api/admin/home/site-profiles/{siteProfileId}`
- 接口说明：查询指定站点档案详情
- 返回字段：同 7.4 单条

### 7.6 编辑站点档案

- 方法/路径：`PUT /api/admin/home/site-profiles/{siteProfileId}`
- 接口说明：更新站点档案
- 请求字段：
  - `communityName`、`bannerSlogan`、`introText`
  - `contactPerson`、`contactPhone`、`contactWechat`、`contactWechatQrUrl`、`addressText`、`active`
- 返回字段：同 7.4 单条

### 7.7 删除站点档案

- 方法/路径：`DELETE /api/admin/home/site-profiles/{siteProfileId}`
- 接口说明：删除站点档案
- 业务约束：
  - 激活中的档案不允许删除
  - 系统必须至少保留一条档案
- 返回字段：无

---

## 附录：Admin 常用枚举（值 -> 中文）

- `user_type`
  - `ADMIN` -> 管理员
  - `WECHAT` -> 微信用户
  - `HYBRID` -> 混合用户
- `user_status`
  - `ACTIVE` -> 正常
  - `DISABLED` -> 已停用
  - `LOCKED` -> 已锁定
- `product_type`
  - `FAMILY_CARD` -> 家庭卡
  - `VALUE_ADDED_SERVICE` -> 增值服务
- `publish_status`
  - `DRAFT` -> 草稿
  - `ON_SHELF` -> 上架
  - `OFF_SHELF` -> 下架
- `order_type`
  - `FAMILY_CARD` -> 家庭卡订单
  - `VALUE_ADDED_SERVICE` -> 增值服务订单
- `order_status`
  - `PENDING_PAYMENT` -> 待支付
  - `PAID` -> 已支付
  - `CANCELLED` -> 已取消
  - `EXPIRED` -> 已过期
  - `REFUNDED` -> 已退款
- `group_status`
  - `ACTIVE` -> 有效
  - `CLOSED` -> 已关闭

---

## 留言管理（Admin）

### 1. 分页查询留言列表

- 方法/路径：`GET /api/admin/home/messages`
- 说明：按创建时间倒序（最新在前）查询留言根节点列表

查询参数：

| 字段 | 类型 | 必填 | 中文说明 |
| --- | --- | --- | --- |
| `viewed` | boolean | 否 | 是否已查看；`true=已查看`，`false=未查看` |
| `replied` | boolean | 否 | 是否已回复；`true=已回复`，`false=未回复` |
| `page_no` | int | 否 | 页码，默认 1 |
| `page_size` | int | 否 | 每页大小，默认 20，最大 200 |

返回 `data.items[]` 字段：

| 字段 | 类型 | 中文说明 |
| --- | --- | --- |
| `id` | long | 留言 ID（根节点 ID） |
| `userId` | long | 留言用户 ID |
| `nickname` | string | 留言昵称快照 |
| `avatarUrl` | string | 留言头像快照 |
| `content` | string | 留言内容 |
| `viewed` | boolean | 是否已查看 |
| `replied` | boolean | 是否已回复（`repliedAt != null`） |
| `closed` | boolean | 是否已关闭 |
| `createdAt` | string(datetime) | 创建时间 |
| `viewedAt` | string(datetime) | 查看时间 |
| `repliedAt` | string(datetime) | 回复时间 |

### 2. 留言详情（链路）

- 方法/路径：`GET /api/admin/home/messages/{messageId}`
- 说明：查看留言链路详情；首次查看会自动标记为已查看

路径参数：

| 字段 | 类型 | 必填 | 中文说明 |
| --- | --- | --- | --- |
| `messageId` | long | 是 | 根留言 ID 或链路任意节点 ID |

返回 `data` 结构：

- `root`：`AdminParentMessageView`
- `nodes`：`AdminParentMessageNodeView[]`（按创建时间升序）

`AdminParentMessageNodeView` 字段：

| 字段 | 类型 | 中文说明 |
| --- | --- | --- |
| `id` | long | 节点 ID |
| `parentId` | long | 父节点 ID |
| `rootId` | long | 根节点 ID |
| `userId` | long | 归属用户 ID |
| `senderType` | string | 发送方类型（`USER`/`ADMIN`） |
| `nickname` | string | 昵称快照 |
| `avatarUrl` | string | 头像快照 |
| `content` | string | 内容 |
| `createdAt` | string(datetime) | 创建时间 |

### 3. 回复留言

- 方法/路径：`POST /api/admin/home/messages/{messageId}/reply`
- 说明：管理员回复留言，写入链路节点，`senderType=ADMIN`

路径参数：

| 字段 | 类型 | 必填 | 中文说明 |
| --- | --- | --- | --- |
| `messageId` | long | 是 | 留言 ID |

请求体：

| 字段 | 类型 | 必填 | 中文说明 |
| --- | --- | --- | --- |
| `content` | string | 是 | 回复内容，最大 500 字符 |

返回：留言详情结构（同“留言详情”）

### 4. 关闭留言

- 方法/路径：`POST /api/admin/home/messages/{messageId}/close`
- 说明：关闭留言，关闭后不可继续回复

路径参数：

| 字段 | 类型 | 必填 | 中文说明 |
| --- | --- | --- | --- |
| `messageId` | long | 是 | 留言 ID |

返回：`data=null`

### 5. 删除留言

- 方法/路径：`DELETE /api/admin/home/messages/{messageId}`
- 说明：逻辑删除留言根节点及其链路节点

路径参数：

| 字段 | 类型 | 必填 | 中文说明 |
| --- | --- | --- | --- |
| `messageId` | long | 是 | 留言 ID |

返回：`data=null`

---

## 家庭成员查询（Admin）

### 1. 分页查询家庭成员列表

- 方法/路径：`GET /api/admin/family-members`
- 接口说明：分页查询家庭成员，支持关键字检索。

查询参数：

| 字段 | 类型 | 必填 | 中文说明 |
| --- | --- | --- | --- |
| `keyword` | string | 否 | 关键字，支持匹配：手机号、组编号(`groupNo`)、成员名字(`memberName`)、归属用户昵称(`ownerNickname`) |
| `page_no` | int | 否 | 页码，默认 1 |
| `page_size` | int | 否 | 每页条数，默认 20，最大 200 |

返回结构：`data` 为分页结构（`PageResult`）

| 字段 | 类型 | 中文说明 |
| --- | --- | --- |
| `items` | `AdminFamilyMemberWithWechatUserView[]` | 当前页数据 |
| `total` | long | 总记录数 |
| `pageNo` | int | 当前页码 |
| `pageSize` | int | 每页条数 |
| `totalPages` | int | 总页数 |

`AdminFamilyMemberWithWechatUserView` 字段：

| 字段 | 类型 | 中文说明 |
| --- | --- | --- |
| `memberId` | long | 成员ID |
| `memberNo` | string | 成员编号 |
| `memberName` | string | 成员姓名 |
| `studentOrCardNo` | string | 学号/卡号 |
| `phone` | string | 手机号 |
| `memberStatus` | string | 成员状态（`ACTIVE`/`EXPIRED`/`CANCELLED`） |
| `joinedAt` | string(datetime) | 加入时间（ISO-8601） |
| `familyGroupId` | long | 家庭组ID |
| `groupNo` | string | 组编号 |
| `ownerUserId` | long | 归属用户ID |
| `ownerNickname` | string | 归属用户昵称 |

---

## 月度收入统计（Admin）

### 1. 月度收入与退款统计

- 方法/路径：`GET /api/admin/orders/monthly-income-stats`
- 接口说明：按月统计收入与退款金额，支持按商品类型与商品 ID 过滤。

查询参数：

| 字段 | 类型 | 必填 | 中文说明 |
| --- | --- | --- | --- |
| `start_month` | string | 否 | 起始月份，格式 `yyyy-MM`；与 `end_month` 成对出现 |
| `end_month` | string | 否 | 结束月份，格式 `yyyy-MM`；与 `start_month` 成对出现 |
| `product_type` | string | 否 | 商品类型：`FAMILY_CARD` / `VALUE_ADDED_SERVICE` |
| `product_id` | long | 否 | 商品 ID |

返回字段（`data`）：

| 字段 | 类型 | 中文说明 |
| --- | --- | --- |
| `monthlyTotalIncome` | `AdminMonthlyAmountView[]` | 每月总收入（分） |
| `monthlyRefundIncome` | `AdminMonthlyAmountView[]` | 每月退款金额（分） |
| `totalIncomeCents` | long | 查询区间总收入（分） |
| `totalRefundCents` | long | 查询区间总退款（分） |
| `netIncomeCents` | long | 净收入（分）=`totalIncomeCents-totalRefundCents` |

`AdminMonthlyAmountView`：

| 字段 | 类型 | 中文说明 |
| --- | --- | --- |
| `month` | string | 月份（`yyyy-MM`） |
| `amountCents` | long | 金额（分） |

---

## 首页通知管理（Admin）
### 1. 分页查询通知列表

- 方法/路径：`GET /api/admin/home/notices`
- 接口说明：分页查询首页通知，按 `sortOrder` 和 `id` 倒序排列。

查询参数：
| 字段 | 类型 | 必填 | 中文说明 |
| --- | --- | --- | --- |
| `page_no` | int | 否 | 页码，默认 1 |
| `page_size` | int | 否 | 每页条数，默认 20，最大 200 |

返回字段（`PageResult<AdminHomeNoticeView>`）：
| 字段 | 类型 | 中文说明 |
| --- | --- | --- |
| `items` | `AdminHomeNoticeView[]` | 当前页数据 |
| `total` | long | 总记录数 |
| `pageNo` | int | 当前页码 |
| `pageSize` | int | 每页条数 |
| `totalPages` | int | 总页数 |

`AdminHomeNoticeView` 字段：
| 字段 | 类型 | 中文说明 |
| --- | --- | --- |
| `id` | long | 通知 ID |
| `title` | string | 主标题（最多 50 字） |
| `content` | string | 通知内容（可为空） |
| `enabled` | boolean | 是否启用 |
| `sortOrder` | int | 排序值（越大越靠前） |
| `startAt` | string(datetime) | 生效开始时间（可空） |
| `endAt` | string(datetime) | 生效结束时间（可空） |
| `createdAt` | string(datetime) | 创建时间 |
| `updatedAt` | string(datetime) | 更新时间 |

### 2. 新增通知

- 方法/路径：`POST /api/admin/home/notices`
- 接口说明：新增一条首页通知。

请求体：
| 字段 | 类型 | 必填 | 中文说明 |
| --- | --- | --- | --- |
| `title` | string | 是 | 主标题，最大 50 字 |
| `content` | string | 否 | 通知内容，可为空 |
| `enabled` | boolean | 否 | 是否启用，默认 `true` |
| `sortOrder` | int | 否 | 排序值，默认 `0` |
| `startAt` | string(datetime) | 否 | 生效开始时间 |
| `endAt` | string(datetime) | 否 | 生效结束时间（不能早于 `startAt`） |

返回：`AdminHomeNoticeView`

### 3. 删除通知

- 方法/路径：`DELETE /api/admin/home/notices/{noticeId}`
- 接口说明：删除指定通知。

路径参数：
| 字段 | 类型 | 必填 | 中文说明 |
| --- | --- | --- | --- |
| `noticeId` | long | 是 | 通知 ID |

返回：`data=null`

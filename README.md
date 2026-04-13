# stuFamily-backend

学生家政服务平台后端（单体架构，Java 17 + Spring Boot 3 + MyBatis-Plus + PostgreSQL 17+）。

## 1. 项目目标

提供两套后端 API：

- `admin-api`：后台管理系统接口（商品管理、运营管理、订单支付查询等）。
- `weixin-api`：微信小程序接口（微信登录、首页展示、下单支付、家人管理）。

支持业务：

- 家庭卡（年/月/学期）购买与家人名额管理。
- 增值服务（车站接送、生日举办、家政打扫等）购买。
- 微信登录、微信支付、支付回调入账。
- 首页内容（轮播图、社区介绍、联系方式、留言等）。
- 订单、支付、家人组与家人卡已通过 MyBatis-Plus 持久化落库。

## 2. 架构设计

### 2.1 运行架构

- 架构类型：单体（Monolith）
- 缓存：不使用 Redis，不引入中间件缓存
- 数据库：PostgreSQL 17+
- 构建：Maven 多模块

### 2.2 模块划分

- `stufamily-core`
  - DDD 核心层（领域对象、应用服务、仓储接口、基础设施实现）
  - 安全（Spring Security + JWT）
  - 微信 SDK 接入（微信登录、微信支付）
  - 统一异常与日志追踪
- `stufamily-admin-api`
  - 后台管理接口控制器（`/api/admin/**`）
- `stufamily-weixin-api`
  - 小程序接口控制器（`/api/weixin/**`）
- `stufamily-boot`
  - 启动模块、配置文件、应用入口

### 2.3 DDD 分层约定

- `...domain...`：领域模型、领域规则、仓储接口
- `...application...`：应用服务（用例编排）
- `...infrastructure...`：MyBatis-Plus 持久化、微信 SDK 适配
- `...shared...`：安全、异常、日志、统一响应

## 3. 认证与权限

- 用户职责拆分：
  - `sys_user`：微信小程序用户
  - `sys_admin_user`：后台管理员用户
- 双体系 JWT：
  - `/api/admin/**`：后台角色鉴权（`ROLE_ADMIN`）
  - `/api/weixin/**`：小程序角色鉴权（`ROLE_WECHAT`）
- 登录入口：
  - `POST /api/admin/auth/login`
  - `POST /api/weixin/auth/login`

## 4. 微信能力接入

已接入 WxJava SDK：

- `weixin-java-miniapp`
- `wx-java-pay-spring-boot-starter`

能力：

- `code2session` 获取 `openid/unionid`
- 小程序统一下单（JSAPI）并返回预支付参数

## 5. 数据库设计

- DDL 脚本：`sql/01_schema.sql`
- 管理员拆分迁移脚本（已有旧库时执行）：`sql/03_admin_user_split.sql`
- 要点：
  - 主键优先自增（`BIGSERIAL`）
  - 金额统一分（`BIGINT`）
  - 微信用户与后台用户分表存储（`sys_user` / `sys_admin_user`）
  - 覆盖商品、订单、支付、家人组、首页内容等业务表

执行：

```bash
psql -h <host> -p <port> -U <user> -d <database> -f sql/01_schema.sql
```

旧库升级（若之前 admin 账号在 `sys_user`）：

```bash
psql -h <host> -p <port> -U <user> -d <database> -f sql/03_admin_user_split.sql
```

## 6. 本地启动

1. 准备 PostgreSQL 17+ 并创建数据库（例如 `stufamily`）。
2. 执行 DDL 脚本。
3. 修改 `stufamily-boot/src/main/resources/application.yml`：
   - 数据库连接
   - JWT 密钥
   - 微信小程序与微信支付配置
4. 启动：

```bash
mvn -Dmaven.repo.local=.m2 -pl stufamily-boot -am spring-boot:run
```

## 7. 测试与质量

- 单元测试 + Controller 测试（核心业务与 API 行为）
- JaCoCo 覆盖率门槛：`LINE >= 90%`
- 执行：

```bash
mvn -Dmaven.repo.local=.m2 clean verify
```

## 8. Docker Compose 部署（test / prod）

### 8.1 目录

- `Dockerfile`
- `deploy/docker-compose.test.yml`
- `deploy/docker-compose.prod.yml`
- `deploy/.env.prod.example`
- `scripts/compose-*.ps1`（Windows）
- `scripts/compose-*.sh`（Linux/macOS）

### 8.2 Test 环境（含种子数据）

Windows:

```powershell
.\scripts\compose-test-up.ps1 -Build
```

Linux/macOS:

```bash
bash ./scripts/compose-test-up.sh --build
```

访问地址：

- Backend: `http://localhost:18080`
- PostgreSQL: `localhost:15432`

停止：

```powershell
.\scripts\compose-test-down.ps1
```

彻底清理（含数据卷）：

```powershell
.\scripts\compose-test-down.ps1 -RemoveVolumes
```

### 8.3 Prod 环境

1. 复制并修改环境变量文件：

```bash
cp deploy/.env.prod.example deploy/.env.prod
```

2. 启动（在项目根目录执行）：

```powershell
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml up -d --build
```

停止：

```powershell
.\scripts\compose-prod-down.ps1
```

## 9. 生产化规范（已落实）

- 统一返回结构 `ApiResponse`
- 全局异常兜底 `GlobalExceptionHandler`
- 请求链路日志 + TraceId `RequestTraceFilter`
- 配置化管理（数据库、安全、微信）
- 模块化边界清晰（admin-api / weixin-api / core / boot）

## Postman Manual Testing
- docs/postman/stuFamily-backend.postman_collection.json
- docs/postman/stuFamily-local.postman_environment.json
- docs/postman/MANUAL_TESTING.md
- sql/02_postman_seed.sql


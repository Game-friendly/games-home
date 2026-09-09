# 跑腿接单小程序（本机运行版 v0.3）

客户发跑腿需求 → 后端放单进「单池」→ 接单人接单（2分钟锁定）→ 高等级压制抢单 → 完成结算。
本版本**不依赖小程序资质**，后端 + 管理后台全部在本机跑通，小程序用开发者工具本地预览；以后拿到资质随时可上架（替换微信登录、订阅消息、图片存储三个实现即可）。

## 核心规则

- 接单人三级 **A > B > C**，严格更高等级才能抢：A抢B/C、B抢C；同级不能互抢、低级不能抢高级。
- 每单可设置 **需要人数**（1-10）。多人单会保留多个接单位置，同一个人不能占多个位置；所有位置接满后才会进入进行中。
- 点「接单」后进入 **2分钟锁定保护期**（RESERVED），期内更高等级可「抢单」，**每次接手都重置 2 分钟**。
- 2 分钟到自动固化 ACCEPTED（定时扫描）。
- **A 级单只有 B 级及以上能首接**（规则 `接单人等级 ≥ 单等级 - 1`，在 `OrderService.canFirstAccept` 一处可改）。
- 接单人可「取消接单」：锁定期退单无惩罚；进行中退单**扣信用分**（-5），信用分低于 `app.min-accept-credit`（默认 60）禁止接单。
- 订单可上传凭证图片（当前存本地，接口已抽象，后续替换 COS）。
- 管理员随时改单内容/价格/等级/人数、派单、结算、给接单人改等级。
- 通知：站内消息（订阅消息的桩实现，换真实微信订阅消息只改 NotificationService.send）。
- 超时：过截止时间无人接自动取消；客户超时 72h 未确认自动完成。
- 防刷：写接口按用户滑动窗口限流（10 秒 30 次）。
- 多人单收益按实际接单人数平均拆分。

## 目录结构

```
接单程序框架/
├── backend/          # Spring Boot 3 后端（含 12 个单元测试，mvn test 全过）
├── admin/            # Vue3 + Element Plus 管理后台（npm run dev 打开）
├── miniprogram/      # 微信小程序前端（开发者工具导入）
├── docker-compose.yml
└── README.md
```

## 一、后端怎么跑

默认 H2 内存库，零依赖直接跑（需要本机 JDK 17）：

```bash
cd backend
mvn -DskipTests package        # 或直接跑已打好的 jar
java -jar target/order-platform-0.1.0-SNAPSHOT.jar
# http://localhost:8080/ping → pong
```

跑测试：`mvn test`（12 个用例：等级门槛、压制抢单、同级禁止、并发抢单、退单扣分、固化、收益、评价、多人单）。

锁定期默认 120 秒（`app.reserve-seconds`），超时确认默认 72 小时（`app.auto-confirm-hours`），改 `application.yml` 或加启动参数。

切 PostgreSQL（可选）：`docker compose up -d` 后 `java -jar ... --spring.profiles.active=postgres`。

## 二、管理后台怎么跑

```bash
cd admin
npm install
npm run dev
# 浏览器打开 http://localhost:5173 ，登录码 demo_admin
```

功能：订单管理（筛选/编辑内容价格等级人数/派单/结算）、接单人管理（等级、信用分）、收益结算（待结算列表、一键结算）。

## 三、小程序怎么跑

1. 微信开发者工具 → 导入项目 → 选 `miniprogram/` 目录（测试号即可，不需要资质）。
2. 详情 → 本地设置 → 勾选「不校验合法域名」。
3. 真机预览时把 `app.js` 的 `baseUrl` 改成电脑局域网 IP。
4. 登录页选身份进入。

页面：接单大厅（待接单+锁定中倒计时，自动轮询）、订单详情（接单/抢单/取消接单/交活/评价）、下单（人数+图片）、我的、收益、消息。

## 演示账号（启动自动创建，登录 `code` 即账号）

| code | 角色 | 等级 |
|------|------|------|
| demo_client_1 | 客户 | - |
| demo_worker_a / b / c | 接单人 | A / B / C |
| demo_admin | 管理员 | - |

## 接口速查

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 登录（code 即 openid，P1 换微信 code2session） |
| GET | /api/me | 我的信息（含信用分） |
| POST | /api/orders | 客户下单 |
| GET | /api/orders/pool | 接单大厅（待接单+锁定中） |
| POST | /api/orders/{id}/accept · /steal · /withdraw · /complete · /confirm · /cancel | 接单/抢单/退单/完成/确认/取消 |
| POST | /api/orders/{id}/review | 评价接单人（完成后，多人单可分别评价） |
| GET | /api/orders/my · /accepted · /{id} | 查询 |
| POST | /api/uploads | 上传图片（本地存储桩，后续换 COS） |
| GET | /api/earnings/mine | 接单人收益 |
| GET · POST | /api/notices · /api/notices/{id}/read | 站内消息 |
| GET | /api/admin/orders · /workers · /earnings | 后台查询 |
| PATCH | /api/admin/orders/{id} | 改内容/价格/等级 |
| POST | /api/admin/orders/{id}/dispatch · /settle | 派单 · 结算 |
| POST | /api/admin/workers/{id}/grade | 设置接单人等级 |

统一返回 `{ code, msg, data }`，`code=0` 成功；鉴权头 `Authorization: Bearer <token>`。

## 上架前需要换的三块（现在都有桩）

1. **微信登录**：`AuthService.login` 现在是 mock，上架时换成微信 code2session。
2. **订阅消息**：`NotificationService.send` 现在只写站内消息表，上架时加微信订阅消息推送，调用方不变。
3. **图片上传**：`FileStorageService` 当前由 `LocalFileStorageService` 存本地，上架时替换为 COS 实现，调用方不变。

其余上架事项（企业主体、备案域名、HTTPS、支付分账）见之前沟通的清单，随时可启动。

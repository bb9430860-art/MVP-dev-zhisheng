# 给朋友 / Cursor 的开发说明文档：客户增长、CRM、公海、贡献值与订单线

> 项目背景：本项目是面向标识标牌/非标制造企业的两周 MVP 演示系统。它既要能演示至胜样板客户的业务闭环，也要为未来非标制造数字化平台沉淀可复用底座。客户演示时不要暴露“未来复用给其他客户”的战略，只表达为“系统支持灵活配置、适配贵公司的业务流程”。
>
> 协作方式：两个人共用 GitHub 仓库，但不按前后端拆分，而按业务闭环拆分。每个人负责自己业务线的数据库、后端、前端、测试、演示数据和验收标准。

## 0. 你的角色定位

你负责的是 **客户增长与订单配置线**，也就是：

> 客户从哪里来，业务员如何上传和认领公海客户，客户如何从潜在客户变成意向客户、正式客户和订单；贡献值如何记录；订单和产品/点位如何被创建并交给生产侧执行。

你使用 **Cursor** 开发。Cursor 可以快速写页面和联调，但本项目不能只追求页面速度，必须严格遵守客户公海规则、贡献值规则、订单与生产边界。

你的核心业务闭环：

```text
业务员上传潜在客户
→ 手机号唯一校验
→ 进入客户公海
→ 业务员领取客户
→ 显示已领取和领取人
→ 限时联系
→ 填写联系记录
→ 客户变成意向客户
→ 上传人获得贡献值
→ 意向客户转正式客户
→ 创建园区导视订单
→ 添加多个产品/点位
→ 交给生产侧配置工艺路线和下发生产
```

本线的最高原则：

```text
上传不奖励，转意向才奖励。
领取不奖励，联系才冷却。
数量算任务，质量算贡献，成交算大奖。
订单由客户线创建，生产线只消费订单。
```

---

## 1. 你必须重视的 Skills / OpenSpec 规则

虽然你使用 Cursor，但项目规范仍然必须按 OpenSpec + Superpowers 的思路执行。

### 1.1 开发前必须检查的规格目录

你负责这些 OpenSpec change：

```text
openspec/changes/crm-public-pool/
openspec/changes/customer-archive/
openspec/changes/contribution-system/
openspec/changes/configurable-order-project/
```

每个 change 至少要有：

```text
proposal.md   # 为什么做
设计文档 design.md   # 怎么做
spec.md       # 做成什么样
tasks.md      # 分几步做
```

### 1.2 Cursor 开发时必须遵守的节奏

每个功能按这个顺序：

```text
1. 先读 spec，不要直接写页面。
2. 先写状态机和接口草案。
3. 再写后端数据结构和接口。
4. 再写前端页面。
5. 最后写验收样例和演示数据。
```

### 1.3 禁止的开发方式

```text
禁止：只做漂亮页面，不实现公海规则。
禁止：领取客户后立刻进入冷却。
禁止：上传客户后立刻发奖励。
禁止：直接修改生产实例或工序任务。
禁止：贡献值只存余额不存流水。
禁止：大屏展示员工真实工资。
```

---

## 2. 你的功能范围

你负责以下模块。

### 2.1 CRM 客户公海模块

这是你的核心模块。

负责内容：

```text
业务员上传潜在客户
手机号唯一校验
客户进入公海
客户公海列表
电话脱敏展示
业务员领取客户
显示已领取状态
显示领取人
显示领取时间
限制每日领取上限
限制同时持有未联系客户数
领取后限时联系
填写联系记录
联系后进入冷却期
冷却期内不能再次领取
冷却期结束后回到可领取
客户转意向客户
客户转正式客户
客户标记无效
客户标记不再联系
```

#### 2.1.1 公海状态

状态必须按下面设计，不要随意新增“重复线索”状态，因为手机号唯一校验会直接阻止重复上传。

```text
AVAILABLE       可领取
CLAIMED         已领取
COOLING         冷却中
INTENTIONAL     意向客户
CUSTOMER        正式客户
DEAL            已成单
INVALID         已无效
DO_NOT_CONTACT  不再联系
```

#### 2.1.2 状态流转

```text
上传客户
→ AVAILABLE 可领取

AVAILABLE
→ 业务员领取
→ CLAIMED 已领取

CLAIMED
→ 超时未联系
→ AVAILABLE 可领取，并扣领取人贡献值

CLAIMED
→ 填写联系记录：未接通/暂不需要
→ COOLING 冷却中
→ 冷却期结束
→ AVAILABLE 可领取

CLAIMED
→ 填写联系记录：有明确需求
→ INTENTIONAL 意向客户
→ 给原始上传人发放线索贡献值
→ 给转化业务员发放转化贡献值

INTENTIONAL
→ 转正式客户
→ CUSTOMER 正式客户

CUSTOMER
→ 创建订单并成交
→ DEAL 已成单
→ 给成交人和原始上传人发放成交贡献值

任意状态
→ 标记无效
→ INVALID 已无效

任意状态
→ 不再联系
→ DO_NOT_CONTACT 不再联系
```

#### 2.1.3 关键业务规则

```text
上传客户不需要人工审核。
上传客户只计入每日上新任务，不直接发奖励。
手机号必须唯一，重复手机号禁止上传。
领取客户不发奖励。
领取客户只代表暂时占用。
只有填写联系记录后才进入冷却。
领取后不联系，不能让客户进入冷却。
领取后超时未联系，客户自动释放回公海。
客户变成意向客户后，原始上传人才获得贡献值。
客户成交后，成交人和原始上传人都获得贡献值。
```

DoD：

```text
业务员可以上传潜在客户。
重复手机号无法上传。
上传成功后客户进入可领取状态。
公海列表显示客户基本信息和脱敏手机号。
业务员可以领取客户。
领取后显示状态为已领取、领取人、领取时间。
领取后未填写联系记录时，不进入冷却。
填写联系记录后，根据结果进入冷却中、意向客户、无效或不再联系。
冷却期内其他业务员不能领取该客户。
```

---

### 2.2 公海领取额度与防占坑模块

负责规则：

```text
每日最大领取数
同时持有未联系客户上限
领取后联系时限
超时自动释放
超时扣贡献值
每日上新任务
```

建议 MVP 配置：

```text
每日最大领取数 = min(公海可领取数 × 10%, 20)
同时持有未联系客户上限 = 10
领取后联系时限 = 24 小时
冷却期 = 7 天
```

关于“每天至少接单”：建议不要按“最少领取”计算，而按“最低有效联系数”或“每日上新任务”计算。因为公司要的是有效开发，不是占坑。

DoD：

```text
业务员达到每日领取上限后不能继续领取。
业务员同时持有未联系客户达到上限后不能继续领取。
领取后超过时限未联系，客户自动回到可领取。
超时未联系会生成贡献值扣减事件。
系统能展示业务员今日已领取数、剩余额度、未联系持有数。
```

---

### 2.3 每日客户上新任务模块

负责内容：

```text
业务员每日必须上传一定数量的潜在客户。
上传客户计入上新任务。
上传本身不直接产生奖励。
上传客户后续转意向/成交，才产生贡献值奖励。
```

建议字段：

```text
业务员
日期
目标上传数
实际上传数
合格上传数
完成率
```

DoD：

```text
业务员个人页可以看到今日上新目标、已完成数量、还差多少。
上传合格客户后，今日上新数量 +1。
无效格式、重复手机号、信息不完整的客户不计入上新任务。
管理端可以查看每个业务员的日/周/月上新完成情况。
```

---

### 2.4 客户档案模块

客户从公海转化后，要进入正式客户档案。

负责内容：

```text
正式客户列表
客户详情
联系人管理
沟通记录
客户附件
客户来源追踪
客户负责人
客户关联订单
```

客户详情至少包括：

```text
客户名称
客户类型
客户地区
联系人
联系电话
客户来源
原始上传人
当前负责人
沟通记录
关联订单
附件
```

DoD：

```text
意向客户可以转为正式客户。
正式客户保留原始上传人和转化人信息。
客户详情可以新增多个联系人。
客户详情可以新增沟通记录。
客户详情可以查看该客户关联的订单。
```

---

### 2.5 贡献值与激励模块

你负责贡献值系统的核心账户和流水。

负责内容：

```text
贡献值账户
贡献值流水
贡献值事件入账
个人贡献值页面
贡献值排行榜
预计奖励金额
上传客户转意向奖励
认领客户转意向奖励
客户成交奖励
管理员手动加减贡献值
接收生产线/考勤线贡献值事件
```

#### 2.5.1 贡献值事件建议

```text
LEAD_SUBMITTED              上传合格客户，仅计任务，不发奖励，可不入账
LEAD_BECAME_INTENTIONAL     上传客户变成意向，上传人加分
LEAD_CONVERTED_BY_CLAIMER   认领客户转意向，转化人加分
CUSTOMER_DEAL_OWNER         客户成单，成交人加分
CUSTOMER_DEAL_SOURCE        客户成单，原始上传人加分
CLAIM_TIMEOUT               领取超时未联系，扣分
INVALID_LEAD                上传客户被确认无效，可扣分或不计奖励
ATTENDANCE_LATE             迟到扣分，来自生产线事件
MANUAL_REWARD               管理员奖励
MANUAL_PENALTY              管理员扣减
```

#### 2.5.2 贡献值底层规则

必须有流水表。

禁止只在员工表里放一个 score 字段。

贡献值变化必须通过：

```text
contribution_transaction
```

DoD：

```text
每一次贡献值变化都能查到流水。
流水包含员工、事件类型、分值、关联业务、原因、发生时间。
员工个人页显示当前贡献值、本月贡献值、预计奖励、贡献明细。
排行榜显示本月贡献值排名和预计奖励，不展示工资。
生产线可以通过 /api/contribution/events 提交贡献值事件。
```

---

### 2.6 订单与项目制产品/点位模块

你负责订单和产品/点位，因为它们来自客户。

负责内容：

```text
从正式客户创建订单
订单列表
订单详情
订单自定义字段
产品/点位列表
产品/点位详情
产品/点位自定义字段
一个订单包含多个产品/点位
产品/点位附件
报价字段预留
订单状态
```

Mock 样例：

```text
客户：某园区开发公司
订单：某园区导视系统项目
产品/点位：
- 入口精神堡垒
- 停车场指示牌
- 楼栋导视牌
- 楼层索引牌
- 办公室门牌
- 卫生间标识
```

订单字段必须支持可配置，但 MVP 可以先做一套通用动态字段能力。

DoD：

```text
正式客户详情页可以创建订单。
订单详情可以添加多个产品/点位。
产品/点位可以填写规格、材质、数量、备注、图片。
订单和产品/点位支持自定义字段。
产品/点位详情中可以看到生产状态，但生产状态由生产线提供。
订单详情页提供“配置生产流程”入口，跳转到生产线页面。
```

---

## 3. 你负责的数据表

建议由你维护以下 Flyway 脚本段：

```text
V010__crm_public_pool.sql
V011__customer_archive.sql
V012__contribution.sql
V020__order_project.sql
V021__custom_fields.sql
```

你主要负责表：

```text
employee
customer_lead
lead_claim
lead_contact_record
lead_daily_submission
customer
customer_contact
customer_communication
contribution_account
contribution_transaction
contribution_rule（可选，MVP 可硬编码后预留）
order
order_item
custom_field_definition
custom_field_value
```

### 3.1 公海客户表建议

```text
customer_lead
- id
- lead_name
- contact_name
- phone
- phone_masked
- region
- source_type
- source_remark
- uploaded_by
- uploaded_at
- status
- current_owner_id
- claimed_at
- claim_expire_at
- cooldown_until
- converted_customer_id
- deal_order_id
```

### 3.2 领取记录表建议

```text
lead_claim
- id
- lead_id
- claimed_by
- claimed_at
- claim_expire_at
- released_at
- release_reason
- status
```

### 3.3 联系记录表建议

```text
lead_contact_record
- id
- lead_id
- claim_id
- contacted_by
- contact_method
- contact_result
- intent_level
- demand_description
- next_follow_time
- remark
- created_at
```

### 3.4 贡献值表建议

```text
contribution_account
- id
- employee_id
- total_score
- month_score
- estimated_reward_amount
- updated_at
```

```text
contribution_transaction
- id
- employee_id
- event_type
- score_delta
- biz_type
- biz_id
- reason
- occurred_at
- created_by
- settled
```

### 3.5 订单表建议

```text
orders
- id
- order_no
- customer_id
- order_name
- project_name
- status
- amount
- delivery_date
- created_by
- created_at
```

```text
order_item
- id
- order_id
- item_name
- product_type
- quantity
- spec
- material
- remark
- production_status
- production_progress
- production_route_instance_id
```

---

## 4. 你可以触碰的模块

```text
backend/src/main/java/.../crm/**
backend/src/main/java/.../customer/**
backend/src/main/java/.../contribution/**
backend/src/main/java/.../order/**
backend/src/main/java/.../customfield/**
web/src/modules/crm/**
web/src/modules/customer/**
web/src/modules/contribution/**
web/src/modules/order/**
docs/customer/**
openspec/changes/crm-public-pool/**
openspec/changes/customer-archive/**
openspec/changes/contribution-system/**
openspec/changes/configurable-order-project/**
```

---

## 5. 你不能触碰的雷区

### 5.1 不要生成生产任务

生产任务归生产线负责。

你只能创建订单和产品/点位。

禁止直接写：

```text
production_route_instance
production_step_instance
production_step_checkin
```

订单详情页只提供入口：

```text
配置生产流程
```

点击后跳转到生产线页面。

---

### 5.2 不要改工艺路线模板

工艺路线模板归生产线负责。

禁止修改：

```text
process_route_template
process_step_template
```

你可以读取生产线提供的生产摘要，但不要自己维护工艺路线。

---

### 5.3 不要直接改生产状态规则

你可以在订单和产品/点位页展示生产状态，但状态来源应该是生产线。

你不要自己判断：

```text
生产中
已完成
待下发
```

应调用生产线接口：

```http
GET /api/production/order-items/{orderItemId}/summary
```

---

### 5.4 不要让上传客户立刻奖励

核心规则：

```text
上传客户只计入每日上新任务。
客户变成意向后，上传人才获得贡献值。
客户成交后，上传人和成交人获得奖励。
```

禁止：

```text
上传成功立即给钱
上传成功立即加大量贡献值
领取成功立即奖励
```

---

### 5.5 不要让领取客户直接冷却

核心规则：

```text
领取只是占用。
联系才触发冷却。
```

禁止：

```text
AVAILABLE → CLAIMED → 自动 COOLING
```

正确做法：

```text
AVAILABLE → CLAIMED
CLAIMED + 填写联系记录 → COOLING / INTENTIONAL / INVALID / DO_NOT_CONTACT
```

---

### 5.6 不要展示真实工资

贡献值大屏只能展示：

```text
贡献值
预计奖励
排行榜
业务指标
```

不能展示：

```text
基本工资
实际工资条
社保
真实扣款明细
银行卡
身份证
```

---

## 6. 你需要向生产线提供的接口

### 6.1 产品/点位详情

```http
GET /api/order-items/{orderItemId}
```

返回建议：

```json
{
  "id": 1001,
  "orderId": 501,
  "itemName": "入口精神堡垒",
  "productType": "SPIRIT_FORTRESS",
  "quantity": 1,
  "spec": "3000mm x 1200mm",
  "material": "镀锌板 + 亚克力",
  "status": "CONFIRMED",
  "productionStatus": "NOT_DISPATCHED"
}
```

### 6.2 订单概要

```http
GET /api/orders/{orderId}/summary
```

返回建议：

```json
{
  "id": 501,
  "orderName": "某园区导视系统项目",
  "customerId": 2001,
  "customerName": "某园区开发公司",
  "deliveryDate": "2026-06-20",
  "projectManagerId": 3
}
```

### 6.3 贡献值事件接收接口

生产线会调用：

```http
POST /api/contribution/events
```

你的模块负责：

```text
校验事件类型
写 contribution_transaction
更新 contribution_account
返回入账结果
```

示例请求：

```json
{
  "employeeId": 12,
  "eventType": "ATTENDANCE_LATE",
  "bizType": "ATTENDANCE",
  "bizId": 88,
  "score": -5,
  "reason": "迟到 12 分钟"
}
```

---

## 7. 你需要消费生产线提供的接口

### 7.1 生产状态摘要

```http
GET /api/production/order-items/{orderItemId}/summary
```

用于订单详情展示：

```text
是否已下发生产
当前生产状态
总工序数
已完成工序数
当前工序
进度百分比
```

### 7.2 库存需求摘要

```http
GET /api/material-requirements/order-items/{orderItemId}
```

用于订单详情展示：

```text
该产品/点位需要哪些物料
库存是否足够
缺口多少
```

---

## 8. 推荐开发顺序

### Day 1：客户线规格和状态机

```text
写 crm-public-pool 的 proposal/design/spec/tasks。
写 contribution-system 的 proposal/design/spec/tasks。
明确公海状态机。
明确贡献值事件。
明确 order_item 给生产线的字段契约。
```

### Day 2-3：客户公海基础

```text
上传潜在客户。
手机号唯一校验。
公海列表。
领取客户。
显示已领取和领取人。
```

### Day 4：领取限制和联系记录

```text
每日领取上限。
同时持有上限。
领取后限时联系。
联系记录。
联系后进入冷却/意向/无效。
```

### Day 5：贡献值基础

```text
贡献值账户。
贡献值流水。
转意向奖励。
领取超时扣分。
贡献值个人页。
```

### Day 6：客户档案

```text
意向客户转正式客户。
客户列表。
客户详情。
联系人。
沟通记录。
```

### Day 7-8：订单和产品/点位

```text
从客户创建订单。
添加多个产品/点位。
动态字段。
附件。
给生产线提供 order_item 接口。
```

### Day 9-10：贡献大屏和排行榜

```text
本月贡献值排行。
预计奖励。
客户上新排行。
意向转化排行。
成交排行。
```

### Day 11-12：联调生产线

```text
订单详情接入生产状态摘要。
产品/点位详情加入“配置生产流程”入口。
调用生产线库存需求摘要。
```

### Day 13-14：演示数据和演示脚本

```text
30 条公海客户。
3 个业务员。
5 条意向客户。
2 个正式客户。
1 个园区导视订单。
6 个产品/点位。
若干贡献值流水。
```

---

## 9. 客户线 PR 检查清单

每次提交 PR 前必须检查：

```text
[ ] 是否有对应 OpenSpec change？
[ ] 是否写清楚 DoD？
[ ] 是否没有修改生产线表？
[ ] 是否保证手机号唯一？
[ ] 是否保证上传客户不立即奖励？
[ ] 是否保证领取客户不立即冷却？
[ ] 是否显示已领取人和领取时间？
[ ] 是否限制每日领取上限？
[ ] 是否限制同时持有未联系客户数量？
[ ] 是否贡献值有流水？
[ ] 是否没有展示真实工资？
[ ] 是否给生产线提供 order/order_item 契约？
[ ] 是否能跑通客户到订单的端到端场景？
```

---

## 10. 给 Cursor 的推荐提示词

每个客户侧功能开始前，你可以这样要求 Cursor：

```text
你现在负责本项目的客户增长、CRM、公海、贡献值和订单线。
不要直接写页面，先读取 OpenSpec 规格。
请严格遵守以下规则：
1. 上传客户只计入上新任务，不立即奖励。
2. 手机号唯一，重复手机号禁止上传。
3. 领取只是占用，联系才触发冷却。
4. 已领取必须显示领取人和领取时间。
5. 客户转意向后，上传人和转化人按规则获得贡献值。
6. 贡献值必须写流水，不能只改余额。
7. 订单和产品/点位由客户线创建，生产任务由生产线生成。
8. 不要修改 process、production、inventory 表。

请先输出：
- 需求理解
- 状态机
- 数据表影响
- API 契约
- 雷区
- 验收标准
确认后再实现。
```

---

## 11. 你的最终验收场景

客户线必须能演示：

```text
1. 业务员 A 上传一个潜在客户。
2. 系统校验手机号不重复，客户进入公海。
3. 业务员 A 的今日上新任务数量 +1，但不发奖励。
4. 业务员 B 从公海领取该客户。
5. 公海列表显示该客户已领取，领取人为业务员 B。
6. 业务员 B 填写联系记录，标记客户有明确需求。
7. 客户变成意向客户。
8. 业务员 A 因原始上传获得贡献值。
9. 业务员 B 因转化意向获得贡献值。
10. 意向客户转为正式客户。
11. 给正式客户创建“某园区导视系统项目”订单。
12. 订单下添加多个产品/点位。
13. 产品/点位详情显示“配置生产流程”入口。
14. 订单详情能读取生产线返回的生产进度摘要。
15. 贡献值大屏展示业务员贡献排行和预计奖励。
```

如果这 15 步跑通，客户增长与订单线就是 MVP 成功的入口。

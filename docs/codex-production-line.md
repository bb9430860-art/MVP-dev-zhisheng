# 给许安 / Codex 的开发说明文档：生产履约与资源看板线

> 项目背景：本项目是面向标识标牌/非标制造企业的两周 MVP 演示系统。它既要能演示至胜样板客户的业务闭环，也要为未来非标制造数字化平台沉淀可复用底座。客户演示时不要暴露“未来复用给其他客户”的战略，只表达为“系统支持灵活配置、适配贵公司的业务流程”。
>
> 协作方式：两个人共用 GitHub 仓库，但不按前后端拆分，而按业务闭环拆分。每个人负责自己业务线的数据库、后端、前端、测试、演示数据和验收标准。

## 0. 你的角色定位

你负责的是 **生产履约与资源看板线**，也就是：

> 订单已经存在之后，系统如何把一个项目制非标订单变成可执行的生产流程；工人如何按工序执行并拍照打卡；库存如何保障生产；老板如何看到生产进度、库存风险和经营状态。

你使用 **Codex** 开发。Codex 在本项目中不只是写代码工具，还要承担生产侧的规格维护、设计决策、验收标准和自检责任。

你的核心目标不是“做一个生产页面”，而是打通这条业务闭环：

```text
从客户线创建的订单/产品点位
→ 选择或配置工艺路线模板
→ 生产前灵活调整工序
→ 确认下发生产
→ 复制模板生成冻结的生产实例
→ 工人移动端查看任务
→ 开始工序 / 上传照片 / 填写备注 / 完成工序
→ 管理端看到生产进度
→ 库存预警提示物料风险
→ 老板驾驶舱看到整体状态
```

本线的最高原则：

```text
配置阶段灵活，执行阶段冻结。
模板可以变，实例不能乱。
生产只消费订单，不反向污染客户与订单核心数据。
```

---

## 1. 你必须重视的 Skills / OpenSpec 规则

本项目已经明确采用 OpenSpec + Superpowers 的工作方法。

### 1.1 Codex 开发前必须做什么

任何生产侧功能开始写代码前，必须先补充或检查 OpenSpec 规格：

```text
openspec/changes/process-route-template/
openspec/changes/production-dispatch-checkin/
openspec/changes/inventory-material/
openspec/changes/attendance-checkin/
openspec/changes/dashboard-demo/
```

每个 change 至少包含：

```text
proposal.md   # 为什么做
设计文档 design.md   # 怎么做
spec.md       # 做成什么样
任务清单 tasks.md    # 分几步做
```

### 1.2 你应该启用的核心思路

不要一次性让 Codex 自由发挥。每个功能按这个流程走：

```text
1. Explore：先确认需求边界和雷区。
2. Design：写清楚数据流、状态机、接口契约。
3. Tasks：拆成可验证的小任务。
4. Implement：按任务实现。
5. Verify：提供证据，不允许口头说“完成了”。
```

优先使用这些技能思想：

```text
brainstorming：复杂功能前先提问和澄清。
writing-plans：把任务拆成小块。
test-driven-development：关键状态机和服务逻辑先写测试。
requesting-code-review：先做规格合规审查，再做代码质量审查。
verification-before-completion：没有验证证据，不得宣称完成。
```

### 1.3 禁止的开发方式

```text
禁止：直接让 Codex “把生产模块全写了”。
禁止：没有 spec 就写表结构。
禁止：没有状态机设计就写生产流程。
禁止：没有 DoD 就合并 PR。
禁止：为了演示随意改客户线的数据表。
```

---

## 2. 你的功能范围

你负责以下模块。

### 2.1 工艺路线模板模块

负责内容：

```text
工艺路线模板列表
新建工艺路线模板
编辑工艺路线模板
工序模板列表
工序顺序调整
增加工序
删除工序
配置工序执行角色
配置是否需要拍照
配置是否需要备注
配置是否允许移动端执行
配置适用产品类型
```

典型模板示例：

```text
精神堡垒工艺路线：设计深化 → 拆单 → 下料 → 焊接 → 打磨 → 喷漆 → 组装 → 质检 → 包装 → 安装
楼层牌工艺路线：设计深化 → 排版 → UV 打印 → 覆膜 → 裁切 → 质检 → 包装
发光字工艺路线：设计深化 → 激光切割 → 焊接 → 打磨 → 烤漆 → 装灯 → 测试 → 包装 → 安装
普通导视牌工艺路线：设计深化 → 下料 → 表面处理 → 图文制作 → 组装 → 质检 → 包装
```

DoD：

```text
管理员可以创建一条路线模板。
管理员可以在模板中添加多道工序。
管理员可以调整工序顺序。
管理员可以配置每道工序是否需要拍照和备注。
管理员可以保存模板。
保存后，产品/点位生产配置页可以选择该模板。
```

---

### 2.2 生产前配置与确认下发模块

负责内容：

```text
读取客户线创建的订单和产品/点位
为产品/点位选择工艺路线模板
把模板步骤复制到“待下发生产配置”中
允许生产主管在下发前调整工序
确认下发生产
生成生产路线实例
生成生产工序实例
冻结流程
```

核心规则：

```text
生产前可以增删改工序。
生产前可以调整顺序。
生产前可以修改负责人、拍照要求、备注要求。
确认下发后不允许再改流程结构。
确认下发后工人只能执行任务，不能改流程。
```

DoD：

```text
生产主管可以在某个 order_item 上选择工艺路线模板。
系统能显示模板带出的工序列表。
生产主管可以在下发前调整工序顺序、增删工序、修改拍照要求。
点击“确认下发生产”后，系统生成 production_route_instance 和 production_step_instance。
已下发实例标记 frozen=true。
再次打开该产品/点位时，不能再修改已冻结工序结构。
```

---

### 2.3 生产工序执行模块

负责内容：

```text
生产任务列表
生产任务状态流转
工序开始
工序完成
工序详情
生产进度计算
管理端生产看板
```

工序状态建议：

```text
PENDING      未开始
IN_PROGRESS 进行中
COMPLETED   已完成
BLOCKED     暂停/异常，可选，MVP 可弱化
```

注意：本项目当前明确 **不做执行中返工、跳过、追加工序**。

DoD：

```text
工人可以看到分配给自己的 PENDING / IN_PROGRESS 工序任务。
工人点击开始后，状态变为 IN_PROGRESS，记录 started_at。
工人点击完成后，状态变为 COMPLETED，记录 completed_at。
如果工序要求拍照，完成前必须至少上传一张照片。
如果工序要求备注，完成前必须填写备注。
管理端可以看到每个产品/点位的完成工序数、总工序数和进度百分比。
```

---

### 2.4 工人移动端 H5 模块

负责内容：

```text
工人登录后的移动端首页
我的工序任务
工序详情
拍照上传
备注填写
开始工序
完成工序
```

移动端原则：

```text
页面越少越好。
按钮越明确越好。
不要让工人看到复杂订单配置。
不要让工人编辑流程。
不要让工人选择客户。
```

建议路由：

```text
/worker/tasks
/worker/tasks/:id
/worker/tasks/:id/checkin
```

DoD：

```text
工人登录后只看到自己的任务。
任务卡片显示：订单名、产品/点位名、工序名、状态、是否需要拍照。
工序详情页显示：工序要求、上一工序状态、图片上传入口、备注输入框。
完成工序后，管理端生产看板能立即看到进度变化。
```

---

### 2.5 文件/图片模块

你负责统一文件上传能力，客户线也会调用。

负责内容：

```text
图片上传
附件记录
文件业务绑定
文件查看
文件删除/软删除
```

统一表：

```text
file_asset
```

建议字段：

```text
id
biz_type
biz_id
file_name
file_url
file_type
file_size
uploaded_by
uploaded_at
remark
deleted
```

biz_type 建议：

```text
CUSTOMER
CUSTOMER_LEAD
ORDER
ORDER_ITEM
PRODUCTION_STEP
PRODUCTION_CHECKIN
MATERIAL
INVENTORY_TRANSACTION
```

DoD：

```text
前端可以调用统一上传接口上传图片。
上传后返回 file_id 和 file_url。
客户线可以把文件绑定到客户、订单、产品/点位。
生产线可以把文件绑定到工序打卡。
不允许每个模块各自实现一套上传逻辑。
```

---

### 2.6 库存与物料模块

负责内容：

```text
物料档案
当前库存
安全库存
库存预警
简单入库
简单出库
订单/产品点位物料需求
库存是否足够判断
```

MVP 库存范围：

```text
物料档案 + 当前库存 + 安全库存 + 入库 + 出库 + 物料需求 + 库存预警
```

明确不做：

```text
多仓库
批次
先进先出
盘点
成本核算
供应商对账
采购付款
复杂 BOM 自动展开
```

DoD：

```text
仓库管理员可以新增物料。
仓库管理员可以录入当前库存和安全库存。
可以创建入库记录，库存增加。
可以创建出库记录，库存减少。
可以为某个 order_item 手动录入物料需求。
系统能判断需求数量是否超过当前库存。
老板驾驶舱能显示库存预警数量。
```

---

### 2.7 考勤打卡模块

你负责基础考勤，因为迟到扣贡献值属于生产/员工执行侧事件。

负责内容：

```text
员工上班打卡
记录打卡时间
判断是否迟到
生成贡献值扣减事件
员工查看自己的考勤记录
```

注意：贡献值账户和流水主表归客户线负责，你不能直接改贡献值余额。

你只调用贡献值事件接口：

```http
POST /api/contribution/events
```

示例：

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

DoD：

```text
员工可以打上班卡。
系统能根据配置的上班时间判断是否迟到。
迟到时生成贡献值事件。
贡献值模块收到事件后生成流水。
考勤页面不展示工资，只展示考勤状态和贡献值影响。
```

---

### 2.8 老板驾驶舱和生产大屏

你负责驾驶舱页面和聚合接口，但部分数据需要客户线提供统计接口。

负责展示：

```text
进行中订单数
生产中产品/点位数
待完成工序数
今日完成工序数
库存预警数
即将交付订单
生产进度排行
本月贡献值排行
本月预计奖励排行
今日新增潜在客户
今日新增意向客户
```

你自己直接负责的数据：

```text
生产中产品/点位数
待完成工序数
今日完成工序数
库存预警数
考勤异常数
```

需要客户线提供的数据：

```text
今日新增潜在客户
今日新增意向客户
客户公海统计
订单总数
贡献值排行
预计奖励排行
```

DoD：

```text
老板打开驾驶舱能看到客户、订单、生产、库存、贡献值的总览。
驾驶舱不展示个人工资、工资条、社保、真实薪资明细。
如果客户线接口暂未完成，允许使用 mock fallback，但必须标注 TODO。
```

---

## 3. 你负责的数据表

建议由你维护以下 Flyway 脚本段：

```text
V030__process_route.sql
V031__production_instance.sql
V032__production_checkin.sql
V040__inventory_material.sql
V041__inventory_transaction.sql
V050__attendance.sql
V060__dashboard_support.sql
```

你主要负责表：

```text
process_route_template
process_step_template
production_route_instance
production_step_instance
production_step_checkin
file_asset
material
inventory_stock
inventory_transaction
material_requirement
attendance_record
dashboard_snapshot（可选）
```

### 3.1 工艺路线模板表建议

```text
process_route_template
- id
- route_name
- product_type
- description
- enabled
- created_by
- created_at
- updated_at
```

```text
process_step_template
- id
- route_template_id
- step_name
- step_order
- assigned_role
- default_assignee_id
- photo_required
- remark_required
- mobile_enabled
- estimated_hours
- enabled
```

### 3.2 生产实例表建议

```text
production_route_instance
- id
- order_id
- order_item_id
- source_template_id
- route_name_snapshot
- status
- frozen
- dispatched_by
- dispatched_at
- created_at
```

```text
production_step_instance
- id
- route_instance_id
- order_id
- order_item_id
- step_name
- step_order
- assigned_role
- assigned_user_id
- status
- photo_required
- remark_required
- planned_start_time
- planned_end_time
- started_at
- completed_at
- frozen
```

### 3.3 打卡表建议

```text
production_step_checkin
- id
- step_instance_id
- employee_id
- checkin_type    # START / COMPLETE / PHOTO / NOTE
- remark
- file_ids        # 可 JSON，也可关联表
- created_at
```

### 3.4 库存表建议

```text
material
- id
- material_code
- material_name
- spec
- unit
- category
- safety_stock
- enabled
```

```text
inventory_stock
- id
- material_id
- current_qty
- safety_stock
- updated_at
```

```text
inventory_transaction
- id
- material_id
- transaction_type   # IN / OUT
- quantity
- related_order_id
- related_order_item_id
- operator_id
- remark
- created_at
```

```text
material_requirement
- id
- order_id
- order_item_id
- material_id
- required_qty
- fulfilled_qty
- remark
```

---

## 4. 你可以触碰的模块

```text
backend/src/main/java/.../process/**
backend/src/main/java/.../production/**
backend/src/main/java/.../inventory/**
backend/src/main/java/.../attendance/**
backend/src/main/java/.../dashboard/**
backend/src/main/java/.../file/**
web/src/modules/process/**
web/src/modules/production/**
web/src/modules/inventory/**
web/src/modules/worker/**
web/src/modules/attendance/**
web/src/modules/dashboard/**
docs/production/**
openspec/changes/process-route-template/**
openspec/changes/production-dispatch-checkin/**
openspec/changes/inventory-material/**
openspec/changes/attendance-checkin/**
openspec/changes/dashboard-demo/**
```

---

## 5. 你不能触碰的雷区

### 5.1 不要改客户公海状态机

客户公海归你朋友负责。你不能修改：

```text
customer_lead
lead_claim
lead_contact_record
public_pool 状态流转
每日上新任务规则
每日领取上限规则
冷却期规则
```

如果生产大屏需要客户统计，只能调用对方提供的统计接口。

---

### 5.2 不要直接修改贡献值余额

贡献值核心归客户线负责。

禁止：

```sql
UPDATE contribution_account SET score = score + 10;
```

必须通过：

```http
POST /api/contribution/events
```

你只负责提交生产/考勤产生的贡献事件，例如：

```text
工序按时完成
生产任务完成
迟到扣分
管理员生产侧奖励
```

最终是否入账，由贡献值模块决定。

---

### 5.3 不要改订单核心字段

客户线负责订单与产品/点位。

你只能读取：

```text
order
order_item
custom_field_value
```

你最多只能更新生产相关字段：

```text
order_item.production_status
order_item.production_progress
order_item.production_route_instance_id
```

禁止修改：

```text
客户名称
联系人
订单金额
订单自定义字段
产品/点位规格
产品/点位数量
报价信息
客户来源
```

---

### 5.4 不要做执行中返工、跳过、追加工序

当前业务规则明确：

```text
生产前灵活配置。
下发后冻结执行。
```

所以 MVP 不做：

```text
执行中追加工序
执行中删除工序
执行中跳过工序
执行中返工
工人调整工序顺序
```

未来可扩展，但本次不要做。

---

### 5.5 不要做复杂库存

不要为了“ERP 完整性”加这些：

```text
多仓库
批次
盘点
成本核算
财务库存
供应商付款
采购流程审批
```

两周 MVP 只做物料保障。

---

## 6. 你需要向客户线提供的接口

### 6.1 生产状态查询

```http
GET /api/production/order-items/{orderItemId}/summary
```

返回建议：

```json
{
  "orderItemId": 1001,
  "productionStatus": "IN_PROGRESS",
  "progress": 45,
  "totalSteps": 10,
  "completedSteps": 4,
  "currentStepName": "喷漆",
  "dispatched": true
}
```

### 6.2 进入生产配置页

客户线订单详情页需要跳转到你的页面：

```text
/process/order-items/:orderItemId/configure
```

### 6.3 库存需求查询

```http
GET /api/material-requirements/order-items/{orderItemId}
```

用于客户线订单详情中展示该产品/点位物料需求和库存是否足够。

---

## 7. 你需要消费客户线提供的接口

### 7.1 产品/点位详情

```http
GET /api/order-items/{orderItemId}
```

你至少需要：

```json
{
  "id": 1001,
  "orderId": 501,
  "itemName": "入口精神堡垒",
  "productType": "SPIRIT_FORTRESS",
  "quantity": 1,
  "status": "CONFIRMED",
  "productionStatus": "NOT_DISPATCHED"
}
```

### 7.2 订单概要

```http
GET /api/orders/{orderId}/summary
```

你至少需要：

```json
{
  "id": 501,
  "orderName": "某园区导视系统项目",
  "customerName": "某园区开发公司",
  "deliveryDate": "2026-06-20",
  "projectManagerId": 3
}
```

### 7.3 贡献值事件接口

```http
POST /api/contribution/events
```

你只提交事件，不直接改贡献值。

---

## 8. 推荐开发顺序

### Day 1：生产侧规格和数据契约

```text
确认 order_item 读取契约。
写 process-route-template 的 proposal/design/spec/tasks。
写 production-dispatch-checkin 的 proposal/design/spec/tasks。
创建 Flyway 表结构草案。
```

### Day 2-3：工艺路线模板

```text
后端：模板 CRUD。
前端：模板列表和编辑页。
测试：工序顺序、拍照要求、备注要求保存正确。
```

### Day 4-5：生产下发和实例冻结

```text
后端：模板复制为实例。
后端：冻结规则。
前端：产品/点位生产配置页。
测试：模板修改不影响已下发实例。
```

### Day 6-7：工人移动端

```text
任务列表。
任务详情。
开始工序。
完成工序。
拍照上传。
```

### Day 8-9：库存

```text
物料档案。
库存余额。
入库/出库。
物料需求。
库存预警。
```

### Day 10：考勤和贡献事件

```text
基础打卡。
迟到判断。
调用贡献值事件接口。
```

### Day 11-12：驾驶舱和演示打磨

```text
生产统计。
库存预警统计。
对接客户线统计。
准备 mock fallback。
```

### Day 13-14：联调、验收、演示脚本

```text
跑完整链路。
准备演示账号。
准备演示图片。
修复 UI 和数据问题。
```

---

## 9. 生产侧 PR 检查清单

每次提交 PR 前必须检查：

```text
[ ] 是否有对应 OpenSpec change？
[ ] 是否写清楚 DoD？
[ ] 是否没有修改客户线表？
[ ] 是否没有直接修改 contribution_account？
[ ] 是否没有把生产任务直接绑定模板执行？
[ ] 是否保证模板可变、实例冻结？
[ ] 是否验证工序下发后不能改结构？
[ ] 是否验证需要拍照的工序不能无图完成？
[ ] 是否验证库存出库会扣减库存？
[ ] 是否提供演示数据？
[ ] 是否能跑通至少一个端到端场景？
```

---

## 10. 给 Codex 的推荐提示词

你可以在每个生产侧功能开始前这样要求 Codex：

```text
你现在负责本项目的生产履约与资源看板线。不要直接写代码。
先阅读当前 change 的 proposal/design/spec/tasks。
你的任务是检查本功能是否符合以下原则：
1. 配置阶段灵活，执行阶段冻结。
2. 模板可变，实例冻结。
3. 生产只消费客户线订单，不修改订单核心字段。
4. 工人端只能执行任务，不能修改流程。
5. 贡献值只能通过事件接口提交，不能直接改账户余额。

请先输出：
- 需求理解
- 数据流
- 状态机
- 接口契约
- 雷区
- 验收标准
确认后再开始实现。
```

---

## 11. 你的最终验收场景

生产侧必须能演示：

```text
1. 从客户线拿到“某园区导视系统项目”的产品/点位。
2. 给“入口精神堡垒”选择精神堡垒工艺路线。
3. 下发前调整工序顺序和拍照要求。
4. 点击确认下发生产。
5. 系统生成冻结生产任务。
6. 工人移动端看到自己的任务。
7. 工人点击开始。
8. 工人上传照片并完成工序。
9. 管理端看到进度变化。
10. 库存模块显示某物料低于安全库存。
11. 老板驾驶舱看到生产进度和库存预警。
```

如果这 11 步跑通，你的生产履约线就是 MVP 成功的核心。

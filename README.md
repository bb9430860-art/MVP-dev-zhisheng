# MVP-dev-zhisheng

> 仓库地址：<https://github.com/bb9430860-art/MVP-dev-zhisheng>
>
> 项目定位：面向标识标牌 / 非标制造企业的两周 MVP 演示系统。当前首个样板客户为安徽六安至胜标识相关业务场景。系统既要能完成客户演示，也要为后续“非标制造数字化平台底座”沉淀可复用能力。

---

## 1. 项目一句话说明

本项目不是传统意义上的单点 ERP、CRM 或 MES，而是一个围绕中小型非标制造企业经营闭环设计的 MVP：

```text
客户增长
→ 客户公海
→ 意向转化
→ 正式客户
→ 项目订单
→ 多产品/点位
→ 可配置工艺路线
→ 确认下发生产
→ 工人移动端拍照打卡
→ 库存预警
→ 贡献值激励
→ 老板驾驶舱 / 大屏
```

客户演示时不要强调“我们以后要复用给其他客户”，而要表达为：

> 系统支持贵公司灵活配置客户、订单、生产流程、库存和员工激励机制。

内部开发时必须明确：

> 这是未来可复用的非标制造数字化平台底座，至胜只是第一个行业样板。

---

## 2. MVP 核心目标

两周内交付一个可演示 MVP，不追求正式生产部署，但必须能跑通完整业务故事。

### 2.1 演示主线

```text
1. 业务员上传潜在客户到客户公海。
2. 系统校验手机号唯一，客户进入可领取状态。
3. 另一个业务员领取客户，系统显示已领取人与领取时间。
4. 业务员限时联系客户并填写联系记录。
5. 客户从潜在客户转为意向客户。
6. 原始上传人和转化业务员获得贡献值。
7. 意向客户转为正式客户。
8. 给正式客户创建“园区导视系统项目”订单。
9. 订单下包含多个产品/点位。
10. 不同产品/点位选择不同工艺路线模板。
11. 生产主管在生产前调整工序并确认下发。
12. 系统复制模板生成冻结的生产实例。
13. 工人移动端查看自己的工序任务。
14. 工人点击开始、上传照片、填写备注、完成工序。
15. 管理端查看生产进度。
16. 库存模块显示物料需求、库存余额和库存预警。
17. 老板驾驶舱看到客户、公海、订单、生产、库存、贡献值整体情况。
18. 大屏展示业务员贡献值、预计奖励和业绩排行，但不展示真实工资。
```

### 2.2 最高优先级原则

```text
上传不奖励，转意向才奖励。
领取不奖励，联系才冷却。
数量算任务，质量算贡献，成交算大奖。
配置阶段灵活，执行阶段冻结。
模板可以变，实例不能乱。
订单由客户线创建，生产线只消费订单。
贡献值必须走流水，不能直接改余额。
大屏展示贡献和预计奖励，不展示真实工资。
```

---

## 3. 两人开发分工

本项目不按“前端 / 后端”拆分，而按业务闭环拆分。每个人都负责自己业务线的数据库、后端接口、前端页面、演示数据、测试和验收。

### 3.1 Cursor 线：客户增长、CRM、公海、贡献值与订单线

负责人：朋友  
工具：Cursor  
主文档：`docs/cursor-customer-line.md`

负责范围：

```text
客户公海
潜在客户上传
手机号唯一校验
客户领取
联系记录
冷却规则
意向客户
正式客户
客户档案
联系人
沟通记录
销售侧贡献值
每日客户上新任务
订单管理
产品/点位管理
订单自定义字段
产品/点位自定义字段
```

核心业务闭环：

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

禁止触碰的生产侧范围：

```text
process_route_template
process_step_template
production_route_instance
production_step_instance
production_step_checkin
inventory_stock
inventory_transaction
material_requirement
attendance_record
dashboard 聚合逻辑中的生产侧计算
```

Cursor 线不得直接修改生产实例、工序任务、库存流水和生产进度计算。如果订单页面需要展示生产信息，应通过生产线提供的接口读取。

---

### 3.2 Codex 线：生产履约、库存、考勤与看板线

负责人：许安  
工具：Codex  
主文档：`docs/codex-production-line.md`

负责范围：

```text
工艺路线模板
工序模板
产品/点位生产配置
确认下发生产
生产路线实例
生产工序实例
工人移动端任务
拍照打卡
生产进度看板
通用文件/图片上传
物料档案
当前库存
安全库存
库存预警
简单入库
简单出库
订单物料需求
考勤打卡
迟到贡献值事件
老板驾驶舱
经营大屏
```

核心业务闭环：

```text
读取客户线创建的订单/产品点位
→ 选择或配置工艺路线模板
→ 生产前灵活调整工序
→ 确认下发生产
→ 复制模板生成冻结生产实例
→ 工人移动端查看任务
→ 开始工序 / 上传照片 / 填写备注 / 完成工序
→ 管理端查看生产进度
→ 库存预警提示物料风险
→ 老板驾驶舱查看整体状态
```

禁止触碰的客户侧范围：

```text
customer_lead
lead_claim
lead_contact_record
customer
customer_contact
customer_communication
order 核心字段
order_item 核心业务字段
contribution_account 余额直接修改
contribution_transaction 销售侧奖励规则
```

Codex 线可以读取订单和产品/点位，但不能反向污染客户与订单核心数据。生产线只能更新生产相关字段，例如 `production_status`、`production_progress`。

---

## 4. 共享契约

为了两个人互不干扰，必须先定共享契约。

### 4.1 API 返回格式

所有接口统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页格式：

```json
{
  "records": [],
  "total": 100,
  "page": 1,
  "pageSize": 20
}
```

### 4.2 订单与产品/点位契约

客户线创建：

```text
order
order_item
```

生产线消费：

```text
order_id
order_item_id
item_name
product_type
quantity
status
```

生产线允许更新：

```text
order_item.production_status
order_item.production_progress
```

生产线禁止修改：

```text
客户信息
订单金额
订单自定义字段
产品规格
产品数量
客户联系人
客户来源
```

### 4.3 生产实例契约

生产线必须遵守：

```text
工艺路线模板 process_route_template
→ 复制
→ 生产路线实例 production_route_instance
→ 生产工序实例 production_step_instance
→ frozen = true
→ 工人执行
```

禁止直接让生产任务引用模板执行。

### 4.4 贡献值事件契约

贡献值由客户线管理账户和流水。生产线、考勤线只能提交贡献值事件，不得直接改余额。

建议统一事件接口：

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

客户线负责：

```text
校验贡献规则
写 contribution_transaction
更新 contribution_account
展示个人贡献页
展示排行榜
```

生产线负责：

```text
产生生产/考勤相关贡献事件
不直接更新贡献值账户余额
```

### 4.5 文件上传契约

统一使用通用附件表：

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
```

支持绑定对象：

```text
CUSTOMER
ORDER
ORDER_ITEM
PRODUCTION_STEP
CHECKIN
MATERIAL
ATTENDANCE
```

---

## 5. 客户公海规则

客户公海是 MVP 的重点之一。

### 5.1 状态

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

### 5.2 状态流转

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
→ 填写联系记录：未接通 / 暂不需要
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

### 5.3 关键规则

```text
上传客户不需要人工审核。
手机号必须唯一，重复手机号禁止上传。
上传客户只计入每日上新任务，不直接发奖励。
领取客户不发奖励。
领取客户只代表暂时占用。
只有填写联系记录后才进入冷却。
领取后不联系，不能让客户进入冷却。
领取后超时未联系，客户自动释放回公海。
客户变成意向客户后，原始上传人才获得贡献值。
客户成交后，成交人和原始上传人都获得贡献值。
```

### 5.4 防止恶意领取

必须支持：

```text
每日领取上限
同时持有未联系客户上限
领取后限时联系
超时未联系自动释放
超时未联系扣贡献值
```

推荐规则：

```text
每日最多领取数 = min(公海可领取客户数 × 10%, 固定上限)
每日最低目标按“有效联系数”计算，而不是按“领取数”计算
同时最多持有 N 个已领取但未联系客户
```

---

## 6. 贡献值系统规则

### 6.1 贡献值不是工资系统

系统展示：

```text
贡献值
预计奖励
排行榜
激励金额
```

系统不展示：

```text
基本工资
真实工资条
社保
个税
实际发薪金额
员工详细扣款工资
```

### 6.2 贡献值事件

MVP 先支持：

```text
上传合格客户：计入每日上新数量，不发奖励
上传客户转意向：上传人 +贡献值
认领客户转意向：认领人 +贡献值
客户成交：成交人 +贡献值
客户成交：原始上传人 +贡献值
迟到：扣贡献值
管理员奖励：加贡献值
管理员扣减：扣贡献值
```

### 6.3 必须有流水

禁止只存余额。

必须有：

```text
contribution_account
contribution_transaction
```

所有贡献值变化都必须通过流水记录。

---

## 7. 工艺路线与生产规则

### 7.1 模板和实例分离

最高优先级规则：

```text
模板可以变，实例不能乱。
```

具体要求：

```text
管理员可以编辑工艺路线模板。
产品/点位下发生产前可以调整工序。
点击确认下发生产后，模板复制为生产实例。
生产实例 frozen = true。
冻结后工人不能增删工序、调整顺序、跳过工序或返工。
```

### 7.2 MVP 不做返工

当前明确不做：

```text
执行中追加工序
执行中跳过工序
执行中返工
执行中调整顺序
复杂 BPM 工作流
```

---

## 8. 库存规则

MVP 只做轻库存：

```text
物料档案
当前库存
安全库存
库存预警
简单入库
简单出库
订单/产品物料需求
判断库存是否足够
```

暂不做：

```text
多仓库
批次
盘点
先进先出
成本核算
供应商对账
采购付款
复杂 BOM 自动展开
```

---

## 9. 技术栈建议

### 9.1 后端

```text
Spring Boot 3
Java 17 或 Java 21
MariaDB
MyBatis-Plus 或 MyBatis-Flex
Flyway
```

### 9.2 前端

```text
Vue 3
Vite
TypeScript
Element Plus
```

移动端 H5 建议先和后台放在同一个前端项目中，通过路由区分：

```text
/admin/*
/worker/*
/screen/*
```

### 9.3 架构

```text
模块化单体
不要微服务
不要重型 BPM
不要一开始做 SaaS 多租户
但核心表预留 tenant_id
```

---

## 10. 推荐目录结构

```text
MVP-dev-zhisheng/
├── backend/
│   └── src/main/java/com/zhisheng/mvp/
│       ├── system/
│       ├── crm/
│       ├── contribution/
│       ├── order/
│       ├── process/
│       ├── production/
│       ├── inventory/
│       ├── attendance/
│       ├── dashboard/
│       └── file/
├── web/
│   └── src/
│       ├── modules/
│       │   ├── crm/
│       │   ├── contribution/
│       │   ├── order/
│       │   ├── process/
│       │   ├── production/
│       │   ├── inventory/
│       │   ├── worker/
│       │   └── dashboard/
├── openspec/
├── docs/
├── database/
├── scripts/
└── README.md
```

---

## 11. Flyway 数据库迁移规则

所有数据库结构变更必须通过 Flyway migration 文件提交，禁止只在本地手动改库。

推荐编号：

```text
V001__init_system.sql
V010__crm_public_pool.sql
V011__customer_archive.sql
V012__contribution.sql
V020__order_project.sql
V030__process_route.sql
V031__production_task.sql
V040__inventory.sql
V050__attendance_dashboard.sql
V900__seed_demo_data.sql
```

分工：

```text
V001 - V009：基础系统，共同维护
V010 - V029：客户线 / Cursor 维护
V030 - V059：生产线 / Codex 维护
V900+：演示数据，双方确认后维护
```

---

## 12. OpenSpec + Superpowers 使用规则

本项目必须重视 skills。它们不是装饰，而是为了防止 AI 写偏。

### 12.1 OpenSpec 负责规格与追溯

OpenSpec 工作流：

```text
explore
→ propose
→ design
→ specs
→ tasks
→ apply
→ verify
→ archive
```

每个 change 至少包含：

```text
proposal.md   # 为什么做
设计文档 design.md   # 怎么做
spec.md       # 做成什么样
tasks.md      # 分几步做
```

推荐 change：

```text
platform-foundation
crm-public-pool
customer-archive
contribution-system
configurable-order-project
process-route-template
production-dispatch-checkin
inventory-material
attendance-checkin
dashboard-demo
```

### 12.2 Superpowers 负责执行质量

优先使用：

```text
brainstorming
writing-plans
test-driven-development
requesting-code-review
verification-before-completion
```

不要一次加载全部 skills。按阶段使用：

```text
需求不清楚：brainstorming
需要拆任务：writing-plans
核心状态机：test-driven-development
准备合并：requesting-code-review
声明完成前：verification-before-completion
```

### 12.3 必须遵守的 AI 开发方式

```text
没有 spec，不写代码。
没有 DoD，不合并任务。
没有验证证据，不说完成。
不能让 AI 自由扩展需求。
不能让 AI 跨职责改对方模块。
```

### 12.4 代码审查顺序

先做规格合规审查：

```text
是否满足 specs？
是否满足 DoD？
是否触碰非目标？
是否越界修改对方模块？
```

再做代码质量审查：

```text
测试是否通过？
状态机是否稳定？
数据库迁移是否完整？
接口返回是否统一？
错误处理是否清楚？
```

---

## 13. GitHub 协作规则

仓库：<https://github.com/bb9430860-art/MVP-dev-zhisheng>

### 13.1 分支建议

```text
main
feature/customer-line
feature/production-line
feature/platform-foundation
feature/demo-seed
```

### 13.2 Commit 信息建议

```text
feat(crm): add public pool claim flow
feat(order): add project order item model
feat(process): add route template editor
feat(production): add worker checkin flow
feat(inventory): add stock warning
feat(contribution): add contribution transaction
fix(crm): prevent cooling before contact
fix(production): freeze step instance after dispatch
docs: update openspec for public pool rules
```

### 13.3 PR 检查清单

每个 PR 必须确认：

```text
是否只修改自己负责的模块？
是否更新 OpenSpec / docs？
是否有 Flyway migration？
是否有演示数据或测试数据？
是否有 DoD 验收说明？
是否没有触碰对方雷区？
是否能本地启动？
是否有必要截图或接口返回示例？
```

---

## 14. MVP 明确不做的事情

为了两周内完成，暂不做：

```text
完整工资系统
真实薪资核算
复杂绩效审批
复杂 BPM 流程引擎
多租户 SaaS
微服务
多仓库
批次库存
库存成本核算
自动 BOM 展开
复杂报价公式
供应商对账
采购付款
电话录音
短信群发
GPS 外勤轨迹
复杂权限矩阵
客户营销自动化
```

但要预留：

```text
tenant_id
动态字段能力
模板与实例分离
贡献值规则配置
流程规则配置
文件通用绑定
操作日志
```

---

## 15. 推荐开发顺序

### 第 0 阶段：项目底座

```text
项目结构
登录角色
菜单布局
API 返回格式
错误码
分页格式
Flyway
文件上传基础
OpenSpec 目录
```

### 第 1 阶段：客户到订单 / 工艺模板并行

Cursor：

```text
公海客户上传
客户领取
联系记录
转意向客户
贡献值流水
正式客户
订单与产品/点位
```

Codex：

```text
工艺路线模板
工序模板
文件上传
库存基础
移动端基础布局
```

### 第 2 阶段：订单到生产

Cursor 提供：

```text
order_id
order_item_id
product_type
```

Codex 实现：

```text
选择工艺路线
下发生产
冻结实例
工人移动端任务
拍照打卡
生产进度
```

### 第 3 阶段：库存、考勤、贡献值、大屏

Cursor：

```text
贡献值个人页
贡献排行榜
公海统计
订单统计
```

Codex：

```text
库存预警
订单物料需求
考勤打卡
老板驾驶舱
生产大屏
```

### 第 4 阶段：演示数据和演示脚本

准备：

```text
3 个业务员
1 个老板
1 个生产主管
2 个工人
1 个仓库管理员
30 条公海客户
5 条意向客户
2 个正式客户
1 个园区导视系统项目
6 个产品/点位
4 条工艺路线模板
30 条工序任务
10 条库存物料
若干贡献值流水
若干生产照片
```

---

## 16. 本地启动占位说明

具体命令以实际工程创建后为准。建议最终补充：

```bash
# 后端
cd backend
./mvnw spring-boot:run

# 前端
cd web
npm install
npm run dev
```

数据库建议：

```text
MariaDB
数据库名：zhisheng_mvp
编码：utf8mb4
迁移：Flyway 自动执行
```

---

## 17. 给 Codex 的开头提示词

详见：

```text
docs/codex-starting-prompt.md
```

Codex 启动新会话时，先粘贴该提示词。Codex 必须先阅读：

```text
docs/codex-production-line.md
docs/cursor-customer-line.md
README.md
```

没有明确“开始实现”之前，不允许直接改代码。

---

## 18. 给 Cursor 的开头提示词

详见：

```text
docs/cursor-starting-prompt.md
```

Cursor 启动新会话时，先粘贴该提示词。Cursor 必须先阅读：

```text
docs/cursor-customer-line.md
docs/codex-production-line.md
README.md
```

没有明确“开始实现”之前，不允许直接改代码。

---

## 19. 最终验收标准

MVP 最终必须能演示以下闭环：

```text
客户公海上传 → 领取 → 联系 → 意向 → 贡献值
意向客户 → 正式客户 → 创建订单
订单 → 多产品/点位 → 自定义字段
产品/点位 → 工艺路线模板 → 生产前调整 → 确认下发
生产实例冻结 → 工人移动端任务 → 拍照打卡 → 完成工序
库存物料 → 物料需求 → 库存预警
考勤打卡 → 迟到贡献值事件
老板驾驶舱 → 客户、公海、订单、生产、库存、贡献值总览
大屏 → 业绩、贡献值、预计奖励排行
```

如果这个闭环不能连起来，即使页面很多，也不算完成。

---

## 20. 项目口号

```text
客户怎么来，看得见。
订单怎么做，管得住。
生产到哪步，拍得下。
库存够不够，提前知。
员工谁贡献，算得清。
老板看全局，一屏懂。
```

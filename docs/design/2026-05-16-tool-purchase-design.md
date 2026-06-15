# 工具模块购买页与支付接口设计

## 1. 目标

为工具模块补齐购买能力，支持：

- 同一个工具对应多个套餐
- 支持两种支付模式：
  - 纯现金支付
  - 现金 + 积分支付
- 用户购买成功后，按套餐为用户发放工具可用次数
- 历史订单必须保留套餐快照，避免套餐后续改价、改次数导致历史订单失真

当前仓库已经具备统一订单与支付能力，本次设计在不污染统一支付主链路的前提下，为工具模块增加独立的购买扩展记录。

## 2. 现状分析

当前工具模块已有以下能力：

- 工具基础信息表：`osh_tool`
- 工具套餐表：`osh_tool_package`
- 用户工具余额表：`osh_tool_user_quota`
- 工具使用时通过 `osh_tool_user_quota.remaining_count` 判断是否可扣减
- 支付统一走订单模块：
  - 订单：`osh_order`
  - 支付流水：`osh_payment`
  - 支付创建入口：`/pc/pay/create`
  - 支付成功后通过 `OrderPaidHandlerRegistry` 分发业务处理器

当前不足：

- `osh_tool_user_quota` 只能表达某个用户在某个工具上的汇总剩余次数，无法表达买的是哪个套餐
- 统一订单表中的 `productId` 只能稳定记录 `toolId`，无法承载套餐快照
- 如果只依赖 `packageId` 回查当前套餐配置，会在套餐改价或改次数后导致历史订单与真实成交事实不一致

因此需要新增工具购买扩展表，作为工具支付业务的事实记录。

## 3. 总体方案

采用推荐方案：

- 统一订单继续只记录通用支付信息
- 工具购买扩展信息单独落表：`osh_tool_purchase_record`
- 支付成功后，由工具类型的支付后置处理器负责：
  - 幂等校验
  - 发放工具次数到 `osh_tool_user_quota`
  - 更新购买记录发放状态

职责边界如下：

- `osh_order`
  - 保存统一订单主信息
  - `productType = TOOL`
  - `productId = toolId`
- `osh_payment`
  - 保存支付流水
- `osh_tool_purchase_record`
  - 保存工具购买业务扩展信息与套餐快照
- `osh_tool_user_quota`
  - 保存用户在某个工具上的汇总剩余次数与已使用次数

## 4. 页面设计

购买页建议采用工具详情页内的独立购买区或支付弹层，避免引入复杂收银台。

### 4.1 页面结构

#### 4.1.1 工具信息区

展示内容：

- 工具名称
- 工具简介
- 当前剩余次数
- 权益说明：购买后按套餐增加使用次数

#### 4.1.2 套餐选择区

同一个工具下可展示多个套餐卡片，每个卡片展示：

- `packageName`
- `useCount`
- 纯现金价格
- 如果支持混合支付，则展示现金 + 积分价格
- 推荐标签、最划算标签（可选）

#### 4.1.3 支付方式区

根据套餐配置动态展示：

- `payType = 1`：只允许纯现金支付
- `payType = 3`：允许两种支付方式展示
  - 纯现金支付
  - 现金 + 积分支付

说明：

- 本期不支持用户自定义积分抵扣比例
- 混合支付金额由套餐预先配置，前端只负责选择，不负责计算

#### 4.1.4 订单确认区

展示内容：

- 工具名称
- 套餐名称
- 增加次数
- 支付方式
- 现金金额
- 积分金额
- 提交按钮：`立即支付`

## 5. 套餐支付规则

当前仅支持以下两种套餐支付类型：

- `1`：纯现金支付
- `3`：现金 + 积分支付

约束如下：

- 前端不得自行上传成交金额
- 后端必须根据套餐配置决定实际现金金额和积分金额
- 同一个套餐在下单时生成快照，支付后发放权益以快照为准

## 6. 数据模型设计

### 6.1 新增表：`osh_tool_purchase_record`

作用：

- 记录工具购买扩展信息
- 保存套餐成交快照
- 记录支付状态与权益发放状态
- 支持历史订单查询、补发、排障与对账

建议字段如下：

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `order_no` | varchar(64) | 统一订单号 |
| `payment_no` | varchar(64) | 支付流水号 |
| `user_id` | bigint | 用户 ID |
| `tool_id` | bigint | 工具 ID |
| `package_id` | bigint | 套餐 ID |
| `tool_name_snapshot` | varchar(128) | 工具名称快照 |
| `package_name_snapshot` | varchar(128) | 套餐名称快照 |
| `package_use_count_snapshot` | int | 套餐次数快照 |
| `package_cash_amount_snapshot` | decimal(10,2) | 套餐现金金额快照 |
| `package_point_amount_snapshot` | int | 套餐积分金额快照 |
| `package_pay_type_snapshot` | tinyint | 套餐支付类型快照：1-纯现金，3-现金+积分 |
| `order_status` | tinyint | 订单状态：0-待支付，1-已支付，2-已取消，3-已关闭 |
| `grant_status` | tinyint | 发放状态：0-待发放，1-已发放，2-发放失败 |
| `grant_time` | datetime | 发放完成时间 |
| `remark` | varchar(255) | 备注 |
| `create_by` | varchar(64) | 创建人 |
| `create_time` | datetime | 创建时间 |
| `update_by` | varchar(64) | 更新人 |
| `update_time` | datetime | 更新时间 |
| `delete_flag` | tinyint | 逻辑删除标记 |

### 6.2 表设计说明

关键设计原则：

- `package_id` 只用于关联当前套餐
- 套餐真实成交事实必须依赖快照字段
- 发放次数时必须使用 `package_use_count_snapshot`
- 页面展示历史购买记录时，优先展示 snapshot 字段

### 6.3 建议索引

- 唯一索引：`uk_order_no(order_no)`
- 普通索引：`idx_user_tool(user_id, tool_id)`
- 普通索引：`idx_user_package(user_id, package_id)`
- 普通索引：`idx_grant_status(grant_status)`

## 7. SQL 草案

```sql
CREATE TABLE `osh_tool_purchase_record`
(
    `id`                           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`                     VARCHAR(64) NOT NULL COMMENT '统一订单号',
    `payment_no`                   VARCHAR(64) NOT NULL COMMENT '支付流水号',
    `user_id`                      BIGINT NOT NULL COMMENT '用户ID',
    `tool_id`                      BIGINT NOT NULL COMMENT '工具ID',
    `package_id`                   BIGINT NOT NULL COMMENT '套餐ID',
    `tool_name_snapshot`           VARCHAR(128) NOT NULL COMMENT '工具名称快照',
    `package_name_snapshot`        VARCHAR(128) NOT NULL COMMENT '套餐名称快照',
    `package_use_count_snapshot`   INT NOT NULL COMMENT '套餐次数快照',
    `package_cash_amount_snapshot` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '套餐现金金额快照',
    `package_point_amount_snapshot` INT NOT NULL DEFAULT 0 COMMENT '套餐积分金额快照',
    `package_pay_type_snapshot`    TINYINT NOT NULL COMMENT '套餐支付类型快照：1-纯现金，3-现金+积分',
    `order_status`                 TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0-待支付，1-已支付，2-已取消，3-已关闭',
    `grant_status`                 TINYINT NOT NULL DEFAULT 0 COMMENT '发放状态：0-待发放，1-已发放，2-发放失败',
    `grant_time`                   DATETIME DEFAULT NULL COMMENT '发放时间',
    `remark`                       VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_by`                    VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time`                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`                    VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time`                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`                  TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_tool` (`user_id`, `tool_id`),
    KEY `idx_user_package` (`user_id`, `package_id`),
    KEY `idx_grant_status` (`grant_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='工具购买记录表';
```

## 8. 接口设计

### 8.1 查询购买页详情

接口：

`GET /app/tool/purchase/detail?toolId={toolId}`

用途：

- 获取工具基础信息
- 获取当前用户剩余次数
- 获取可购买套餐列表

返回示例：

```json
{
  "toolId": 1001,
  "toolName": "AI海报生成器",
  "description": "用于生成封面与营销海报",
  "remainingCount": 12,
  "packages": [
    {
      "packageId": 2001,
      "packageName": "体验包",
      "useCount": 10,
      "cashAmount": 9.90,
      "pointAmount": 0,
      "payType": 1
    },
    {
      "packageId": 2002,
      "packageName": "推荐包",
      "useCount": 50,
      "cashAmount": 29.90,
      "pointAmount": 100,
      "payType": 3
    }
  ]
}
```

返回说明：

- 套餐列表仅返回启用状态套餐
- `remainingCount` 从 `osh_tool_user_quota` 中获取
- 若用户未登录，`remainingCount` 返回 0

### 8.2 创建工具购买订单

接口：

`POST /app/tool/purchase/create`

请求参数：

```json
{
  "toolId": 1001,
  "packageId": 2002,
  "payType": 3,
  "channel": "wxpay"
}
```

参数说明：

- `toolId`：工具 ID
- `packageId`：套餐 ID
- `payType`：支付方式，允许值 `1` 或 `3`
- `channel`：支付渠道，如 `wxpay`、`alipay`

校验规则：

- 用户必须登录
- 工具必须存在且可购买
- 套餐必须存在，且属于当前工具
- 套餐必须为启用状态
- 请求中的 `payType` 必须与套餐允许的支付方式一致
- 不允许前端上传金额

处理逻辑：

1. 查询工具与套餐
2. 根据套餐构建成交快照
3. 构建统一订单参数
4. 调统一订单服务创建订单与支付流水
5. 新增 `osh_tool_purchase_record`
6. 返回支付二维码、支付链接、订单号、支付流水号

### 8.3 查询我的工具购买记录

接口：

`GET /app/tool/purchase/list?toolId=&pageNum=&pageSize=`

用途：

- 用户查看购买历史
- 后台补发或排查问题

返回内容建议包括：

- 订单号
- 工具名称快照
- 套餐名称快照
- 次数快照
- 现金金额快照
- 积分金额快照
- 订单状态
- 发放状态
- 创建时间
- 发放时间

## 9. 统一订单参数映射

工具购买仍然走统一支付链路，映射关系如下：

- `productType = TOOL`
- `productId = toolId`
- `productName = toolName`
- `purchaseMode = NORMAL`
- `originalAmount = package_cash_amount_snapshot`
- `payableAmount = package_cash_amount_snapshot`
- `discountAmount = 0`

说明：

- 对于现金 + 积分支付场景，统一支付只负责现金部分
- 积分部分在工具购买业务中单独扣减或冻结
- 当前设计文档以“现金部分走统一支付”为基线

## 10. 支付成功后的幂等与发放流程

### 10.1 新增商品类型

在 `ProductTypeEnum` 中新增：

- `TOOL`

建议编码：

- `TOOL(5, "工具")`

### 10.2 新增支付后置处理器

新增：

- `ToolPaidHandler implements OrderPaidHandler`

职责：

- 根据 `orderNo` 查询 `osh_tool_purchase_record`
- 幂等判断
- 为用户发放次数
- 更新购买记录发放状态

### 10.3 发放流程

支付成功回调进入统一支付服务后：

1. 更新 `osh_payment` 为支付成功
2. 更新 `osh_order` 为已支付
3. 根据 `productType = TOOL` 调用 `ToolPaidHandler`
4. `ToolPaidHandler` 查询 `osh_tool_purchase_record`
5. 若 `grant_status = 1`，直接返回，保证幂等
6. 按 `package_use_count_snapshot` 发放工具次数
7. 更新 `grant_status = 1`
8. 更新 `grant_time`
9. 若发放异常，记录为 `grant_status = 2`

## 11. 用户工具次数发放逻辑

发放目标表：`osh_tool_user_quota`

处理规则：

- 若用户该工具余额记录不存在，则新增一条
- 若已存在，则：
  - `remaining_count += package_use_count_snapshot`
- 不修改 `used_count`
- 更新 `update_by`、`update_time`

消费时仍沿用现有逻辑：

- 使用工具前校验 `remaining_count > 0`
- 使用成功后执行扣减

## 12. 取消与关闭订单处理

### 12.1 用户取消支付

当统一订单取消时：

- `osh_order.status` 更新为已取消或已关闭
- `osh_tool_purchase_record.order_status` 同步更新为已取消或已关闭
- `grant_status` 不变，保持待发放

### 12.2 支付超时关闭

支付超时后：

- 统一订单关闭
- 工具购买记录同步关闭
- 不发放权益

## 13. 混合支付说明

当前期望支持：

- 纯现金支付
- 现金 + 积分支付

本设计建议：

- 纯现金支付：
  - 直接走统一支付
- 现金 + 积分支付：
  - 下单前先校验用户积分余额是否足够
  - 统一支付只处理现金部分
  - 积分扣减建议与订单创建放在一个本地事务中，至少保证“订单创建成功时已完成积分侧占用或扣减”

若暂时没有统一积分账户体系，可先按以下方式预留：

- `package_point_amount_snapshot` 必填
- 创建工具购买订单时只校验并返回
- 真正的积分扣减逻辑后续补齐

## 14. 推荐的实现落点

建议新增如下代码：

- 域对象
  - `com.backstage.system.domain.tool.OshToolPurchaseRecord`
- Mapper
  - `com.backstage.system.mapper.tool.OshToolPurchaseRecordMapper`
  - `mapper/tool/OshToolPurchaseRecordMapper.xml`
- 请求对象
  - `com.backstage.system.request.tool.ToolPurchaseCreateRequest`
- VO
  - `ToolPurchaseDetailVO`
  - `ToolPurchasePackageVO`
  - `ToolPurchaseListVO`
- Service
  - `ToolPurchaseService`
  - `ToolPurchaseServiceImpl`
- Controller
  - `com.backstage.system.controller.tool.ToolPurchaseController`
- 支付后置处理器
  - `com.backstage.system.service.order.handler.ToolPaidHandler`

## 15. 时序概要

### 15.1 下单流程

1. 前端调用购买详情接口
2. 用户选择套餐与支付方式
3. 前端调用创建工具订单接口
4. 后端校验工具与套餐
5. 后端生成套餐快照
6. 后端调用统一支付创建订单
7. 后端落 `osh_tool_purchase_record`
8. 返回支付参数

### 15.2 支付成功流程

1. 支付平台回调统一支付服务
2. 统一支付更新订单与支付状态
3. 根据 `productType = TOOL` 路由到工具支付处理器
4. 工具支付处理器幂等校验 `grant_status`
5. 发放工具次数
6. 更新 `grant_status = 已发放`

## 16. 风险与注意事项

- 历史订单展示必须依赖快照字段，不能回查当前套餐表
- 发放工具次数时必须做幂等处理
- 混合支付中的积分侧，如果当前系统还没有完整扣减链路，需要提前补上或显式降级
- 订单取消、超时关闭要同步更新工具购买记录状态
- 新增 `TOOL` 商品类型后，需要同步保证统一支付回调处理器可识别

## 17. 最终结论

工具购买能力建议采用以下最终实现方式：

- 新增 `osh_tool_purchase_record` 保存工具购买事实与套餐快照
- 统一订单继续只记录 `toolId`
- `osh_tool_user_quota` 继续做汇总余额
- 支付成功后通过 `TOOL` 类型处理器为用户发放次数
- 页面侧围绕“选套餐 -> 选支付方式 -> 立即支付”构建最小闭环

该方案能兼容现有支付主链路，同时满足“同一个工具多个套餐”和“套餐后续变更不影响历史订单”的核心要求。

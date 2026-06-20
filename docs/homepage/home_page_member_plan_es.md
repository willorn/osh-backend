# 首页会员套餐模块技术方案

## 1. 方案目标

首页“选择套餐”模块改为独立查询 Elasticsearch，不再直接复用会员中心接口，也不走 `Flink -> ES` 的消费链路。

本次方案目标：

- 首页新增独立接口，专门返回首页套餐展示数据
- 首页查询走 ES，会员中心继续走 MySQL
- 会员套餐基础字段尽量复用现有表结构，不新增业务字段
- 实现方式参考课程模块 ES 方案
- 支持全量同步、单条 upsert、单条删除
- 只改首页套餐读取链路，不影响会员中心下单、支付、订单逻辑

## 2. 当前现状

### 2.1 会员中心当前实现

当前会员模块接口：

- `GET /pc/user/member/center`
- `GET /pc/user/member/plans`
- `GET /pc/user/member/admin/plans`

当前实现是直接查 MySQL，不是查 ES。

关键代码位置：

- `backstage-system/src/main/java/com/backstage/system/controller/member/MemberCenterController.java`
- `backstage-system/src/main/java/com/backstage/system/service/member/impl/MemberCenterServiceImpl.java`
- `backstage-system/src/main/java/com/backstage/system/mapper/member/OshMemberPlanMapper.java`

当前 `listPlans()` 的核心逻辑是：

- 基于 `osh_member_plan` 查询启用套餐
- 再附加 `osh_member_benefit` 权益列表
- 最终返回 `List<OshMemberPlan>`

### 2.2 当前 ES 实现风格

项目中课程、工具、网站、信息差模块已经有成熟的 ES 接入方式，主要特点如下：

- 使用 `RestHighLevelClient`
- 使用独立的 `*EsMapper`
- 使用独立的 `*EsService`
- 支持全量同步到 ES
- 支持单条 upsert 到 ES
- 查询接口直接查 ES

可参考模块：

- `backstage-system/src/main/java/com/backstage/system/mapper/course/OshCourseEsMapper.java`
- `backstage-system/src/main/java/com/backstage/system/service/impl/course/OshCourseEsServiceImpl.java`
- `backstage-system/src/main/java/com/backstage/system/mapper/tool/OshToolEsMapper.java`

因此首页会员套餐模块应对齐这套实现，不单独引入 Flink 宽表消费链路。

## 3. 数据来源设计

本次方案不新增业务字段，直接复用现有会员表。

### 3.1 套餐主表

表名：

- `osh_member_plan`

现有核心字段：

- `id`
- `plan_code`
- `plan_name`
- `member_type`
- `period_type`
- `duration_months`
- `price`
- `original_price`
- `description`
- `min_purchase_quantity`
- `max_purchase_quantity`
- `growth_coefficient`
- `cap_plan_code`
- `cap_ratio`
- `sort`
- `status`

对应实体：

- `backstage-system/src/main/java/com/backstage/system/domain/member/OshMemberPlan.java`

### 3.2 权益表

表名：

- `osh_member_benefit`

现有核心字段：

- `id`
- `plan_id`
- `benefit_title`
- `benefit_description`
- `icon`
- `sort`
- `status`

对应实体：

- `backstage-system/src/main/java/com/backstage/system/domain/member/OshMemberBenefit.java`

### 3.3 本次明确不新增的字段

本次首页套餐方案先不新增以下首页专属字段：

- `recommended`
- `badgeText`
- `buttonText`
- `homepageVisible`
- `homepageSort`
- `homepagePriceOverride`

原因：

- 用户要求字段尽量不要新增
- 当前首页套餐展示可先直接复用会员套餐已有字段
- 先把“首页独立接口 + ES 查询链路”打通，再考虑首页专属运营字段扩展

## 4. 首页接口设计

首页套餐不再复用会员中心接口，单独提供首页查询接口。

### 4.1 首页查询接口

接口：

- `GET /pc/homepage/member/plans`

返回：

- `R<List<OshMemberPlan>>`

说明：

- 返回结构尽量复用 `OshMemberPlan`
- `benefits` 继续挂在 `OshMemberPlan` 下
- 前端接入成本最小
- 不额外定义过多首页专用字段

### 4.2 查询规则

首页查询 ES 时，建议统一按以下规则返回：

- 只返回 `status = 1` 的套餐
- 排序规则为 `sort asc, id asc`
- 每个套餐只返回启用状态的权益
- 权益排序规则为 `sort asc, id asc`
- 每个套餐最多返回前 3 条权益

### 4.3 与会员中心接口的边界

会员中心接口：

- 继续查 MySQL
- 继续承担购买、结算、后台配置等功能

首页套餐接口：

- 只负责首页展示
- 只查 ES
- 不承担下单、支付、价格计算规则修改等会员域职责

## 5. ES 索引设计

### 5.1 索引名

建议索引名：

- `homepage_member_plan`

本次不强制上 alias，先按单索引实现即可。后续若要做平滑重建，再增加读写别名。

### 5.2 ES 文档结构

建议新增 ES 文档类：

- `HomepageMemberPlanEsDocument`

建议字段如下，尽量复用现有 `OshMemberPlan` 和 `OshMemberBenefit` 字段：

```json
{
  "id": 1,
  "planCode": "vip_month",
  "planName": "VIP月卡",
  "memberType": "vip",
  "periodType": "month",
  "durationMonths": 1,
  "price": 88.00,
  "originalPrice": 128.00,
  "description": "适合阶段学习",
  "minPurchaseQuantity": 1,
  "maxPurchaseQuantity": 12,
  "growthCoefficient": 1.00,
  "capPlanCode": "vip_year",
  "capRatio": 0.80,
  "sort": 10,
  "status": 1,
  "benefits": [
    {
      "id": 11,
      "planId": 1,
      "benefitTitle": "会员课程可学",
      "benefitDescription": "解锁会员课程",
      "icon": "",
      "sort": 10,
      "status": 1
    }
  ],
  "createTime": "2026-06-19 10:00:00",
  "updateTime": "2026-06-19 12:00:00"
}
```

### 5.3 Mapping 建议

- `id`: `long`
- `planCode`: `keyword`
- `planName`: `text`，补充 `keyword` 子字段
- `memberType`: `keyword`
- `periodType`: `keyword`
- `durationMonths`: `integer`
- `price`: `scaled_float`
- `originalPrice`: `scaled_float`
- `description`: `text`
- `minPurchaseQuantity`: `integer`
- `maxPurchaseQuantity`: `integer`
- `growthCoefficient`: `scaled_float`
- `capPlanCode`: `keyword`
- `capRatio`: `scaled_float`
- `sort`: `integer`
- `status`: `integer`
- `benefits`: `nested`
- `benefits.id`: `long`
- `benefits.planId`: `long`
- `benefits.benefitTitle`: `text`，补充 `keyword` 子字段
- `benefits.benefitDescription`: `text`
- `benefits.icon`: `keyword`
- `benefits.sort`: `integer`
- `benefits.status`: `integer`
- `createTime`: `date`
- `updateTime`: `date`

说明：

- `scaled_float` 统一设置 `scaling_factor = 100`
- 首页当前主要是列表展示，查询条件不复杂，mapping 可以先保持简单

## 6. 包结构与类设计

建议新增以下类，风格对齐课程模块。

### 6.1 Controller

新增：

- `backstage-system/src/main/java/com/backstage/system/controller/homepage/HomepageMemberPlanController.java`

职责：

- 提供首页套餐查询接口 `GET /pc/homepage/member/plans`

建议方法：

- `public R<List<OshMemberPlan>> plans()`

### 6.2 ES Service 接口

新增：

- `backstage-system/src/main/java/com/backstage/system/service/homepage/IHomepageMemberPlanEsService.java`

建议方法：

- `List<OshMemberPlan> listHomepagePlans()`
- `int syncAllPlansToEs()`
- `void upsertPlanById(Long planId)`
- `void deletePlanById(Long planId)`
- `boolean indexExists()`
- `void recreateIndex(String indexDefinitionJson)`

### 6.3 ES Service 实现

新增：

- `backstage-system/src/main/java/com/backstage/system/service/impl/homepage/HomepageMemberPlanEsServiceImpl.java`

职责：

- 首页套餐查询走 ES
- 从 MySQL 查询源数据并组装 ES 文档
- 提供全量同步
- 提供单条 upsert
- 提供单条删除

### 6.4 ES Mapper

新增：

- `backstage-system/src/main/java/com/backstage/system/mapper/homepage/HomepageMemberPlanEsMapper.java`

职责：

- 直接操作 Elasticsearch
- 查询首页套餐文档
- bulk upsert 文档
- 删除单条文档
- 判断索引是否存在
- 重建索引

### 6.5 ES Document

新增：

- `backstage-system/src/main/java/com/backstage/system/domain/homepage/es/HomepageMemberPlanEsDocument.java`

可包含内部静态类：

- `BenefitDocument`

### 6.6 可选的管理接口

如果需要和课程模块一样补同步入口，可以新增一个后台管理 Controller：

- `HomepageMemberPlanEsIndexController`

建议仅提供内部管理能力：

- 全量同步
- 单条同步
- 重建索引

如果当前阶段不需要对外开放，也可以先不做 Controller，只保留 Service 供内部调用。

## 7. 查询实现方案

### 7.1 首页查询流程

首页请求进入：

`GET /pc/homepage/member/plans`

调用链建议如下：

1. `HomepageMemberPlanController.plans()`
2. `IHomepageMemberPlanEsService.listHomepagePlans()`
3. `HomepageMemberPlanEsMapper.searchHomepagePlans()`
4. ES 返回文档列表
5. Service 将文档转成 `OshMemberPlan`
6. 返回前端

### 7.2 ES 查询规则

建议查询条件：

- `status = 1`

建议排序：

- `sort asc`
- `id asc`

建议返回上限：

- 首页当前如果是固定 3 张卡片，可以先不写死 size
- 默认返回全部启用套餐
- 由前端按实际展示决定是否只取前几条

如果后端要强约束首页展示数量，也可以固定：

- `size = 3`

但从扩展性看，建议先不写死。

### 7.3 查询后的轻量转换

Service 层转换建议：

- 将 ES 文档转换为 `OshMemberPlan`
- 将 `benefits` 转换为 `List<OshMemberBenefit>`
- 统一过滤掉停用权益
- 只保留前三条权益

首页查询只做轻量转换，不做复杂业务逻辑。

## 8. 同步实现方案

本次不走 Flink，不走独立消费链路，改为项目内同步。

### 8.1 全量同步

适用场景：

- 首次上线
- ES 数据修复
- 索引重建后重新灌数

实现流程：

1. 分页查询 `osh_member_plan`
2. 只取 `delete_flag = 0`
3. 可按需要决定是否只同步 `status = 1`
4. 按 planId 批量查询 `osh_member_benefit`
5. 将权益组装到套餐对象
6. 转为 `HomepageMemberPlanEsDocument`
7. 调用 `bulkUpsert`

建议保留两种同步策略：

- 同步全部未删除套餐
- 只同步启用套餐

更推荐：

- ES 中只保留未删除套餐
- 首页查询时只查 `status = 1`

这样便于后台调试和数据核对。

### 8.2 单条 upsert

适用场景：

- 后台修改套餐配置后
- 后台修改套餐权益后
- 套餐状态变更后

实现流程：

1. 按 `planId` 查 `osh_member_plan`
2. 如果套餐不存在或已删除，则执行 ES 删除
3. 查询该套餐对应权益
4. 组装 ES 文档
5. 调用 `upsert`

### 8.3 单条删除

适用场景：

- 套餐被逻辑删除
- 套餐不再需要在首页展示

实现流程：

1. 根据 `planId` 删除 ES 文档

### 8.4 权益变更后的同步

因为 ES 文档是以“套餐”为聚合单位，所以权益变更时不能只更新权益子节点，建议统一按套餐重建该文档。

建议逻辑：

- 新增权益时，调用 `upsertPlanById(planId)`
- 修改权益时，调用 `upsertPlanById(planId)`
- 删除权益时，调用 `upsertPlanById(planId)`

## 9. 与会员模块的集成方案

### 9.1 不改会员中心查询接口

以下接口保持原状：

- `GET /pc/user/member/center`
- `GET /pc/user/member/plans`
- `GET /pc/user/member/admin/plans`

它们继续走 MySQL。

### 9.2 在会员配置更新后触发 ES 同步

当前会员配置更新入口：

- `POST /pc/user/member/admin/plan/config`
- `POST /pc/user/member/admin/plan/pricing-rule`

建议在以下场景补充调用：

- 套餐基础信息更新完成后，调用 `upsertPlanById(planId)`
- 权益更新完成后，调用 `upsertPlanById(planId)`
- 套餐状态变更后，调用 `upsertPlanById(planId)` 或 `deletePlanById(planId)`

### 9.3 集成位置建议

优先放在会员服务写库成功后触发：

- `MemberCenterServiceImpl`

原因：

- 会员模块是源数据维护入口
- 写库成功后立即同步 ES，链路最短
- 不需要额外引入消息中间件

### 9.4 事务边界建议

建议做法：

- 先提交 MySQL 事务
- 再做 ES upsert

原因：

- 避免 ES 写失败导致数据库事务长时间占用
- 与课程模块当前风格更一致

注意：

- 如果 ES 同步失败，要记录日志
- 后续可通过全量同步或单条重试修复

## 10. 详细开发清单

### 10.1 新增类

需要新增：

- `HomepageMemberPlanController`
- `IHomepageMemberPlanEsService`
- `HomepageMemberPlanEsServiceImpl`
- `HomepageMemberPlanEsMapper`
- `HomepageMemberPlanEsDocument`

### 10.2 需要复用的现有类

直接复用：

- `OshMemberPlan`
- `OshMemberBenefit`
- `OshMemberPlanMapper`
- `OshMemberBenefitMapper`
- `ElasticsearchConfig`
- `SearchEsProperties`

### 10.3 需要补充的资源文件

建议补充：

- `backstage-system/src/main/resources/es/homepage_member_plan_index.json`

用于定义索引 mapping。

### 10.4 需要改动的现有代码

建议改动：

- `MemberCenterServiceImpl`

改动内容：

- 在套餐保存成功后调用 `upsertPlanById(planId)`
- 在权益保存成功后调用 `upsertPlanById(planId)`
- 如果后续有删除逻辑，则补 `deletePlanById(planId)`

说明：

- 只补充首页套餐 ES 同步逻辑
- 不改变会员中心原有返回结构
- 不改变原有下单和支付流程

## 11. 伪代码设计

### 11.1 首页查询

```java
@GetMapping("/pc/homepage/member/plans")
public R<List<OshMemberPlan>> plans() {
    return R.ok(homepageMemberPlanEsService.listHomepagePlans());
}
```

### 11.2 全量同步

```java
public int syncAllPlansToEs() {
    int pageNum = 1;
    int pageSize = 200;
    int total = 0;
    while (true) {
        List<OshMemberPlan> plans = queryPlansFromMysql(pageNum, pageSize);
        if (plans.isEmpty()) {
            break;
        }
        attachBenefits(plans);
        List<HomepageMemberPlanEsDocument> docs = buildDocuments(plans);
        total += homepageMemberPlanEsMapper.bulkUpsert(docs);
        if (plans.size() < pageSize) {
            break;
        }
        pageNum++;
    }
    return total;
}
```

### 11.3 单条 upsert

```java
public void upsertPlanById(Long planId) {
    OshMemberPlan plan = memberPlanMapper.selectById(planId);
    if (plan == null || isDeleted(plan)) {
        homepageMemberPlanEsMapper.deleteById(planId);
        return;
    }
    List<OshMemberBenefit> benefits = memberBenefitMapper.selectByPlanId(planId);
    plan.setBenefits(filterAndSortBenefits(benefits));
    HomepageMemberPlanEsDocument doc = buildDocument(plan);
    homepageMemberPlanEsMapper.upsert(doc);
}
```

## 12. 验收标准

### 12.1 功能验收

- 首页新增接口 `GET /pc/homepage/member/plans`
- 接口数据来源为 ES，不是 MySQL
- 返回字段不脱离现有 `OshMemberPlan` 结构
- 每个套餐包含权益列表
- 权益按排序升序返回

### 12.2 同步验收

- 支持全量同步套餐到 ES
- 支持单条套餐 upsert
- 支持单条删除
- 修改会员套餐后，首页 ES 数据可同步更新

### 12.3 稳定性验收

- ES 不可用时记录错误日志
- 不影响会员中心原有 MySQL 读写逻辑
- 可通过全量同步任务修复 ES 数据

## 13. 风险与后续优化

### 13.1 当前方案限制

当前方案刻意不新增首页专属字段，因此首页展示能力受限：

- 暂时不支持首页单独推荐标记
- 暂时不支持首页单独按钮文案
- 暂时不支持首页单独价格覆盖
- 暂时不支持首页单独展示开关

### 13.2 后续可扩展方向

如果后续首页运营诉求增强，再考虑新增首页配置表，例如：

- `osh_homepage_member_plan_cfg`

但这不属于本次范围，本次先把 ES 读模型跑通。

### 13.3 未来演进路线

如果后续会员套餐数据规模、变更频率、跨域聚合复杂度明显提升，再评估是否升级为：

- `MySQL -> MQ / outbox -> 消费者 -> ES`

当前阶段不建议直接上 Flink。

## 14. 最终结论

首页会员套餐模块建议采用以下方案落地：

- 会员中心继续查 MySQL
- 首页新增独立接口 `GET /pc/homepage/member/plans`
- 首页接口只查 ES
- 数据源直接复用 `osh_member_plan` 和 `osh_member_benefit`
- 不新增首页专属业务字段
- 实现方式参考课程模块 `EsMapper + EsService + 全量同步 + 单条 upsert`

这套方案和当前项目现有实现风格一致，开发成本可控，落地路径清晰，适合当前阶段直接实施。

# 反馈模块 ES 搜索 Bug 分析与修复记录

## 1. Bug 概述

### 1.1 问题现象

反馈列表搜索功能中，使用 `termQuery` 对 `status`、`categoryId` 等字段进行精确过滤时，**ES 返回 0 条数据，但 MySQL 路径正常返回
**。

### 1.2 影响范围

| 影响项   | 说明                                                      |
|-------|---------------------------------------------------------|
| 受影响接口 | `/feedback/page` 的 ES 搜索分支                              |
| 受影响字段 | `status`、`categoryId`、`userId`、`tagIds`、`id` 等枚举/ID 类字段 |
| 触发条件  | 使用 `termQuery` 进行精确匹配过滤                                 |
| 根治方案  | 重建索引，将字段类型从 `text` 改为纯 `keyword`                        |

---

## 2. 根因分析

### 2.1 ES 核心机制：分析器（Analyzer）

ES 处理字段时有两个独立时机：

```
写入时（Indexing）：原始文本 → 分析器处理 → 存入倒排索引
查询时（Searching）：查询词   → 分析器处理 → 去倒排索引匹配
```

**核心原则**：存和查必须用同一套逻辑（同时走分析器，或同时不走），才能对称匹配。

### 2.2 默认 Standard 分析器的行为

ES 默认的 `standard` 分析器包含 `lowercase` Token Filter，**会将所有词元转成小写**：

```
"CLOSED" → tokenizer → ["CLOSED"] → lowercase → ["closed"]
```

### 2.3 两种查询类型的区别

| 查询类型     | 代表                      | 是否走分析器 | 适用场景          |
|----------|-------------------------|--------|---------------|
| **全文查询** | `match`、`multi_match`   | ✅ 走    | 标题、正文等自然语言搜索  |
| **精确查询** | `term`、`terms`、`filter` | ❌ 不走   | 枚举、ID、分类等精确过滤 |

### 2.4 Bug 发生的完整链路

```
写入阶段：
  "CLOSED" → text 字段走 standard 分析器 → 倒排索引存 "closed"

查询阶段：
  termQuery("status", "CLOSED")
    → termQuery 不走分析器，直接拿 "CLOSED" 去 text 倒排索引找
    → 索引里只有 "closed"，没有 "CLOSED"
    → 命中 0 条 ❌
```

**迷惑性**：`_source` 显示 `"CLOSED"` 是正确的，因为它是原始备份，不代表倒排索引里存的是什么。

---

## 3. 解决方案对比

| 方案         | 实现方式                    | 结果           | 状态      |
|------------|-------------------------|--------------|---------|
| **Bug 状态** | `text` 字段 + `termQuery` | ❌ 查不到        | 已修复     |
| **历史临时方案** | `.keyword` 子字段          | ✅ 能查到，但有额外开销 | 已废弃     |
| **根治方案**   | 纯 `keyword` 类型          | ✅ 能查到，无多余开销  | **已执行** |

---

## 4. 字段类型选型原则

```
字段类型    适合场景                查询方式              分析器
─────────────────────────────────────────────────────────────────────
text        自然语言全文搜索        match/multi_match     走（分词+lowercase）
            （标题、正文、描述）
keyword     枚举/ID/精确过滤        term/filter           不走（原始值）
            （状态、分类、用户ID）
```

**判断依据**：这个字段会不会被用来做全文模糊搜索？

- 会 → `text`，配合 `match`
- 不会，只做精确过滤 → `keyword`，配合 `term`

---

## 5. 修复详情

### 5.1 已执行的根治方案

重建索引，将以下字段从 `text + keyword` 改为纯 `keyword`：

| 字段名            | 目标类型    | 说明       |
|----------------|---------|----------|
| `id`           | keyword | 主键 ID    |
| `userId`       | keyword | 用户 ID    |
| `categoryId`   | keyword | 分类 ID    |
| `categoryCode` | keyword | 分类编码     |
| `ticketNo`     | keyword | 工单编号     |
| `status`       | keyword | 状态枚举     |
| `tagIds`       | keyword | 标签 ID 数组 |

**保持 `text` 类型的字段**（需要全文搜索）：

| 字段名       | 类型   | 分析器配置                         |
|-----------|------|-------------------------------|
| `title`   | text | ik_max_word（索引）/ ik_smart（搜索） |
| `content` | text | ik_max_word（索引）/ ik_smart（搜索） |

---

## 6. 查资料指南

遇到类似问题，参考以下资源：

| 主题               | 参考链接/关键词                                                                                  |
|------------------|-------------------------------------------------------------------------------------------|
| term query 官方文档  | https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-term-query.html |
| keyword datatype | https://www.elastic.co/guide/en/elasticsearch/reference/current/keyword.html              |
| 调试分词结果           | `GET /_analyze` API                                                                       |
| 核心概念             | `analyzer`, `token filter`, `lowercase`, `match vs term`                                  |

---

## 7. 经验教训

1. **枚举和 ID 字段坚决不用 Text**：不要偷懒依赖 ES 动态推断的 `text + keyword`。浪费资源且极其容易踩坑。
2. **理解分析器的双刃剑**：`lowercase` 让全文搜索更友好，但会让精确匹配失效。`term` 查询不去分词，必须与不分词的 `keyword`
   字段类型配合使用，保证存与查的对称性。
3. **`_source` 具有极大的欺骗性**：看到查询结果里显示 `"CLOSED"`，不代表倒排索引里存的就是 `"CLOSED"`。排查此类问题时，必须用
   `_analyze` API 确认倒排索引中到底生成了什么 Token。
4. **Mapping 即契约**：ES 的 Mapping 是底层查询行为的契约。必须在功能开发初期用 `dynamic: false` 锁死结构并明确定义每一个字段的业务类型。

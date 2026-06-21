# Baseline Spec: OSH Backend

## CURRENT Requirements

### Requirement: 后端保持主网站核心接口兼容

后端 SHALL 保持当前 `/pc/**` 和已有后台管理接口的路径兼容，除非 OpenSpec change 明确说明迁移策略。

#### Scenario: 前端旧页面调用已有接口

- GIVEN 前端仍调用已有 `/api/**` 代理路径
- WHEN Nuxt 代理把请求转发到后端 `/pc/**`
- THEN 后端 SHALL 返回兼容的业务响应结构

### Requirement: 鉴权和权限变更必须显式评审

任何登录、Token、角色、课程权益、小班权益、创始人权益、VIP 权限相关变更 SHALL 建立 OpenSpec change。

#### Scenario: 修改课程观看权限

- GIVEN 用户已经购买课程或具备权益
- WHEN 后端调整课程访问判断
- THEN change SHALL 写明影响角色、兼容性、回归用例和失败回滚策略

### Requirement: 文件上传和对象存储变更必须可回滚

任何 R2/S3、桶名、上传大小、上传路径、老文件删除策略变更 SHALL 写明配置来源、生产差异和回滚方式。

#### Scenario: 替换生产存储桶

- GIVEN 生产环境切换到新 bucket
- WHEN 新代码发布
- THEN 老文件访问、上传新文件、删除旧文件都 SHALL 有验证步骤

### Requirement: 中间件升级必须先做兼容性矩阵

升级 Redis、Kafka、Elasticsearch、Nacos、Spring Boot、JDK 等基础组件 SHALL 先完成兼容性矩阵和灰度方案。

#### Scenario: 升级 Elasticsearch

- GIVEN 当前业务依赖 ES 查询和同步
- WHEN 计划升级 ES 版本
- THEN design SHALL 说明客户端兼容、索引迁移、查询语法差异和回滚方式

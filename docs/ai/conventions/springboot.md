# Spring Boot 约定

## Controller

- 使用清晰路径。
- 参数校验前置。
- 不写复杂业务。
- 权限注解和登录要求明确。

## Service

- 事务放 Service。
- 外部调用和数据库事务边界要谨慎，不要长事务包住远程调用。
- 幂等逻辑写清楚。

## Configuration

- 配置项必须有默认值或环境说明。
- 生产差异写进文档。
- 不在配置类里写业务规则。

## Upgrade

升级 Spring Boot 必须检查：

- Spring Security 行为变化。
- Servlet/Tomcat 版本。
- 配置属性改名。
- 依赖冲突。
- Actuator/健康检查。

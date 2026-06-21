# Code Review

代码审核以 `docs/ai/review-rubric.md` 为准。

Reviewer 必须检查：

- OpenSpec change 是否存在或是否有合理豁免。
- 实现是否满足 spec。
- 是否破坏接口兼容。
- 是否影响权限、安全、数据、中间件。
- 是否有验证和回滚。
- 是否存在无关重构。

没有 OpenSpec 且没有合理豁免的 PR 不应合并。

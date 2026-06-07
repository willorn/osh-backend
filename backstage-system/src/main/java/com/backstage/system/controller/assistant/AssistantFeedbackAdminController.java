package com.backstage.system.controller.assistant;

import com.backstage.common.constant.HttpStatus;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.assistant.dto.AssistantFeedbackPageDTO;
import com.backstage.system.domain.assistant.dto.AssistantFeedbackTagCreateDTO;
import com.backstage.system.domain.assistant.dto.AssistantTicketStatusUpdateDTO;
import com.backstage.system.domain.assistant.dto.UpdateRemarkDTO;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackTagVO;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackVO;
import com.backstage.system.service.assistant.*;
import com.backstage.system.utils.UserContextUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * AI 助手反馈管理接口 Controller（需要管理员权限）
 *
 * @author backstage
 */
@Api(tags = "AI助手反馈-管理接口")
@RestController
@RequestMapping("/pc/admin/feedback")
public class AssistantFeedbackAdminController extends BaseController {

    public AssistantFeedbackAdminController(IAssistantFeedbackService feedbackService,
                                            IAssistantFeedbackCategoryService categoryService,
                                            IAssistantFeedbackTagService feedbackTagService,
                                            IAssistantFeedbackProcessRecordService processRecordService,
                                            IAssistantFeedbackEsService feedbackEsService) {
        this.feedbackService = feedbackService;
        this.categoryService = categoryService;
        this.feedbackTagService = feedbackTagService;
        this.processRecordService = processRecordService;
        this.feedbackEsService = feedbackEsService;
    }

    private final IAssistantFeedbackService feedbackService;
    private final IAssistantFeedbackCategoryService categoryService;
    private final IAssistantFeedbackTagService feedbackTagService;
    private final IAssistantFeedbackProcessRecordService processRecordService;
    private final IAssistantFeedbackEsService feedbackEsService;

    /**
     * 创建反馈标签
     */
    @ApiOperation("创建反馈标签")
    @PreAuthorize("hasAuthority('system:feedback:manage')")
    @PostMapping("/tag/create")
    public R<AssistantFeedbackTagVO> createTag(@Validated @RequestBody AssistantFeedbackTagCreateDTO dto) {
        ensureAdmin();
        AssistantFeedbackTagVO tag = feedbackTagService.createTag(dto, getCurrentUserId());
        return R.ok(tag, "标签创建成功");
    }

    /**
     * 反馈管理列表（分页）
     */
    @ApiOperation("反馈管理列表")
    @PreAuthorize("hasAuthority('system:feedback:manage')")
    @PostMapping("/page")
    public TableDataInfo pageFeedback(@RequestBody AssistantFeedbackPageDTO dto) {
        ensureAdmin();
        return feedbackService.pageFeedback(dto);
    }

    /**
     * 更新反馈状态
     */
    @ApiOperation("更新反馈状态")
    @PreAuthorize("hasAuthority('system:feedback:manage')")
    @PostMapping("/{id}/status")
    public R<AssistantFeedbackVO> updateStatus(@PathVariable("id") Long feedbackId,
                                                 @Validated @RequestBody AssistantTicketStatusUpdateDTO dto) {
        ensureAdmin();
        Long handlerId = getCurrentUserId();
        AssistantFeedbackVO feedback = feedbackService.updateTicketStatus(feedbackId, handlerId, dto);
        return R.ok(feedback, "状态更新成功");
    }

    /**
     * 修改处理记录备注
     */
    @ApiOperation("修改处理记录备注")
    @PreAuthorize("hasAuthority('system:feedback:manage')")
    @PutMapping("/process-record/{recordId}/remark")
    public R<String> updateProcessRecordRemark(@PathVariable("recordId") Long recordId,
                                               @Validated @RequestBody UpdateRemarkDTO dto) {
        ensureAdmin();
        Long operatorId = getCurrentUserId();
        processRecordService.updateRemark(recordId, dto.getRemark(), operatorId);
        return R.ok("备注修改成功");
    }

    /**
     * 全量同步反馈数据到 Elasticsearch
     * <p>
     * 使用场景：
     * 1. <b>ES 初始化</b> - 新环境部署或 ES 索引重建时，将历史数据全量同步到 ES
     * 2. <b>数据修复</b> - 当 ES 数据与 MySQL 不一致时，手动触发同步修复
     * 3. <b>灾难恢复</b> - ES 数据丢失或损坏后的数据恢复
     * </p>
     * <p>
     * 注意：
     * - 此接口会<b>先清空 ES 索引</b>，再全量从 MySQL 同步，执行期间 ES 查询可能不完整
     * - 日常数据同步通过实时写入机制（创建/更新反馈时自动同步），无需调用此接口
     * - 建议在低峰期执行，避免影响线上搜索服务
     * </p>
     *
     * @return 同步的数据条数
     */
    @ApiOperation("全量同步反馈到ES（初始化/修复专用）")
    @PreAuthorize("hasAuthority('system:feedback:manage')")
    @PostMapping("/esSync/all")
    public R<Integer> syncAllFeedbackToEs() {
        ensureAdmin();
        return R.ok(feedbackEsService.syncAllFeedbacksToEs(), "ok");
    }

    /**
     * 重建 ES 索引（删除旧索引 → 创建新索引 → 全量同步数据）
     * <p>
     * 适用场景：
     * 1. mapping 变更（如字段类型从 text 改为 keyword）
     * 2. 分片数、副本数等 settings 调整
     * 3. 分析器配置变更
     * </p>
     * 执行期间 ES 查询会降级到 MySQL，业务不中断。
     *
     * @return 同步的数据条数
     */
    @ApiOperation("重建ES索引（mapping变更/灾难恢复专用）")
    @PreAuthorize("hasAuthority('system:feedback:manage')")
    @PostMapping("/esIndex/rebuild")
    public R<Integer> rebuildEsIndex() {
        ensureAdmin();
        return R.ok(feedbackEsService.rebuildIndex(), "索引重建完成");
    }

    /**
     * 获取当前用户 ID
     */
    private Long getCurrentUserId() {
        try {
            return UserContextUtil.getCurrentUserId();
        } catch (Exception e) {
            throw new ServiceException("获取用户信息失败", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * 确保当前用户是管理员（level >= 4）。
     * 当前阶段保留等级校验作为兜底，避免仅凭新权限点放开历史管理员边界。
     */
    private void ensureAdmin() {
        try {
            Integer level = UserContextUtil.getCurrentLevel();
            if (level == null || level < 4) {
                throw new ServiceException("无权限操作", HttpStatus.FORBIDDEN);
            }
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception e) {
            throw new ServiceException("权限验证失败", HttpStatus.FORBIDDEN);
        }
    }
}

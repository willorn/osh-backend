package com.backstage.system.controller.seckill;

import com.backstage.common.annotation.Log;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.enums.BusinessType;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.dto.seckill.SeckillActivityAddDTO;
import com.backstage.system.domain.dto.seckill.SeckillActivityStatusDTO;
import com.backstage.system.domain.dto.seckill.SeckillActivityUpdateDTO;
import com.backstage.system.domain.seckill.OshSeckillActivity;
import com.backstage.system.domain.seckill.OshSeckillOrder;
import com.backstage.system.domain.vo.seckill.SeckillActivityVO;
import com.backstage.system.domain.vo.seckill.SeckillOrderAdminVO;
import com.backstage.system.service.seckill.IOshSeckillActivityService;
import com.backstage.system.service.seckill.IOshSeckillOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 秒杀活动 Controller
 *
 * @author backstage
 * @date 2026-04-28
 */
@RestController
@RequestMapping("/pc/seckill/activity")
public class OshSeckillActivityController extends BaseController {

    @Autowired
    private IOshSeckillActivityService activityService;

    @Autowired
    private IOshSeckillOrderService orderService;

    /**
     * 接口7：查询秒杀订单列表（管理端）
     */
    @PreAuthorize("hasAuthority('seckill:order:list')")
    @GetMapping("/order/list")
    public TableDataInfo orderList(OshSeckillOrder order) {
        startPage();
        List<SeckillOrderAdminVO> list = orderService.selectOrderList(order);
        return getDataTable(list);
    }

    /**
     * 接口5：查询活动列表
     */
    @PreAuthorize("hasAuthority('seckill:activity:list')")
    @GetMapping("/list")
    public TableDataInfo list(OshSeckillActivity activity) {
        startPage();
        List<SeckillActivityVO> list = activityService.selectActivityList(activity);
        return getDataTable(list);
    }

    /**
     * 接口6：查询活动详情
     */
    @PreAuthorize("hasAuthority('seckill:activity:query')")
    @GetMapping("/detail/{id}")
    public R<SeckillActivityVO> getInfo(@PathVariable Long id) {
        SeckillActivityVO vo = activityService.selectActivityById(id);
        return vo != null ? R.ok(vo) : R.fail("秒杀活动不存在");
    }

    /**
     * 接口1：创建秒杀活动
     */
    @PreAuthorize("hasAuthority('seckill:activity:add')")
    @Log(title = "秒杀活动", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public R<String> add(@Validated @RequestBody SeckillActivityAddDTO dto) {
        try {
            activityService.insertActivity(dto);
            return R.ok();
        } catch (ServiceException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 接口3：修改秒杀活动（仅草稿状态可修改）
     */
    @PreAuthorize("hasAuthority('seckill:activity:edit')")
    @Log(title = "秒杀活动", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<String> edit(@Validated @RequestBody SeckillActivityUpdateDTO dto) {
        try {
            activityService.updateActivity(dto);
            return R.ok();
        } catch (ServiceException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 接口2：发布 / 下架活动
     */
    @PreAuthorize("hasAuthority('seckill:activity:status')")
    @Log(title = "秒杀活动", businessType = BusinessType.UPDATE)
    @PostMapping("/status")
    public R<String> updateStatus(@Validated @RequestBody SeckillActivityStatusDTO dto) {
        try {
            activityService.updateActivityStatus(dto);
            return R.ok();
        } catch (ServiceException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 接口4：批量逻辑删除活动
     */
    @PreAuthorize("hasAuthority('seckill:activity:remove')")
    @Log(title = "秒杀活动", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch")
    public R<String> batchDelete(@RequestParam List<Long> ids) {
        try {
            int result = activityService.deleteActivityByIds(ids);
            return R.ok("成功删除 " + result + " 个活动");
        } catch (ServiceException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            return R.fail("删除失败");
        }
    }
}

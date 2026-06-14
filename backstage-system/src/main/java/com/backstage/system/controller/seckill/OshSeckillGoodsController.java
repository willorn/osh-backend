package com.backstage.system.controller.seckill;

import com.backstage.common.annotation.Log;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.enums.BusinessType;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.dto.seckill.SeckillGoodsAddDTO;
import com.backstage.system.domain.dto.seckill.SeckillGoodsStatusDTO;
import com.backstage.system.domain.dto.seckill.SeckillGoodsUpdateDTO;
import com.backstage.system.domain.seckill.OshSeckillGoods;
import com.backstage.system.domain.vo.seckill.SeckillGoodsPreviewVO;
import com.backstage.system.domain.vo.seckill.SeckillGoodsVO;
import com.backstage.system.service.seckill.IOshSeckillGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 秒杀商品池 Controller
 *
 * @author backstage
 * @date 2026-04-28
 */
@RestController
@RequestMapping("/pc/seckill/goods")
public class OshSeckillGoodsController extends BaseController {

    @Autowired
    private IOshSeckillGoodsService seckillGoodsService;

    /**
     * 预览商品信息（根据 goodsType + goodsId 查对应表，供前端表单自动回填）
     * GET /pc/seckill/goods/preview?goodsType=1&goodsId=101
     */
    @PreAuthorize("hasAuthority('seckill:goods:add')")
    @GetMapping("/preview")
    public R<SeckillGoodsPreviewVO> preview(@RequestParam Integer goodsType,
                                            @RequestParam Long goodsId) {
        try {
            SeckillGoodsPreviewVO vo = seckillGoodsService.previewGoods(goodsType, goodsId);
            return R.ok(vo);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 查询秒杀商品池列表
     */
    @PreAuthorize("hasAuthority('seckill:goods:list')")
    @GetMapping("/list")
    public TableDataInfo list(OshSeckillGoods goods) {
        startPage();
        List<SeckillGoodsVO> list = seckillGoodsService.selectSeckillGoodsList(goods);
        return getDataTable(list);
    }

    /**
     * 查询秒杀商品详情
     */
    @PreAuthorize("hasAuthority('seckill:goods:query')")
    @GetMapping("/detail/{id}")
    public R<SeckillGoodsVO> getInfo(@PathVariable Long id) {
        SeckillGoodsVO vo = seckillGoodsService.selectSeckillGoodsById(id);
        return vo != null ? R.ok(vo) : R.fail("秒杀商品不存在");
    }

    /**
     * 添加商品到秒杀商品池
     */
    @PreAuthorize("hasAuthority('seckill:goods:add')")
    @Log(title = "秒杀商品", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public R<String> add(@Validated @RequestBody SeckillGoodsAddDTO dto) {
        seckillGoodsService.insertSeckillGoods(dto);
        return R.ok();
    }

    /**
     * 批量上架 / 下架秒杀商品
     */
    @PreAuthorize("hasAuthority('seckill:goods:status')")
    @Log(title = "秒杀商品", businessType = BusinessType.UPDATE)
    @PostMapping("/status")
    public R<String> updateStatus(@Validated @RequestBody SeckillGoodsStatusDTO dto) {
        seckillGoodsService.updateSeckillGoodsStatus(dto.getIds(), dto.getStatus());
        return R.ok();
    }

    /**
     * 修改秒杀商品信息
     */
    @PreAuthorize("hasAuthority('seckill:goods:edit')")
    @Log(title = "秒杀商品", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<String> edit(@Validated @RequestBody SeckillGoodsUpdateDTO dto) {
        seckillGoodsService.updateSeckillGoods(dto);
        return R.ok();
    }

    /**
     * 批量逻辑删除秒杀商品
     */
    @PreAuthorize("hasAuthority('seckill:goods:remove')")
    @Log(title = "秒杀商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch")
    public R<String> batchDelete(@RequestParam List<Long> ids) {
        try {
            int result = seckillGoodsService.deleteSeckillGoodsByIds(ids.toArray(new Long[0]));
            return R.ok("成功删除 " + result + " 个商品");
        } catch (ServiceException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            return R.fail("删除失败");
        }
    }
}

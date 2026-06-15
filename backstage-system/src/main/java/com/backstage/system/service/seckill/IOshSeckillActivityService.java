package com.backstage.system.service.seckill;

import com.backstage.system.domain.dto.seckill.SeckillActivityAddDTO;
import com.backstage.system.domain.dto.seckill.SeckillActivityStatusDTO;
import com.backstage.system.domain.dto.seckill.SeckillActivityUpdateDTO;
import com.backstage.system.domain.seckill.OshSeckillActivity;
import com.backstage.system.domain.vo.seckill.SeckillActivityUserVO;
import com.backstage.system.domain.vo.seckill.SeckillActivityVO;

import java.util.List;

/**
 * 秒杀活动 Service 接口
 *
 * @author backstage
 * @date 2026-04-28
 */
public interface IOshSeckillActivityService {

    /** 根据ID查询活动（含商品明细列表） */
    SeckillActivityVO selectActivityById(Long id);

    /** 查询活动列表（含商品明细列表） */
    List<SeckillActivityVO> selectActivityList(OshSeckillActivity activity);

    /** 用户端：按当前时间窗口查询活动列表（含商品明细，支持按商品名称/类型/标签筛选） */
    List<SeckillActivityUserVO> selectActiveActivityList(String title, Integer goodsType, List<String> tagNameList);

    /** 用户端：按当前时间窗口查询活动详情（含商品明细） */
    SeckillActivityUserVO selectActiveActivityById(Long id);

    /** 创建秒杀活动（含商品明细） */
    int insertActivity(SeckillActivityAddDTO dto);

    /** 修改秒杀活动（仅草稿状态可修改，含明细全量替换） */
    int updateActivity(SeckillActivityUpdateDTO dto);

    /** 批量发布/下架活动 */
    int updateActivityStatus(SeckillActivityStatusDTO dto);

    /** 批量逻辑删除活动（同时删除明细） */
    int deleteActivityByIds(List<Long> ids);
}

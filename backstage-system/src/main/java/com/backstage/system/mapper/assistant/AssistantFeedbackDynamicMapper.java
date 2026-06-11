package com.backstage.system.mapper.assistant;

import com.backstage.system.domain.assistant.vo.FeedbackDynamicVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 反馈互动动态 Mapper 接口
 * <p>
 * 用于查询反馈相关的实时互动事件（点赞、收藏等），
 * 使用 XML mapper 定义复杂 SQL 查询。
 *
 * @author backstage
 */
public interface AssistantFeedbackDynamicMapper {

    /**
     * 查询最近互动动态（UNION ALL 合并点赞+收藏）
     * <p>
     * 在数据库层面完成合并、排序、截断，性能最优。
     * 使用 MyBatis 自动映射到 VO 对象，无需手动转换。
     *
     * @param likeLimit     点赞记录查询数量
     * @param favoriteLimit 收藏记录查询数量
     * @param finalLimit    最终返回数量
     * @return 动态 VO 列表（MyBatis 自动映射）
     */
    List<FeedbackDynamicVO> selectRecentDynamics(
            @Param("likeLimit") Integer likeLimit,
            @Param("favoriteLimit") Integer favoriteLimit,
            @Param("finalLimit") Integer finalLimit
    );
}

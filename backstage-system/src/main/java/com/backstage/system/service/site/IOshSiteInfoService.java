package com.backstage.system.service.site;

import com.backstage.system.domain.site.OshSiteInfo;
import com.backstage.system.domain.site.OshSiteMaintainer;
import com.backstage.system.domain.site.OshSiteResourceRelation;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.service.site.impl.DemoSiteConfig;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内部网站信息 Service 接口
 *
 * @author backstage
 */
public interface IOshSiteInfoService extends IService<OshSiteInfo> {

    /**
     * 新增网站使用记录
     *
     * @param siteInfo 网站信息
     * @return 结果
     */
    int insertUsage(OshSiteInfo siteInfo, OshUser oshUser);

    boolean saveSiteInfo(OshSiteInfo siteInfo);

    boolean updateSiteInfo(OshSiteInfo siteInfo);

    void setRelatedResources(Collection<OshSiteInfo> oshSiteInfos);

    List<OshSiteInfo> listSites(OshSiteInfo siteInfo);

    MultiValueMap<Long, OshSiteMaintainer> getSiteMaintainers(Collection<Long> siteIds);

    boolean testConnection(List<OshSiteInfo> oshSiteInfos);

    boolean testConnection(List<OshSiteInfo> oshSiteInfos, int timeout);

    List<OshSiteInfo> checkConnectionStatus(List<OshSiteInfo> oshSiteInfos, int timeout);

    /**
     * 异步启动演示站点（供外部轮询）。
     * 首次调用提交后台启动任务；后续调用返回当前任务状态。
     *
     * @param siteId 站点ID
     * @return 当前任务状态（含 status, statusName, message, checkCount 等字段）
     */
    Map<String, Object> startDemoAsync(Long siteId);

    // ==================== 演示站点公共方法 ====================

    /**
     * 加载并校验演示站点配置。
     * 校验：站点存在、类型为 demo、配置非空、SSH 连接参数完整、登录凭证完整。
     *
     * @param siteId 站点ID
     * @return 解析后的演示站点配置
     * @throws IllegalArgumentException 校验失败时抛出
     */
    DemoSiteConfig loadDemoSiteConfig(Long siteId);

    /**
     * 校验必需的配置字段不为空。
     *
     * @param fieldName 字段中文名（用于错误提示）
     * @param fieldValue 字段值
     * @throws IllegalArgumentException 字段为空时抛出
     */
    void requireConfigField(String fieldName, String fieldValue);

    /**
     * 同步启动演示站点（阻塞等待健康检查）。
     *
     * @param config 演示站点配置
     * @return 启动结果（started, frontendUrl, loginUsername, healthCheckOutput 等）
     */
    Map<String, Object> startDemo(DemoSiteConfig config);

    /**
     * 检查演示站点服务状态。
     *
     * @param config 演示站点配置
     * @return 检查结果（healthy, exitCode, output, frontendUrl 等）
     */
    Map<String, Object> checkDemo(DemoSiteConfig config);

    /**
     * 停止演示站点服务。
     *
     * @param config 演示站点配置
     * @return 停止结果（stopped, exitCode, output）
     */
    Map<String, Object> stopDemo(DemoSiteConfig config);

    // ==================== 网站资源关联管理 ====================

    /**
     * 查询网站的资源关联列表
     *
     * @param siteId 网站ID
     * @return 资源关联列表
     */
    List<OshSiteResourceRelation> listSiteResources(Long siteId);

    /**
     * 新增网站资源关联
     *
     * @param relation 资源关联信息
     * @return 是否成功
     */
    boolean addSiteResource(OshSiteResourceRelation relation);

    /**
     * 批量新增网站资源关联
     *
     * @param relations 资源关联列表
     * @return 是否成功
     */
    boolean batchAddSiteResources(List<OshSiteResourceRelation> relations);

    /**
     * 删除网站资源关联
     *
     * @param id 关联ID
     * @return 是否成功
     */
    boolean removeSiteResource(Long id);

    /**
     * 批量删除网站资源关联
     *
     * @param ids 关联ID列表
     * @return 是否成功
     */
    boolean batchRemoveSiteResources(List<Long> ids);

    /**
     * 删除网站的所有资源关联
     *
     * @param siteId 网站ID
     * @return 是否成功
     */
    boolean removeSiteResourcesBySiteId(Long siteId);
}

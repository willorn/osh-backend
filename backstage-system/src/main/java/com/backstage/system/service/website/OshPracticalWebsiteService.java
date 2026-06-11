package com.backstage.system.service.website;

import com.backstage.common.core.page.TableDataInfo;
import com.backstage.system.domain.dto.website.WebsiteAuditDTO;
import com.backstage.system.domain.dto.website.WebsiteQueryDTO;
import com.backstage.system.domain.dto.website.WebsiteSubmitDTO;
import com.backstage.system.domain.vo.website.OshPracticalWebsiteVO;
import com.backstage.system.domain.vo.website.WebsiteImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 实用网站 Service 接口
 */
public interface OshPracticalWebsiteService {

    /**
     * 分页查询实用网站列表（带筛选条件）
     *
     * @param queryDTO 查询参数
     * @return 实用网站列表
     */
    List<OshPracticalWebsiteVO> selectWebsitePage(WebsiteQueryDTO queryDTO);

    /**
     * 增加网站点击次数
     *
     * @param websiteId 网站 ID
     * @return 结果
     */
    int incrementClickCount(Long websiteId);


    /**
     * 用户提交网站
     *
     * @param submitDto 提交的网站信息
     * @return 影响行数
     */
    int submitWebsite(WebsiteSubmitDTO submitDto);

    /**
     * 管理员审核网站
     *
     * @param auditDto 审核信息
     * @return 是否审核成功
     */
    Boolean auditWebsite(WebsiteAuditDTO auditDto);

    /**
     * 查询待审核的网站列表
     *
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 待审核的网站列表
     */
    TableDataInfo selectAuditList(Integer pageNum, Integer pageSize);

    /**
     * 批量删除网站
     *
     * @param websiteIds 要删除的网站 ID 列表
     * @return 删除的网站数量
     */
    int batchDeleteWebsite(List<Integer> websiteIds);

    OshPracticalWebsiteVO getAuditDetail(Long websiteId);

    /**
     * 更新单个网站的评分
     * @param websiteId 网站主键ID
     */
    void updateWebsiteRatingScore(Long websiteId);

    /**
     * 批量更新所有网站的评分
     */
    void batchUpdateAllWebsiteRatingScores();

    /**
     * 批量导入网站（Excel）
     * <p>
     * 管理员导入：status=4，直接发布；普通用户导入：status=2，进入审核队列
     *
     * @param file     上传的 Excel 文件
     * @param status   入库状态：4-直接发布，2-待审核
     * @param operator 操作人用户名
     * @return 导入结果（成功数、失败数、失败明细）
     */
    WebsiteImportResultVO batchImport(MultipartFile file, int status, String operator);

    /**
     * 生成导入模板并写入响应流
     *
     * @param response HttpServletResponse
     */
    void downloadImportTemplate(javax.servlet.http.HttpServletResponse response);
}

package com.backstage.system.service.impl.info_gap;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.dto.info_gap.InfoGapCreateDTO;
import com.backstage.system.domain.dto.info_gap.InfoGapSearchReqDTO;
import com.backstage.system.domain.dto.info_gap.InfoGapUpdateReqDTO;
import com.backstage.system.domain.info_gap.*;
import com.backstage.system.domain.user.risk.OshUserRiskProfile;
import com.backstage.system.domain.vo.info_gap.InfoGapVO;
import com.backstage.system.enums.behavior.ContributionResourceType;
import com.backstage.system.mapper.info_gap.*;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.behavior.ContributionService;
import com.backstage.system.service.info_gap.InfoGapAnnoService;
import com.backstage.system.service.info_gap.InfoGapService;
import com.backstage.system.service.info_gap.InfoGapUniqueService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InfoGapServiceImpl implements InfoGapService {

    @Autowired
    private OshUserRiskProfileMapper riskMapper;
    @Autowired
    private OshInfoGapVoteMapper voteMapper;
    @Autowired
    private OshInfoGapMapper infoGapMapper;
    @Autowired
    private OshInfoGapFollowMapper followMapper;
    @Autowired
    private OshInfoGapTagRelMapper infoGapTagRelMapper;
    @Autowired
    private OshInfoGapTagMapper infoGapTagMapper;
    @Autowired
    private OshUserMapper oshUserMapper;
    @Autowired
    private InfoGapUniqueService infoGapUniqueService;
    @Autowired
    private InfoGapAnnoService infoGapAnnoService;
    @Autowired
    private ContributionService contributionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleFollow(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new RuntimeException("操作无效：不能关注你自己");
        }
        OshInfoGapFollow existFollow = followMapper.selectFollowRecord(currentUserId, targetUserId);
        if (existFollow != null) {
            followMapper.deleteFollowRecord(existFollow.getId());
        } else {
            OshInfoGapFollow newFollow = new OshInfoGapFollow();
            newFollow.setUserId(currentUserId);
            newFollow.setTargetUserId(targetUserId);
            followMapper.insertFollowRecord(newFollow);
        }
    }

    @Override
    public List<InfoGapVO> getInfoGapList(Integer pageNum, Integer pageSize, String type, Long currentUserId) {
        if (type != null && type.equals("follow")) {
            // 我收藏的信息差列表
            PageHelper.startPage(pageNum, pageSize);
            return infoGapMapper.selectInfoGapPageForFollow(currentUserId);
        } else if (type != null && type.equals("myself")) {
            // 我发布的信息差列表
            PageHelper.startPage(pageNum, pageSize);
            return infoGapMapper.selectInfoGapPageForMySelf(currentUserId);
        } else {
            // 热门/最新发布信息差列表
            PageHelper.startPage(pageNum, pageSize);
            return infoGapMapper.selectInfoGapPage(type, currentUserId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createInfoGap(InfoGapCreateDTO dto, Long userId, String userName) {
        if (dto.getContent() == null || dto.getContent().length() > 500) {
            throw new RuntimeException("内容不能为空且不能超过500字");
        }

        // 手动查询风控信息
        OshUserRiskProfile risk = riskMapper.selectRiskByUserId(userId);
        if (risk != null && risk.getIsBanned() == 1) {
            throw new RuntimeException("操作受限：您的账号因违规已被封禁");
        }

        // 存储信息差详细
        OshInfoGap entity = new OshInfoGap();
        entity.setUserId(userId);
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setTag(dto.getTag());
        entity.setStatus(2);
        entity.setUserName(userName);

        // 手动保存
        infoGapMapper.insertInfoGap(entity);

        Long infoGapId = entity.getId();
        contributionService.recordContribution(ContributionResourceType.INFO_GAP.getCode(), infoGapId, dto.getTitle());
        // 为当前信息差生成并保存唯一标签记录
        String no = infoGapUniqueService.createUniqueRecord(infoGapId);
        infoGapAnnoService.publishUserNotice(infoGapId, dto.getTitle(), userName, no);
        infoGapAnnoService.publishSystemNotice(infoGapId, dto.getTitle(), no);
        List<Long> tagIds = dto.getTagIds();
        int sort = 1;
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                OshInfoGapTagRel oshInfoGapTagRel = new OshInfoGapTagRel();
                oshInfoGapTagRel.setInfoGapId(infoGapId);
                oshInfoGapTagRel.setGapTagId(tagId);
                oshInfoGapTagRel.setSortNo(sort++);

                infoGapTagRelMapper.insert(oshInfoGapTagRel);

                LambdaUpdateWrapper<OshInfoGapTag> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(OshInfoGapTag::getId, tagId)
                        .setSql("tag_use_count = tag_use_count + 1");

                infoGapTagMapper.update(null, updateWrapper);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void vote(Long userId, Long infoId, Integer type) {
        // 查出该用户对该信息的旧评价记录
        OshInfoGapVote existVote = voteMapper.selectVoteRecord(userId, infoId);

        // 定义当前点击类型的列名
        String currentColumn = getColumnByType(type);

        // 情况1：从未评价 → 新增评价
        if (existVote == null) {
            OshInfoGapVote vote = new OshInfoGapVote();
            vote.setUserId(userId);
            vote.setInfoGapId(infoId);
            vote.setVoteType(type);
            voteMapper.insertVoteRecord(vote);

            if (!type.equals(0)) {
                infoGapMapper.updateCountAtomically(infoId, currentColumn);
            }

            return;
        }

        Integer oldType = existVote.getVoteType();
        if (oldType.equals(type)) {
            // 重复点击同一评价（取消评价）
            voteMapper.cancelVoteRecord(userId, infoId);
            infoGapMapper.decrementCountAtomically(infoId, currentColumn);
            return;
        }

        if (oldType.equals(0)) {
            // 从 0 → 新评价（恢复/重新评价）
            existVote.setVoteType(type);
            voteMapper.updateVoteRecord(existVote);
        } else {
            // 切换评价（如 1 → 2 / 2 → 3）

            // 1. 减去旧类型的计数
            String oldColumn = getColumnByType(oldType);
            infoGapMapper.decrementCountAtomically(infoId, oldColumn);

            // 2. 更新评价记录的类型
            existVote.setVoteType(type);
            voteMapper.updateVoteRecord(existVote);

            // 3. 增加新类型的计数
            infoGapMapper.updateCountAtomically(infoId, currentColumn);
        }
    }

    @Override
    public List<InfoGapVO> recommend() {
        List<InfoGapVO> recommendList = infoGapMapper.selectRecommendList();
        return recommendList;
    }

    // 辅助方法：抽取列名逻辑，避免代码重复
    private String getColumnByType(Integer type) {
        return (type == 1) ? "good_count" : (type == 2 ? "middle_count" : "bad_count");
    }

    @Override
    public void infoGapCollectAdd(Long userId, String username, Long infoGapId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        if (infoGapId == null) {
            throw new ServiceException("信息差ID不能为空");
        }

        // 检查当前用户是否已经收藏了目标信息差


        // 记录收藏关系，统计到 osh_info_gap_follow

        // 计算当前信息差被收藏次数


    }

    @Override
    public void infoGapCollectRemove(Long userId, String username, Long infoGapId) {

    }

    @Override
    public void viewCount(Long infoGapId) {
        LambdaUpdateWrapper<OshInfoGap> updateWrapper = Wrappers.lambdaUpdate(OshInfoGap.class)
                .eq(OshInfoGap::getId, infoGapId)
                .setSql("view_count = view_count + 1");

        infoGapMapper.update(null, updateWrapper);
    }

    @Override
    public List<InfoGapVO> searchInfoGap(InfoGapSearchReqDTO request, Long currentUserId) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        return infoGapMapper.searchInfoGap(request.getKeyword(), request.getTagId(), request.getCategory(), currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInfoGap(InfoGapUpdateReqDTO dto, Long userId) {
        if (dto.getContent() == null || dto.getContent().length() > 500) {
            throw new ServiceException("内容不能为空且不能超过500字");
        }

        // 手动查询风控信息
        OshUserRiskProfile risk = riskMapper.selectRiskByUserId(userId);
        if (risk != null && risk.getIsBanned() == 1) {
            throw new ServiceException("操作受限：您的账号因违规已被封禁");
        }

        OshInfoGap oshInfoGap = infoGapMapper.selectById(dto.getId());
        if (oshInfoGap == null) {
            throw new ServiceException("信息差不存在！");
        }

        LambdaUpdateWrapper<OshInfoGap> updateWrapper = Wrappers.lambdaUpdate(OshInfoGap.class)
                .eq(OshInfoGap::getId, dto.getId())
                .eq(OshInfoGap::getUserId, userId)
                .set(OshInfoGap::getTitle, dto.getTitle())
                .set(OshInfoGap::getContent, dto.getContent())
                .set(OshInfoGap::getTag, dto.getTag())
                .set(OshInfoGap::getStatus, 2);

        int updateRows = infoGapMapper.update(null, updateWrapper);
        if (updateRows <= 0) {
            throw new RuntimeException("修改信息差失败");
        }

        List<OshInfoGapTagRel> oldRelList = infoGapTagRelMapper.selectList(
                Wrappers.lambdaQuery(OshInfoGapTagRel.class)
                        .eq(OshInfoGapTagRel::getInfoGapId, dto.getId())
        );

        Set<Long> oldTagIdSet = oldRelList.stream()
                .map(OshInfoGapTagRel::getGapTagId)
                .collect(Collectors.toSet());

        List<Long> newTagIds = dto.getTagIds() == null
                ? Collections.emptyList()
                : dto.getTagIds();

        List<Long> removedTagIds = oldTagIdSet.stream()
                .filter(tagId -> !newTagIds.contains(tagId))
                .collect(Collectors.toList());

        List<Long> addedTagIds = newTagIds.stream()
                .filter(tagId -> !oldTagIdSet.contains(tagId))
                .collect(Collectors.toList());

        if (!removedTagIds.isEmpty()) {
            LambdaUpdateWrapper<OshInfoGapTagRel> relDeleteWrapper = Wrappers.lambdaUpdate(OshInfoGapTagRel.class)
                    .eq(OshInfoGapTagRel::getInfoGapId, dto.getId())
                    .in(OshInfoGapTagRel::getGapTagId, removedTagIds)
                    .eq(OshInfoGapTagRel::getDeleteFlag, 0)
                    .set(OshInfoGapTagRel::getDeleteFlag, 1);

            infoGapTagRelMapper.update(null, relDeleteWrapper);
        }

        for (int i = 0; i < newTagIds.size(); i++) {
            Long tagId = newTagIds.get(i);
            int sortNo = i + 1;

            if (oldTagIdSet.contains(tagId)) {
                LambdaUpdateWrapper<OshInfoGapTagRel> relUpdateWrapper = Wrappers.lambdaUpdate(OshInfoGapTagRel.class)
                        .eq(OshInfoGapTagRel::getInfoGapId, dto.getId())
                        .eq(OshInfoGapTagRel::getGapTagId, tagId)
                        .eq(OshInfoGapTagRel::getDeleteFlag, 0)
                        .set(OshInfoGapTagRel::getSortNo, sortNo);

                infoGapTagRelMapper.update(null, relUpdateWrapper);
            } else {
                OshInfoGapTagRel rel = new OshInfoGapTagRel();
                rel.setInfoGapId(dto.getId());
                rel.setGapTagId(tagId);
                rel.setSortNo(sortNo);
                infoGapTagRelMapper.insert(rel);
            }
        }

        for (Long tagId : removedTagIds) {
            LambdaUpdateWrapper<OshInfoGapTag> tagUpdateWrapper = Wrappers.lambdaUpdate(OshInfoGapTag.class)
                    .eq(OshInfoGapTag::getId, tagId)
                    .setSql("tag_use_count = GREATEST(tag_use_count - 1, 0)");
            infoGapTagMapper.update(null, tagUpdateWrapper);
        }

        for (Long tagId : addedTagIds) {
            LambdaUpdateWrapper<OshInfoGapTag> tagUpdateWrapper = Wrappers.lambdaUpdate(OshInfoGapTag.class)
                    .eq(OshInfoGapTag::getId, tagId)
                    .setSql("tag_use_count = tag_use_count + 1");
            infoGapTagMapper.update(null, tagUpdateWrapper);
        }

    }

    @Override
    public void deleteInfoGap(Long infoGapId) {
        LambdaQueryWrapper<OshInfoGap> queryWrapper = Wrappers.lambdaQuery(OshInfoGap.class)
                .eq(OshInfoGap::getId, infoGapId)
                .eq(OshInfoGap::getDeleteFlag, 0);
        OshInfoGap currentInfoGap = infoGapMapper.selectOne(queryWrapper);

        if (currentInfoGap == null) {
            throw new ServiceException("信息差不存在！");
        }

        LambdaUpdateWrapper<OshInfoGap> updateWrapper = Wrappers.lambdaUpdate(OshInfoGap.class)
                .eq(OshInfoGap::getId, infoGapId)
                .set(OshInfoGap::getDeleteFlag, 1);
        int rows = infoGapMapper.update(null, updateWrapper);

        if (rows <= 0) {
            throw new ServiceException("信息差删除失败！");
        }
    }

    @Override
    public Integer getHotPageNumByInfoGapId(Long infoGapId, Integer pageSize) {
        if (infoGapId == null) {
            throw new ServiceException("信息差ID不能为空");
        }

        int actualPageSize = (pageSize == null || pageSize <= 0) ? 10 : pageSize;
        Integer rank = infoGapMapper.selectHotRankByInfoGapId(infoGapId);
        if (rank == null || rank <= 0) {
            throw new ServiceException("未找到该信息差对应的热门页码");
        }

        return (rank - 1) / actualPageSize + 1;
    }
}

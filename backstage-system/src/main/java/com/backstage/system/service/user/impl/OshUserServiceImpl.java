package com.backstage.system.service.user.impl;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.enums.ResultCode;
import com.backstage.common.enums.UploadPathEnum;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.common.utils.email.EmailUtil;
import com.backstage.common.utils.generate.GenerateUtil;
import com.backstage.common.utils.jwt.JwtUtil;
import com.backstage.common.utils.SecurityUtils;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.user.*;
import com.backstage.system.domain.user.vo.OshUserLoginVO;
import com.backstage.system.mapper.user.*;
import com.backstage.system.mapper.user.OshUserInvitationMapper;
import com.backstage.system.request.UserListRequest;
import com.backstage.system.service.common.OssService;
import com.backstage.system.service.user.IOshUserService;
import com.backstage.system.utils.OssUtil;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/3/7
 * Time: 16:42
 */
@Service
public class OshUserServiceImpl implements IOshUserService {
    private static final int ASSET_INCOME = 0;
    private static final int ASSET_EXPENSE = 1;
    private static final String DEFAULT_ROLE_NAME = "普通用户";
    private static final String DEFAULT_ROLE_CODE = "user";
    private static final String DEFAULT_ROLE_LEVEL = "1";

    @Autowired
    private OshUserMapper oshUserMapper;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private EmailUtil emailUtil;
    @Autowired
    private OshRoleMapper oshRoleMapper;
    @Autowired
    private OshPermissionMapper oshPermissionMapper;
    @Autowired
    private OshUserAssetMapper oshUserAssetMapper;
    @Autowired
    private OshUserAssetRecordMapper oshUserAssetRecordMapper;
    @Autowired
    private OshUserInvitationMapper oshUserInvitationMapper;
    @Autowired
    private UserManageMapper userManageMapper;

    @Override
    public R<OshUserLoginVO> login(String username, String password) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return R.fail(ResultCode.FAILED_USER_NAME_OR_PASSWORD_EMPTY.getMsg());
        }
        LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getUsername, username)
                .or()
                .eq(OshUser::getEmail, username);
        OshUser oshUser = oshUserMapper.selectOne(wrapper);
        if (oshUser == null) {
            return R.fail(ResultCode.FAILED_USER_NOT_EXISTS.getMsg());
        }
        if (oshUser.getStatus() == 1) {
            return R.fail(ResultCode.FAILED_USER_BANNED.getMsg());
        }
        if (password.length() == 50) {
            String uniqueIdByUserId = oshUserMapper.getUniqueIdByUserId(oshUser.getId());
            if (!uniqueIdByUserId.equals(password)) {
                return R.fail(ResultCode.FAILED_USER_UNIQUEID_ERROR.getMsg());
            }
        }else {
            if (!SecurityUtils.matchesPassword(password, oshUser.getPassword())) {
                return R.fail(ResultCode.FAILED_USER_PASSWORD_ERROR.getMsg());
            }
        }
        String token = createToken(oshUser);
        OshUserLoginVO userLoginVo = new OshUserLoginVO();
        userLoginVo.setToken(token);
        List<Integer> roleIds = oshRoleMapper.getRoleIdsByUserId(oshUser.getId());
        Map<String, String> asset = getAsset(oshUser.getId());
        Map<String, String> role = getRole(roleIds);
        Map<String, List<String>> permissionList = getPermission(roleIds);
        userLoginVo.setAsset(asset);
        userLoginVo.setRole(role);
        userLoginVo.setPermissionList(permissionList);
        Map<String, Object> map = new HashMap<>();
        map.put(OshUserConstants.ASSET, asset);
        map.put(OshUserConstants.ROLE, role);
        map.put(OshUserConstants.PERMISSION, permissionList);

        // 新登录直接覆盖旧会话（踢掉旧设备）
        String loginKey = OshUserConstants.LOGIN_USER + oshUser.getId();
        map.put(OshUserConstants.LOGINCOUNT, 1);
        map.put(OshUserConstants.TOKEN, token); // 存储当前有效 token，用于踢掉旧设备
        redisCache.setCacheObject(loginKey, map, 500, TimeUnit.MINUTES);
        return R.ok(userLoginVo);
    }


    @Override
    public R<String> registerSubmit(String username, String password, String repassword, String email) throws MessagingException {
        return registerSubmit(username, password, repassword, email, null);
    }

    @Override
    public R<String> registerSubmit(String username, String password, String repassword, String email, String inviteCode) throws MessagingException {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return R.fail(ResultCode.FAILED_USER_NAME_OR_PASSWORD_EMPTY.getMsg());
        }
        if (username.length() < OshUserConstants.USERNAME_MIN_LENGTH || username.length() > OshUserConstants.USERNAME_MAX_LENGTH || !username.matches(OshUserConstants.USERNAME_PATTERN)) {
            return R.fail(ResultCode.FAILED_USER_USERNAME_NOT_IN_RANGE.getMsg());
        }
        if (password.length() < OshUserConstants.PASSWORD_MIN_LENGTH || password.length() > OshUserConstants.PASSWORD_MAX_LENGTH || !password.matches(OshUserConstants.PASSWORD_PATTERN)) {
            return R.fail(ResultCode.FAILED_USER_PASSWORD_NOT_MATCHES.getMsg());
        }
        if(!password.equals(repassword)){
            return R.fail(ResultCode.FAILED_USER_PASSWORD_NOT_MATCH.getMsg());
        }
        LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getUsername, username);
        OshUser oshUser = oshUserMapper.selectOne(wrapper);
        if (oshUser != null && oshUser.getUsername().equals(username)) {
            return R.fail(ResultCode.AILED_USER_EXISTS.getMsg());
        }
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getEmail, email);
        oshUser = oshUserMapper.selectOne(wrapper);
        if (oshUser != null && oshUser.getEmail().equals(email)) {
            return R.fail(ResultCode.FAILED_USER_EMAIL_BOUND.getMsg());
        }
        // 校验邀请码（选填，填了就必须有效）
        if (StringUtils.isNotEmpty(inviteCode)) {
            LambdaQueryWrapper<OshUser> inviteWrapper = new LambdaQueryWrapper<>();
            inviteWrapper.eq(OshUser::getInviteCode, inviteCode);
            OshUser inviter = oshUserMapper.selectOne(inviteWrapper);
            if (inviter == null) {
                return R.fail("邀请码无效");
            }
        }
        String uniqueId = emailUtil.sendEmailGetUniqueId(username, email);
        Map<String,String> userMap = new HashMap<>();
        userMap.put(OshUserConstants.USERNAME, username);
        userMap.put(OshUserConstants.PASSWORD, password);
        userMap.put(OshUserConstants.EMAIL, email);
        if (StringUtils.isNotEmpty(inviteCode)) {
            userMap.put(OshUserConstants.INVITE_CODE, inviteCode);
        }
        redisCache.setCacheObject(OshUserConstants.UNIQUE_ID + uniqueId, userMap, 500, TimeUnit.MINUTES);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> registerVerity(String uniqueId) {
        Map<String,String> userMap = redisCache.getCacheObject(OshUserConstants.UNIQUE_ID + uniqueId);
        if(userMap == null) return R.fail("唯一标识错误或已过期");
        Long userId = GenerateUtil.generateSnowflakeId();
        OshUser oshUser = new OshUser();
        oshUser.setId(userId);
        oshUser.setUsername(userMap.get(OshUserConstants.USERNAME));
        oshUser.setPassword(SecurityUtils.encryptPassword(userMap.get(OshUserConstants.PASSWORD)));
        oshUser.setEmail(userMap.get(OshUserConstants.EMAIL));
        oshUser.setDeleteFlag((byte) 0);  // 明确设置，避免拦截器过滤
        // 生成该用户自己的邀请码
        oshUser.setInviteCode(generateUniqueInviteCode());
        ThreadLocalUtil.set(OshUserConstants.USER_ID, userId);
        oshUserMapper.insert(oshUser);
        oshUserMapper.addUniqueId(oshUser.getId(), uniqueId);
        oshUserMapper.addRole(oshUser.getId());
        OshUserAsset oshUserAsset = new OshUserAsset();
        oshUserAsset.setPoints(188L);
        oshUserAsset.setUserId(oshUser.getId());
        oshUserAssetMapper.insert(oshUserAsset);
        // 记录邀请关系
        String inviteCode = userMap.get(OshUserConstants.INVITE_CODE);
        if (StringUtils.isNotEmpty(inviteCode)) {
            LambdaQueryWrapper<OshUser> inviteWrapper = new LambdaQueryWrapper<>();
            inviteWrapper.eq(OshUser::getInviteCode, inviteCode);
            OshUser inviter = oshUserMapper.selectOne(inviteWrapper);
            if (inviter != null) {
                OshUserInvitation invitation = new OshUserInvitation();
                invitation.setInviterId(inviter.getId());
                invitation.setInviteeId(userId);
                invitation.setInviteCode(inviteCode);
                invitation.setCreateTime(LocalDateTime.now());
                oshUserInvitationMapper.insertInvitation(invitation);
            }
        }
        redisCache.deleteObject(OshUserConstants.UNIQUE_ID + uniqueId);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    /**
     * 生成唯一邀请码，冲突时重试
     */
    private String generateUniqueInviteCode() {
        for (int i = 0; i < 10; i++) {
            String code = GenerateUtil.generateInviteCode();
            LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OshUser::getInviteCode, code);
            if (oshUserMapper.selectCount(wrapper) == 0) {
                return code;
            }
        }
        // 极端情况兜底：用雪花ID后8位
        return String.valueOf(GenerateUtil.generateSnowflakeId()).substring(10);
    }

    @Override
    public R<String> logout() {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID,Long.class);
        String key = OshUserConstants.LOGIN_USER + userId;
        if (redisCache.hasKey(key)) {
            redisCache.deleteObject(key);
            return R.ok(ResultCode.SUCCESS.getMsg());
        }
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> changeEmailSubmit(String uniqueId, String newEmail) throws MessagingException {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID,Long.class);
        LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getId, userId);
        OshUser oshUser = oshUserMapper.selectOne(wrapper);
        if (oshUser == null) {
            return R.fail(ResultCode.FAILED_USER_NOT_EXISTS.getMsg());
        }
        String uniqueIdByUserId = oshUserMapper.getUniqueIdByUserId(userId);
        if (!uniqueIdByUserId.equals(uniqueId)) {
            return R.fail(ResultCode.FAILED_USER_UNIQUEID_ERROR.getMsg());
        }
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getEmail, newEmail);
        OshUser emailOshUser = oshUserMapper.selectOne(wrapper);
        if (emailOshUser != null) {
            return R.fail(ResultCode.FAILED_USER_EMAIL_BOUND.getMsg());
        }
        String newUniqueId = emailUtil.sendEmailGetUniqueId(oshUser.getUsername(), newEmail);
        Map<String,String> userMap = new HashMap<>();
        userMap.put(OshUserConstants.USER_ID, userId.toString());
        userMap.put(OshUserConstants.EMAIL, newEmail);
        redisCache.setCacheObject(OshUserConstants.RE_UNIQUE_ID + newUniqueId, userMap, 500, TimeUnit.MINUTES);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> changeEmailVerity(String uniqueId) {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID,Long.class);
        LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getId, userId);
        OshUser oshUser = oshUserMapper.selectOne(wrapper);
        if (oshUser == null) {
            return R.fail(ResultCode.FAILED_USER_NOT_EXISTS.getMsg());
        }
        Map<String,String> userMap = redisCache.getCacheObject(OshUserConstants.RE_UNIQUE_ID + uniqueId);
        if (userMap == null) return R.fail("新的唯一标识错误或已过期");
        if (!userId.equals(Long.parseLong(userMap.get(OshUserConstants.USER_ID)))) return R.fail(ResultCode.FAILED.getMsg());
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getId, userId);
        OshUser user = new OshUser();
        user.setEmail(userMap.get(OshUserConstants.EMAIL));
        oshUserMapper.update(user, wrapper);
        oshUserMapper.updateUniqueIdByUserId(userId, uniqueId);
        redisCache.deleteObject(OshUserConstants.RE_UNIQUE_ID + uniqueId);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> forget(String uniqueId, String password, String repassword) {
        if (password.length() < OshUserConstants.PASSWORD_MIN_LENGTH || password.length() > OshUserConstants.PASSWORD_MAX_LENGTH || !password.matches(OshUserConstants.PASSWORD_PATTERN)) {
            return R.fail(ResultCode.FAILED_USER_PASSWORD_NOT_MATCHES.getMsg());
        }
        Long userId = oshUserMapper.getUserIdByUniqueId(uniqueId);
        LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getId, userId);
        OshUser oshUser = oshUserMapper.selectOne(wrapper);
        if (oshUser == null) {
            return R.fail(ResultCode.FAILED_USER_NOT_EXISTS.getMsg());
        }
        if(!password.equals(repassword)){
            return R.fail(ResultCode.FAILED_USER_PASSWORD_NOT_MATCH.getMsg());
        }
        String uniqueIdByUserId = oshUserMapper.getUniqueIdByUserId(userId);
        if (!uniqueIdByUserId.equals(uniqueId)) {
            return R.fail(ResultCode.FAILED_USER_UNIQUEID_ERROR.getMsg());
        }
        oshUser.setPassword(SecurityUtils.encryptPassword(password));
        oshUserMapper.update(oshUser, wrapper);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> updateInfo(String username, String sex, String introduction) {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID,Long.class);
        if (StringUtils.isEmpty(username)) {
            return R.fail(ResultCode.FAILED_USER_USERNAME_NOT_IN_RANGE.getMsg());
        }
        String trimmedUsername = username.trim();
        if (trimmedUsername.length() < OshUserConstants.USERNAME_MIN_LENGTH
                || trimmedUsername.length() > OshUserConstants.USERNAME_MAX_LENGTH
                || !trimmedUsername.matches(OshUserConstants.USERNAME_PATTERN)) {
            return R.fail(ResultCode.FAILED_USER_USERNAME_NOT_IN_RANGE.getMsg());
        }
        LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getId, userId);
        OshUser oshUser = oshUserMapper.selectOne(wrapper);
        if (oshUser == null) {
            return R.fail(ResultCode.FAILED_USER_NOT_EXISTS.getMsg());
        }
        if (!trimmedUsername.equals(oshUser.getUsername())) {
            LambdaQueryWrapper<OshUser> usernameWrapper = new LambdaQueryWrapper<>();
            usernameWrapper.eq(OshUser::getUsername, trimmedUsername)
                    .ne(OshUser::getId, userId);
            if (oshUserMapper.selectCount(usernameWrapper) > 0) {
                return R.fail(ResultCode.AILED_USER_EXISTS.getMsg());
            }
        }
        oshUser.setUsername(trimmedUsername);
        oshUser.setSex(sex);
        oshUser.setIntroduction(introduction);
        oshUserMapper.update(oshUser, wrapper);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Autowired
    private OssService ossService;

    @Autowired
    private OssUtil ossUtil;

    @Override
    public R<String> uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }
        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return R.fail("文件名不能为空");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!extension.matches("jpg|jpeg|png|gif|webp")) {
            return R.fail("仅支持 jpg/png/gif/webp 格式的图片");
        }
        // 校验文件大小（最大 3MB）
        if (file.getSize() > 3 * 1024 * 1024) {
            return R.fail("头像图片大小不能超过 3MB");
        }
        try {
            Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
            
            // 获取旧头像路径，上传成功后删除旧文件
            LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OshUser::getId, userId);
            OshUser oshUser = oshUserMapper.selectOne(wrapper);
            String oldAvatar = oshUser.getAvatar();
            
            // 上传到 OSS，路径：common/image/avatar/{userId}/
            String filePath = ossService.upload(file, UploadPathEnum.AVATAR, String.valueOf(userId));
            if (filePath == null || filePath.contains("不能超过")) {
                return R.fail(filePath);
            }
            
            // 删除旧头像文件（避免垃圾文件堆积）
            if (StringUtils.isNotEmpty(oldAvatar)) {
                String oldKey = oldAvatar;
                // 兼容旧数据：如果存的是完整URL，提取相对路径
                if (oldKey.startsWith("http")) {
                    String publicDomain = ossUtil.getOssProperties().getPublicDomain();
                    if (oldKey.startsWith(publicDomain)) {
                        oldKey = oldKey.substring(publicDomain.length());
                        if (oldKey.startsWith("/")) {
                            oldKey = oldKey.substring(1);
                        }
                    }
                }
                ossUtil.deleteFile(oldKey);
            }
            
            // 数据库存储相对路径（不再存完整公开URL，因为Bucket未开放公开访问）
            oshUser.setAvatar(filePath);
            oshUserMapper.update(oshUser, wrapper);
            // 返回临时签名URL给前端（有效期30分钟）
            String avatarUrl = ossService.getLimitedUrl(filePath, 30);
            return R.ok(avatarUrl);
        } catch (Exception e) {
            return R.fail("头像上传失败：" + e.getMessage());
        }
    }

    @Override
    public R<String> updatePassword(String opassword, String password, String repassword) {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID,Long.class);
        LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(OshUser::getPassword).eq(OshUser::getId, userId);
        OshUser user = oshUserMapper.selectOne(wrapper);
        if (user == null) {
            return R.fail(ResultCode.FAILED_USER_NOT_EXISTS.getMsg());
        }
        if (user.getPassword() != null && !SecurityUtils.matchesPassword(opassword, user.getPassword())) {
            return R.fail(ResultCode.FAILED_USER_PASSWORD_ERROR.getMsg());
        }
        if(!password.equals(repassword)){
            return R.fail(ResultCode.FAILED_USER_PASSWORD_NOT_MATCH.getMsg());
        }
        if (password.length() < OshUserConstants.PASSWORD_MIN_LENGTH || password.length() > OshUserConstants.PASSWORD_MAX_LENGTH || !password.matches(OshUserConstants.PASSWORD_PATTERN)) {
            return R.fail(ResultCode.FAILED_USER_PASSWORD_NOT_MATCHES.getMsg());
        }
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getId, userId);
        user.setPassword(SecurityUtils.encryptPassword(password));
        oshUserMapper.update(user, wrapper);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<OshUser> getUserInfo() {
        OshUser oshUser = UserContextUtil.getCurrentUser();
        oshUser.setPassword(null);
        // 将头像相对路径转为临时签名URL（有效期30分钟）
        if (StringUtils.isNotEmpty(oshUser.getAvatar())) {
            String avatar = oshUser.getAvatar();
            // 兼容旧数据：如果存的是完整URL（http开头），提取相对路径
            if (avatar.startsWith("http")) {
                // 旧数据存的是完整公开URL，尝试提取相对路径部分
                String basePath = ossUtil.getOssProperties().getBasePath();
                String publicDomain = ossUtil.getOssProperties().getPublicDomain();
                if (avatar.startsWith(publicDomain)) {
                    avatar = avatar.substring(publicDomain.length());
                    if (avatar.startsWith("/")) {
                        avatar = avatar.substring(1);
                    }
                }
            }
            oshUser.setAvatar(ossService.getLimitedUrl(avatar, 30));
        }
        return R.ok(oshUser);
    }

    @Override
    public R<?> getUserRoles() {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        List<Long> userIds = Collections.singletonList(userId);
        List<Map<String, Object>> roles = userManageMapper.selectUserRolesByUserIds(userIds);
        return R.ok(roles);
    }

    @Override
    public R<String> deleteUser() {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        OshUser user = oshUserMapper.selectOne(new LambdaQueryWrapper<OshUser>().eq(OshUser::getId, userId));
        if (user == null) {
            return R.fail(ResultCode.FAILED_USER_NOT_EXISTS.getMsg());
        }
        user.setDeleteFlag((byte) 1);
        oshUserMapper.update(user, new LambdaQueryWrapper<OshUser>().eq(OshUser::getId, userId));
        oshUserMapper.deleteUniqueId(userId);
        oshRoleMapper.deleteUserRole(userId);
        OshUserAsset oshUserAsset = oshUserAssetMapper.selectOne(new LambdaQueryWrapper<OshUserAsset>().eq(OshUserAsset::getUserId, userId));
        if (oshUserAsset != null) {
            oshUserAsset.setDeleteFlag((byte) 1);
            oshUserAssetMapper.update(oshUserAsset, new LambdaQueryWrapper<OshUserAsset>().eq(OshUserAsset::getUserId, userId));
        }
        redisCache.deleteObject(OshUserConstants.LOGIN_USER + userId);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateAsset(Integer changeType, Integer changeSource, Long changeAmount, String remark) {
        Long userId = UserContextUtil.getCurrentUserId();
        Map<String,Object> userMap = redisCache.getCacheObject(OshUserConstants.LOGIN_USER + userId);
        if (userMap == null) {
            return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        }
        if (changeType == null || (changeType != ASSET_INCOME && changeType != ASSET_EXPENSE)) {
            return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        }
        if (changeAmount == null || changeAmount <= 0) {
            return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        }

        OshUserAsset beforeAsset = ensureUserAsset(userId);
        Long beforePoints = Optional.ofNullable(beforeAsset.getPoints()).orElse(0L);
        Long delta = changeType == ASSET_INCOME ? changeAmount : -changeAmount;
        boolean requireEnough = changeType == ASSET_EXPENSE;
        int updated = oshUserAssetMapper.updatePointsAtomic(userId, delta, requireEnough);
        if (updated <= 0) {
            return R.fail(requireEnough ? "积分余额不足" : "资产变更失败");
        }

        OshUserAsset afterAsset = oshUserAssetMapper.selectOne(new LambdaQueryWrapper<OshUserAsset>()
                .eq(OshUserAsset::getUserId, userId)
                .select(OshUserAsset::getPoints));
        Long afterPoints = afterAsset == null || afterAsset.getPoints() == null ? beforePoints + delta : afterAsset.getPoints();

        OshUserAssetRecord oshUserAssetRecord = new OshUserAssetRecord();
        oshUserAssetRecord.setUserId(userId);
        oshUserAssetRecord.setChangeType(changeType);
        oshUserAssetRecord.setChangeSource(changeSource);
        oshUserAssetRecord.setChangeAmount(changeAmount);
        oshUserAssetRecord.setRemark(remark);
        oshUserAssetRecord.setBeforeBalance(beforePoints);
        oshUserAssetRecord.setAfterBalance(afterPoints);
        oshUserAssetRecordMapper.insert(oshUserAssetRecord);

        // 更新redis
        Map<String,String> asset = (Map<String,String>)userMap.get(OshUserConstants.ASSET);
        if (asset == null) {
            asset = new HashMap<>();
        }
        asset.put(OshUserConstants.POINTS, String.valueOf(afterPoints));
        userMap.put(OshUserConstants.ASSET, asset);
        redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }


    public String createToken(OshUser oshUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(OshUserConstants.USER_ID, oshUser.getId());
        claims.put(OshUserConstants.USERNAME, oshUser.getUsername());
        return JwtUtil.createToken(claims);
    }

    private Map<String, String> getAsset(Long id) {
        OshUserAsset asset = ensureUserAsset(id);
        HashMap<String, String> result = new HashMap<>();
        result.put(OshUserConstants.POINTS, String.valueOf(Optional.ofNullable(asset.getPoints()).orElse(0L)));
        return result;
    }

    private OshUserAsset ensureUserAsset(Long userId) {
        OshUserAsset asset = oshUserAssetMapper.selectOne(new LambdaQueryWrapper<OshUserAsset>()
                .eq(OshUserAsset::getUserId, userId));
        if (asset != null) {
            if (asset.getPoints() == null) {
                asset.setPoints(0L);
            }
            return asset;
        }

        OshUserAsset created = new OshUserAsset();
        LocalDateTime now = LocalDateTime.now();
        created.setUserId(userId);
        created.setPoints(0L);
        created.setCreateBy(userId);
        created.setUpdateBy(userId);
        created.setCreateTime(now);
        created.setUpdateTime(now);
        created.setDeleteFlag((byte) 0);
        oshUserAssetMapper.insert(created);
        return created;
    }

    public Map<String,String> getRole(List<Integer> roleId) {
        if (roleId == null || roleId.isEmpty()) {
            return defaultRole();
        }
        LambdaQueryWrapper<OshRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(OshRole::getId, roleId).eq(OshRole::getDeleteFlag, 0)
                .select(OshRole::getRoleName, OshRole::getRoleCode, OshRole::getLevel);
        List<OshRole> oshRoleList = oshRoleMapper.selectList(roleWrapper);
        if (oshRoleList == null || oshRoleList.isEmpty()) {
            return defaultRole();
        }
        OshRole oshRole = oshRoleList.get(0);
        for (OshRole curRole : oshRoleList) {
            if (curRole.getLevel() > oshRole.getLevel()) {
                oshRole = curRole;
            }
        }
        Map<String, String> roleMap = new HashMap<>();
        roleMap.put("roleName", oshRole.getRoleName());
        roleMap.put("roleCode", oshRole.getRoleCode());
        roleMap.put("level", oshRole.getLevel().toString());
        return roleMap;
    }

    public Map<String,List<String>> getPermission(List<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Integer> ids = oshPermissionMapper.selectPermissionIdsByRoleIds(roleIds);
        if (ids == null || ids.isEmpty()) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<OshPermission> permissionWrapper = new LambdaQueryWrapper<>();
        permissionWrapper.in(OshPermission::getId, ids).eq(OshPermission::getDeleteFlag, 0);
        List<OshPermission> oshPermissions = oshPermissionMapper.selectList(permissionWrapper);
        Map<Integer, OshPermission> permissionMap = oshPermissions.stream()
                .collect(Collectors.toMap(OshPermission::getId, p -> p));
        Map<String, List<String>> result = new HashMap<>();
        for (OshPermission permission : oshPermissions) {
            Integer parentId = permission.getParentId();
            String currentCode = permission.getPermissionCode();
            if (currentCode == null || currentCode.isEmpty()) {
                continue;
            }
            OshPermission parent = permissionMap.get(parentId);
            if (parent != null) {
                String parentCode = parent.getPermissionCode();
                if (parentCode != null && !parentCode.isEmpty()) {
                    result.computeIfAbsent(parentCode, k -> new ArrayList<>()).add(currentCode);
                }
            } else if (parentId == null || parentId == 0) {
                result.computeIfAbsent(currentCode, k -> new ArrayList<>());
            }
        }
        result.values().forEach(Collections::sort);

        return result;
    }

    private Map<String, String> defaultRole() {
        Map<String, String> roleMap = new HashMap<>();
        roleMap.put("roleName", DEFAULT_ROLE_NAME);
        roleMap.put("roleCode", DEFAULT_ROLE_CODE);
        roleMap.put("level", DEFAULT_ROLE_LEVEL);
        return roleMap;
    }

    /**
     * 查询用户列表
     *
     * @param req 用户
     * @return 用户
     */
    @Override
    public List<OshUser> selectUserList(UserListRequest req) {
        return oshUserMapper.selectList(Wrappers.lambdaQuery());
    }
}

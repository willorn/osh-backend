package com.backstage.system.service.user;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.enums.ResultCode;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.user.OshUserAsset;
import com.backstage.system.domain.user.OshUserAssetRecord;
import com.backstage.system.mapper.user.OshPermissionMapper;
import com.backstage.system.mapper.user.OshRoleMapper;
import com.backstage.system.mapper.user.OshUserAssetMapper;
import com.backstage.system.mapper.user.OshUserAssetRecordMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.user.impl.OshUserServiceImpl;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OshUserServiceImplRiskFixTest {

    private static final Long USER_ID = 1001L;

    @InjectMocks
    private OshUserServiceImpl userService;

    @Mock
    private OshUserMapper oshUserMapper;

    @Mock
    private OshRoleMapper oshRoleMapper;

    @Mock
    private OshPermissionMapper oshPermissionMapper;

    @Mock
    private OshUserAssetMapper oshUserAssetMapper;

    @Mock
    private OshUserAssetRecordMapper oshUserAssetRecordMapper;

    @Mock
    private RedisCache redisCache;

    @After
    public void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    public void updateInfoRejectsInvalidUsername() {
        ThreadLocalUtil.set(OshUserConstants.USER_ID, USER_ID);

        R<String> result = userService.updateInfo("1bad", "男", "intro", null, null);

        assertEquals(R.FAIL, result.getCode());
        assertEquals(ResultCode.FAILED_USER_USERNAME_NOT_IN_RANGE.getMsg(), result.getMsg());
        verify(oshUserMapper, never()).update(any(OshUser.class), any(Wrapper.class));
    }

    @Test
    public void updateInfoRejectsDuplicateUsername() {
        ThreadLocalUtil.set(OshUserConstants.USER_ID, USER_ID);
        OshUser current = new OshUser();
        current.setId(USER_ID);
        current.setUsername("oldName");
        when(oshUserMapper.selectOne(any(Wrapper.class))).thenReturn(current);
        when(oshUserMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        R<String> result = userService.updateInfo("newName", "男", "intro", null, null);

        assertEquals(R.FAIL, result.getCode());
        assertEquals(ResultCode.AILED_USER_EXISTS.getMsg(), result.getMsg());
        verify(oshUserMapper, never()).update(any(OshUser.class), any(Wrapper.class));
    }

    @Test
    public void getRoleFallsBackWhenUserHasNoRoles() {
        Map<String, String> role = userService.getRole(Collections.emptyList());

        assertEquals("普通用户", role.get("roleName"));
        assertEquals("user", role.get("roleCode"));
        assertEquals("1", role.get("level"));
    }

    @Test
    public void getPermissionReturnsEmptyWhenUserHasNoRoles() {
        Map<String, java.util.List<String>> permission = userService.getPermission(Collections.emptyList());

        assertTrue(permission.isEmpty());
        verify(oshPermissionMapper, never()).selectPermissionIdsByRoleIds(any());
    }

    @Test
    public void updateAssetRejectsInsufficientBalanceWithoutWritingRecord() {
        ThreadLocalUtil.set(OshUserConstants.USER_ID, USER_ID);
        when(redisCache.getCacheObject(OshUserConstants.LOGIN_USER + USER_ID)).thenReturn(loginCache(10L));
        when(oshUserAssetMapper.selectOne(any(Wrapper.class))).thenReturn(asset(10L));
        when(oshUserAssetMapper.updatePointsAtomic(USER_ID, -20L, true)).thenReturn(0);

        R<String> result = userService.updateAsset(1, 4, 20L, "buy");

        assertEquals(R.FAIL, result.getCode());
        assertEquals("积分余额不足", result.getMsg());
        verify(oshUserAssetRecordMapper, never()).insert(any(OshUserAssetRecord.class));
    }

    @Test
    public void updateAssetWritesRecordAndRefreshesRedisOnSuccess() {
        ThreadLocalUtil.set(OshUserConstants.USER_ID, USER_ID);
        Map<String, Object> loginCache = loginCache(10L);
        when(redisCache.getCacheObject(OshUserConstants.LOGIN_USER + USER_ID)).thenReturn(loginCache);
        when(oshUserAssetMapper.selectOne(any(Wrapper.class))).thenReturn(asset(10L), asset(25L));
        when(oshUserAssetMapper.updatePointsAtomic(USER_ID, 15L, false)).thenReturn(1);

        R<String> result = userService.updateAsset(0, 1, 15L, "daily");

        assertEquals(R.SUCCESS, result.getCode());
        Map<String, String> asset = (Map<String, String>) loginCache.get(OshUserConstants.ASSET);
        assertEquals("25", asset.get(OshUserConstants.POINTS));
        verify(oshUserAssetRecordMapper).insert(any(OshUserAssetRecord.class));
        verify(redisCache).setCacheObject(OshUserConstants.LOGIN_USER + USER_ID, loginCache);
    }

    @Test
    public void deleteUserClearsRedisSession() {
        ThreadLocalUtil.set(OshUserConstants.USER_ID, USER_ID);
        OshUser user = new OshUser();
        user.setId(USER_ID);
        when(oshUserMapper.selectOne(any(Wrapper.class))).thenReturn(user);

        R<String> result = userService.deleteUser();

        assertEquals(R.SUCCESS, result.getCode());
        verify(redisCache).deleteObject(OshUserConstants.LOGIN_USER + USER_ID);
    }

    private Map<String, Object> loginCache(Long points) {
        Map<String, String> asset = new HashMap<>();
        asset.put(OshUserConstants.POINTS, String.valueOf(points));
        Map<String, Object> cache = new HashMap<>();
        cache.put(OshUserConstants.ASSET, asset);
        return cache;
    }

    private OshUserAsset asset(Long points) {
        OshUserAsset asset = new OshUserAsset();
        asset.setUserId(USER_ID);
        asset.setPoints(points);
        return asset;
    }
}

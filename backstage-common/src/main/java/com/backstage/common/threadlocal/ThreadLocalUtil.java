package com.backstage.common.threadlocal;

import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.backstage.common.constant.OshUserConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 九转苍翎
 * Date: 2026/3/30
 * Time: 14:11
 */
public class ThreadLocalUtil {
    private static final TransmittableThreadLocal<Map<String, Object>> THREAD_LOCAL = new TransmittableThreadLocal<>();
    public static void set(String key, Object value) {
        Map<String, Object> map = getLocalMap();
        map.put(key, value == null ? StrUtil.EMPTY : value);
    }

    public static void setResourceId(String key, Object value) {
        Map<String, Object> map = getLocalMap();
        if (value == null) map.put(key, StrUtil.EMPTY);
        if (value instanceof Long) {
            List<Long> longs = new ArrayList<>();
            longs.add((Long) value);
            map.put(key, longs);
        } else if (value instanceof List) {
            map.put(key, value);
        } else {
            map.put(key, StrUtil.EMPTY);
        }
    }

    public static <T> T get(String key, Class<T> clazz) {
        Map<String, Object> map = getLocalMap();
        return (T) map.getOrDefault(key, null);

    }
    public static Map<String, Object> getLocalMap() {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map == null) {
            map = new ConcurrentHashMap<>();
            THREAD_LOCAL.set(map);
        }
        return map;
    }
    public static void remove() {
        THREAD_LOCAL.remove();
    }

    public static void remove(String key) {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map != null) {
            map.remove(key);
        }
    }

    public static long getCurrentUserId() {
        return Objects.requireNonNull(ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class), "user is not logged in");
    }
}

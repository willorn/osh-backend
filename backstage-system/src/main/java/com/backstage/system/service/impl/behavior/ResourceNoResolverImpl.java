package com.backstage.system.service.impl.behavior;

import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.system.service.behavior.ResourceNoResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResourceNoResolverImpl implements ResourceNoResolver {
    private static final Map<String, String> RESOURCE_TABLES = new HashMap<>();

    static {
        for (ResourceTypeEnum value : ResourceTypeEnum.values()) {
            RESOURCE_TABLES.put(value.getType(), value.getMysqlTableName());
        }
    }

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public String resolveResourceNo(String resourceType, Long resourceId) {
        String tableName = RESOURCE_TABLES.get(resourceType);
        if (tableName == null || resourceId == null) {
            return null;
        }
        try {
            List<String> values = jdbcTemplate.queryForList(
                    "select no from " + tableName + " where id = ? limit 1",
                    String.class,
                    resourceId
            );
            return values.isEmpty() ? null : values.get(0);
        } catch (Exception ignored) {
            return null;
        }
    }
}

package com.backstage.system.service.impl.homepage;

import com.backstage.system.domain.homepage.constants.FrontPathConstants;
import com.backstage.system.service.homepage.IOshHomePageModulePathService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 首页模块路径 Service 实现
 * <p>
 * 路径数据统一维护在 {@link FrontPathConstants} 中，
 * 本类仅负责查询逻辑。
 *
 * @author jayTatum
 */
@Service
public class OshHomePageModulePathServiceImpl implements IOshHomePageModulePathService {

    @Override
    public String getDetailPath(String module, Object id) {
        String prefix = FrontPathConstants.DETAIL_PATH_MAP.getOrDefault(module, "/");
        return prefix + id;

    }

    @Override
    public String getListPath(String module) {
        return FrontPathConstants.LIST_PATH_MAP.getOrDefault(module, "/");
    }

    @Override
    public Map<String, String> getAllListPaths() {
        return FrontPathConstants.LIST_PATH_MAP;
    }
}

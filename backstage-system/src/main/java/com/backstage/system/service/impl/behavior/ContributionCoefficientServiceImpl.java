package com.backstage.system.service.impl.behavior;

import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.SysConfig;
import com.backstage.system.domain.behavior.ContributionCoefficientConfig;
import com.backstage.system.service.ISysConfigService;
import com.backstage.system.service.behavior.ContributionCoefficientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ContributionCoefficientServiceImpl implements ContributionCoefficientService {
    public static final String CONFIG_KEY = "behavior.contribution.coefficients";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ISysConfigService sysConfigService;

    @Override
    public ContributionCoefficientConfig getConfig() {
        String value = sysConfigService.selectConfigByKey(CONFIG_KEY);
        if (StringUtils.isBlank(value)) {
            return new ContributionCoefficientConfig();
        }
        try {
            return objectMapper.readValue(value, ContributionCoefficientConfig.class);
        } catch (Exception e) {
            return new ContributionCoefficientConfig();
        }
    }

    @Override
    public ContributionCoefficientConfig saveConfig(ContributionCoefficientConfig config) {
        ContributionCoefficientConfig normalized = config == null ? new ContributionCoefficientConfig() : config;
        try {
            String value = objectMapper.writeValueAsString(normalized);
            SysConfig query = new SysConfig();
            query.setConfigKey(CONFIG_KEY);
            SysConfig existed = null;
            for (SysConfig item : sysConfigService.selectConfigList(query)) {
                if (CONFIG_KEY.equals(item.getConfigKey())) {
                    existed = item;
                    break;
                }
            }
            if (existed == null) {
                SysConfig created = new SysConfig();
                created.setConfigName("行为贡献点系数");
                created.setConfigKey(CONFIG_KEY);
                created.setConfigValue(value);
                created.setConfigType("N");
                sysConfigService.insertConfig(created);
            } else {
                existed.setConfigValue(value);
                sysConfigService.updateConfig(existed);
            }
        } catch (Exception e) {
            throw new ServiceException("保存贡献系数失败");
        }
        return getConfig();
    }
}

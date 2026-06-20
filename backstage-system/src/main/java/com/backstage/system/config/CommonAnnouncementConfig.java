package com.backstage.system.config;

import com.backstage.system.enums.homepage.HomepageResourceTypeEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 通用公告配置。
 */
@Configuration
@ConfigurationProperties(prefix = "common.announcement")
public class CommonAnnouncementConfig {

    private PushConfig push = new PushConfig();
    private DataConfig data = new DataConfig();
    private WebSocketConfig webSocket = new WebSocketConfig();

    public PushConfig getPush() {
        return push;
    }

    public void setPush(PushConfig push) {
        this.push = push;
    }

    public DataConfig getData() {
        return data;
    }

    public void setData(DataConfig data) {
        this.data = data;
    }

    public WebSocketConfig getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(WebSocketConfig webSocket) {
        this.webSocket = webSocket;
    }

    public static class PushConfig {

        private boolean enabled = false;
        private int intervalMinutes = 30;
        private int cacheRefreshHours = 1;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getIntervalMinutes() {
            return intervalMinutes;
        }

        public void setIntervalMinutes(int intervalMinutes) {
            this.intervalMinutes = intervalMinutes;
        }

        public int getCacheRefreshHours() {
            return cacheRefreshHours;
        }

        public void setCacheRefreshHours(int cacheRefreshHours) {
            this.cacheRefreshHours = cacheRefreshHours;
        }
    }

    public static class DataConfig {

        private int httpChannelLimit = 2;
        private int wsChannelLimit = 4;
        private int maxLimit = 50;
        private String[] supportedResourceTypes = HomepageResourceTypeEnum.getAllCodes();

        public int getHttpChannelLimit() {
            return httpChannelLimit;
        }

        public void setHttpChannelLimit(int httpChannelLimit) {
            this.httpChannelLimit = httpChannelLimit;
        }

        public int getWsChannelLimit() {
            return wsChannelLimit;
        }

        public void setWsChannelLimit(int wsChannelLimit) {
            this.wsChannelLimit = wsChannelLimit;
        }

        public int getMaxLimit() {
            return maxLimit;
        }

        public void setMaxLimit(int maxLimit) {
            this.maxLimit = maxLimit;
        }

        public String[] getSupportedResourceTypes() {
            return supportedResourceTypes;
        }

        public void setSupportedResourceTypes(String[] supportedResourceTypes) {
            this.supportedResourceTypes = supportedResourceTypes;
        }
    }

    public static class WebSocketConfig {

        private String dataMessageType = "HOMEPAGE_ANNOUNCEMENT_DATA";
        private int retryTimes = 3;
        private long timeoutMs = 5000;

        public String getDataMessageType() {
            return dataMessageType;
        }

        public void setDataMessageType(String dataMessageType) {
            this.dataMessageType = dataMessageType;
        }

        public int getRetryTimes() {
            return retryTimes;
        }

        public void setRetryTimes(int retryTimes) {
            this.retryTimes = retryTimes;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}

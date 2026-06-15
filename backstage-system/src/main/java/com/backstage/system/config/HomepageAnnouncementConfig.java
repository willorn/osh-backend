package com.backstage.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 首页公告推送配置
 * 
 * @author jayTatum
 */
@Configuration
@ConfigurationProperties(prefix = "homepage.announcement")
public class HomepageAnnouncementConfig {
    
    /**
     * 是否启用首页公告推送
     */
    private PushConfig push = new PushConfig();
    
    /**
     * 数据查询配置
     */
    private DataConfig data = new DataConfig();
    
    /**
     * WebSocket配置
     */
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
    
    /**
     * 推送相关配置
     */
    public static class PushConfig {
        
        /**
         * 是否启用定时推送任务
         */
        private boolean enabled = false;
        
        /**
         * 定时推送间隔（分钟）
         */
        private int intervalMinutes = 30;
        
        /**
         * 缓存刷新间隔（小时）
         */
        private int cacheRefreshHours = 1;
        
        /**
         * 热点资源推送间隔（分钟）
         */
        private int hotResourceIntervalMinutes = 10;
        
        /**
         * 普通资源推送间隔（小时）
         */
        private int normalResourceIntervalHours = 2;
        
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
        
        public int getHotResourceIntervalMinutes() {
            return hotResourceIntervalMinutes;
        }
        
        public void setHotResourceIntervalMinutes(int hotResourceIntervalMinutes) {
            this.hotResourceIntervalMinutes = hotResourceIntervalMinutes;
        }
        
        public int getNormalResourceIntervalHours() {
            return normalResourceIntervalHours;
        }
        
        public void setNormalResourceIntervalHours(int normalResourceIntervalHours) {
            this.normalResourceIntervalHours = normalResourceIntervalHours;
        }
    }
    
    /**
     * 数据查询相关配置
     */
    public static class DataConfig {
        
        /**
         * HTTP GET 接口每个channel返回的数量（2条 channel=1 + 2条 channel=2）
         */
        private int httpChannelLimit = 2;
        
        /**
         * WebSocket 定时推送每个channel返回的数量（4条 channel=1 + 4条 channel=2）
         */
        private int wsChannelLimit = 4;
        
        /**
         * 最大查询限制
         */
        private int maxLimit = 50;
        
        /**
         * 支持的资源类型列表
         */
        private String[] supportedResourceTypes = 
            com.backstage.system.enums.homepage.HomepageResourceTypeEnum.getAllCodes();
        
        /**
         * 热点资源类型列表
         */
        private String[] hotResourceTypes = 
            com.backstage.system.enums.homepage.HomepageResourceTypeEnum.getHotCodes();
        
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
        
        public String[] getHotResourceTypes() {
            return hotResourceTypes;
        }
        
        public void setHotResourceTypes(String[] hotResourceTypes) {
            this.hotResourceTypes = hotResourceTypes;
        }
    }
    
    /**
     * WebSocket相关配置
     */
    public static class WebSocketConfig {
        
        /**
         * 数据推送消息类型
         */
        private String dataMessageType = "HOMEPAGE_ANNOUNCEMENT_DATA";
        
        /**
         * 刷新通知消息类型
         */
        private String refreshMessageType = "HOMEPAGE_ANNOUNCEMENT_REFRESH";
        
        /**
         * 推送失败重试次数
         */
        private int retryTimes = 3;
        
        /**
         * 推送超时时间（毫秒）
         */
        private long timeoutMs = 5000;
        
        public String getDataMessageType() {
            return dataMessageType;
        }
        
        public void setDataMessageType(String dataMessageType) {
            this.dataMessageType = dataMessageType;
        }
        
        public String getRefreshMessageType() {
            return refreshMessageType;
        }
        
        public void setRefreshMessageType(String refreshMessageType) {
            this.refreshMessageType = refreshMessageType;
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
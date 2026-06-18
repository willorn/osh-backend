package com.backstage.system.service.resource.impl;

import com.google.gson.annotations.SerializedName;

/**
 * 轮询成功 —— Token 响应
 */
public class TokenResponse {
    @SerializedName("access_token")
    String accessToken;

    @SerializedName("refresh_token")
    String refreshToken;

    /**
     * access_token 有效时长（秒），通常 2592000 = 30 天
     */
    @SerializedName("expires_in")
    long expiresIn;

    @SerializedName("scope")
    String scope;

    @SerializedName("session_secret")
    String sessionSecret;

    @SerializedName("session_key")
    String sessionKey;

    /**
     * 错误码（正常时不返回）
     */
    @SerializedName("error_code")
    Integer errorCode;

    /**
     * 错误标识，如 "authorization_pending"
     */
    @SerializedName("error")
    String error;

    /**
     * 错误描述
     */
    @SerializedName("error_description")
    String errorDescription;

    boolean isSuccess() {
        return accessToken != null && !accessToken.isEmpty();
    }

    @Override
    public String toString() {
        if (isSuccess()) {
            return "access_token:  " + accessToken + "\n"
                   + "refresh_token: " + refreshToken + "\n"
                   + "expires_in:    " + expiresIn + " 秒\n"
                   + "scope:         " + scope;
        }
        return "错误: [" + errorCode + "] " + error + " - " + errorDescription;
    }
}

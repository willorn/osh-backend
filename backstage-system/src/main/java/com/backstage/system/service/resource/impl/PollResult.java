package com.backstage.system.service.resource.impl;

/**
 * 轮询最终结果
 */
public class PollResult {
    final boolean success;
    final TokenResponse response;
    final String message;

    private PollResult(boolean success, TokenResponse response, String message) {
        this.success = success;
        this.response = response;
        this.message = message;
    }

    static PollResult ok(TokenResponse resp) {
        return new PollResult(true, resp, "授权成功");
    }

    static PollResult fail(String msg) {
        return new PollResult(false, null, msg);
    }
}
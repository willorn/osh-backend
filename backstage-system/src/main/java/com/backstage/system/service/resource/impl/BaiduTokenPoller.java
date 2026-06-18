package com.backstage.system.service.resource.impl;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * 百度网盘 OAuth 2.0 —— 使用 Device Code 轮询换取 Access Token
 * <p>
 * 前置步骤：调用 BaiduDeviceCodeDemo.fetchDeviceCode() 获取 device_code
 * 文档参考：https://pan.baidu.com/union/doc/fl1x114ti
 * <p>
 * 依赖（Maven）：
 * <dependency>
 * <groupId>com.squareup.okhttp3</groupId>
 * <artifactId>okhttp</artifactId>
 * <version>4.12.0</version>
 * </dependency>
 * <dependency>
 * <groupId>com.google.code.gson</groupId>
 * <artifactId>gson</artifactId>
 * <version>2.11.0</version>
 * </dependency>
 */
public class BaiduTokenPoller {

    // ========== 响应模型 ==========


    // ========== 核心方法 ==========

    private static final String TOKEN_URL =
            "https://openapi.baidu.com/oauth/2.0/token";

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final Gson GSON = new Gson();

    /**
     * 单次请求 Token 接口
     */
    private static TokenResponse requestToken(String deviceCode,
                                              String appKey,
                                              String secretKey) throws IOException {
        String url = TOKEN_URL
                     + "?grant_type=device_token"
                     + "&code=" + deviceCode
                     + "&client_id=" + appKey
                     + "&client_secret=" + secretKey;

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "pan.baidu.com")
                .get()
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP 请求失败, code=" + response.code());
            }
            String body = response.body() != null ? response.body().string() : "";
            if (body.isEmpty()) {
                throw new IOException("响应体为空");
            }
            return GSON.fromJson(body, TokenResponse.class);
        }
    }

    /**
     * 轮询换取 Access Token
     *
     * <p>按照 OAuth 2.0 Device Flow 标准，持续轮询直到用户完成授权或超时。
     * 百度文档建议每次轮询间隔不少于 5 秒。</p>
     *
     * @param deviceCode  上一步获取的 device_code
     * @param appKey      应用的 AppKey（client_id）
     * @param secretKey   应用的 SecretKey（client_secret）
     * @param intervalSec 轮询间隔（秒），建议 >= 5
     * @param expiresSec  总超时（秒），即 device_code 的有效期
     */
    public static PollResult pollForToken(String deviceCode,
                                          String appKey,
                                          String secretKey,
                                          int intervalSec,
                                          int expiresSec) {
        // 确保轮询间隔不低于 5 秒
        int pollInterval = Math.max(intervalSec, 5);
        long deadline = System.currentTimeMillis() + expiresSec * 1000L;

        System.out.println("开始轮询，间隔 " + pollInterval + " 秒，超时 " + expiresSec + " 秒 ...");

        while (System.currentTimeMillis() < deadline) {
            TokenResponse resp;
            try {
                resp = requestToken(deviceCode, appKey, secretKey);
            } catch (IOException e) {
                System.err.println("网络异常，" + pollInterval + " 秒后重试: " + e.getMessage());
                sleep(pollInterval);
                continue;
            }

            // ① 成功拿到 Token
            if (resp.isSuccess()) {
                return PollResult.ok(resp);
            }

            // ② 用户尚未授权，继续等待
            if ("authorization_pending".equals(resp.error)) {
                System.out.print(".");  // 进度指示
                System.out.flush();
                sleep(pollInterval);
                continue;
            }

            // ③ 请求过于频繁，拉长间隔
            if ("slow_down".equals(resp.error)) {
                pollInterval += 5;
                System.out.println("\n请求过快，间隔调整为 " + pollInterval + " 秒");
                sleep(pollInterval);
                continue;
            }

            // ④ device_code 已过期
            if ("expired_token".equals(resp.error)) {
                return PollResult.fail("device_code 已过期，请重新获取设备码");
            }

            // ⑤ 用户拒绝授权
            if ("access_denied".equals(resp.error)) {
                return PollResult.fail("用户拒绝了授权");
            }

            // ⑥ 其他未知错误
            return PollResult.fail("未知错误: " + resp);
        }

        return PollResult.fail("轮询超时，用户未在规定时间内完成授权");
    }

    private static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("轮询被中断", e);
        }
    }

    // ========== 演示入口 ==========

    public static void main(String[] args) {
        // ---- 替换为你自己的凭证 ----
        String appKey = "q1srNKOg5TsWrTMDowO6ugtFEU1440dU";
        String secretKey = "41cyGGLmMx81iMXdNgirt68oDqFKY9h1";

        // ---- 第一步：获取设备码 ----
        System.out.println("===== 第一步：获取设备码 & 用户码 =====");
        BaiduDeviceCodeDemo.DeviceCodeResponse deviceResp;
        try {
            deviceResp = BaiduDeviceCodeDemo.fetchDeviceCode(appKey);
        } catch (IOException e) {
            System.err.println("获取设备码失败: " + e.getMessage());
            return;
        }

        if (deviceResp.isError()) {
            System.err.println(deviceResp);
            return;
        }

        System.out.println(deviceResp);
        System.out.println();
        System.out.println(">>> 请用户访问: " + deviceResp.verificationUrl);
        System.out.println(">>> 输入验证码: " + deviceResp.userCode);
        System.out.println();

        // ---- 第二步：轮询换取 Token ----
        System.out.println("===== 第二步：轮询换取 Access Token =====");
        PollResult result = pollForToken(
                deviceResp.deviceCode,
                appKey,
                secretKey,
                deviceResp.interval,
                deviceResp.expiresIn
        );

        System.out.println();
        if (result.success) {
            System.out.println("===== 授权成功 =====");
            System.out.println(result.response);
            System.out.println();
            System.out.println("access_token 可直接用于后续网盘 API 调用。");
            System.out.println("refresh_token 用于刷新，有效期 10 年，请妥善保管。");
        } else {
            System.err.println("授权失败: " + result.message);
        }
    }
}
package com.backstage.system.service.resource.impl;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * 百度网盘 OAuth 2.0 设备码授权 —— 获取设备码 & 用户码
 *
 * 文档参考：https://pan.baidu.com/union/doc/fl1x114ti
 *
 * 依赖（Maven）：
 *   <dependency>
 *     <groupId>com.squareup.okhttp3</groupId>
 *     <artifactId>okhttp</artifactId>
 *     <version>4.12.0</version>
 *   </dependency>
 *   <dependency>
 *     <groupId>com.google.code.gson</groupId>
 *     <artifactId>gson</artifactId>
 *     <version>2.11.0</version>
 *   </dependency>
 */
public class BaiduDeviceCodeDemo {

    // ========== 响应模型 ==========

    static class DeviceCodeResponse {
        /** 设备码，用于后续轮询获取 access_token */
        @SerializedName("device_code")
        String deviceCode;

        /** 用户码，展示给用户输入到授权页面 */
        @SerializedName("user_code")
        String userCode;

        /** 二维码链接，供智能终端扫码 */
        @SerializedName("qrcode_url")
        String qrcodeUrl;

        /** 用户输入验证码的授权页面 */
        @SerializedName("verification_url")
        String verificationUrl;

        /** 凭证过期时间（秒） */
        @SerializedName("expires_in")
        int expiresIn;

        /** 轮询间隔时间（秒） */
        @SerializedName("interval")
        int interval;

        /** 错误码（正常时不返回） */
        @SerializedName("error_code")
        Integer errorCode;

        /** 错误描述 */
        @SerializedName("error_msg")
        String errorMsg;

        boolean isError() {
            return errorCode != null && errorCode != 0;
        }

        @Override
        public String toString() {
            if (isError()) {
                return "请求失败: [" + errorCode + "] " + errorMsg;
            }
            return "设备码(device_code): " + deviceCode + "\n"
                 + "用户码(user_code):   " + userCode + "\n"
                 + "二维码链接:          " + qrcodeUrl + "\n"
                 + "授权页面:            " + verificationUrl + "\n"
                 + "过期时间(秒):        " + expiresIn + "\n"
                 + "轮询间隔(秒):        " + interval;
        }
    }

    // ========== 核心方法 ==========

    private static final String DEVICE_CODE_URL =
            "https://openapi.baidu.com/oauth/2.0/device/code";

    /**
     * 获取设备码和用户码
     *
     * @param appKey 应用的 AppKey（即 client_id）
     * @return DeviceCodeResponse 包含 device_code、user_code 等信息
     */
    public static DeviceCodeResponse fetchDeviceCode(String appKey) throws IOException {
        // 拼接请求参数
        String url = DEVICE_CODE_URL
                + "?response_type=device_code"
                + "&client_id=" + appKey
                + "&scope=basic,netdisk";

        OkHttpClient client = new OkHttpClient();
        // 文档要求 User-Agent 必须设置为 pan.baidu.com
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "pan.baidu.com")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP 请求失败, code=" + response.code());
            }
            String body = response.body() != null ? response.body().string() : "";
            if (body.isEmpty()) {
                throw new IOException("响应体为空");
            }
            return new Gson().fromJson(body, DeviceCodeResponse.class);
        }
    }

    // ========== 演示入口 ==========

    public static void main(String[] args) {
        // 替换为你自己的 AppKey
        String appKey = "q1srNKOg5TsWrTMDowO6ugtFEU1440dU";

        try {
            DeviceCodeResponse resp = fetchDeviceCode(appKey);

            if (resp.isError()) {
                System.err.println(resp);
                return;
            }

            System.out.println("===== 获取设备码 & 用户码成功 =====");
            System.out.println(resp);
            System.out.println();
            System.out.println(">>> 请让用户访问 " + resp.verificationUrl);
            System.out.println(">>> 并输入验证码: " + resp.userCode);
            System.out.println(">>> 有效期 " + resp.expiresIn + " 秒，建议每 "
                    + resp.interval + " 秒轮询一次获取 access_token");

        } catch (IOException e) {
            System.err.println("请求异常: " + e.getMessage());
        }
    }
}
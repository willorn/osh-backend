package com.backstage.system.service.resource.impl;

import cn.hutool.core.io.FileUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 百度网盘客户端 —— 基于已有 access_token 实现上传和下载
 * <p>
 * 前置条件：已通过 OAuth 2.0 获取 access_token（有效期 30 天）
 * 错误码参考：https://pan.baidu.com/union/doc/okumlx17r
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
public class BaiduPanClient {

    private static final int BLOCK_SIZE = 4 * 1024 * 1024; // 4MB 分片（普通用户）
    private static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");
    private static final int MAX_RETRY = 3;                // 最大重试次数
    private static final long RETRY_BASE_DELAY_MS = 2000;  // 重试基础延迟（毫秒）
    private static final Logger log = LoggerFactory.getLogger(BaiduPanClient.class);

    private final String accessToken;
    private final OkHttpClient http;
    private final Gson gson = new Gson();

    public BaiduPanClient(String accessToken) {
        this.accessToken = accessToken;
        this.http = new OkHttpClient.Builder()
                .followRedirects(true)   // 下载时有 302 跳转
                .followSslRedirects(true)
                .build();
    }

    // ================================================================
    //                    错误码定义 & 异常体系
    // ================================================================

    /**
     * 百度网盘公共错误码枚举
     * 参考文档：https://pan.baidu.com/union/doc/okumlx17r
     */
    enum BaiduErrno {
        // ---- 成功 ----
        SUCCESS(0, "请求成功", ErrCategory.SUCCESS, false),

        // ---- 资源与状态 ----
        RIGHTS_EXPIRED(-1, "用户权益已过期", ErrCategory.PERMISSION, false),
        FILE_NOT_FOUND(-9, "文件或目录不存在", ErrCategory.RESOURCE, false),
        FILE_NOT_FOUND_ALT(-3, "文件或目录不存在", ErrCategory.RESOURCE, false),
        FILE_NOT_FOUND_UPLOAD(31066, "文件不存在", ErrCategory.RESOURCE, false),
        ACCESS_DENIED(-7, "无权访问该文件或目录", ErrCategory.PERMISSION, false),
        RESOURCE_CONFLICT(-8, "资源冲突，文件已存在", ErrCategory.RESOURCE, false),
        RESOURCE_CONFLICT_ALT(10, "资源冲突，文件已存在", ErrCategory.RESOURCE, false),
        RESOURCE_CONFLICT_SAVE(31061, "已保存文件冲突", ErrCategory.RESOURCE, false),
        USER_NOT_FOUND(11, "用户不存在", ErrCategory.RESOURCE, false),
        ASYNC_TASK_BUSY(111, "异步任务进行中", ErrCategory.RETRYABLE, true),
        AD_PLAYING(133, "广告播放中", ErrCategory.RETRYABLE, true),
        SAVE_LIMIT_EXCEEDED(255, "保存文件数超限", ErrCategory.RESOURCE, false),
        SHARE_NOT_EXIST(2131, "分享不存在", ErrCategory.RESOURCE, false),
        SUBTITLE_NOT_FOUND(31649, "字幕文件未找到", ErrCategory.RESOURCE, false),

        // ---- 权限与鉴权 ----
        TOKEN_INVALID(-6, "access_token 验证失败，请检查 token 有效性", ErrCategory.TOKEN, false),
        PATH_OUT_OF_SANDBOX(-10, "路径不在应用沙箱目录内，上传路径必须以 /apps/{应用名}/ 开头", ErrCategory.PARAM, false),
        TOKEN_INVALID_ALT(31045, "access_token 验证失败或已过期", ErrCategory.TOKEN, false),
        DATA_ACCESS_DENIED(6, "用户数据访问被拒绝，请等待 10 分钟后重新授权", ErrCategory.TOKEN, false),
        APP_UNDER_REVIEW(20011, "应用审核中，仅前 10 个 OAuth 用户可测试", ErrCategory.PERMISSION, false),
        RATE_LIMIT(20012, "请求频率超限", ErrCategory.RETRYABLE, true),
        PERMISSION_INSUFFICIENT(20013, "权限不足，请确认已申请该 API 权限", ErrCategory.PERMISSION, false),
        ACCESS_DENIED_SHARE(31024, "访问被拒绝，请检查授权方式或共享目录权限", ErrCategory.PERMISSION, false),
        ACCESS_DENIED_ALT(42213, "访问被拒绝", ErrCategory.PERMISSION, false),
        FREQUENCY_CONTROL(31034, "API 频率控制触发，请求过于频繁", ErrCategory.RETRYABLE, true),

        // ---- 参数错误 ----
        PARAM_ERROR(2, "参数错误，请检查必填字段", ErrCategory.PARAM, false),
        PARAM_ERROR_ALT(31023, "参数错误", ErrCategory.PARAM, false),
        PARAM_ERROR_UPLOADID(31355, "uploadid 参数错误，请确认与预上传返回一致", ErrCategory.PARAM, false),
        BATCH_SAVE_UID_ERROR(12, "批量转存错误，源和目标 UID 不能相同", ErrCategory.PARAM, false),
        INVALID_FILENAME(31062, "文件名无效，请检查是否包含特殊字符", ErrCategory.PARAM, false),
        INVALID_UPLOAD_PATH(31064, "上传路径错误，需使用注册应用名的绝对路径", ErrCategory.PARAM, false),

        // ---- 上传相关 ----
        UPLOAD_FILE_NOT_FOUND(31190, "上传时文件未找到，请检查 block_list 和 size", ErrCategory.UPLOAD, false),
        CHUNK_SIZE_ERROR(31299, "分片大小错误，首片和标准片须为 4MB", ErrCategory.UPLOAD, false),
        CHUNK_SIZE_ERROR_ALT(31364, "分片大小不符合要求", ErrCategory.UPLOAD, false),
        CHUNK_MISSING(31363, "分片缺失，请确保所有分片已上传", ErrCategory.UPLOAD, false),
        FILE_SIZE_EXCEEDED(31365, "文件总大小超限（普通用户 4GB / 会员 10GB / 超会 20GB）", ErrCategory.UPLOAD, false),

        // ---- 下载 & 链接相关 ----
        HOTLINK_BLOCKED(31326, "防盗链触发，请检查 User-Agent 设置", ErrCategory.DOWNLOAD, false),
        LINK_EXPIRED(31360, "下载链接已过期，请重新获取", ErrCategory.DOWNLOAD, true),
        LINK_SIGNATURE_ERROR(31362, "链接签名错误，请重新生成下载链接", ErrCategory.DOWNLOAD, true),

        // ---- 媒体相关 ----
        MEDIA_NOT_AUDIO(31301, "非音频文件", ErrCategory.MEDIA, false),
        MEDIA_FORMAT_UNSUPPORTED(31304, "媒体格式不支持", ErrCategory.MEDIA, false),
        MEDIA_CONTENT_ILLEGAL(31339, "媒体内容不合规", ErrCategory.MEDIA, false),
        MEDIA_TRANSCODE_FAILED(31346, "转码失败", ErrCategory.MEDIA, false),
        VIDEO_BITRATE_HIGH(31338, "视频码率过高，建议下载后播放", ErrCategory.MEDIA, false),
        VIDEO_DURATION_LONG(31347, "视频时长过长，建议下载后播放", ErrCategory.MEDIA, false),
        VIDEO_TRANSCODING(31341, "视频转码中，请稍后重试", ErrCategory.RETRYABLE, true),

        // ---- 相册相关 ----
        ALBUM_CAPACITY_EXCEEDED(42202, "相册容量超限", ErrCategory.RESOURCE, false),
        ALBUM_NOT_EXIST(42203, "相册不存在", ErrCategory.RESOURCE, false),
        ALBUM_OP_FAILED(42210, "相册操作失败", ErrCategory.RESOURCE, false),
        ALBUM_IMAGE_RES_FAIL(42211, "图片分辨率获取失败", ErrCategory.RESOURCE, false),
        ALBUM_SHARE_UPLOADER(42212, "共享目录上传者信息获取失败", ErrCategory.RESOURCE, false),
        ALBUM_FILE_DETAIL(42214, "文件详情获取失败", ErrCategory.RESOURCE, false),

        // ---- 其他 ----
        USERNAME_QUERY_FAILED(42905, "用户名查询失败", ErrCategory.RETRYABLE, true),
        PLAYLIST_NOT_EXIST(50002, "播放列表 ID 不存在", ErrCategory.RESOURCE, false);

        final int code;
        final String description;
        final ErrCategory category;
        final boolean retryable;

        BaiduErrno(int code, String description, ErrCategory category, boolean retryable) {
            this.code = code;
            this.description = description;
            this.category = category;
            this.retryable = retryable;
        }

        static BaiduErrno fromCode(int code) {
            for (BaiduErrno e : values()) {
                if (e.code == code) return e;
            }
            return null;
        }
    }

    /**
     * 错误分类
     */
    enum ErrCategory {
        SUCCESS,      // 成功
        TOKEN,        // Token 相关（过期/失效）
        PERMISSION,   // 权限不足
        PARAM,        // 参数错误
        RESOURCE,     // 资源不存在/冲突
        UPLOAD,       // 上传相关
        DOWNLOAD,     // 下载相关
        MEDIA,        // 媒体相关
        RETRYABLE     // 可重试
    }

    /**
     * 百度网盘业务异常
     */
    static class BaiduPanException extends IOException {
        final int errno;
        final BaiduErrno knownErrno;
        final String rawBody;

        BaiduPanException(int errno, String step, String rawBody) {
            super(buildMessage(errno, step, rawBody));
            this.errno = errno;
            this.knownErrno = BaiduErrno.fromCode(errno);
            this.rawBody = rawBody;
        }

        private static String buildMessage(int errno, String step, String rawBody) {
            BaiduErrno known = BaiduErrno.fromCode(errno);
            if (known != null) {
                return String.format("[%s] errno=%d: %s（分类: %s, 可重试: %s）",
                        step, errno, known.description, known.category, known.retryable);
            }
            return String.format("[%s] 未知错误 errno=%d, body=%s", step, errno, rawBody);
        }

        /**
         * 是否为可重试的错误
         */
        boolean isRetryable() {
            return knownErrno != null && knownErrno.retryable;
        }

        /**
         * 是否为 Token 失效
         */
        boolean isTokenError() {
            return knownErrno != null && knownErrno.category == ErrCategory.TOKEN;
        }

        /**
         * 是否为参数错误（不应重试）
         */
        boolean isParamError() {
            return knownErrno != null && knownErrno.category == ErrCategory.PARAM;
        }
    }

    // ================================================================
    //                    容错请求执行器
    // ================================================================

    /**
     * 带重试的 HTTP 请求执行
     * 对可重试的错误（频率限制、异步任务进行中等）和偶发网络异常自动重试
     */
    private JsonObject executeWithRetry(Request req, String step) throws IOException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                Response resp = http.newCall(req).execute();
                String body = readBody(resp);

                if (body.isEmpty()) {
                    lastException = new IOException("响应体为空, HTTP " + resp.code());
                    System.err.printf("  [%s] 第 %d 次请求响应为空，重试...%n", step, attempt);
                    sleepBeforeRetry(attempt);
                    continue;
                }

                JsonObject json = gson.fromJson(body, JsonObject.class);
                int errno = json.has("errno") ? json.get("errno").getAsInt() : 0;

                if (errno == 0) {
                    return json; // 成功
                }

                // 构建业务异常
                BaiduPanException bizEx = new BaiduPanException(errno, step, body);

                // Token 失效 —— 不重试，直接抛出
                if (bizEx.isTokenError()) {
                    throw bizEx;
                }

                // 参数错误 —— 不重试，直接抛出
                if (bizEx.isParamError()) {
                    throw bizEx;
                }

                // 可重试的错误
                if (bizEx.isRetryable()) {
                    lastException = bizEx;
                    System.err.printf("  [%s] %s，第 %d 次重试...%n",
                            step, bizEx.getMessage(), attempt);
                    sleepBeforeRetry(attempt);
                    continue;
                }

                // 不可重试的业务错误 —— 直接抛出
                throw bizEx;

            } catch (BaiduPanException e) {
                // 业务异常直接向上传播（Token/参数/不可重试错误）
                if (!e.isRetryable()) throw e;
                lastException = e;
            } catch (IOException e) {
                // 网络异常（超时、连接失败等）—— 可重试
                lastException = e;
                if (attempt < MAX_RETRY) {
                    System.err.printf("  [%s] 网络异常: %s，第 %d 次重试...%n",
                            step, e.getMessage(), attempt);
                    sleepBeforeRetry(attempt);
                }
            }
        }

        throw new IOException(String.format("[%s] 重试 %d 次后仍失败: %s",
                step, MAX_RETRY, lastException != null ? lastException.getMessage() : "未知"));
    }

    /**
     * 指数退避等待
     */
    private void sleepBeforeRetry(int attempt) {
        long delay = RETRY_BASE_DELAY_MS * (long) Math.pow(2, attempt - 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("重试等待被中断", e);
        }
    }

    // ================================================================
    //                          文件列表
    // ================================================================

    /**
     * 获取指定目录下的文件列表
     *
     * @param dir 网盘目录路径，如 "/apps/我的应用"，传 null 表示根目录
     * @return 文件信息列表
     */
    public List<FileInfo> listFiles(String dir) throws IOException {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(
                        "https://pan.baidu.com/rest/2.0/xpan/file")).newBuilder()
                .addQueryParameter("method", "list")
                .addQueryParameter("access_token", accessToken)
                .addQueryParameter("limit", "1000")
                .addQueryParameter("web", "1");

        if (dir != null && !dir.isEmpty()) {
            urlBuilder.addQueryParameter("dir", dir);
        }

        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", "pan.baidu.com")
                .get().build();

        JsonObject json = executeWithRetry(req, "listFiles");

        List<FileInfo> result = new ArrayList<>();
        JsonArray list = json.getAsJsonArray("list");
        if (list != null) {
            for (JsonElement elem : list) {
                result.add(gson.fromJson(elem, FileInfo.class));
            }
        }
        return result;
    }

    /**
     * 文件信息模型
     */
    static class FileInfo {
        @SerializedName("fs_id")
        public long fsId;
        @SerializedName("path")
        public String path;
        @SerializedName("server_filename")
        public String serverFilename;
        @SerializedName("size")
        public long size;
        @SerializedName("isdir")
        public int isDir;
        @SerializedName("md5")
        public String md5;
        @SerializedName("category")
        public int category;
        @SerializedName("server_mtime")
        public long serverMtime;

        @Override
        public String toString() {
            String type = isDir == 1 ? "[目录]" : "[文件]";
            return type + " " + serverFilename + "  fs_id=" + fsId
                   + "  size=" + humanSize(size);
        }
    }

    // ================================================================
    //                          上传文件
    // ================================================================


    /**
     * 生成 时间戳+精简UUID 文件名
     *
     * @return 文件名（无后缀）
     */
    public static String generateFileName() {
        // 13位毫秒时间戳
        long timestamp = System.currentTimeMillis();
        // 原生UUID截取前16位，去除横杠
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return timestamp + "_" + shortUuid;
    }

    /**
     * 上传本地文件到百度网盘（不自动创建远程目录）
     *
     * @param localPath  本地文件路径
     * @param remotePath 网盘目标路径（如 "/apps/我的应用/test.zip"）
     */
    public void upload(String localPath, String remotePath) throws Exception {
        upload(localPath, remotePath, false);
    }

    public String getPath(String appName, String... paths) {
        return Paths.get("/apps/" + appName, paths).toString();
    }

    public void upload(MultipartFile file, String remotePath, boolean createDir) {
        Path tmpFile = Paths.get(getSystemTempDir(), "osh_resource", file.getOriginalFilename(), generateFileName() + ".tmp");
        try {
            Files.copy(file.getInputStream(), tmpFile);
            upload(tmpFile.toString(), remotePath, createDir);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new RuntimeException(e);
        } finally {
            FileUtil.del(tmpFile);
        }
    }

    /**
     * 获取操作系统默认临时文件目录
     *
     * @return 临时目录绝对路径
     */
    static String getSystemTempDir() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * 上传本地文件到百度网盘
     *
     * @param localPath  本地文件路径
     * @param remotePath 网盘目标路径（如 "/apps/我的应用/test.zip"）
     * @param createDir  为 true 时，若网盘上的父目录不存在则自动创建
     */
    public void upload(String localPath, String remotePath, boolean createDir) throws Exception {
        Path file = Paths.get(localPath);
        if (!Files.exists(file)) {
            throw new FileNotFoundException("本地文件不存在: " + localPath);
        }

        long fileSize = Files.size(file);
        String fileName = file.getFileName().toString();

        // remotePath 若以 / 结尾则自动追加文件名
        if (remotePath.endsWith("/")) {
            remotePath = remotePath + fileName;
        }

        log.info("[上传] {} ({})", fileName, humanSize(fileSize));
        log.info("[上传] 目标: {}", remotePath);

        // 若需要自动创建远程目录
        if (createDir) {
            String parentDir = extractParentDir(remotePath);
            if (parentDir != null && !parentDir.equals("/")) {
                ensureRemoteDir(parentDir);
            }
        }

        // 1) 计算分片 MD5 + 整文件 MD5
        List<String> blockMd5s = computeBlockMd5s(file);
        String contentMd5 = computeFileMd5(file);
        log.info("[上传] 分片数: {}", blockMd5s.size());

        // 2) 预上传
        String uploadId = precreate(remotePath, fileSize, blockMd5s, contentMd5);
        if (uploadId == null) {
            log.info("[上传] 秒传成功！");
            return;
        }

        // 3) 分片上传（带重试）
        uploadBlocks(file, remotePath, uploadId, blockMd5s.size());

        log.info("[上传] 分片上传完成！");
        // 4) 创建文件
        createFile(remotePath, fileSize, uploadId, blockMd5s);

        log.info("[上传] 完成！");
    }

    // ================================================================
    //                    远程目录管理
    // ================================================================

    /**
     * 从完整路径中提取父目录路径
     * 例："/apps/我的应用/sub/test.zip" → "/apps/我的应用/sub"
     */
    private static String extractParentDir(String remotePath) {
        int lastSlash = remotePath.lastIndexOf('/');
        if (lastSlash <= 0) return "/";
        return remotePath.substring(0, lastSlash);
    }

    /**
     * 确保网盘上的目录存在（逐层创建）
     * 例：传入 "/apps/我的应用/sub" 时，会依次确认 /apps、/apps/我的应用、/apps/我的应用/sub 都存在
     */
    private void ensureRemoteDir(String remotePath) throws IOException {
        // 拆分路径，逐层检查并创建
        String[] segments = remotePath.split("/");
        StringBuilder currentPath = new StringBuilder();

        for (String segment : segments) {
            if (segment.isEmpty()) continue;
            currentPath.append("/").append(segment);
            String dirPath = currentPath.toString();

            // 调用 mkdir —— 目录已存在时 errno 也为 0，不会报错
            FormBody form = new FormBody.Builder()
                    .add("path", dirPath)
                    .build();

            String url = "https://pan.baidu.com/rest/2.0/xpan/file?method=mkdir"
                         + "&access_token=" + accessToken;

            Request req = new Request.Builder()
                    .url(url).header("User-Agent", "pan.baidu.com").post(form).build();

            try {
                JsonObject json = executeWithRetry(req, "mkdir[" + dirPath + "]");
                // mkdir 成功或目录已存在
            } catch (BaiduPanException e) {
                // errno=-8 表示目录/文件已存在，属于正常情况，忽略
                if (e.errno == -8 || e.errno == 10 || e.errno == 31061) {
                    continue;
                }
                throw e;
            }
        }
        log.info("[上传] 远程目录已就绪: {}", remotePath);
    }

    /**
     * 预上传 —— 返回 uploadid（需继续上传）或 null（秒传成功）
     */
    private String precreate(String remotePath, long fileSize,
                             List<String> blockMd5s, String contentMd5) throws IOException {
        JsonArray blockList = new JsonArray();
        blockMd5s.forEach(blockList::add);

        FormBody form = new FormBody.Builder()
                .add("path", remotePath)
                .add("size", String.valueOf(fileSize))
                .add("isdir", "0")
                .add("autoinit", "1")
                .add("block_list", gson.toJson(blockList))
                .add("content-md5", contentMd5)
                .add("rtype", "3")
                .build();

        String url = "https://pan.baidu.com/rest/2.0/xpan/file?method=precreate"
                     + "&access_token=" + accessToken;

        Request req = new Request.Builder()
                .url(url).header("User-Agent", "pan.baidu.com").post(form).build();

        JsonObject json = executeWithRetry(req, "precreate");

        int returnType = json.has("return_type") ? json.get("return_type").getAsInt() : 0;
        if (returnType == 2) {
            return null; // 秒传
        }
        return json.get("uploadid").getAsString();
    }

    /**
     * 分片上传（每个分片失败后最多重试 MAX_RETRY 次）
     */
    private void uploadBlocks(Path file, String remotePath,
                              String uploadId, int blockCount) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            byte[] buffer = new byte[BLOCK_SIZE];

            for (int i = 0; i < blockCount; i++) {
                raf.seek((long) i * BLOCK_SIZE);
                int read = raf.read(buffer);
                if (read <= 0) break;

                byte[] chunk = new byte[read];
                System.arraycopy(buffer, 0, chunk, 0, read);

                uploadSingleBlock(chunk, remotePath, uploadId, i, blockCount);
            }
        }
    }

    /**
     * 单个分片上传（带重试）
     */
    private void uploadSingleBlock(byte[] chunk, String remotePath,
                                   String uploadId, int partSeq,
                                   int totalBlocks) throws IOException {
        String url = "https://d.pcs.baidu.com/rest/2.0/pcs/superfile2?method=upload"
                     + "&access_token=" + accessToken
                     + "&path=" + remotePath
                     + "&uploadid=" + uploadId
                     + "&partseq=" + partSeq;

        IOException lastEx = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "chunk",
                            RequestBody.create(OCTET_STREAM, chunk))
                    .build();

            Request req = new Request.Builder()
                    .url(url).header("User-Agent", "pan.baidu.com").post(body).build();

            try (Response resp = http.newCall(req).execute()) {
                String respBody = readBody(resp);
                JsonObject json = gson.fromJson(respBody, JsonObject.class);

                if (json.has("md5")) {
                    log.info("  分片 {} OK", partSeq + 1);
                    return; // 成功
                }

                // 解析 errno
                int errno = json.has("errno") ? json.get("errno").getAsInt() : -1;
                BaiduPanException bizEx = new BaiduPanException(errno, "superfile2[part=" + partSeq + "]", respBody);

                // Token 失效 / 参数错误 —— 不重试
                if (bizEx.isTokenError() || bizEx.isParamError()) {
                    throw bizEx;
                }

                // 分片大小错误 —— 不重试
                if (bizEx.knownErrno == BaiduErrno.CHUNK_SIZE_ERROR
                    || bizEx.knownErrno == BaiduErrno.CHUNK_SIZE_ERROR_ALT) {
                    throw bizEx;
                }

                // 其他错误 —— 重试
                lastEx = bizEx;
                log.error("  分片 {}/{} 第 {} 次失败: {}，重试...",
                        partSeq + 1, totalBlocks, attempt, bizEx.getMessage());
                sleepBeforeRetry(attempt);

            } catch (IOException e) {
                if (e instanceof BaiduPanException && !((BaiduPanException) e).isRetryable()) {
                    throw e;
                }
                lastEx = e;
                if (attempt < MAX_RETRY) {
                    log.error("  分片 {}/{} 网络异常: {}，第 {} 次重试...",
                            partSeq + 1, totalBlocks, e.getMessage(), attempt);
                    sleepBeforeRetry(attempt);
                }
            }
        }

        throw new IOException(String.format("分片 %d 上传重试 %d 次后仍失败: %s",
                partSeq, MAX_RETRY, lastEx.getMessage()));
    }

    /**
     * 创建文件（合并分片）
     */
    private void createFile(String remotePath, long fileSize,
                            String uploadId, List<String> blockMd5s) throws IOException {
        JsonArray blockList = new JsonArray();
        blockMd5s.forEach(blockList::add);

        FormBody form = new FormBody.Builder()
                .add("path", remotePath)
                .add("size", String.valueOf(fileSize))
                .add("isdir", "0")
                .add("block_list", gson.toJson(blockList))
                .add("uploadid", uploadId)
                .add("rtype", "3")
                .build();

        String url = "https://pan.baidu.com/rest/2.0/xpan/file?method=create"
                     + "&access_token=" + accessToken;

        Request req = new Request.Builder()
                .url(url).header("User-Agent", "pan.baidu.com").post(form).build();

        JsonObject json = executeWithRetry(req, "create");
        log.info("  create OK, fs_id={}", json.has("fs_id") ? json.get("fs_id").getAsString() : "?");
    }

    // ================================================================
    //                          下载文件
    // ================================================================

    /**
     * 从百度网盘下载文件到本地（不自动创建本地目录）
     *
     * @param remotePath 网盘文件路径（如 "/apps/我的应用/test.zip"）
     * @param localDir   本地保存目录
     */
    public Path download(String remotePath, String localDir) throws IOException {
        return download(remotePath, localDir, false);
    }

    /**
     * 从百度网盘下载文件到本地
     *
     * <p>流程：filemetas 获取 dlink → GET dlink 下载文件内容</p>
     *
     * @param remotePath 网盘文件路径（如 "/apps/我的应用/test.zip"）
     * @param localDir   本地保存目录
     * @param createDir  为 true 时，若本地目录不存在则自动创建
     */
    public Path download(String remotePath, String localDir, boolean createDir) throws IOException {
        log.info("[下载] 网盘路径: {}", remotePath);

        // 0) 检查/创建本地目录
        ensureLocalDir(localDir, createDir);

        // 1) 通过文件列表找到 fs_id
        remotePath = remotePath.replace("\\", "/");
        long fsId = findFsId(remotePath);
        log.info("[下载] fs_id={}", fsId);

        // 2) 获取下载链接 dlink
        DlinkInfo dlinkInfo = getDlink(fsId);
        log.info("[下载] 文件名: {}  大小: {}", dlinkInfo.filename, humanSize(dlinkInfo.size));

        // 3) 下载文件内容
        Path targetFile = Paths.get(localDir).resolve(dlinkInfo.filename);

        downloadFile(dlinkInfo.dlink, targetFile);

        log.info("[下载] 完成！保存到: {}", targetFile);
        return targetFile;
    }

    /**
     * 通过路径查找文件的 fs_id
     */
    private long findFsId(String remotePath) throws IOException {
        String parentDir;
        String fileName;
        int lastSlash = remotePath.lastIndexOf('/');
        if (lastSlash > 0) {
            parentDir = remotePath.substring(0, lastSlash);
            fileName = remotePath.substring(lastSlash + 1);
        } else {
            parentDir = "/";
            fileName = remotePath.startsWith("/") ? remotePath.substring(1) : remotePath;
        }

        List<FileInfo> files = listFiles(parentDir);
        for (FileInfo f : files) {
            if (f.serverFilename.equals(fileName) && f.isDir == 0) {
                return f.fsId;
            }
        }
        throw new BaiduPanException(-9, "findFsId",
                "文件不存在: " + remotePath);
    }

    /**
     * 下载链接信息
     */
    static class DlinkInfo {
        String dlink;
        String filename;
        long size;
    }

    /**
     * 调用 filemetas 获取 dlink（下载链接，有效期 8 小时）
     */
    private DlinkInfo getDlink(long fsId) throws IOException {
        String fsidsParam = "[" + fsId + "]";

        String url = "https://pan.baidu.com/rest/2.0/xpan/multimedia?method=filemetas"
                     + "&access_token=" + accessToken
                     + "&fsids=" + fsidsParam
                     + "&dlink=1";

        Request req = new Request.Builder()
                .url(url).header("User-Agent", "pan.baidu.com").get().build();

        JsonObject json = executeWithRetry(req, "filemetas");

        JsonArray list = json.getAsJsonArray("list");
        if (list == null || list.size() == 0) {
            throw new IOException("filemetas 返回空列表, fs_id=" + fsId);
        }

        JsonObject fileObj = list.get(0).getAsJsonObject();
        DlinkInfo info = new DlinkInfo();
        info.dlink = fileObj.get("dlink").getAsString();
        info.filename = fileObj.get("filename").getAsString();
        info.size = fileObj.has("size") ? fileObj.get("size").getAsLong() : 0;
        return info;
    }

    /**
     * 通过 dlink 下载文件到本地（支持大文件流式写入，带重试）
     */
    private void downloadFile(String dlink, Path targetFile) throws IOException {
        String downloadUrl = dlink + "&access_token=" + accessToken;

        IOException lastEx = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            Request req = new Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "pan.baidu.com") // 必须设置，否则触发防盗链 (errno=31326)
                    .get().build();

            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    // 尝试解析错误响应体
                    String body = readBody(resp);
                    try {
                        JsonObject json = gson.fromJson(body, JsonObject.class);
                        int errno = json.has("errno") ? json.get("errno").getAsInt() : 0;
                        if (errno != 0) {
                            BaiduPanException bizEx = new BaiduPanException(errno, "download", body);

                            // 链接过期 → 需重新获取 dlink，此处无法自动修复
                            if (bizEx.knownErrno == BaiduErrno.LINK_EXPIRED
                                || bizEx.knownErrno == BaiduErrno.LINK_SIGNATURE_ERROR) {
                                throw bizEx;
                            }
                            // 防盗链触发 → 不重试，需检查请求
                            if (bizEx.knownErrno == BaiduErrno.HOTLINK_BLOCKED) {
                                throw bizEx;
                            }
                            // Token 错误 → 不重试
                            if (bizEx.isTokenError()) {
                                throw bizEx;
                            }
                        }
                    } catch (Exception ignored) {
                        // body 不是 JSON，忽略
                    }

                    lastEx = new IOException("下载失败, HTTP " + resp.code());
                    if (attempt < MAX_RETRY) {
                        System.err.println("  [下载] HTTP " + resp.code() + "，第 " + attempt + " 次重试...");
                        sleepBeforeRetry(attempt);
                        continue;
                    }
                    throw lastEx;
                }

                ResponseBody body = resp.body();
                if (body == null) {
                    throw new IOException("下载响应体为空");
                }

                long totalSize = body.contentLength();
                long downloaded = 0;
                int lastPercent = -1;

                try (InputStream is = body.byteStream();
                     OutputStream os = new BufferedOutputStream(Files.newOutputStream(targetFile))) {

                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        os.write(buf, 0, n);
                        downloaded += n;

                        if (totalSize > 0) {
                            int percent = (int) (downloaded * 100 / totalSize);
                            if (percent != lastPercent && percent % 10 == 0) {
                                log.info("  下载进度: {}% ({}/{})", percent, humanSize(downloaded), humanSize(totalSize));
                                lastPercent = percent;
                            }
                        }
                    }
                }
                return; // 下载成功

            } catch (BaiduPanException e) {
                throw e; // 业务异常不重试
            } catch (IOException e) {
                lastEx = e;
                if (attempt < MAX_RETRY) {
                    System.err.println("  [下载] 网络异常: " + e.getMessage() + "，第 " + attempt + " 次重试...");
                    sleepBeforeRetry(attempt);
                }
            }
        }

        throw new IOException("下载重试 " + MAX_RETRY + " 次后仍失败: "
                              + (lastEx != null ? lastEx.getMessage() : "未知"));
    }

    // ================================================================
    //                     通过 fs_id 直接下载
    // ================================================================

    /**
     * 如果已经知道 fs_id，可以跳过列表查询直接下载（不自动创建本地目录）
     */
    public void downloadByFsId(long fsId, String localDir) throws IOException {
        downloadByFsId(fsId, localDir, false);
    }

    /**
     * 如果已经知道 fs_id，可以跳过列表查询直接下载
     *
     * @param fsId      网盘文件 ID
     * @param localDir  本地保存目录
     * @param createDir 为 true 时，若本地目录不存在则自动创建
     */
    public void downloadByFsId(long fsId, String localDir, boolean createDir) throws IOException {
        // 检查/创建本地目录
        ensureLocalDir(localDir, createDir);

        DlinkInfo dlinkInfo = getDlink(fsId);
        log.info("[下载] {}  大小: {}", dlinkInfo.filename, humanSize(dlinkInfo.size));

        Path targetFile = Paths.get(localDir).resolve(dlinkInfo.filename);

        downloadFile(dlinkInfo.dlink, targetFile);
        log.info("[下载] 完成！保存到: {}", targetFile);
    }

    /**
     * 确保本地目录存在
     *
     * @param localDir  本地目录路径
     * @param createDir 为 true 时自动创建，为 false 时目录不存在则抛异常
     */
    private void ensureLocalDir(String localDir, boolean createDir) throws IOException {
        Path dir = Paths.get(localDir);
        if (!Files.exists(dir)) {
            if (createDir) {
                Files.createDirectories(dir);
                log.info("[下载] 已创建本地目录: {}", localDir);
            } else {
                throw new FileNotFoundException("本地目录不存在: " + localDir
                                                + "（设置 createDir=true 可自动创建）");
            }
        }
    }

    // ================================================================
    //                       MD5 工具方法
    // ================================================================

    private List<String> computeBlockMd5s(Path file) throws IOException {
        List<String> md5s = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long remaining = raf.length();
            byte[] buffer = new byte[BLOCK_SIZE];
            while (remaining > 0) {
                int toRead = (int) Math.min(BLOCK_SIZE, remaining);
                byte[] chunk = new byte[toRead];
                raf.readFully(chunk);
                md5s.add(md5Hex(chunk));
                remaining -= toRead;
            }
        }
        if (md5s.isEmpty()) {
            md5s.add(md5Hex(new byte[0]));
        }
        return md5s;
    }

    private String computeFileMd5(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = new BufferedInputStream(Files.newInputStream(file))) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    md.update(buf, 0, n);
                }
            }
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // ================================================================
    //                       通用工具方法
    // ================================================================

    private static String md5Hex(byte[] data) {
        try {
            return bytesToHex(MessageDigest.getInstance("MD5").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String readBody(Response resp) throws IOException {
        return resp.body() != null ? resp.body().string() : "";
    }

    // ================================================================
    //                         使用示例
    // ================================================================

    public static void main(String[] args) throws Exception {
        String accessToken = "126.f770af46d73f3d6997d9f670b9699ede.Yg9_425rbbCT3pSbUzk4VccAcxQGul41LpU6HWY.7whr8Q";

        // 【重要】百度网盘第三方应用有沙箱限制：
        // 所有文件路径必须在 /apps/{应用名}/ 下，应用名 = 你在百度开放平台注册时填的产品名称
        // 例如应用名叫 "osh"，则路径为 /apps/osh/xxx
        String appName = "test"; // TODO: 替换为你实际注册的应用名
        String remoteDir = "/apps/" + appName;

        BaiduPanClient client = new BaiduPanClient(accessToken);

        try {
            // ---------- 列出文件 ----------
            System.out.println("===== 文件列表 =====");
            List<FileInfo> files = client.listFiles(remoteDir);
            for (FileInfo f : files) {
                System.out.println(f);
            }
            System.out.println();

            // ---------- 上传 ----------
            System.out.println("===== 上传 =====");
            client.upload(
                    "C:\\Users\\lenovo\\Desktop\\test.zip",
                    remoteDir + "/test.zip",
                    true
            );
            System.out.println();

            // ---------- 按路径下载 ----------
            System.out.println("===== 按路径下载 =====");
            client.download(
                    remoteDir + "/test.zip",
                    "C:\\Users\\lenovo\\Downloads",
                    true
            );
            System.out.println();

            // ---------- 按 fs_id 下载 ----------
            System.out.println("===== 按 fs_id 下载 =====");
            if (!files.isEmpty()) {
                FileInfo target = files.stream()
                        .filter(f -> f.isDir == 0)
                        .findFirst()
                        .orElse(null);
                if (target != null) {
                    client.downloadByFsId(target.fsId, "C:\\Users\\lenovo\\Downloads", true);
                }
            }

        } catch (BaiduPanException e) {
            // 精确捕获百度业务错误
            System.err.println("百度网盘业务错误:");
            System.err.println("  errno:    " + e.errno);
            System.err.println("  已知错误: " + (e.knownErrno != null ? e.knownErrno.name() : "否"));
            System.err.println("  分类:     " + (e.knownErrno != null ? e.knownErrno.category : "未知"));
            System.err.println("  可重试:   " + e.isRetryable());
            System.err.println("  详情:     " + e.getMessage());

            if (e.isTokenError()) {
                System.err.println("\n>>> access_token 已失效，请重新授权获取！");
            }
        }
    }
}

package com.backstage.system.service.resource.impl;

import com.backstage.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * BaiduPCS-Go 命令执行封装。
 *
 * <p>适用于后端 Java 进程中调用 Linux 上已部署好的 BaiduPCS-Go 二进制命令，
 * 以结构化方式完成命令构建、执行、超时控制、标准输出/错误输出采集和异常抛出。</p>
 *
 * <p>典型调用方式：</p>
 * <pre>
 * BaiduPcsGoCommandService.BaiduPcsGoUploadRequest request =
 *         BaiduPcsGoCommandService.BaiduPcsGoUploadRequest.builder()
 *                 .addLocalPath("/data/upload/demo.zip")
 *                 .setRemoteTarget("/apps/osh/materials")
 *                 .setDisableRapidUpload(true)
 *                 .build();
 *
 * BaiduPcsGoCommandService.BaiduPcsGoCommandResult result = baiduPcsGoCommandService.upload(request);
 * </pre>
 */
@Service
public class BaiduPcsGoCommandService {

    private static final Logger log = LoggerFactory.getLogger(BaiduPcsGoCommandService.class);

    private static final long DEFAULT_TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private static final int DEFAULT_MAX_CAPTURE_CHARS = 200_000;
    private static final int DEFAULT_MAX_CAPTURE_LINES = 2_000;
    private static final int PROCESS_STREAM_THREADS = 2;
    private static final Pattern ANSI_PATTERN = Pattern.compile("\\u001B\\[[;\\d]*m");

    @Value("${baidu.pcs-go.binary-path:BaiduPCS-Go}")
    private String defaultBinaryPath;

    @Value("${baidu.pcs-go.working-directory:}")
    private String defaultWorkingDirectory;

    @Value("${baidu.pcs-go.default-timeout-millis:1800000}")
    private long defaultTimeoutMillis;

    @Value("${baidu.pcs-go.default-charset:UTF-8}")
    private String defaultCharsetName;

    @Value("${baidu.pcs-go.max-capture-chars:200000}")
    private int defaultMaxCaptureChars;

    @Value("${baidu.pcs-go.max-capture-lines:2000}")
    private int defaultMaxCaptureLines;

    @Value("${baidu.pcs-go.temp-dir:}")
    private String defaultTempDirectory;

    @Value("${baidu.pcs-go.delete-temp-file:true}")
    private boolean deleteTempFileAfterMultipartUpload;

    /**
     * 上传本地文件到百度网盘。
     */
    public BaiduPcsGoCommandResult upload(BaiduPcsGoUploadRequest request) {
        validateUploadRequest(request);

        BaiduPcsGoCommandRequest commandRequest = request.toCommandRequest(resolveDefaultBinaryPath());
        return execute(commandRequest);
    }

    /**
     * 上传前端传入的 MultipartFile。
     *
     * <p>实现思路：先将 MultipartFile 落到本地临时文件，再调用 BaiduPCS-Go 的 upload 命令。
     * 这样可以复用统一的命令执行流程，也方便后续把上传前后的处理逻辑沉淀在一处。</p>
     */
    public BaiduPcsGoCommandResult uploadMultipartFile(MultipartFile multipartFile,
                                                       BaiduPcsGoUploadRequest request) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        if (request == null) {
            throw new ServiceException("BaiduPCS-Go 上传请求不能为空");
        }
        if (!CollectionUtils.isEmpty(request.getLocalPaths())) {
            throw new ServiceException("uploadMultipartFile 不允许同时传入 localPaths，请只传 MultipartFile");
        }

        Path tempFile = null;
        try {
            tempFile = createTempFile(multipartFile.getOriginalFilename());
            Files.copy(multipartFile.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            BaiduPcsGoUploadRequest actualRequest = request.copy();
            actualRequest.setLocalPaths(Collections.singletonList(tempFile.toAbsolutePath().toString()));

            return upload(actualRequest);
        } catch (IOException exception) {
            throw new BaiduPcsGoCommandException(
                    "写入 BaiduPCS-Go 临时上传文件失败: " + exception.getMessage(),
                    null,
                    null,
                    exception
            );
        } finally {
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 执行任意 BaiduPCS-Go 命令。
     */
    public BaiduPcsGoCommandResult execute(BaiduPcsGoCommandRequest request) {
        validateCommandRequest(request);

        Charset charset = resolveCharset(request.getCharsetName());
        long timeoutMillis = request.getTimeoutMillis() != null && request.getTimeoutMillis() > 0
                ? request.getTimeoutMillis()
                : resolveDefaultTimeoutMillis();
        int maxCaptureChars = request.getMaxCaptureChars() != null && request.getMaxCaptureChars() > 0
                ? request.getMaxCaptureChars()
                : resolveDefaultMaxCaptureChars();
        int maxCaptureLines = request.getMaxCaptureLines() != null && request.getMaxCaptureLines() > 0
                ? request.getMaxCaptureLines()
                : resolveDefaultMaxCaptureLines();

        List<String> command = buildCommand(request);
        File workingDirectory = resolveWorkingDirectory(request.getWorkingDirectory());
        Map<String, String> environment = buildEnvironment(request);
        Set<Integer> expectedExitCodes = buildExpectedExitCodes(request);

        Instant startedAt = Instant.now();
        long startNanos = System.nanoTime();

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory);
        }
        if (request.isMergeErrorStream()) {
            processBuilder.redirectErrorStream(true);
        }
        if (!environment.isEmpty()) {
            processBuilder.environment().putAll(environment);
        }

        Process process = null;
        ExecutorService executorService = Executors.newFixedThreadPool(PROCESS_STREAM_THREADS);
        StreamCapture stdoutCapture = new StreamCapture(maxCaptureChars, maxCaptureLines, request.isStripAnsi());
        StreamCapture stderrCapture = new StreamCapture(maxCaptureChars, maxCaptureLines, request.isStripAnsi());
        Future<StreamCapture> stdoutFuture = null;
        Future<StreamCapture> stderrFuture = null;

        try {
            process = processBuilder.start();

            stdoutFuture = executorService.submit(new StreamCollector(process.getInputStream(), charset, stdoutCapture));
            if (!request.isMergeErrorStream()) {
                stderrFuture = executorService.submit(new StreamCollector(process.getErrorStream(), charset, stderrCapture));
            }

            boolean finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroy();
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            }

            StreamCapture finalStdout = getFutureValue(stdoutFuture);
            StreamCapture finalStderr = request.isMergeErrorStream()
                    ? new StreamCapture(maxCaptureChars, maxCaptureLines, request.isStripAnsi())
                    : getFutureValue(stderrFuture);

            long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            Instant finishedAt = Instant.now();

            boolean timedOut = !finished;
            int exitCode = timedOut ? -1 : process.exitValue();

            BaiduPcsGoCommandResult result = new BaiduPcsGoCommandResult(
                    request.getDescription(),
                    command,
                    toCommandLine(command),
                    workingDirectory != null ? workingDirectory.getAbsolutePath() : null,
                    exitCode,
                    !timedOut && expectedExitCodes.contains(exitCode),
                    timedOut,
                    durationMillis,
                    startedAt,
                    finishedAt,
                    finalStdout.toText(),
                    finalStdout.getLines(),
                    finalStdout.isTruncated(),
                    finalStderr.toText(),
                    finalStderr.getLines(),
                    finalStderr.isTruncated(),
                    FailureHint.detect(exitCode, timedOut, finalStdout.toText(), finalStderr.toText())
            );

            if (!result.isSuccess() && request.isFailOnNonZeroExit()) {
                throw new BaiduPcsGoCommandException(buildExecutionFailureMessage(result), request, result, null);
            }

            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BaiduPcsGoCommandException("执行 BaiduPCS-Go 命令时线程被中断", request, null, exception);
        } catch (IOException exception) {
            throw new BaiduPcsGoCommandException(
                    "启动 BaiduPCS-Go 进程失败: " + exception.getMessage(),
                    request,
                    null,
                    exception
            );
        } finally {
            if (process != null) {
                safeClose(process.getInputStream());
                safeClose(process.getErrorStream());
                safeClose(process.getOutputStream());
            }
            executorService.shutdownNow();
        }
    }

    /**
     * 检查 BaiduPCS-Go 是否已安装，并尝试获取版本信息。
     */
    public BaiduPcsGoInstallCheckResult checkInstallation(BaiduPcsGoInstallCheckRequest request) {
        BaiduPcsGoInstallCheckRequest actualRequest = request != null
                ? request
                : BaiduPcsGoInstallCheckRequest.builder().build();

        String script = buildCheckInstallationScript(actualRequest);
        BaiduPcsGoCommandRequest commandRequest = buildShellCommandRequest(
                StringUtils.hasText(actualRequest.getDescription())
                        ? actualRequest.getDescription()
                        : "Check BaiduPCS-Go installation",
                script,
                actualRequest.getWorkingDirectory(),
                actualRequest.getTimeoutMillis(),
                actualRequest.getCharsetName(),
                actualRequest.getEnvironment(),
                false
        );

        BaiduPcsGoCommandResult commandResult = execute(commandRequest);
        return parseInstallCheckResult(commandResult);
    }

    /**
     * 安装 BaiduPCS-Go（Linux 环境）。
     *
     * <p>默认流程：</p>
     * <p>1. 下载 release 包或二进制</p>
     * <p>2. 解压/定位目标二进制</p>
     * <p>3. 安装到指定目录</p>
     * <p>4. 可选创建软链</p>
     * <p>5. 安装后再次检查版本</p>
     */
    public BaiduPcsGoInstallResult install(BaiduPcsGoInstallRequest request) {
        validateInstallRequest(request);

        String installScript = buildInstallScript(request);
        BaiduPcsGoCommandRequest commandRequest = buildShellCommandRequest(
                StringUtils.hasText(request.getDescription())
                        ? request.getDescription()
                        : "Install BaiduPCS-Go",
                installScript,
                request.getWorkingDirectory(),
                request.getTimeoutMillis(),
                request.getCharsetName(),
                request.getEnvironment(),
                true
        );

        BaiduPcsGoCommandResult installCommandResult = execute(commandRequest);

        BaiduPcsGoInstallCheckRequest checkRequest = BaiduPcsGoInstallCheckRequest.builder()
                .setExecutablePath(resolveInstalledBinaryPath(request))
                .setBinaryName(request.getFinalBinaryName())
                .setWorkingDirectory(request.getWorkingDirectory())
                .setTimeoutMillis(request.getTimeoutMillis())
                .setCharsetName(request.getCharsetName())
                .setEnvironment(request.getEnvironment())
                .build();

        BaiduPcsGoInstallCheckResult checkResult = checkInstallation(checkRequest);
        return new BaiduPcsGoInstallResult(
                request.getDownloadUrl(),
                request.getInstallDirectory(),
                resolveInstalledBinaryPath(request),
                request.isCreateSymlink() ? request.getSymlinkPath() : null,
                checkResult.getVersionOutput(),
                installCommandResult,
                checkResult
        );
    }

    private void validateUploadRequest(BaiduPcsGoUploadRequest request) {
        if (request == null) {
            throw new ServiceException("BaiduPCS-Go 上传请求不能为空");
        }
        if (CollectionUtils.isEmpty(request.getLocalPaths())) {
            throw new ServiceException("BaiduPCS-Go localPaths cannot be empty");
        }
        for (String localPath : request.getLocalPaths()) {
            if (!StringUtils.hasText(localPath)) {
                throw new ServiceException("BaiduPCS-Go localPaths contains blank value");
            }
        }
        if (!StringUtils.hasText(request.getRemoteTarget())) {
            throw new ServiceException("BaiduPCS-Go remoteTarget cannot be empty");
        }
    }

    private void validateCommandRequest(BaiduPcsGoCommandRequest request) {
        if (request == null) {
            throw new ServiceException("BaiduPCS-Go 命令请求不能为空");
        }
        if (!StringUtils.hasText(request.getExecutablePath()) && !StringUtils.hasText(defaultBinaryPath)) {
            throw new ServiceException("BaiduPCS-Go executable path is not configured");
        }
        if (!StringUtils.hasText(request.getCommand())) {
            throw new ServiceException("BaiduPCS-Go command cannot be empty");
        }
    }

    private void validateInstallRequest(BaiduPcsGoInstallRequest request) {
        if (request == null) {
            throw new ServiceException("BaiduPCS-Go install request cannot be null");
        }
        if (!StringUtils.hasText(request.getDownloadUrl())) {
            throw new ServiceException("BaiduPCS-Go downloadUrl cannot be empty");
        }
        if (!StringUtils.hasText(request.getInstallDirectory())) {
            throw new ServiceException("BaiduPCS-Go installDirectory cannot be empty");
        }
        if (!StringUtils.hasText(request.getFinalBinaryName())) {
            throw new ServiceException("BaiduPCS-Go finalBinaryName cannot be empty");
        }
        if (request.isCreateSymlink() && !StringUtils.hasText(request.getSymlinkPath())) {
            throw new ServiceException("BaiduPCS-Go symlinkPath cannot be empty when createSymlink=true");
        }
    }

    private List<String> buildCommand(BaiduPcsGoCommandRequest request) {
        List<String> command = new ArrayList<>();
        command.add(StringUtils.hasText(request.getExecutablePath())
                ? request.getExecutablePath()
                : resolveDefaultBinaryPath());
        if (!CollectionUtils.isEmpty(request.getGlobalArguments())) {
            command.addAll(request.getGlobalArguments());
        }
        command.add(request.getCommand());
        if (!CollectionUtils.isEmpty(request.getArguments())) {
            command.addAll(request.getArguments());
        }
        return command;
    }

    private Map<String, String> buildEnvironment(BaiduPcsGoCommandRequest request) {
        Map<String, String> environment = new HashMap<>();
        if (request.getEnvironment() != null) {
            environment.putAll(request.getEnvironment());
        }
        return environment;
    }

    private Set<Integer> buildExpectedExitCodes(BaiduPcsGoCommandRequest request) {
        Set<Integer> exitCodes = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(request.getExpectedExitCodes())) {
            exitCodes.addAll(request.getExpectedExitCodes());
        } else {
            exitCodes.add(0);
        }
        return exitCodes;
    }

    private File resolveWorkingDirectory(String requestWorkingDirectory) {
        String actualWorkingDirectory = StringUtils.hasText(requestWorkingDirectory)
                ? requestWorkingDirectory
                : defaultWorkingDirectory;
        if (!StringUtils.hasText(actualWorkingDirectory)) {
            return null;
        }

        File directory = new File(actualWorkingDirectory);
        if (!directory.exists()) {
            throw new ServiceException("BaiduPCS-Go working directory does not exist: " + actualWorkingDirectory);
        }
        if (!directory.isDirectory()) {
            throw new ServiceException("BaiduPCS-Go working directory is not a directory: " + actualWorkingDirectory);
        }
        return directory;
    }

    private Charset resolveCharset(String requestCharset) {
        String actualCharset = StringUtils.hasText(requestCharset) ? requestCharset : defaultCharsetName;
        if (!StringUtils.hasText(actualCharset)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(actualCharset);
        } catch (Exception exception) {
            throw new ServiceException("Invalid BaiduPCS-Go charset: " + actualCharset);
        }
    }

    private Path createTempFile(String originalFilename) throws IOException {
        String safeName = StringUtils.hasText(originalFilename) ? originalFilename : "upload.bin";
        safeName = safeName.replace("\\", "_").replace("/", "_");

        String prefix = "baidupcsgo-" + UUID.randomUUID().toString().replace("-", "");
        String suffix = extractSuffix(safeName);
        Path tempDirectory;
        if (StringUtils.hasText(defaultTempDirectory)) {
            tempDirectory = new File(defaultTempDirectory).toPath();
            Files.createDirectories(tempDirectory);
        } else {
            tempDirectory = Files.createTempDirectory("baidupcsgo");
        }
        return Files.createTempFile(tempDirectory, prefix + "-", suffix);
    }

    private void cleanupTempFile(Path tempFile) {
        if (!deleteTempFileAfterMultipartUpload || tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
            Path parent = tempFile.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                tryDeleteEmptyDirectory(parent);
            }
        } catch (IOException exception) {
            log.warn("删除 BaiduPCS-Go 临时文件失败: {}", tempFile, exception);
        }
    }

    private void tryDeleteEmptyDirectory(Path directory) {
        try (java.util.stream.Stream<Path> stream = Files.list(directory)) {
            if (directory.getFileName() != null
                    && directory.getFileName().toString().startsWith("baidupcsgo")
                    && stream.findAny().isPresent()) {
                return;
            }
        } catch (IOException exception) {
            return;
        }
        try {
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // 忽略空目录清理失败，不影响主流程
        }
    }

    private String buildExecutionFailureMessage(BaiduPcsGoCommandResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("BaiduPCS-Go 命令执行失败");
        if (result.isTimedOut()) {
            builder.append(": timeout");
        } else {
            builder.append(": exitCode=").append(result.getExitCode());
        }
        builder.append(", failureHint=").append(result.getFailureHint());

        String stderr = result.getStderr();
        String stdout = result.getStdout();
        String detail = StringUtils.hasText(stderr) ? stderr : stdout;
        if (StringUtils.hasText(detail)) {
            builder.append(", output=").append(limit(detail, 500));
        }
        return builder.toString();
    }

    private BaiduPcsGoCommandRequest buildShellCommandRequest(String description,
                                                              String script,
                                                              String workingDirectory,
                                                              Long timeoutMillis,
                                                              String charsetName,
                                                              Map<String, String> environment,
                                                              boolean failOnNonZeroExit) {
        BaiduPcsGoCommandRequest request = new BaiduPcsGoCommandRequest();
        request.setExecutablePath("/bin/sh");
        request.setWorkingDirectory(workingDirectory);
        request.setTimeoutMillis(timeoutMillis);
        request.setCharsetName(charsetName);
        request.setDescription(description);
        request.setCommand("-lc");
        request.setArguments(Collections.singletonList(script));
        request.setEnvironment(environment != null ? new HashMap<>(environment) : new HashMap<String, String>());
        request.setExpectedExitCodes(new LinkedHashSet<>(Collections.singletonList(0)));
        request.setFailOnNonZeroExit(failOnNonZeroExit);
        request.setMergeErrorStream(false);
        request.setStripAnsi(true);
        return request;
    }

    private String buildCheckInstallationScript(BaiduPcsGoInstallCheckRequest request) {
        String executablePath = request.getExecutablePath();
        String binaryName = StringUtils.hasText(request.getBinaryName()) ? request.getBinaryName() : "BaiduPCS-Go";

        StringBuilder script = new StringBuilder();
        script.append("set +e\n");
        script.append("PCS_PATH=").append(shQuote(executablePath == null ? "" : executablePath)).append("\n");
        script.append("PCS_NAME=").append(shQuote(binaryName)).append("\n");
        script.append("RESOLVED=\"\"\n");
        script.append("if [ -n \"$PCS_PATH\" ] && [ -x \"$PCS_PATH\" ]; then RESOLVED=\"$PCS_PATH\"; fi\n");
        script.append("if [ -z \"$RESOLVED\" ] && command -v \"$PCS_NAME\" >/dev/null 2>&1; then RESOLVED=\"$(command -v \"$PCS_NAME\")\"; fi\n");
        script.append("if [ -z \"$RESOLVED\" ]; then echo \"__PCS_INSTALLED__=false\"; exit 0; fi\n");
        script.append("echo \"__PCS_INSTALLED__=true\"\n");
        script.append("echo \"__PCS_PATH__=$RESOLVED\"\n");
        script.append("VERSION_OUTPUT=\"$($RESOLVED version 2>&1)\"\n");
        script.append("if [ -z \"$VERSION_OUTPUT\" ]; then VERSION_OUTPUT=\"$($RESOLVED -v 2>&1)\"; fi\n");
        script.append("if [ -z \"$VERSION_OUTPUT\" ]; then VERSION_OUTPUT=\"$($RESOLVED --help 2>&1 | head -n 5)\"; fi\n");
        script.append("printf '%s\\n' \"$VERSION_OUTPUT\" | sed 's/^/__PCS_VERSION__=/'\n");
        script.append("exit 0\n");
        return script.toString();
    }

    private String buildInstallScript(BaiduPcsGoInstallRequest request) {
        String archiveType = StringUtils.hasText(request.getArchiveType()) ? request.getArchiveType() : "tar.gz";
        String installDirectory = request.getInstallDirectory();
        String finalBinaryName = request.getFinalBinaryName();
        String symlinkPath = request.getSymlinkPath();
        String workingDirectory = request.getInstallerWorkspace();
        String binaryRelativePath = request.getBinaryRelativePathInArchive();
        String executableName = StringUtils.hasText(request.getExecutableName())
                ? request.getExecutableName()
                : finalBinaryName;

        StringBuilder script = new StringBuilder();
        script.append("set -e\n");
        script.append("DOWNLOAD_URL=").append(shQuote(request.getDownloadUrl())).append("\n");
        script.append("ARCHIVE_TYPE=").append(shQuote(archiveType)).append("\n");
        script.append("INSTALL_DIR=").append(shQuote(installDirectory)).append("\n");
        script.append("FINAL_BINARY_NAME=").append(shQuote(finalBinaryName)).append("\n");
        script.append("EXECUTABLE_NAME=").append(shQuote(executableName)).append("\n");
        script.append("BINARY_RELATIVE_PATH=").append(shQuote(binaryRelativePath == null ? "" : binaryRelativePath)).append("\n");
        script.append("WORKSPACE=").append(shQuote(StringUtils.hasText(workingDirectory) ? workingDirectory : "")).append("\n");
        script.append("OVERWRITE=").append(shQuote(String.valueOf(request.isOverwrite()))).append("\n");
        script.append("CREATE_SYMLINK=").append(shQuote(String.valueOf(request.isCreateSymlink()))).append("\n");
        script.append("SYMLINK_PATH=").append(shQuote(symlinkPath == null ? "" : symlinkPath)).append("\n");
        script.append("SUDO_PREFIX=").append(shQuote(request.isUseSudo() ? "sudo " : "")).append("\n");
        script.append("if [ -n \"$WORKSPACE\" ]; then mkdir -p \"$WORKSPACE\"; TMP_DIR=\"$WORKSPACE\"; else TMP_DIR=\"$(mktemp -d)\"; fi\n");
        script.append("DOWNLOAD_PATH=\"$TMP_DIR/package\"\n");
        script.append("EXTRACT_DIR=\"$TMP_DIR/extracted\"\n");
        script.append("mkdir -p \"$EXTRACT_DIR\"\n");
        script.append("if command -v curl >/dev/null 2>&1; then curl -L --fail -o \"$DOWNLOAD_PATH\" \"$DOWNLOAD_URL\"; ")
                .append("elif command -v wget >/dev/null 2>&1; then wget -O \"$DOWNLOAD_PATH\" \"$DOWNLOAD_URL\"; ")
                .append("else echo 'No downloader found: curl/wget'; exit 21; fi\n");
        script.append("case \"$ARCHIVE_TYPE\" in\n");
        script.append("  tar.gz|tgz)\n");
        script.append("    tar -xzf \"$DOWNLOAD_PATH\" -C \"$EXTRACT_DIR\"\n");
        script.append("    ;;\n");
        script.append("  zip)\n");
        script.append("    if ! command -v unzip >/dev/null 2>&1; then echo 'unzip not found'; exit 22; fi\n");
        script.append("    unzip -o \"$DOWNLOAD_PATH\" -d \"$EXTRACT_DIR\" >/dev/null\n");
        script.append("    ;;\n");
        script.append("  binary)\n");
        script.append("    cp \"$DOWNLOAD_PATH\" \"$EXTRACT_DIR/$FINAL_BINARY_NAME\"\n");
        script.append("    ;;\n");
        script.append("  *)\n");
        script.append("    echo \"Unsupported archive type: $ARCHIVE_TYPE\"\n");
        script.append("    exit 23\n");
        script.append("    ;;\n");
        script.append("esac\n");
        script.append("if [ -n \"$BINARY_RELATIVE_PATH\" ]; then SOURCE_BINARY=\"$EXTRACT_DIR/$BINARY_RELATIVE_PATH\"; ")
                .append("else SOURCE_BINARY=\"$(find \"$EXTRACT_DIR\" -type f \\( -name \"$EXECUTABLE_NAME\" -o -name \"$FINAL_BINARY_NAME\" \\) | head -n 1)\"; fi\n");
        script.append("if [ ! -f \"$SOURCE_BINARY\" ]; then echo 'BaiduPCS-Go binary not found after extraction'; exit 24; fi\n");
        script.append("chmod +x \"$SOURCE_BINARY\"\n");
        script.append("TARGET_BINARY=\"$INSTALL_DIR/$FINAL_BINARY_NAME\"\n");
        script.append("if [ -e \"$TARGET_BINARY\" ] && [ \"$OVERWRITE\" != \"true\" ]; then echo 'Target binary already exists'; exit 25; fi\n");
        script.append("sh -lc \"$SUDO_PREFIX mkdir -p \\\"$INSTALL_DIR\\\"\"\n");
        script.append("sh -lc \"$SUDO_PREFIX install -m 755 \\\"$SOURCE_BINARY\\\" \\\"$TARGET_BINARY\\\"\"\n");
        script.append("echo \"__PCS_INSTALL_PATH__=$TARGET_BINARY\"\n");
        script.append("if [ \"$CREATE_SYMLINK\" = \"true\" ] && [ -n \"$SYMLINK_PATH\" ]; then ")
                .append("SYMLINK_DIR=\"$(dirname \"$SYMLINK_PATH\")\"; ")
                .append("sh -lc \"$SUDO_PREFIX mkdir -p \\\"$SYMLINK_DIR\\\"\"; ")
                .append("sh -lc \"$SUDO_PREFIX ln -sf \\\"$TARGET_BINARY\\\" \\\"$SYMLINK_PATH\\\"\"; ")
                .append("echo \"__PCS_SYMLINK__=$SYMLINK_PATH\"; fi\n");
        script.append("VERSION_OUTPUT=\"$($TARGET_BINARY version 2>&1 || true)\"\n");
        script.append("printf '%s\\n' \"$VERSION_OUTPUT\" | sed 's/^/__PCS_VERSION__=/'\n");
        return script.toString();
    }

    private BaiduPcsGoInstallCheckResult parseInstallCheckResult(BaiduPcsGoCommandResult commandResult) {
        String installedFlag = extractTaggedValue(commandResult.getStdoutLines(), "__PCS_INSTALLED__=");
        String resolvedPath = extractTaggedValue(commandResult.getStdoutLines(), "__PCS_PATH__=");
        List<String> versionLines = extractTaggedValues(commandResult.getStdoutLines(), "__PCS_VERSION__=");
        String versionOutput = versionLines.isEmpty() ? null : String.join(System.lineSeparator(), versionLines);

        return new BaiduPcsGoInstallCheckResult(
                "true".equalsIgnoreCase(installedFlag),
                resolvedPath,
                versionOutput,
                commandResult
        );
    }

    private String resolveInstalledBinaryPath(BaiduPcsGoInstallRequest request) {
        return request.getInstallDirectory() + "/" + request.getFinalBinaryName();
    }

    private String extractTaggedValue(List<String> lines, String prefix) {
        if (CollectionUtils.isEmpty(lines)) {
            return null;
        }
        for (String line : lines) {
            if (line != null && line.startsWith(prefix)) {
                return line.substring(prefix.length());
            }
        }
        return null;
    }

    private List<String> extractTaggedValues(List<String> lines, String prefix) {
        if (CollectionUtils.isEmpty(lines)) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (String line : lines) {
            if (line != null && line.startsWith(prefix)) {
                values.add(line.substring(prefix.length()));
            }
        }
        return values;
    }

    private String shQuote(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private StreamCapture getFutureValue(Future<StreamCapture> future) {
        if (future == null) {
            return new StreamCapture(resolveDefaultMaxCaptureChars(), resolveDefaultMaxCaptureLines(), true);
        }
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BaiduPcsGoCommandException("读取 BaiduPCS-Go 输出时线程被中断", null, null, exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            throw new BaiduPcsGoCommandException("读取 BaiduPCS-Go 输出失败: " + cause.getMessage(), null, null, cause);
        }
    }

    private void safeClose(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }
        try {
            inputStream.close();
        } catch (IOException ignored) {
            // ignore
        }
    }

    private void safeClose(java.io.OutputStream outputStream) {
        if (outputStream == null) {
            return;
        }
        try {
            outputStream.close();
        } catch (IOException ignored) {
            // ignore
        }
    }

    private String resolveDefaultBinaryPath() {
        if (!StringUtils.hasText(defaultBinaryPath)) {
            throw new ServiceException("未配置 baidu.pcs-go.binary-path");
        }
        return defaultBinaryPath;
    }

    private long resolveDefaultTimeoutMillis() {
        return defaultTimeoutMillis > 0 ? defaultTimeoutMillis : DEFAULT_TIMEOUT_MILLIS;
    }

    private int resolveDefaultMaxCaptureChars() {
        return defaultMaxCaptureChars > 0 ? defaultMaxCaptureChars : DEFAULT_MAX_CAPTURE_CHARS;
    }

    private int resolveDefaultMaxCaptureLines() {
        return defaultMaxCaptureLines > 0 ? defaultMaxCaptureLines : DEFAULT_MAX_CAPTURE_LINES;
    }

    private String extractSuffix(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return ".tmp";
        }
        return fileName.substring(index);
    }

    private static String toCommandLine(List<String> command) {
        List<String> escaped = new ArrayList<>(command.size());
        for (String item : command) {
            if (item == null) {
                continue;
            }
            if (item.contains(" ") || item.contains("\"")) {
                escaped.add("\"" + item.replace("\"", "\\\"") + "\"");
            } else {
                escaped.add(item);
            }
        }
        return String.join(" ", escaped);
    }

    private static String limit(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private static final class StreamCollector implements Callable<StreamCapture> {
        private final InputStream inputStream;
        private final Charset charset;
        private final StreamCapture capture;

        private StreamCollector(InputStream inputStream, Charset charset, StreamCapture capture) {
            this.inputStream = inputStream;
            this.charset = charset;
            this.capture = capture;
        }

        @Override
        public StreamCapture call() throws Exception {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    capture.appendLine(line);
                }
            }
            return capture;
        }
    }

    private static final class StreamCapture {
        private final int maxChars;
        private final int maxLines;
        private final boolean stripAnsi;
        private final StringBuilder buffer = new StringBuilder();
        private final List<String> lines = new ArrayList<>();
        private boolean truncated;

        private StreamCapture(int maxChars, int maxLines, boolean stripAnsi) {
            this.maxChars = maxChars;
            this.maxLines = maxLines;
            this.stripAnsi = stripAnsi;
        }

        private synchronized void appendLine(String line) {
            String actualLine = stripAnsi ? ANSI_PATTERN.matcher(line).replaceAll("") : line;
            if (lines.size() < maxLines) {
                lines.add(actualLine);
            } else {
                truncated = true;
            }

            if (buffer.length() < maxChars) {
                String appendText = actualLine + System.lineSeparator();
                int remain = maxChars - buffer.length();
                if (appendText.length() <= remain) {
                    buffer.append(appendText);
                } else {
                    buffer.append(appendText, 0, remain);
                    truncated = true;
                }
            } else {
                truncated = true;
            }
        }

        private synchronized String toText() {
            return buffer.toString();
        }

        private synchronized List<String> getLines() {
            return Collections.unmodifiableList(new ArrayList<>(lines));
        }

        private synchronized boolean isTruncated() {
            return truncated;
        }
    }

    public static class BaiduPcsGoUploadRequest {
        private String executablePath;
        private String workingDirectory;
        private Long timeoutMillis;
        private String charsetName;
        private Integer maxCaptureChars;
        private Integer maxCaptureLines;
        private String description;
        private final List<String> globalArguments = new ArrayList<>();
        private final List<String> localPaths = new ArrayList<>();
        private String remoteTarget;
        private boolean disableRapidUpload;
        private final List<String> extraArguments = new ArrayList<>();
        private final Map<String, String> environment = new HashMap<>();
        private final Set<Integer> expectedExitCodes = new LinkedHashSet<>();
        private boolean failOnNonZeroExit = true;
        private boolean mergeErrorStream;
        private boolean stripAnsi = true;

        public static BaiduPcsGoUploadRequest builder() {
            return new BaiduPcsGoUploadRequest();
        }

        public BaiduPcsGoUploadRequest copy() {
            BaiduPcsGoUploadRequest request = new BaiduPcsGoUploadRequest();
            request.executablePath = this.executablePath;
            request.workingDirectory = this.workingDirectory;
            request.timeoutMillis = this.timeoutMillis;
            request.charsetName = this.charsetName;
            request.maxCaptureChars = this.maxCaptureChars;
            request.maxCaptureLines = this.maxCaptureLines;
            request.description = this.description;
            request.globalArguments.addAll(this.globalArguments);
            request.localPaths.addAll(this.localPaths);
            request.remoteTarget = this.remoteTarget;
            request.disableRapidUpload = this.disableRapidUpload;
            request.extraArguments.addAll(this.extraArguments);
            request.environment.putAll(this.environment);
            request.expectedExitCodes.addAll(this.expectedExitCodes);
            request.failOnNonZeroExit = this.failOnNonZeroExit;
            request.mergeErrorStream = this.mergeErrorStream;
            request.stripAnsi = this.stripAnsi;
            return request;
        }

        public BaiduPcsGoCommandRequest toCommandRequest(String fallbackExecutablePath) {
            List<String> arguments = new ArrayList<>();
            if (disableRapidUpload) {
                arguments.add("--norapid");
            }
            if (!CollectionUtils.isEmpty(extraArguments)) {
                arguments.addAll(extraArguments);
            }
            arguments.addAll(localPaths);
            arguments.add(remoteTarget);

            BaiduPcsGoCommandRequest request = new BaiduPcsGoCommandRequest();
            request.setExecutablePath(StringUtils.hasText(executablePath) ? executablePath : fallbackExecutablePath);
            request.setWorkingDirectory(workingDirectory);
            request.setTimeoutMillis(timeoutMillis);
            request.setCharsetName(charsetName);
            request.setMaxCaptureChars(maxCaptureChars);
            request.setMaxCaptureLines(maxCaptureLines);
            request.setDescription(StringUtils.hasText(description) ? description : "BaiduPCS-Go upload");
            request.setGlobalArguments(new ArrayList<>(globalArguments));
            request.setCommand("upload");
            request.setArguments(arguments);
            request.setEnvironment(new HashMap<>(environment));
            request.setExpectedExitCodes(expectedExitCodes.isEmpty()
                    ? new LinkedHashSet<>(Collections.singletonList(0))
                    : new LinkedHashSet<>(expectedExitCodes));
            request.setFailOnNonZeroExit(failOnNonZeroExit);
            request.setMergeErrorStream(mergeErrorStream);
            request.setStripAnsi(stripAnsi);
            return request;
        }

        public String getExecutablePath() {
            return executablePath;
        }

        public BaiduPcsGoUploadRequest setExecutablePath(String executablePath) {
            this.executablePath = executablePath;
            return this;
        }

        public String getWorkingDirectory() {
            return workingDirectory;
        }

        public BaiduPcsGoUploadRequest setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Long getTimeoutMillis() {
            return timeoutMillis;
        }

        public BaiduPcsGoUploadRequest setTimeoutMillis(Long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public String getCharsetName() {
            return charsetName;
        }

        public BaiduPcsGoUploadRequest setCharsetName(String charsetName) {
            this.charsetName = charsetName;
            return this;
        }

        public Integer getMaxCaptureChars() {
            return maxCaptureChars;
        }

        public BaiduPcsGoUploadRequest setMaxCaptureChars(Integer maxCaptureChars) {
            this.maxCaptureChars = maxCaptureChars;
            return this;
        }

        public Integer getMaxCaptureLines() {
            return maxCaptureLines;
        }

        public BaiduPcsGoUploadRequest setMaxCaptureLines(Integer maxCaptureLines) {
            this.maxCaptureLines = maxCaptureLines;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public BaiduPcsGoUploadRequest setDescription(String description) {
            this.description = description;
            return this;
        }

        public List<String> getGlobalArguments() {
            return Collections.unmodifiableList(globalArguments);
        }

        public BaiduPcsGoUploadRequest addGlobalArgument(String argument) {
            if (StringUtils.hasText(argument)) {
                this.globalArguments.add(argument);
            }
            return this;
        }

        public BaiduPcsGoUploadRequest setGlobalArguments(List<String> arguments) {
            this.globalArguments.clear();
            if (!CollectionUtils.isEmpty(arguments)) {
                this.globalArguments.addAll(arguments);
            }
            return this;
        }

        public List<String> getLocalPaths() {
            return Collections.unmodifiableList(localPaths);
        }

        public BaiduPcsGoUploadRequest addLocalPath(String localPath) {
            if (StringUtils.hasText(localPath)) {
                this.localPaths.add(localPath);
            }
            return this;
        }

        public BaiduPcsGoUploadRequest setLocalPaths(List<String> localPaths) {
            this.localPaths.clear();
            if (!CollectionUtils.isEmpty(localPaths)) {
                this.localPaths.addAll(localPaths);
            }
            return this;
        }

        public String getRemoteTarget() {
            return remoteTarget;
        }

        public BaiduPcsGoUploadRequest setRemoteTarget(String remoteTarget) {
            this.remoteTarget = remoteTarget;
            return this;
        }

        public boolean isDisableRapidUpload() {
            return disableRapidUpload;
        }

        public BaiduPcsGoUploadRequest setDisableRapidUpload(boolean disableRapidUpload) {
            this.disableRapidUpload = disableRapidUpload;
            return this;
        }

        public List<String> getExtraArguments() {
            return Collections.unmodifiableList(extraArguments);
        }

        public BaiduPcsGoUploadRequest addExtraArgument(String argument) {
            if (StringUtils.hasText(argument)) {
                this.extraArguments.add(argument);
            }
            return this;
        }

        public BaiduPcsGoUploadRequest setExtraArguments(List<String> extraArguments) {
            this.extraArguments.clear();
            if (!CollectionUtils.isEmpty(extraArguments)) {
                this.extraArguments.addAll(extraArguments);
            }
            return this;
        }

        public Map<String, String> getEnvironment() {
            return Collections.unmodifiableMap(environment);
        }

        public BaiduPcsGoUploadRequest putEnvironment(String key, String value) {
            if (StringUtils.hasText(key) && value != null) {
                this.environment.put(key, value);
            }
            return this;
        }

        public BaiduPcsGoUploadRequest setEnvironment(Map<String, String> environment) {
            this.environment.clear();
            if (environment != null) {
                this.environment.putAll(environment);
            }
            return this;
        }

        public Set<Integer> getExpectedExitCodes() {
            return Collections.unmodifiableSet(expectedExitCodes);
        }

        public BaiduPcsGoUploadRequest addExpectedExitCode(int exitCode) {
            this.expectedExitCodes.add(exitCode);
            return this;
        }

        public BaiduPcsGoUploadRequest setExpectedExitCodes(Set<Integer> expectedExitCodes) {
            this.expectedExitCodes.clear();
            if (expectedExitCodes != null) {
                this.expectedExitCodes.addAll(expectedExitCodes);
            }
            return this;
        }

        public boolean isFailOnNonZeroExit() {
            return failOnNonZeroExit;
        }

        public BaiduPcsGoUploadRequest setFailOnNonZeroExit(boolean failOnNonZeroExit) {
            this.failOnNonZeroExit = failOnNonZeroExit;
            return this;
        }

        public boolean isMergeErrorStream() {
            return mergeErrorStream;
        }

        public BaiduPcsGoUploadRequest setMergeErrorStream(boolean mergeErrorStream) {
            this.mergeErrorStream = mergeErrorStream;
            return this;
        }

        public boolean isStripAnsi() {
            return stripAnsi;
        }

        public BaiduPcsGoUploadRequest setStripAnsi(boolean stripAnsi) {
            this.stripAnsi = stripAnsi;
            return this;
        }

        public BaiduPcsGoUploadRequest build() {
            return this;
        }
    }

    public static class BaiduPcsGoInstallCheckRequest {
        private String executablePath;
        private String binaryName = "BaiduPCS-Go";
        private String workingDirectory;
        private Long timeoutMillis;
        private String charsetName;
        private String description;
        private final Map<String, String> environment = new HashMap<>();

        public static BaiduPcsGoInstallCheckRequest builder() {
            return new BaiduPcsGoInstallCheckRequest();
        }

        public String getExecutablePath() {
            return executablePath;
        }

        public BaiduPcsGoInstallCheckRequest setExecutablePath(String executablePath) {
            this.executablePath = executablePath;
            return this;
        }

        public String getBinaryName() {
            return binaryName;
        }

        public BaiduPcsGoInstallCheckRequest setBinaryName(String binaryName) {
            this.binaryName = binaryName;
            return this;
        }

        public String getWorkingDirectory() {
            return workingDirectory;
        }

        public BaiduPcsGoInstallCheckRequest setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Long getTimeoutMillis() {
            return timeoutMillis;
        }

        public BaiduPcsGoInstallCheckRequest setTimeoutMillis(Long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public String getCharsetName() {
            return charsetName;
        }

        public BaiduPcsGoInstallCheckRequest setCharsetName(String charsetName) {
            this.charsetName = charsetName;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public BaiduPcsGoInstallCheckRequest setDescription(String description) {
            this.description = description;
            return this;
        }

        public Map<String, String> getEnvironment() {
            return Collections.unmodifiableMap(environment);
        }

        public BaiduPcsGoInstallCheckRequest putEnvironment(String key, String value) {
            if (StringUtils.hasText(key) && value != null) {
                this.environment.put(key, value);
            }
            return this;
        }

        public BaiduPcsGoInstallCheckRequest setEnvironment(Map<String, String> environment) {
            this.environment.clear();
            if (environment != null) {
                this.environment.putAll(environment);
            }
            return this;
        }

        public BaiduPcsGoInstallCheckRequest build() {
            return this;
        }
    }

    public static class BaiduPcsGoInstallRequest {
        private String downloadUrl;
        private String archiveType = "tar.gz";
        private String executableName = "BaiduPCS-Go";
        private String finalBinaryName = "BaiduPCS-Go";
        private String binaryRelativePathInArchive;
        private String installDirectory = "/usr/local/lib/baidupcs-go";
        private String symlinkPath = "/usr/local/bin/BaiduPCS-Go";
        private boolean createSymlink = true;
        private boolean overwrite = true;
        private boolean useSudo;
        private String installerWorkspace;
        private String workingDirectory;
        private Long timeoutMillis;
        private String charsetName;
        private String description;
        private final Map<String, String> environment = new HashMap<>();

        public static BaiduPcsGoInstallRequest builder() {
            return new BaiduPcsGoInstallRequest();
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public BaiduPcsGoInstallRequest setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }

        public String getArchiveType() {
            return archiveType;
        }

        public BaiduPcsGoInstallRequest setArchiveType(String archiveType) {
            this.archiveType = archiveType;
            return this;
        }

        public String getExecutableName() {
            return executableName;
        }

        public BaiduPcsGoInstallRequest setExecutableName(String executableName) {
            this.executableName = executableName;
            return this;
        }

        public String getFinalBinaryName() {
            return finalBinaryName;
        }

        public BaiduPcsGoInstallRequest setFinalBinaryName(String finalBinaryName) {
            this.finalBinaryName = finalBinaryName;
            return this;
        }

        public String getBinaryRelativePathInArchive() {
            return binaryRelativePathInArchive;
        }

        public BaiduPcsGoInstallRequest setBinaryRelativePathInArchive(String binaryRelativePathInArchive) {
            this.binaryRelativePathInArchive = binaryRelativePathInArchive;
            return this;
        }

        public String getInstallDirectory() {
            return installDirectory;
        }

        public BaiduPcsGoInstallRequest setInstallDirectory(String installDirectory) {
            this.installDirectory = installDirectory;
            return this;
        }

        public String getSymlinkPath() {
            return symlinkPath;
        }

        public BaiduPcsGoInstallRequest setSymlinkPath(String symlinkPath) {
            this.symlinkPath = symlinkPath;
            return this;
        }

        public boolean isCreateSymlink() {
            return createSymlink;
        }

        public BaiduPcsGoInstallRequest setCreateSymlink(boolean createSymlink) {
            this.createSymlink = createSymlink;
            return this;
        }

        public boolean isOverwrite() {
            return overwrite;
        }

        public BaiduPcsGoInstallRequest setOverwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        public boolean isUseSudo() {
            return useSudo;
        }

        public BaiduPcsGoInstallRequest setUseSudo(boolean useSudo) {
            this.useSudo = useSudo;
            return this;
        }

        public String getInstallerWorkspace() {
            return installerWorkspace;
        }

        public BaiduPcsGoInstallRequest setInstallerWorkspace(String installerWorkspace) {
            this.installerWorkspace = installerWorkspace;
            return this;
        }

        public String getWorkingDirectory() {
            return workingDirectory;
        }

        public BaiduPcsGoInstallRequest setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Long getTimeoutMillis() {
            return timeoutMillis;
        }

        public BaiduPcsGoInstallRequest setTimeoutMillis(Long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public String getCharsetName() {
            return charsetName;
        }

        public BaiduPcsGoInstallRequest setCharsetName(String charsetName) {
            this.charsetName = charsetName;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public BaiduPcsGoInstallRequest setDescription(String description) {
            this.description = description;
            return this;
        }

        public Map<String, String> getEnvironment() {
            return Collections.unmodifiableMap(environment);
        }

        public BaiduPcsGoInstallRequest putEnvironment(String key, String value) {
            if (StringUtils.hasText(key) && value != null) {
                this.environment.put(key, value);
            }
            return this;
        }

        public BaiduPcsGoInstallRequest setEnvironment(Map<String, String> environment) {
            this.environment.clear();
            if (environment != null) {
                this.environment.putAll(environment);
            }
            return this;
        }

        public BaiduPcsGoInstallRequest build() {
            return this;
        }
    }

    public static class BaiduPcsGoCommandRequest {
        private String executablePath;
        private String workingDirectory;
        private Long timeoutMillis;
        private String charsetName;
        private Integer maxCaptureChars;
        private Integer maxCaptureLines;
        private String description;
        private List<String> globalArguments = new ArrayList<>();
        private String command;
        private List<String> arguments = new ArrayList<>();
        private Map<String, String> environment = new HashMap<>();
        private Set<Integer> expectedExitCodes = new LinkedHashSet<>(Collections.singletonList(0));
        private boolean failOnNonZeroExit = true;
        private boolean mergeErrorStream;
        private boolean stripAnsi = true;

        public String getExecutablePath() {
            return executablePath;
        }

        public void setExecutablePath(String executablePath) {
            this.executablePath = executablePath;
        }

        public String getWorkingDirectory() {
            return workingDirectory;
        }

        public void setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        public Long getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(Long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public String getCharsetName() {
            return charsetName;
        }

        public void setCharsetName(String charsetName) {
            this.charsetName = charsetName;
        }

        public Integer getMaxCaptureChars() {
            return maxCaptureChars;
        }

        public void setMaxCaptureChars(Integer maxCaptureChars) {
            this.maxCaptureChars = maxCaptureChars;
        }

        public Integer getMaxCaptureLines() {
            return maxCaptureLines;
        }

        public void setMaxCaptureLines(Integer maxCaptureLines) {
            this.maxCaptureLines = maxCaptureLines;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getGlobalArguments() {
            return globalArguments;
        }

        public void setGlobalArguments(List<String> globalArguments) {
            this.globalArguments = globalArguments;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public List<String> getArguments() {
            return arguments;
        }

        public void setArguments(List<String> arguments) {
            this.arguments = arguments;
        }

        public Map<String, String> getEnvironment() {
            return environment;
        }

        public void setEnvironment(Map<String, String> environment) {
            this.environment = environment;
        }

        public Set<Integer> getExpectedExitCodes() {
            return expectedExitCodes;
        }

        public void setExpectedExitCodes(Set<Integer> expectedExitCodes) {
            this.expectedExitCodes = expectedExitCodes;
        }

        public boolean isFailOnNonZeroExit() {
            return failOnNonZeroExit;
        }

        public void setFailOnNonZeroExit(boolean failOnNonZeroExit) {
            this.failOnNonZeroExit = failOnNonZeroExit;
        }

        public boolean isMergeErrorStream() {
            return mergeErrorStream;
        }

        public void setMergeErrorStream(boolean mergeErrorStream) {
            this.mergeErrorStream = mergeErrorStream;
        }

        public boolean isStripAnsi() {
            return stripAnsi;
        }

        public void setStripAnsi(boolean stripAnsi) {
            this.stripAnsi = stripAnsi;
        }
    }

    public static class BaiduPcsGoCommandResult {
        private final String description;
        private final List<String> command;
        private final String commandLine;
        private final String workingDirectory;
        private final int exitCode;
        private final boolean success;
        private final boolean timedOut;
        private final long durationMillis;
        private final Instant startedAt;
        private final Instant finishedAt;
        private final String stdout;
        private final List<String> stdoutLines;
        private final boolean stdoutTruncated;
        private final String stderr;
        private final List<String> stderrLines;
        private final boolean stderrTruncated;
        private final FailureHint failureHint;

        public BaiduPcsGoCommandResult(String description,
                                       List<String> command,
                                       String commandLine,
                                       String workingDirectory,
                                       int exitCode,
                                       boolean success,
                                       boolean timedOut,
                                       long durationMillis,
                                       Instant startedAt,
                                       Instant finishedAt,
                                       String stdout,
                                       List<String> stdoutLines,
                                       boolean stdoutTruncated,
                                       String stderr,
                                       List<String> stderrLines,
                                       boolean stderrTruncated,
                                       FailureHint failureHint) {
            this.description = description;
            this.command = command == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(command));
            this.commandLine = commandLine;
            this.workingDirectory = workingDirectory;
            this.exitCode = exitCode;
            this.success = success;
            this.timedOut = timedOut;
            this.durationMillis = durationMillis;
            this.startedAt = startedAt;
            this.finishedAt = finishedAt;
            this.stdout = stdout;
            this.stdoutLines = stdoutLines == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(stdoutLines));
            this.stdoutTruncated = stdoutTruncated;
            this.stderr = stderr;
            this.stderrLines = stderrLines == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(stderrLines));
            this.stderrTruncated = stderrTruncated;
            this.failureHint = failureHint == null ? FailureHint.UNKNOWN : failureHint;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getCommand() {
            return command;
        }

        public String getCommandLine() {
            return commandLine;
        }

        public String getWorkingDirectory() {
            return workingDirectory;
        }

        public int getExitCode() {
            return exitCode;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isTimedOut() {
            return timedOut;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public Instant getStartedAt() {
            return startedAt;
        }

        public Instant getFinishedAt() {
            return finishedAt;
        }

        public String getStdout() {
            return stdout;
        }

        public List<String> getStdoutLines() {
            return stdoutLines;
        }

        public boolean isStdoutTruncated() {
            return stdoutTruncated;
        }

        public String getStderr() {
            return stderr;
        }

        public List<String> getStderrLines() {
            return stderrLines;
        }

        public boolean isStderrTruncated() {
            return stderrTruncated;
        }

        public FailureHint getFailureHint() {
            return failureHint;
        }

        public String getCombinedOutput() {
            if (!StringUtils.hasText(stderr)) {
                return stdout;
            }
            if (!StringUtils.hasText(stdout)) {
                return stderr;
            }
            return stdout + System.lineSeparator() + stderr;
        }
    }

    public static class BaiduPcsGoInstallCheckResult {
        private final boolean installed;
        private final String resolvedBinaryPath;
        private final String versionOutput;
        private final BaiduPcsGoCommandResult commandResult;

        public BaiduPcsGoInstallCheckResult(boolean installed,
                                            String resolvedBinaryPath,
                                            String versionOutput,
                                            BaiduPcsGoCommandResult commandResult) {
            this.installed = installed;
            this.resolvedBinaryPath = resolvedBinaryPath;
            this.versionOutput = versionOutput;
            this.commandResult = commandResult;
        }

        public boolean isInstalled() {
            return installed;
        }

        public String getResolvedBinaryPath() {
            return resolvedBinaryPath;
        }

        public String getVersionOutput() {
            return versionOutput;
        }

        public BaiduPcsGoCommandResult getCommandResult() {
            return commandResult;
        }
    }

    public static class BaiduPcsGoInstallResult {
        private final String downloadUrl;
        private final String installDirectory;
        private final String installedBinaryPath;
        private final String symlinkPath;
        private final String versionOutput;
        private final BaiduPcsGoCommandResult installCommandResult;
        private final BaiduPcsGoInstallCheckResult checkResult;

        public BaiduPcsGoInstallResult(String downloadUrl,
                                       String installDirectory,
                                       String installedBinaryPath,
                                       String symlinkPath,
                                       String versionOutput,
                                       BaiduPcsGoCommandResult installCommandResult,
                                       BaiduPcsGoInstallCheckResult checkResult) {
            this.downloadUrl = downloadUrl;
            this.installDirectory = installDirectory;
            this.installedBinaryPath = installedBinaryPath;
            this.symlinkPath = symlinkPath;
            this.versionOutput = versionOutput;
            this.installCommandResult = installCommandResult;
            this.checkResult = checkResult;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public String getInstallDirectory() {
            return installDirectory;
        }

        public String getInstalledBinaryPath() {
            return installedBinaryPath;
        }

        public String getSymlinkPath() {
            return symlinkPath;
        }

        public String getVersionOutput() {
            return versionOutput;
        }

        public BaiduPcsGoCommandResult getInstallCommandResult() {
            return installCommandResult;
        }

        public BaiduPcsGoInstallCheckResult getCheckResult() {
            return checkResult;
        }
    }

    public static class BaiduPcsGoCommandException extends RuntimeException {
        private final BaiduPcsGoCommandRequest request;
        private final BaiduPcsGoCommandResult result;

        public BaiduPcsGoCommandException(String message,
                                          BaiduPcsGoCommandRequest request,
                                          BaiduPcsGoCommandResult result,
                                          Throwable cause) {
            super(message, cause);
            this.request = request;
            this.result = result;
        }

        public BaiduPcsGoCommandRequest getRequest() {
            return request;
        }

        public BaiduPcsGoCommandResult getResult() {
            return result;
        }
    }

    public enum FailureHint {
        NONE,
        TIMEOUT,
        COMMAND_NOT_FOUND,
        NOT_LOGGED_IN,
        LOCAL_FILE_NOT_FOUND,
        REMOTE_PATH_INVALID,
        FILE_ALREADY_EXISTS,
        PERMISSION_DENIED,
        NETWORK_ERROR,
        UNKNOWN;

        public static FailureHint detect(int exitCode, boolean timedOut, String stdout, String stderr) {
            if (timedOut) {
                return TIMEOUT;
            }
            if (exitCode == 0) {
                return NONE;
            }

            String text = ((stdout == null ? "" : stdout) + "\n" + (stderr == null ? "" : stderr))
                    .toLowerCase(Locale.ROOT);

            if (containsAny(text, "command not found", "not recognized as an internal or external command")) {
                return COMMAND_NOT_FOUND;
            }
            if (containsAny(text, "login", "need login", "not login", "please login")) {
                return NOT_LOGGED_IN;
            }
            if (containsAny(text, "no such file", "not found", "cannot find the file", "file does not exist")) {
                return LOCAL_FILE_NOT_FOUND;
            }
            if (containsAny(text, "invalid path", "remote path error", "directory does not exist", "target path")) {
                return REMOTE_PATH_INVALID;
            }
            if (containsAny(text, "already exists", "file exists")) {
                return FILE_ALREADY_EXISTS;
            }
            if (containsAny(text, "permission denied", "forbidden", "access denied")) {
                return PERMISSION_DENIED;
            }
            if (containsAny(text, "timeout", "timed out", "connection reset", "network", "i/o timeout", "eof")) {
                return NETWORK_ERROR;
            }
            return UNKNOWN;
        }

        private static boolean containsAny(String text, String... keywords) {
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
    }
}

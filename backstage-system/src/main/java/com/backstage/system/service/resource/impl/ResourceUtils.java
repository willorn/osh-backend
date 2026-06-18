package com.backstage.system.service.resource.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

public class ResourceUtils {

    static final Logger logger = LoggerFactory.getLogger(ResourceUtils.class);

    public static String getExtension(String originalFileName) {
        // 提取扩展名
        String extension = "";
        int extIndex = originalFileName.lastIndexOf('.');
        if (extIndex > 0) {
            extension = originalFileName.substring(extIndex);
        }
        return extension;
    }

    /**
     * 根据文件名获取Content-Type
     */
    public static String getContentTypeByFileName(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        String extension = "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = fileName.substring(lastDotIndex + 1).toLowerCase();
        }

        switch (extension) {
            // 文本文件
            case "txt":
                return "text/plain";
            case "csv":
                return "text/csv";
            case "html":
            case "htm":
                return "text/html";
            case "xml":
                return "text/xml";
            case "json":
                return "application/json";

            // 文档
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";

            // 图片
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "svg":
                return "image/svg+xml";
            case "webp":
                return "image/webp";

            // 视频
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            case "mov":
                return "video/quicktime";
            case "wmv":
                return "video/x-ms-wmv";
            case "flv":
                return "video/x-flv";
            case "mkv":
                return "video/x-matroska";

            // 音频
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "aac":
                return "audio/aac";
            case "flac":
                return "audio/flac";

            // 压缩文件
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            case "7z":
                return "application/x-7z-compressed";
            case "tar":
                return "application/x-tar";
            case "gz":
                return "application/gzip";

            // 代码文件
            case "js":
                return "application/javascript";
            case "ts":
                return "application/typescript";
            case "java":
                return "text/x-java-source";
            case "py":
                return "text/x-python";
            case "c":
            case "cpp":
                return "text/x-c";
            case "css":
                return "text/css";
            case "sql":
                return "text/x-sql";

            // 默认
            default:
                return "application/octet-stream";
        }
    }

    public static void writeToResponse(String fileName, InputStream inputStream, HttpServletResponse response) throws IOException {
        String contentType = getContentTypeByFileName(fileName);
        // 设置响应头
        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");
        // 处理文件名（支持中文）
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        // 写入响应流
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            response.getOutputStream().write(buffer, 0, bytesRead);
        }
        response.getOutputStream().flush();
        inputStream.close();
    }

    public static void writeError(HttpServletResponse response, Throwable throwable) {
        try {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("文件下载失败：" + throwable.getMessage());
        } catch (Exception ex) {
            logger.error("文件下载失败：{}", throwable.getMessage(), throwable);
        }
    }
}

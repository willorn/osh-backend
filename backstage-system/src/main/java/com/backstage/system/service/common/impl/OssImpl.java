package com.backstage.system.service.common.impl;

import com.backstage.common.enums.UploadPathEnum;
import com.backstage.common.utils.DateUtils;
import com.backstage.common.utils.ServletUtils;
import com.backstage.common.utils.ip.IpUtils;
import com.backstage.system.constants.CourseUploadConstants;
import com.backstage.system.domain.vo.common.OssOperationLogVo;
import com.backstage.system.exception.UpLoadException;
import com.backstage.system.mapper.common.OssMapper;
import com.backstage.system.service.common.OssService;
import com.backstage.system.utils.OssUtil;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;



@Service
public class OssImpl implements OssService {

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private OssMapper ossMapper;

    @Autowired
    private OssService ossService;


    // 需要OSS的文件路径，怎么存的就怎么从数据库里面取
    /**
     * @param path oss存储路径如 common/image/avatar/202604/12345asdaasd_file.jpg
     * @param minute 分钟数，表示URL的有效期
     * @return 临时访问URL
    */
    public String getLimitedUrl(String path, int minute) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return ossUtil.getSignedUrl(path, minute);
    }


    // TODO
    // enum 固定路径类型 course_code.zip 7z

    /**
     * 上传文件
     * @param file  文件
     * @param pathEnum  枚举路径固定
     * @return oss服务的文件路径
     * @throws Exception
     */
    public String upload(MultipartFile file, UploadPathEnum pathEnum, String id) throws UpLoadException, Exception {


        String customPath;
        if(id== null){
            id="";
        }else {
            id=id+"/";
        }

        // 获取年月
        String ym = DateUtils.getDate().substring(0,7).replace("-", "");;


        if(UploadPathEnum.IMAGE.equals(pathEnum)){
            customPath = UploadPathEnum.IMAGE.getPath()+id+ym+"/";
            if(file.getSize() > 1024 * 1024 * 3){
                return "图片大小不能超过3M";
            }

        }else if(UploadPathEnum.COURSE_VIDEO.equals(pathEnum)){
            customPath = UploadPathEnum.COURSE_VIDEO.getPath()+id+ym+"/";
            if(file.getSize() > CourseUploadConstants.MAX_VIDEO_SIZE){
                return CourseUploadConstants.VIDEO_SIZE_ERROR;
            }
        }else if(UploadPathEnum.COURSE_MATERIAL.equals(pathEnum)){
            customPath = UploadPathEnum.COURSE_MATERIAL.getPath()+id+ym+"/";
            if(file.getSize() > 1024 * 1024 * 100){
                return "资料大小不能超过100MB";
            }
        }else if(UploadPathEnum.COURSE_COVER.equals(pathEnum)){
            customPath = UploadPathEnum.COURSE_COVER.getPath()+id+ym+"/";
            if(file.getSize() > 1024 * 1024 * 5){
                return "封面图片大小不能超过5MB";
            }
        }else if(UploadPathEnum.TOOL_COVER.equals(pathEnum)){
            customPath = UploadPathEnum.TOOL_COVER.getPath()+id+ym+"/";
            if(file.getSize() > 1024 * 1024 * 5){
                return "封面图片大小不能超过5MB";
            }
        }else if(UploadPathEnum.COURSE_MATERIAL.equals(pathEnum)){
            customPath = UploadPathEnum.COURSE_MATERIAL.getPath()+id+ym+"/";
            if(file.getSize() > 1024 * 1024 * 100){
                return "资料文件大小不能超过100MB";
            }
        } else if(UploadPathEnum.AVATAR.equals(pathEnum)){
            customPath = UploadPathEnum.AVATAR.getPath()+id+ym+"/";
            if(file.getSize() > 1024 * 1024 * 3){
                return "头像大小不能超过3MB";
            }
        } else {
            return "类型不正确";
        }


        return ossUtil.uploadFile(file, customPath);
    }



    public boolean existsFileKey(String fileKey) {
        if (fileKey == null || fileKey.trim().isEmpty()) {
            return false;
        }

        LambdaQueryWrapper<OssOperationLogVo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OssOperationLogVo::getFileKey, fileKey);
        return ossMapper.exists(wrapper);
    }



    public int incrementOperationCount(String fileKey) {
        if (fileKey == null || fileKey.trim().isEmpty()) {
            return 0;
        }

        // 直接使用 updateWrapper 让 operation_count + 1
        LambdaUpdateWrapper<OssOperationLogVo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OssOperationLogVo::getFileKey, fileKey)
                .setSql("operation_count = operation_count + 1");

        return ossMapper.update(null, updateWrapper);
    }


    public void insertMapper(MultipartFile file, String customPath) {

        OssOperationLogVo log = new OssOperationLogVo();

        if (!ossService.existsFileKey(file.getOriginalFilename())) {
            // 获取浏览器 OshUser-Agent
            UserAgent userAgent = UserAgent.parseUserAgentString(
                    ServletUtils.getRequest().getHeader("OshUser-Agent")
            );
            // 获取客户端IP
            // getRemoteAddr
            String ip = IpUtils.getIpAddr();
            // 原始文件名
            log.setOriginalName(file.getOriginalFilename());
            log.setFileKey(customPath);
            // 文件后缀
            log.setFileSuffix(file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1, file.getOriginalFilename().length()));
            // 文件大小字节
            log.setFileSize(file.getSize() / 1024 / 1024);
            log.setFileType(file.getContentType());
            log.setOperationType("upload");
            log.setOperationCount(1);
            try {
                if (UserContextUtil.getCurrentUser().getUsername()!= null)
                    log.setUsername(UserContextUtil.getCurrentUser().getUsername());
            } catch (Exception e) {
                log.setUsername("");
            }

            log.setIp(ip);
            // 浏览器的userAgent
            log.setUserAgent(userAgent.toString());
            log.setBucket(ossUtil.getOssProperties().getBucketName());

        }else {
            ossService.incrementOperationCount(file.getOriginalFilename());
        }


        ossMapper.insert(log);
    }


}

package com.backstage.system.controller.user;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.user.dto.*;
import com.backstage.system.domain.user.vo.OshUserLoginVO;
import com.backstage.system.request.UserListRequest;
import com.backstage.system.service.user.IOshUserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/3/7
 * Time: 16:37
 */
@RestController
@RequestMapping("/pc/user")
public class OshUserController extends BaseController {

    private final IOshUserService userService;

    @Autowired
    public OshUserController(IOshUserService userService) {
        this.userService = userService;
    }

    @ApiOperation("账号登录")
    @PostMapping("/login")
    @OshUserEvent(module = "用户模块", actionType = "登录", description = "用户登录")
    @Anonymous
    public R<OshUserLoginVO> login(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid,
            @RequestBody UserLoginDTO userLoginDTO) {
        return userService.login(userLoginDTO.getUsername(),userLoginDTO.getPassword());
    }

    @ApiOperation("注册请求")
    @PostMapping("/register/submit")
    @OshUserEvent(module = "用户模块", actionType = "注册", description = "用户提交注册请求")
    @Anonymous
    public R<String> registerSubmit(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid,
            @RequestBody UserRegisterDTO userRegisterDTO) throws MessagingException {
        return userService.registerSubmit(userRegisterDTO.getUsername(),userRegisterDTO.getPassword(),userRegisterDTO.getRepassword(),userRegisterDTO.getEmail(),userRegisterDTO.getInviteCode());
    }

    @ApiOperation("账号注册")
    @PostMapping("/register/verity")
    @OshUserEvent(module = "用户模块", actionType = "注册", description = "验证用户注册")
    @Anonymous
    public R<String> registerVerity(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid,
            @ApiParam("用户的唯一标识") @RequestBody VerityRequestDTO verityRequestDTO) {
        return userService.registerVerity(verityRequestDTO.getUniqueId());
    }

    @ApiOperation("退出登录")
    @PostMapping("/logout")
    @PreAuthorize("hasAuthority('user:logout')")
    @OshUserEvent(module = "用户模块", actionType = "登出", description = "用户登出")
    public R<String> logout(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid) {
        return userService.logout();
    }

    @ApiOperation("改绑邮箱请求 根据唯一标识改绑邮箱")
    @PostMapping("/changeEmail/submit")
    @PreAuthorize("hasAuthority('user:email:change:submit')")
    @OshUserEvent(module = "用户模块", actionType = "修改邮箱", description = "用户提交修改邮箱请求")
    public R<String> changeEmailSubmit(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid,
            @RequestBody UserChangeEmailDTO userChangeEmailDTO) throws MessagingException {
        return userService.changeEmailSubmit(userChangeEmailDTO.getUniqueId(), userChangeEmailDTO.getNewEmail());
    }

    @ApiOperation("改绑邮箱验证")
    @PostMapping("/changeEmail/verity")
    @PreAuthorize("hasAuthority('user:email:change:verity')")
    @OshUserEvent(module = "用户模块", actionType = "修改邮箱", description = "用户验证修改邮箱")
    public R<String> changeEmailVerity(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid,
            @ApiParam("用户的唯一标识") @RequestBody VerityRequestDTO verityRequestDTO) {
        return userService.changeEmailVerity(verityRequestDTO.getUniqueId());
    }

    @ApiOperation("找回密码")
    @PostMapping("/forget")
    @OshUserEvent(module = "用户模块", actionType = "找回密码", description = "用户找回密码")
    @Anonymous
    public R<String> forget(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid,
            @RequestBody UserForgetDTO userForgetDTO) {
        return userService.forget(userForgetDTO.getUniqueId(), userForgetDTO.getPassword(), userForgetDTO.getRepassword());
    }

    @ApiOperation("修改资料")
    @PostMapping("/update_info")
    @PreAuthorize("hasAuthority('user:info:update')")
    @OshUserEvent(module = "用户模块", actionType = "修改资料", description = "用户修改资料")
    public R<String> updateInfo(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid,
            @RequestBody UserUpdateInfoDTO userUpdateInfoDTO) {
        return userService.updateInfo(userUpdateInfoDTO.getUsername(),userUpdateInfoDTO.getSex(),userUpdateInfoDTO.getIntroduction());
    }

    @ApiOperation("上传头像")
    @PostMapping("/upload_avatar")
    @PreAuthorize("hasAuthority('user:info:update')")
    public R<String> uploadAvatar(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid,
            @RequestParam("file") MultipartFile file) {
        return userService.uploadAvatar(file);
    }

    @ApiOperation("修改密码")
    @PostMapping("/update_password")
    @PreAuthorize("hasAuthority('user:password:update')")
    @OshUserEvent(module = "用户模块", actionType = "修改密码", description = "用户修改密码")
    public R<String> updatePassword(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid,
            @RequestBody UserPasswordDTO userPasswordDTO) {
        return userService.updatePassword(userPasswordDTO.getOpassword(),userPasswordDTO.getPassword(),userPasswordDTO.getRepassword());
    }

    @ApiOperation("获取用户信息")
    @GetMapping("/getinfo")
    @OshUserEvent(module = "用户模块", actionType = "查询", description = "获取用户信息")
    public R<OshUser> getUserInfo(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid) {
        return userService.getUserInfo();
    }

    @ApiOperation("获取当前用户角色列表（含有效期）")
    @GetMapping("/roles")
    public R<?> getUserRoles(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid) {
        return userService.getUserRoles();
    }

    @ApiOperation("注销用户")
    @PostMapping("/deleteUser")
    @PreAuthorize("hasAuthority('user:delete')")
    @OshUserEvent(module = "用户模块", actionType = "注销用户", description = "注销用户")
    public R<String> deleteUser(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid) {
        return userService.deleteUser();
    }

    @PostMapping("/asset/update")
    @PreAuthorize("hasAuthority('user:asset:update')")
    @OshUserEvent(module = "用户模块", actionType = "更新资产", description = "用户更新资产")
    public R<String> updateAsset(
            @ApiParam("网校 appid") @RequestHeader(value = "appid", required = false) String appid,
            @RequestBody UserAssetDTO userAssetDTO) {
        return userService.updateAsset(userAssetDTO.getChangeType(), userAssetDTO.getChangeSource(), userAssetDTO.getChangeAmount(), userAssetDTO.getRemark());
    }

    /**
     * 查询用户列表
     */
    @GetMapping("/all")
    public R<List<OshUser>> list(UserListRequest req) {
        return R.ok(userService.selectUserList(req));
    }
}
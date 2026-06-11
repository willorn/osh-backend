package com.backstage.system.service.user;

import com.backstage.common.core.domain.R;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.user.vo.OshUserLoginVO;
import com.backstage.system.request.UserListRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/3/7
 * Time: 16:42
 */
public interface IOshUserService {
    R<OshUserLoginVO> login(String username, String password);

    R<String> registerSubmit(String username, String password, String repassword, String email) throws MessagingException;

    R<String> registerSubmit(String username, String password, String repassword, String email, String inviteCode) throws MessagingException;

    R<String> registerVerity(String uniqueId);

    R<String> logout();

    R<String> changeEmailSubmit(String uniqueId, String newEmail) throws MessagingException;

    R<String> changeEmailVerity(String uniqueId);

    R<String> forget(String uniqueId, String password, String repassword);

    R<String> updateInfo(String username, String sex, String introduction);

    R<String> uploadAvatar(MultipartFile file);

    R<String> updatePassword(String opassword, String password, String repassword);

    R<OshUser> getUserInfo();

    R<?> getUserRoles();

    R<String> deleteUser();

    R<String> updateAsset(Integer changeType, Integer changeSource, Long changeAmount, String remark);

    List<OshUser> selectUserList(UserListRequest req);
}

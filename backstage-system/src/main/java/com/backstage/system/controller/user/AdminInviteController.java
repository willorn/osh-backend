package com.backstage.system.controller.user;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.utils.SecurityUtils;
import com.backstage.common.utils.StringUtils;
import com.backstage.common.utils.generate.GenerateUtil;
import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.user.OshRole;
import com.backstage.system.domain.user.OshUserAsset;
import com.backstage.system.mapper.user.OshRoleMapper;
import com.backstage.system.mapper.user.OshUserAssetMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.mapper.user.UserManageMapper;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.servlet.http.HttpServletRequest;
import java.net.URL;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 邀请用户注册（仅创始人 level=6 可用）
 * 可邀请任意角色的用户（普通用户到核心开发者，level 1~5）
 */
@RestController
@RequestMapping("/pc/admin/invite")
public class AdminInviteController {

    private static final Logger log = LoggerFactory.getLogger(AdminInviteController.class);

    /** Redis key 前缀 */
    private static final String INVITE_KEY_PREFIX = "admin:invite:";
    /** 邮箱待注册邀请索引前缀 */
    private static final String INVITE_EMAIL_KEY_PREFIX = "admin:invite:email:";
    /** 邀请链接有效期：7天 */
    private static final long INVITE_EXPIRE_DAYS = 7;

    @Resource
    private RedisCache redisCache;
    @Resource
    private OshUserMapper oshUserMapper;
    @Resource
    private OshUserAssetMapper oshUserAssetMapper;
    @Resource
    private UserManageMapper userManageMapper;
    @Resource
    private OshRoleMapper oshRoleMapper;
    @Resource
    private JavaMailSender javaMailSender;

    private static final String MAIL_FROM = "18482663265@163.com";

    /**
     * 创建用户邀请（创始人专属）
     * @param params: email, roleId (任意有效角色ID，level 2~5)
     */
    @PostMapping("/create")
    @OshUserLevel(value = 6)
    public R createInvite(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        return createSingleInvite(params, request);
    }

    @PostMapping("/batch")
    @OshUserLevel(value = 6)
    public R createBatchInvite(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        List<Map<String, Object>> inviteItems = parseInviteItems(params);
        if (inviteItems.isEmpty()) {
            return R.fail("邮箱不能为空");
        }

        List<LinkedHashMap<String, Object>> items = new ArrayList<>();
        int successCount = 0;
        for (Map<String, Object> inviteItem : inviteItems) {
            String email = String.valueOf(inviteItem.get("email"));
            Map<String, Object> singleParams = new HashMap<>(params);
            singleParams.putAll(inviteItem);
            singleParams.remove("emails");
            singleParams.remove("items");

            R singleResult = createSingleInvite(singleParams, request);
            boolean success = R.SUCCESS == singleResult.getCode();
            if (success) {
                successCount++;
            }

            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("email", email);
            item.put("success", success);
            item.put("message", singleResult.getMsg());
            if (success) {
                item.put("data", singleResult.getData());
            }
            items.add(item);
        }

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("total", inviteItems.size());
        data.put("successCount", successCount);
        data.put("failCount", inviteItems.size() - successCount);
        data.put("items", items);
        return R.ok(data);
    }

    private R createSingleInvite(Map<String, Object> params, HttpServletRequest request) {
        String email = normalizeEmail(params.get("email"));
        Integer roleId = Integer.valueOf(params.get("roleId").toString());
        // 自定义积分，默认188
        Long points = 188L;
        if (params.get("points") != null) {
            points = Long.valueOf(params.get("points").toString());
            if (points < 0) points = 0L;
        }

        if (StringUtils.isEmpty(email)) {
            return R.fail("邮箱不能为空");
        }

        // 邮箱格式校验
        if (!isValidEmailFormat(email)) {
            return R.fail("邮箱格式不正确");
        }
        if (!hasMailExchange(email)) {
            return R.fail("邮箱域名不存在或未配置邮件服务");
        }

        // 校验角色ID有效性（只允许 level 2~5 的角色）
        OshRole role = findRoleById(roleId);
        if (role == null) {
            return R.fail("角色不存在");
        }
        if (role.getLevel() == null || role.getLevel() < 2 || role.getLevel() > 5) {
            return R.fail("不能邀请该等级的角色");
        }

        // VIP用户(role_id=3)和小班用户(role_id=4)需要指定有效期
        String expireTime = null;
        if (roleId == 3 || roleId == 4) {
            Object expireObj = params.get("expireTime");
            Boolean permanent = params.get("permanent") != null && Boolean.parseBoolean(params.get("permanent").toString());
            if (permanent) {
                expireTime = "2099-12-31 23:59:59";
            } else if (expireObj != null && !expireObj.toString().isEmpty()) {
                expireTime = expireObj.toString();
            } else {
                return R.fail("VIP用户和小班用户角色需要指定有效期");
            }
        }

        // 检查邮箱是否已存在
        LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getEmail, email);
        if (oshUserMapper.selectOne(wrapper) != null) {
            return R.fail("邮箱已被注册");
        }
        if (hasPendingInvite(email)) {
            return R.fail("该邮箱已有未使用的邀请链接");
        }

        // 生成邀请 token
        String inviteToken = UUID.randomUUID().toString().replace("-", "");

        // 存入 Redis
        Map<String, String> inviteData = new HashMap<>();
        inviteData.put("email", email);
        inviteData.put("roleId", roleId.toString());
        inviteData.put("points", points.toString());
        inviteData.put("inviterId", UserContextUtil.getCurrentUserId().toString());
        if (expireTime != null) {
            inviteData.put("expireTime", expireTime);
        }
        redisCache.setCacheObject(INVITE_KEY_PREFIX + inviteToken, inviteData, (int)(INVITE_EXPIRE_DAYS * 24 * 60), TimeUnit.MINUTES);
        redisCache.setCacheObject(INVITE_EMAIL_KEY_PREFIX + email, inviteToken, (int)(INVITE_EXPIRE_DAYS * 24 * 60), TimeUnit.MINUTES);

        // 发送邮件通知被邀请人
        String roleName = role.getRoleName();
        // 动态获取前端域名：优先从 Origin/Referer 头获取
        String origin = request.getHeader("Origin");
        if (StringUtils.isEmpty(origin)) {
            String referer = request.getHeader("Referer");
            if (StringUtils.isNotEmpty(referer)) {
                try {
                    URL url = new URL(referer);
                    origin = url.getProtocol() + "://" + url.getHost() + (url.getPort() > 0 && url.getPort() != 80 && url.getPort() != 443 ? ":" + url.getPort() : "");
                } catch (Exception e) {
                    origin = "http://juegeresource.top";
                }
            } else {
                origin = "http://juegeresource.top";
            }
        }
        String inviteLink = origin + "/admin-register?token=" + inviteToken;
        String subject = "您被邀请成为平台" + roleName;
        String content = "您好，\n\n"
                + "您已被邀请注册为平台「" + roleName + "」。\n"
                + "请点击以下链接完成注册（7天内有效）：\n\n"
                + inviteLink + "\n\n"
                + "如非本人操作，请忽略此邮件。";
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(MAIL_FROM);
            mail.setTo(email);
            mail.setSubject(subject);
            mail.setText(content);
            javaMailSender.send(mail);
        } catch (Exception e) {
            log.warn("发送邀请邮件失败, email={}", email, e);
            redisCache.deleteObject(INVITE_KEY_PREFIX + inviteToken);
            redisCache.deleteObject(INVITE_EMAIL_KEY_PREFIX + email);
            return R.fail("邀请邮件发送失败");
        }

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("inviteToken", inviteToken);
        data.put("inviteLink", inviteLink);
        data.put("expireDays", INVITE_EXPIRE_DAYS);
        return R.ok(data);
    }

    private List<String> parseInviteEmails(Map<String, Object> params) {
        LinkedHashSet<String> emails = new LinkedHashSet<>();
        Object emailsObj = params.get("emails");
        if (emailsObj instanceof Collection) {
            for (Object item : (Collection<?>) emailsObj) {
                addInviteEmail(emails, item);
            }
        } else {
            addInviteEmail(emails, emailsObj);
        }
        addInviteEmail(emails, params.get("email"));
        return new ArrayList<>(emails);
    }

    private List<Map<String, Object>> parseInviteItems(Map<String, Object> params) {
        List<Map<String, Object>> inviteItems = new ArrayList<>();
        Object itemsObj = params.get("items");
        if (itemsObj instanceof Collection) {
            for (Object itemObj : (Collection<?>) itemsObj) {
                if (!(itemObj instanceof Map)) {
                    continue;
                }
                Map<?, ?> source = (Map<?, ?>) itemObj;
                Object emailObj = source.get("email");
                if (emailObj == null || StringUtils.isEmpty(emailObj.toString().trim())) {
                    continue;
                }
                LinkedHashSet<String> emails = new LinkedHashSet<>();
                addInviteEmail(emails, emailObj);
                for (String email : emails) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("email", email);
                    copyInviteItemValue(source, item, "roleId");
                    copyInviteItemValue(source, item, "points");
                    copyInviteItemValue(source, item, "permanent");
                    copyInviteItemValue(source, item, "expireTime");
                    inviteItems.add(item);
                }
            }
            return inviteItems;
        }

        for (String email : parseInviteEmails(params)) {
            Map<String, Object> item = new HashMap<>();
            item.put("email", email);
            inviteItems.add(item);
        }
        return inviteItems;
    }

    private void copyInviteItemValue(Map<?, ?> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private String normalizeEmail(Object value) {
        return value == null ? "" : value.toString().trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidEmailFormat(String email) {
        return email.matches("^[a-zA-Z0-9._%+\\-]+@([a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$");
    }

    private boolean hasMailExchange(String email) {
        int atIndex = email.lastIndexOf("@");
        if (atIndex < 0 || atIndex == email.length() - 1) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        try {
            Attributes mx = new InitialDirContext().getAttributes("dns:/" + domain, new String[] { "MX" });
            if (mx.get("MX") != null && mx.get("MX").size() > 0) {
                return true;
            }
            Attributes a = new InitialDirContext().getAttributes("dns:/" + domain, new String[] { "A", "AAAA" });
            return (a.get("A") != null && a.get("A").size() > 0) || (a.get("AAAA") != null && a.get("AAAA").size() > 0);
        } catch (NamingException e) {
            log.info("邮箱域名解析失败, email={}, domain={}", email, domain, e);
            return false;
        }
    }

    private boolean hasPendingInvite(String email) {
        String inviteToken = redisCache.getCacheObject(INVITE_EMAIL_KEY_PREFIX + email);
        if (StringUtils.isNotEmpty(inviteToken)) {
            Map<String, String> inviteData = redisCache.getCacheObject(INVITE_KEY_PREFIX + inviteToken);
            if (inviteData != null && email.equals(normalizeEmail(inviteData.get("email")))) {
                return true;
            }
            redisCache.deleteObject(INVITE_EMAIL_KEY_PREFIX + email);
        }

        Collection<String> keys = redisCache.keys(INVITE_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return false;
        }
        for (String key : keys) {
            if (key.startsWith(INVITE_EMAIL_KEY_PREFIX)) {
                continue;
            }
            Map<String, String> inviteData = redisCache.getCacheObject(key);
            if (inviteData != null && email.equals(normalizeEmail(inviteData.get("email")))) {
                String token = key.substring(INVITE_KEY_PREFIX.length());
                redisCache.setCacheObject(INVITE_EMAIL_KEY_PREFIX + email, token, (int)(INVITE_EXPIRE_DAYS * 24 * 60), TimeUnit.MINUTES);
                return true;
            }
        }
        return false;
    }

    private void addInviteEmail(Set<String> emails, Object value) {
        if (value == null) {
            return;
        }
        String[] parts = value.toString().split("[\\s,;，；]+");
        for (String part : parts) {
            String email = normalizeEmail(part);
            if (StringUtils.isNotEmpty(email)) {
                emails.add(email);
            }
        }
    }

    /**
     * 查询邀请信息（被邀请人打开链接时调用，匿名可访问）
     */
    @Anonymous
    @GetMapping("/info")
    public R getInviteInfo(@RequestParam String token) {
        Map<String, String> inviteData = redisCache.getCacheObject(INVITE_KEY_PREFIX + token);
        if (inviteData == null) {
            return R.fail("邀请链接无效或已过期");
        }
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("email", inviteData.get("email"));
        Integer roleId = Integer.valueOf(inviteData.get("roleId"));
        OshRole role = findRoleById(roleId);
        data.put("roleName", role != null ? role.getRoleName() : "未知角色");
        return R.ok(data);
    }

    /**
     * 被邀请人确认注册（匿名可访问）
     */
    @Anonymous
    @PostMapping("/confirm")
    public R confirmInvite(@RequestBody Map<String, Object> params) {
        String token = (String) params.get("token");
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String repassword = (String) params.get("repassword");

        if (StringUtils.isEmpty(token) || StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return R.fail("参数不完整");
        }
        if (!password.equals(repassword)) {
            return R.fail("两次密码不一致");
        }

        // 用户名格式校验
        if (!username.matches(OshUserConstants.USERNAME_PATTERN)) {
            return R.fail("用户名必须是4-20位字母、数字、下划线组成，且以字母开头");
        }

        // 从 Redis 获取邀请数据
        Map<String, String> inviteData = redisCache.getCacheObject(INVITE_KEY_PREFIX + token);
        if (inviteData == null) {
            return R.fail("邀请链接无效或已过期");
        }

        String email = inviteData.get("email");
        Integer roleId = Integer.valueOf(inviteData.get("roleId"));

        // 检查用户名和邮箱
        LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getUsername, username);
        if (oshUserMapper.selectOne(wrapper) != null) {
            return R.fail("用户名已被注册");
        }
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getEmail, email);
        if (oshUserMapper.selectOne(wrapper) != null) {
            return R.fail("邮箱已被注册");
        }

        // 创建用户
        Long userId = GenerateUtil.generateSnowflakeId();
        OshUser oshUser = new OshUser();
        oshUser.setId(userId);
        oshUser.setUsername(username);
        oshUser.setPassword(SecurityUtils.encryptPassword(password));
        oshUser.setEmail(email);
        oshUser.setInviteCode(generateUniqueInviteCode());
        oshUser.setDeleteFlag((byte) 0);
        ThreadLocalUtil.set(OshUserConstants.USER_ID, userId);
        oshUserMapper.insert(oshUser);

        // 生成唯一标识（与正常注册格式一致：UUID-时间戳）
        String uniqueId = UUID.randomUUID() + "-" + System.currentTimeMillis();
        oshUserMapper.addUniqueId(userId, uniqueId);

        // 添加普通用户角色（默认）
        oshUserMapper.addRole(userId);

        // 添加邀请指定的角色（如果不是普通用户角色）
        Long operatorId = Long.valueOf(inviteData.get("inviterId"));
        if (roleId != 1) {
            // 从邀请数据中读取有效期（创建邀请时已指定）
            String expireTime = inviteData.get("expireTime");
            userManageMapper.insertUserRole(userId, roleId, operatorId, expireTime);
        }

        // 初始化用户资产（使用邀请时设定的积分，默认188）
        Long points = 188L;
        if (inviteData.get("points") != null) {
            points = Long.valueOf(inviteData.get("points"));
        }
        OshUserAsset oshUserAsset = new OshUserAsset();
        oshUserAsset.setPoints(points);
        oshUserAsset.setUserId(userId);
        oshUserAssetMapper.insert(oshUserAsset);

        // 删除 Redis 邀请数据
        redisCache.deleteObject(INVITE_KEY_PREFIX + token);
        redisCache.deleteObject(INVITE_EMAIL_KEY_PREFIX + normalizeEmail(email));

        // 发送注册成功邮件
        try {
            OshRole role = findRoleById(roleId);
            String roleName = role != null ? role.getRoleName() : "用户";
            String successContent = "恭喜 " + username + "，\n\n"
                    + "您已成功注册为平台「" + roleName + "」。\n"
                    + "您的唯一标识为：" + uniqueId + "\n"
                    + "请妥善保管，用于找回密码等操作。\n\n"
                    + "现在可以使用用户名和密码登录平台。";
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(MAIL_FROM);
            mail.setTo(email);
            mail.setSubject("注册成功通知");
            mail.setText(successContent);
            javaMailSender.send(mail);
        } catch (Exception e) {
            log.warn("发送注册成功邮件失败, email={}", email, e);
        }

        return R.ok("注册成功");
    }

    /**
     * 查询可邀请的角色列表（level 2~5）
     */
    @GetMapping("/roles")
    @OshUserLevel(value = 6)
    public R getInviteRoles() {
        List<Map<String, Object>> roles = userManageMapper.selectAssignableRoles(5);
        // 过滤掉普通用户角色（level=1）
        roles.removeIf(r -> {
            Object level = r.get("roleLevel");
            return level != null && Integer.parseInt(level.toString()) < 2;
        });
        return R.ok(roles);
    }

    private OshRole findRoleById(Integer roleId) {
        return oshRoleMapper.selectById(roleId);
    }

    /**
     * 生成唯一邀请码
     */
    private String generateUniqueInviteCode() {
        for (int i = 0; i < 10; i++) {
            String code = GenerateUtil.generateInviteCode();
            LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OshUser::getInviteCode, code);
            if (oshUserMapper.selectCount(wrapper) == 0) {
                return code;
            }
        }
        return String.valueOf(GenerateUtil.generateSnowflakeId()).substring(10);
    }
}

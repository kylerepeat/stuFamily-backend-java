package com.stufamily.backend.identity.application.service;

import com.stufamily.backend.identity.application.command.AdminLoginCommand;
import com.stufamily.backend.identity.application.command.UpdateWechatProfileCommand;
import com.stufamily.backend.identity.application.command.WechatLoginCommand;
import com.stufamily.backend.identity.application.dto.LoginResult;
import com.stufamily.backend.identity.application.dto.WechatUserProfileView;
import com.stufamily.backend.identity.domain.repository.AdminUserRepository;
import com.stufamily.backend.identity.domain.repository.UserRepository;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysAdminUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import com.stufamily.backend.shared.security.AuthAudience;
import com.stufamily.backend.shared.security.JwtTokenProvider;
import com.stufamily.backend.wechat.gateway.WechatAuthGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatSession;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthApplicationService {

    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final WechatAuthGateway wechatAuthGateway;

    public AuthApplicationService(AdminUserRepository adminUserRepository, UserRepository userRepository, PasswordEncoder passwordEncoder,
                                  JwtTokenProvider jwtTokenProvider, WechatAuthGateway wechatAuthGateway) {
        this.adminUserRepository = adminUserRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.wechatAuthGateway = wechatAuthGateway;
    }

    @Transactional
    public LoginResult adminLogin(AdminLoginCommand command) {
        SysAdminUserDO user = adminUserRepository.findByUsername(command.username())
            .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED, "username or password incorrect"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin account is disabled");
        }
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "username or password incorrect");
        }
        user.setLastLoginAt(OffsetDateTime.now());
        adminUserRepository.save(user);
        List<String> roles = List.of("ADMIN");
        String token = jwtTokenProvider.createAccessToken(
            user.getId(), user.getUsername(), roles, AuthAudience.ADMIN, adminTokenVersion(user));
        return new LoginResult(user.getId(), token, "Bearer", user.getUsername(), roles);
    }

    @Transactional
    public LoginResult wechatLogin(WechatLoginCommand command) {
        WechatSession session = wechatAuthGateway.code2Session(command.code());
        if (!StringUtils.hasText(session.openid())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "invalid wechat session");
        }
        Optional<SysUserDO> existing = userRepository.findByOpenid(session.openid());
        SysUserDO user = existing.orElseGet(() -> createWechatUser(session, command));
        if (existing.isPresent()) {
            if (StringUtils.hasText(command.avatarUrl())) {
                user.setAvatarUrl(command.avatarUrl());
            }
        }
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        List<String> roles = List.of("WECHAT");
        String username = StringUtils.hasText(user.getNickname()) ? user.getNickname() : "wx_" + user.getId();
        String token = jwtTokenProvider.createAccessToken(
            user.getId(), username, roles, AuthAudience.WEIXIN, tokenVersion(user));
        return new LoginResult(user.getId(), token, "Bearer", username, roles);
    }

    private SysUserDO createWechatUser(WechatSession session, WechatLoginCommand command) {
        SysUserDO user = new SysUserDO();
        user.setUserNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
        user.setUserType("WECHAT");
        user.setStatus("ACTIVE");
        user.setOpenid(session.openid());
        user.setUnionid(session.unionid());
        user.setNickname(command.nickname());
        user.setAvatarUrl(command.avatarUrl());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public String requireOpenid(Long userId) {
        SysUserDO user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "user not found"));
        if (!StringUtils.hasText(user.getOpenid())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "current account does not bind wechat");
        }
        return user.getOpenid();
    }

    @Transactional
    public WechatUserProfileView updateWechatProfile(UpdateWechatProfileCommand command) {
        SysUserDO user = userRepository.findById(command.userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "user not found"));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "user status is not active");
        }
        String nickname = command.nickname() == null ? "" : command.nickname().trim();
        String phone = command.phone() == null ? "" : command.phone().trim();
        if (!StringUtils.hasText(nickname)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "nickname cannot be blank");
        }
        if (!phone.matches("^1\\d{10}$")) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "phone format invalid");
        }
        user.setNickname(nickname);
        user.setPhone(phone);
        SysUserDO saved = userRepository.save(user);
        return new WechatUserProfileView(saved.getId(), saved.getNickname(), saved.getPhone(), saved.getAvatarUrl());
    }

    @Transactional
    public void adminLogout(Long userId) {
        SysAdminUserDO user = adminUserRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "user not found"));
        user.setTokenVersion(adminTokenVersion(user) + 1);
        adminUserRepository.save(user);
    }

    private long tokenVersion(SysUserDO user) {
        return user.getTokenVersion() == null ? 0L : user.getTokenVersion();
    }

    private long adminTokenVersion(SysAdminUserDO user) {
        return user.getTokenVersion() == null ? 0L : user.getTokenVersion();
    }
}

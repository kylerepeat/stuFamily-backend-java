package com.stufamily.backend.identity.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stufamily.backend.identity.application.command.AdminLoginCommand;
import com.stufamily.backend.identity.application.command.UpdateWechatProfileCommand;
import com.stufamily.backend.identity.application.command.WechatLoginCommand;
import com.stufamily.backend.identity.domain.repository.AdminUserRepository;
import com.stufamily.backend.identity.domain.repository.UserRepository;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysAdminUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.security.JwtTokenProvider;
import com.stufamily.backend.wechat.gateway.WechatAuthGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthApplicationServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private WechatAuthGateway wechatAuthGateway;

    private AuthApplicationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AuthApplicationService(
            adminUserRepository, userRepository, passwordEncoder, jwtTokenProvider, wechatAuthGateway);
    }

    @Test
    void adminLoginShouldReturnTokenWhenPasswordMatches() {
        SysAdminUserDO user = new SysAdminUserDO();
        user.setId(1L);
        user.setUsername("admin");
        user.setStatus("ACTIVE");
        user.setPasswordHash("hash");
        user.setTokenVersion(3L);
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pwd", "hash")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(any(), any(), any(), any(), anyLong())).thenReturn("jwt");
        when(adminUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.adminLogin(new AdminLoginCommand("admin", "pwd"));

        assertEquals("jwt", result.accessToken());
        assertEquals("admin", result.username());
        assertTrue(result.roles().contains("ADMIN"));
    }

    @Test
    void adminLoginShouldThrowWhenPasswordMismatch() {
        SysAdminUserDO user = new SysAdminUserDO();
        user.setUsername("admin");
        user.setStatus("ACTIVE");
        user.setPasswordHash("hash");
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.adminLogin(new AdminLoginCommand("admin", "bad")));
    }

    @Test
    void adminLoginShouldThrowWhenUserIsNotAdmin() {
        when(adminUserRepository.findByUsername("wx")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.adminLogin(new AdminLoginCommand("wx", "pwd")));
    }

    @Test
    void adminLoginShouldThrowWhenDisabled() {
        SysAdminUserDO user = new SysAdminUserDO();
        user.setUsername("admin");
        user.setStatus("DISABLED");
        user.setPasswordHash("hash");
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> service.adminLogin(new AdminLoginCommand("admin", "pwd")));
    }

    @Test
    void wechatLoginShouldCreateUserWhenNotExists() {
        when(wechatAuthGateway.code2Session("code")).thenReturn(new WechatSession("openid-x", "union-x", "session"));
        when(userRepository.findByOpenid("openid-x")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> {
            SysUserDO user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(99L);
            }
            return user;
        });
        when(jwtTokenProvider.createAccessToken(any(), any(), any(), any(), anyLong())).thenReturn("wx-jwt");

        var result = service.wechatLogin(new WechatLoginCommand("code", "Tom", "avatar"));

        assertEquals(99L, result.userId());
        assertEquals("wx-jwt", result.accessToken());
        assertTrue(result.roles().contains("WECHAT"));
    }

    @Test
    void wechatLoginShouldNotOverrideNicknameAndPhoneWhenUserExists() {
        when(wechatAuthGateway.code2Session("code")).thenReturn(new WechatSession("openid-x", "union-x", "session"));
        SysUserDO existing = new SysUserDO();
        existing.setId(77L);
        existing.setOpenid("openid-x");
        existing.setNickname("old-name");
        existing.setPhone("13800138000");
        existing.setAvatarUrl("old-avatar");
        when(userRepository.findByOpenid("openid-x")).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.createAccessToken(any(), any(), any(), any(), anyLong())).thenReturn("wx-jwt");

        service.wechatLogin(new WechatLoginCommand("code", "new-name", "new-avatar"));

        assertEquals("old-name", existing.getNickname());
        assertEquals("13800138000", existing.getPhone());
    }

    @Test
    void requireOpenidShouldThrowWhenUserNotBindWechat() {
        SysUserDO user = new SysUserDO();
        user.setId(2L);
        user.setOpenid(null);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> service.requireOpenid(2L));
    }

    @Test
    void wechatLoginShouldThrowWhenOpenidMissing() {
        when(wechatAuthGateway.code2Session("code")).thenReturn(new WechatSession("", "u", "s"));
        assertThrows(BusinessException.class, () -> service.wechatLogin(new WechatLoginCommand("code", "n", "a")));
    }

    @Test
    void requireOpenidShouldReturnValue() {
        SysUserDO user = new SysUserDO();
        user.setId(2L);
        user.setOpenid("openid-2");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertEquals("openid-2", service.requireOpenid(2L));
    }

    @Test
    void requireOpenidShouldThrowWhenUserNotFound() {
        when(userRepository.findById(66L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.requireOpenid(66L));
    }

    @Test
    void adminLogoutShouldIncreaseTokenVersion() {
        SysAdminUserDO user = new SysAdminUserDO();
        user.setId(8L);
        user.setTokenVersion(2L);
        when(adminUserRepository.findById(8L)).thenReturn(Optional.of(user));
        when(adminUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.adminLogout(8L);

        assertEquals(3L, user.getTokenVersion());
    }

    @Test
    void updateWechatProfileShouldSaveNicknameAndPhone() {
        SysUserDO user = new SysUserDO();
        user.setId(18L);
        user.setStatus("ACTIVE");
        when(userRepository.findById(18L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateWechatProfile(new UpdateWechatProfileCommand(18L, "  XiaoZhang  ", "13800138000"));

        assertEquals("XiaoZhang", user.getNickname());
        assertEquals("13800138000", user.getPhone());
        assertEquals(18L, result.userId());
        assertEquals("XiaoZhang", result.nickname());
        assertEquals("13800138000", result.phone());
        verify(userRepository).save(user);
    }

    @Test
    void updateWechatProfileShouldThrowWhenPhoneInvalid() {
        SysUserDO user = new SysUserDO();
        user.setId(18L);
        user.setStatus("ACTIVE");
        when(userRepository.findById(18L)).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class,
            () -> service.updateWechatProfile(new UpdateWechatProfileCommand(18L, "XiaoZhang", "abc")));
    }
}

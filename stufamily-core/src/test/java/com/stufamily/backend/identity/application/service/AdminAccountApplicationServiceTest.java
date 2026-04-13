package com.stufamily.backend.identity.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.stufamily.backend.identity.application.command.ChangeAdminPasswordCommand;
import com.stufamily.backend.identity.application.command.CreateAdminAccountCommand;
import com.stufamily.backend.identity.domain.repository.AdminUserRepository;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysAdminUserDO;
import com.stufamily.backend.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminAccountApplicationServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminAccountApplicationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AdminAccountApplicationService(adminUserRepository, passwordEncoder);
    }

    @Test
    void shouldListAdminAccounts() {
        SysAdminUserDO user = new SysAdminUserDO();
        user.setId(1L);
        user.setUserNo("U1");
        user.setUsername("admin1");
        user.setStatus("ACTIVE");
        when(adminUserRepository.countAdminUsers(null, null)).thenReturn(1L);
        when(adminUserRepository.findAdminUsers(null, null, 0, 20)).thenReturn(List.of(user));

        var result = service.listAdminAccounts(null, null, null, null);
        assertEquals(1, result.items().size());
        assertEquals("admin1", result.items().get(0).username());
        assertEquals(1, result.total());
    }

    @Test
    void shouldCreateAdminAccount() {
        when(adminUserRepository.findByUsername("new_admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Aa1@5678")).thenReturn("hashed");
        when(adminUserRepository.save(any())).thenAnswer(invocation -> {
            SysAdminUserDO user = invocation.getArgument(0);
            user.setId(100L);
            return user;
        });

        var result = service.createAdminAccount(new CreateAdminAccountCommand(
            "new_admin", "Aa1@5678", "nick", "138", "a@b.com"));
        assertEquals(100L, result.id());
        assertEquals("new_admin", result.username());
    }

    @Test
    void shouldDisableAdminAccount() {
        SysAdminUserDO user = new SysAdminUserDO();
        user.setId(10L);
        user.setStatus("ACTIVE");
        user.setTokenVersion(4L);
        when(adminUserRepository.findById(10L)).thenReturn(Optional.of(user));
        when(adminUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.disableAdminAccount(10L, 1L);
        assertEquals("DISABLED", user.getStatus());
        assertEquals(5L, user.getTokenVersion());
    }

    @Test
    void shouldChangeAdminPassword() {
        SysAdminUserDO user = new SysAdminUserDO();
        user.setId(10L);
        user.setUsername("admin1");
        when(adminUserRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewAa1@78")).thenReturn("new-hash");
        when(adminUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.changeAdminPassword(new ChangeAdminPasswordCommand(10L, "NewAa1@78"));
        assertEquals("new-hash", user.getPasswordHash());
    }

    @Test
    void shouldRejectWeakPassword() {
        assertThrows(BusinessException.class, () -> service.validatePasswordStrength("abc", "admin"));
    }
}

package com.stufamily.backend.identity.application.service;

import com.stufamily.backend.identity.application.command.ChangeAdminPasswordCommand;
import com.stufamily.backend.identity.application.command.CreateAdminAccountCommand;
import com.stufamily.backend.identity.application.dto.AdminAccountView;
import com.stufamily.backend.identity.domain.repository.AdminUserRepository;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysAdminUserDO;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminAccountApplicationService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[^A-Za-z0-9]");

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountApplicationService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResult<AdminAccountView> listAdminAccounts(String keyword, String status, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        long total = adminUserRepository.countAdminUsers(keyword, status);
        List<AdminAccountView> items = adminUserRepository.findAdminUsers(keyword, status, offset, normalizedPageSize).stream()
            .map(this::toAdminAccountView)
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    @Transactional
    public AdminAccountView createAdminAccount(CreateAdminAccountCommand command) {
        validatePasswordStrength(command.password(), command.username());
        adminUserRepository.findByUsername(command.username()).ifPresent(user -> {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "username already exists");
        });
        SysAdminUserDO user = new SysAdminUserDO();
        user.setUserNo(generateUserNo());
        user.setStatus("ACTIVE");
        user.setUsername(command.username());
        user.setPasswordHash(passwordEncoder.encode(command.password()));
        user.setNickname(command.nickname());
        user.setPhone(command.phone());
        user.setEmail(command.email());
        return toAdminAccountView(adminUserRepository.save(user));
    }

    @Transactional
    public void disableAdminAccount(Long userId, Long operatorUserId) {
        if (userId.equals(operatorUserId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "current admin cannot disable itself");
        }
        SysAdminUserDO target = loadAdminUser(userId);
        target.setStatus("DISABLED");
        target.setTokenVersion(tokenVersion(target) + 1);
        adminUserRepository.save(target);
    }

    @Transactional
    public void changeAdminPassword(ChangeAdminPasswordCommand command) {
        SysAdminUserDO target = loadAdminUser(command.userId());
        validatePasswordStrength(command.newPassword(), target.getUsername());
        target.setPasswordHash(passwordEncoder.encode(command.newPassword()));
        adminUserRepository.save(target);
    }

    public void validatePasswordStrength(String password, String username) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "password is required");
        }
        String normalized = password.trim();
        if (normalized.length() < 8 || normalized.length() > 32) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "password must be 8-32 chars");
        }
        if (normalized.contains(" ")) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "password must not contain spaces");
        }
        if (!UPPERCASE_PATTERN.matcher(normalized).find()
            || !LOWERCASE_PATTERN.matcher(normalized).find()
            || !DIGIT_PATTERN.matcher(normalized).find()
            || !SPECIAL_PATTERN.matcher(normalized).find()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM,
                "password must include uppercase, lowercase, digit and special char");
        }
        if (StringUtils.hasText(username)
            && normalized.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "password must not contain username");
        }
    }

    private SysAdminUserDO loadAdminUser(Long userId) {
        return adminUserRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "user not found"));
    }

    private AdminAccountView toAdminAccountView(SysAdminUserDO user) {
        return new AdminAccountView(
            user.getId(),
            user.getUserNo(),
            user.getUsername(),
            "ADMIN",
            user.getStatus(),
            user.getNickname(),
            user.getPhone(),
            user.getEmail(),
            user.getLastLoginAt(),
            user.getCreatedAt()
        );
    }

    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo <= 0) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String generateUserNo() {
        return "U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
    }

    private long tokenVersion(SysAdminUserDO user) {
        return user.getTokenVersion() == null ? 0L : user.getTokenVersion();
    }
}

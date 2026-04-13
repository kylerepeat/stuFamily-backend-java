package com.stufamily.backend.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stufamily.backend.identity.domain.repository.AdminUserRepository;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysAdminUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.mapper.SysAdminUserMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class MybatisAdminUserRepository implements AdminUserRepository {

    private final SysAdminUserMapper sysAdminUserMapper;

    public MybatisAdminUserRepository(SysAdminUserMapper sysAdminUserMapper) {
        this.sysAdminUserMapper = sysAdminUserMapper;
    }

    @Override
    public Optional<SysAdminUserDO> findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<SysAdminUserDO> wrapper = new LambdaQueryWrapper<SysAdminUserDO>()
            .eq(SysAdminUserDO::getUsername, username);
        return Optional.ofNullable(sysAdminUserMapper.selectOne(wrapper));
    }

    @Override
    public Optional<SysAdminUserDO> findById(Long id) {
        return Optional.ofNullable(sysAdminUserMapper.selectById(id));
    }

    @Override
    public long countAdminUsers(String keyword, String status) {
        return sysAdminUserMapper.selectCount(buildAdminQuery(keyword, status));
    }

    @Override
    public List<SysAdminUserDO> findAdminUsers(String keyword, String status, int offset, int limit) {
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.max(limit, 1);
        LambdaQueryWrapper<SysAdminUserDO> wrapper = buildAdminQuery(keyword, status)
            .orderByDesc(SysAdminUserDO::getId)
            .last("limit " + safeLimit + " offset " + safeOffset);
        return sysAdminUserMapper.selectList(wrapper);
    }

    private LambdaQueryWrapper<SysAdminUserDO> buildAdminQuery(String keyword, String status) {
        LambdaQueryWrapper<SysAdminUserDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysAdminUserDO::getStatus, status.trim().toUpperCase());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysAdminUserDO::getUsername, keyword)
                .or().like(SysAdminUserDO::getNickname, keyword)
                .or().like(SysAdminUserDO::getPhone, keyword)
                .or().like(SysAdminUserDO::getEmail, keyword)
                .or().like(SysAdminUserDO::getUserNo, keyword));
        }
        return wrapper;
    }

    @Override
    public SysAdminUserDO save(SysAdminUserDO user) {
        OffsetDateTime now = OffsetDateTime.now();
        if (user.getId() == null) {
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            sysAdminUserMapper.insert(user);
        } else {
            user.setUpdatedAt(now);
            sysAdminUserMapper.updateById(user);
        }
        return user;
    }
}

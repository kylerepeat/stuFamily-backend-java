package com.stufamily.backend.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stufamily.backend.identity.domain.repository.UserRepository;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.mapper.SysUserMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class MybatisUserRepository implements UserRepository {

    private final SysUserMapper sysUserMapper;

    public MybatisUserRepository(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public Optional<SysUserDO> findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<SysUserDO>()
            .eq(SysUserDO::getUsername, username);
        return Optional.ofNullable(sysUserMapper.selectOne(wrapper));
    }

    @Override
    public Optional<SysUserDO> findByOpenid(String openid) {
        if (!StringUtils.hasText(openid)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<SysUserDO>()
            .eq(SysUserDO::getOpenid, openid);
        return Optional.ofNullable(sysUserMapper.selectOne(wrapper));
    }

    @Override
    public Optional<SysUserDO> findById(Long id) {
        return Optional.ofNullable(sysUserMapper.selectById(id));
    }

    @Override
    public long countAdminUsers(String keyword, String status) {
        return sysUserMapper.selectCount(buildAdminQuery(keyword, status));
    }

    @Override
    public List<SysUserDO> findAdminUsers(String keyword, String status, int offset, int limit) {
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.max(limit, 1);
        LambdaQueryWrapper<SysUserDO> wrapper = buildAdminQuery(keyword, status)
            .orderByDesc(SysUserDO::getId)
            .last("limit " + safeLimit + " offset " + safeOffset);
        return sysUserMapper.selectList(wrapper);
    }

    private LambdaQueryWrapper<SysUserDO> buildAdminQuery(String keyword, String status) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<SysUserDO>()
            .in(SysUserDO::getUserType, List.of("ADMIN", "HYBRID"));
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysUserDO::getStatus, status.trim().toUpperCase());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUserDO::getUsername, keyword)
                .or().like(SysUserDO::getNickname, keyword)
                .or().like(SysUserDO::getPhone, keyword)
                .or().like(SysUserDO::getEmail, keyword)
                .or().like(SysUserDO::getUserNo, keyword));
        }
        return wrapper;
    }

    @Override
    public SysUserDO save(SysUserDO user) {
        OffsetDateTime now = OffsetDateTime.now();
        if (user.getId() == null) {
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            sysUserMapper.insert(user);
        } else {
            user.setUpdatedAt(now);
            sysUserMapper.updateById(user);
        }
        return user;
    }
}

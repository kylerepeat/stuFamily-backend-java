package com.stufamily.backend.identity.domain.repository;

import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<SysUserDO> findByUsername(String username);

    Optional<SysUserDO> findByOpenid(String openid);

    Optional<SysUserDO> findById(Long id);

    long countAdminUsers(String keyword, String status);

    List<SysUserDO> findAdminUsers(String keyword, String status, int offset, int limit);

    SysUserDO save(SysUserDO user);
}

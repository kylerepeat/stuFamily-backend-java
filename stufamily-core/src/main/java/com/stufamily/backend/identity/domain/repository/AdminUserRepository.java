package com.stufamily.backend.identity.domain.repository;

import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysAdminUserDO;
import java.util.List;
import java.util.Optional;

public interface AdminUserRepository {
    Optional<SysAdminUserDO> findByUsername(String username);

    Optional<SysAdminUserDO> findById(Long id);

    long countAdminUsers(String keyword, String status);

    List<SysAdminUserDO> findAdminUsers(String keyword, String status, int offset, int limit);

    SysAdminUserDO save(SysAdminUserDO user);
}

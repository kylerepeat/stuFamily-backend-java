package com.stufamily.backend.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysAdminUserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysAdminUserMapper extends BaseMapper<SysAdminUserDO> {
}

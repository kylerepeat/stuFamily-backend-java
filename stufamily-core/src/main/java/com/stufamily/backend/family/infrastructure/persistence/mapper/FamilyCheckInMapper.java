package com.stufamily.backend.family.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyCheckInDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FamilyCheckInMapper extends BaseMapper<FamilyCheckInDO> {
}

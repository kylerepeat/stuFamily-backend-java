package com.stufamily.backend.home.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stufamily.backend.home.infrastructure.persistence.dataobject.SiteProfileDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SiteProfileMapper extends BaseMapper<SiteProfileDO> {
}


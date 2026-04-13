package com.stufamily.backend.home.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stufamily.backend.home.infrastructure.persistence.dataobject.ParentMessageDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParentMessageMapper extends BaseMapper<ParentMessageDO> {
}


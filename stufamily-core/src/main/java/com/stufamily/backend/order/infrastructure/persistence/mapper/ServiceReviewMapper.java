package com.stufamily.backend.order.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.ServiceReviewDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServiceReviewMapper extends BaseMapper<ServiceReviewDO> {
}

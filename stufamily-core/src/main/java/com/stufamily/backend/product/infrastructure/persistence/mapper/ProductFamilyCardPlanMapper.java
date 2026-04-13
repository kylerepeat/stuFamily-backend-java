package com.stufamily.backend.product.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductFamilyCardPlanDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductFamilyCardPlanMapper extends BaseMapper<ProductFamilyCardPlanDO> {
}


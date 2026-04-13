package com.stufamily.backend.order.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.MonthlyAmountRowDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderMainDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMainMapper extends BaseMapper<OrderMainDO> {

    @Select("""
        <script>
        SELECT
            TO_CHAR(om.paid_at AT TIME ZONE 'Asia/Shanghai', 'YYYY-MM') AS month,
            COALESCE(SUM(om.payable_amount_cents), 0) AS amountCents
        FROM order_main om
        WHERE om.paid_at IS NOT NULL
          AND om.order_status IN ('PAID', 'REFUNDED')
          <if test="startMonth != null and startMonth != ''">
            AND TO_CHAR(om.paid_at AT TIME ZONE 'Asia/Shanghai', 'YYYY-MM') &gt;= #{startMonth}
          </if>
          <if test="endMonth != null and endMonth != ''">
            AND TO_CHAR(om.paid_at AT TIME ZONE 'Asia/Shanghai', 'YYYY-MM') &lt;= #{endMonth}
          </if>
          <if test="productType != null and productType != ''">
            AND EXISTS (
                SELECT 1 FROM order_item oi
                WHERE oi.order_id = om.id
                  AND oi.product_type_snapshot = #{productType}
                  <if test="productId != null">
                    AND oi.product_id = #{productId}
                  </if>
            )
          </if>
          <if test="(productType == null or productType == '') and productId != null">
            AND EXISTS (
                SELECT 1 FROM order_item oi
                WHERE oi.order_id = om.id
                  AND oi.product_id = #{productId}
            )
          </if>
        GROUP BY TO_CHAR(om.paid_at AT TIME ZONE 'Asia/Shanghai', 'YYYY-MM')
        ORDER BY TO_CHAR(om.paid_at AT TIME ZONE 'Asia/Shanghai', 'YYYY-MM') ASC
        </script>
        """)
    List<MonthlyAmountRowDO> selectMonthlyPaidIncomeStats(
        @Param("startMonth") String startMonth,
        @Param("endMonth") String endMonth,
        @Param("productType") String productType,
        @Param("productId") Long productId
    );
}

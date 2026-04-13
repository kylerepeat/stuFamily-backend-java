package com.stufamily.backend.order.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderItemDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderPurchasedProductRowDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemDO> {
    @Select("""
        <script>
        SELECT COUNT(DISTINCT om.order_no)
        FROM order_item oi
        JOIN order_main om ON om.id = oi.order_id
        WHERE om.buyer_user_id = #{buyerUserId}
          AND om.order_status = 'PAID'
          <if test="productType != null and productType != ''">
            AND oi.product_type_snapshot = #{productType}
          </if>
        </script>
        """)
    long countPaidProductsByBuyerUserId(@Param("buyerUserId") Long buyerUserId,
                                        @Param("productType") String productType);

    @Select("""
        <script>
        SELECT
            t.orderNo,
            t.orderType,
            t.orderStatus,
            t.paidAt,
            t.productId,
            t.productType,
            t.productTitle,
            t.productBrief,
            t.productImageUrls,
            t.selectedDurationType,
            t.selectedDurationMonths,
            t.serviceStartAt,
            t.serviceEndAt,
            t.unitPriceCents,
            t.quantity,
            t.totalPriceCents,
            sr.stars AS reviewStars,
            sr.content AS reviewContent,
            sr.updated_at AS reviewedAt
        FROM (
            SELECT
                om.id AS orderId,
                om.order_no AS orderNo,
                om.order_type AS orderType,
                om.order_status AS orderStatus,
                om.paid_at AS paidAt,
                oi.product_id AS productId,
                oi.product_type_snapshot AS productType,
                oi.product_title_snapshot AS productTitle,
                oi.product_brief_snapshot AS productBrief,
                COALESCE(p.image_urls, '[]'::jsonb)::text AS productImageUrls,
                oi.selected_duration_type AS selectedDurationType,
                oi.selected_duration_months AS selectedDurationMonths,
                oi.service_start_at AS serviceStartAt,
                oi.service_end_at AS serviceEndAt,
                oi.unit_price_cents AS unitPriceCents,
                oi.quantity AS quantity,
                oi.total_price_cents AS totalPriceCents,
                ROW_NUMBER() OVER (PARTITION BY om.order_no ORDER BY oi.id DESC) AS rn
            FROM order_item oi
            JOIN order_main om ON om.id = oi.order_id
            LEFT JOIN product p ON p.id = oi.product_id
            WHERE om.buyer_user_id = #{buyerUserId}
              AND om.order_status = 'PAID'
              <if test="productType != null and productType != ''">
                AND oi.product_type_snapshot = #{productType}
              </if>
        ) t
        LEFT JOIN service_review sr ON sr.order_id = t.orderId
        WHERE t.rn = 1
        ORDER BY t.paidAt DESC NULLS LAST, t.orderNo DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<OrderPurchasedProductRowDO> selectPaidProductsByBuyerUserId(
        @Param("buyerUserId") Long buyerUserId,
        @Param("productType") String productType,
        @Param("limit") Integer limit,
        @Param("offset") Integer offset
    );
}

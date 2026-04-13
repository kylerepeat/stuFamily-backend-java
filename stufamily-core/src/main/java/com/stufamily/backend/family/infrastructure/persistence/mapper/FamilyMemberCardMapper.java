package com.stufamily.backend.family.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyMemberCardDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyMemberListRowDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FamilyMemberCardMapper extends BaseMapper<FamilyMemberCardDO> {
    @Select("""
        <script>
        SELECT COUNT(1)
        FROM family_member_card mc
        JOIN family_group fg ON fg.id = mc.group_id
        LEFT JOIN sys_user su ON su.id = fg.owner_user_id
        WHERE mc.group_id = #{groupId}
          AND fg.owner_user_id = #{ownerUserId}
          AND mc.status != 'CANCELLED'
          <if test="keyword != null and keyword != ''">
            AND (
                mc.member_name ILIKE '%' || #{keyword} || '%'
                OR mc.student_or_card_no ILIKE '%' || #{keyword} || '%'
                OR mc.phone ILIKE '%' || #{keyword} || '%'
                OR mc.member_no ILIKE '%' || #{keyword} || '%'
                OR su.nickname ILIKE '%' || #{keyword} || '%'
            )
          </if>
        </script>
        """)
    long countByGroupAndKeyword(@Param("groupId") Long groupId, @Param("ownerUserId") Long ownerUserId,
                                @Param("keyword") String keyword);

    @Select("""
        <script>
        SELECT
            mc.member_no AS memberNo,
            mc.member_name AS memberName,
            mc.student_or_card_no AS studentOrCardNo,
            mc.phone AS phone,
            mc.joined_at AS joinedAt,
            mc.status AS status,
            fg.expire_at AS familyGroupExpireAt,
            su.avatar_url AS wechatAvatarUrl
        FROM family_member_card mc
        JOIN family_group fg ON fg.id = mc.group_id
        LEFT JOIN sys_user su ON su.id = fg.owner_user_id
        WHERE mc.group_id = #{groupId}
          AND fg.owner_user_id = #{ownerUserId}
          AND mc.status != 'CANCELLED'
          <if test="keyword != null and keyword != ''">
            AND (
                mc.member_name ILIKE '%' || #{keyword} || '%'
                OR mc.student_or_card_no ILIKE '%' || #{keyword} || '%'
                OR mc.phone ILIKE '%' || #{keyword} || '%'
                OR mc.member_no ILIKE '%' || #{keyword} || '%'
                OR su.nickname ILIKE '%' || #{keyword} || '%'
            )
          </if>
        ORDER BY mc.joined_at DESC, mc.id DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<FamilyMemberListRowDO> selectPageByGroupAndKeyword(@Param("groupId") Long groupId,
                                                            @Param("ownerUserId") Long ownerUserId,
                                                            @Param("keyword") String keyword,
                                                            @Param("limit") Integer limit,
                                                            @Param("offset") Integer offset);

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM family_member_card mc
        JOIN family_group fg ON fg.id = mc.group_id
        LEFT JOIN sys_user su ON su.id = fg.owner_user_id
        WHERE fg.owner_user_id = #{ownerUserId}
          AND fg.status = 'ACTIVE'
          AND fg.expire_at > NOW()
          AND mc.status != 'CANCELLED'
          <if test="keyword != null and keyword != ''">
            AND (
                mc.member_name ILIKE '%' || #{keyword} || '%'
                OR mc.student_or_card_no ILIKE '%' || #{keyword} || '%'
                OR mc.phone ILIKE '%' || #{keyword} || '%'
                OR mc.member_no ILIKE '%' || #{keyword} || '%'
                OR su.nickname ILIKE '%' || #{keyword} || '%'
            )
          </if>
        </script>
        """)
    long countByOwnerAndKeyword(@Param("ownerUserId") Long ownerUserId, @Param("keyword") String keyword);

    @Select("""
        <script>
        SELECT
            mc.member_no AS memberNo,
            mc.member_name AS memberName,
            mc.student_or_card_no AS studentOrCardNo,
            mc.phone AS phone,
            mc.joined_at AS joinedAt,
            mc.status AS status,
            fg.expire_at AS familyGroupExpireAt,
            su.avatar_url AS wechatAvatarUrl
        FROM family_member_card mc
        JOIN family_group fg ON fg.id = mc.group_id
        LEFT JOIN sys_user su ON su.id = fg.owner_user_id
        WHERE fg.owner_user_id = #{ownerUserId}
          AND fg.status = 'ACTIVE'
          AND fg.expire_at > NOW()
          AND mc.status != 'CANCELLED'
          <if test="keyword != null and keyword != ''">
            AND (
                mc.member_name ILIKE '%' || #{keyword} || '%'
                OR mc.student_or_card_no ILIKE '%' || #{keyword} || '%'
                OR mc.phone ILIKE '%' || #{keyword} || '%'
                OR mc.member_no ILIKE '%' || #{keyword} || '%'
                OR su.nickname ILIKE '%' || #{keyword} || '%'
            )
          </if>
        ORDER BY mc.joined_at DESC, mc.id DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<FamilyMemberListRowDO> selectPageByOwnerAndKeyword(@Param("ownerUserId") Long ownerUserId,
                                                            @Param("keyword") String keyword,
                                                            @Param("limit") Integer limit,
                                                            @Param("offset") Integer offset);
}

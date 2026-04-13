package com.stufamily.backend.family.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.stufamily.backend.family.application.command.AddFamilyMemberCommand;
import com.stufamily.backend.family.application.command.AddFamilyCheckInCommand;
import com.stufamily.backend.family.application.dto.FamilyGroupQuotaView;
import com.stufamily.backend.family.application.dto.FamilyCheckInView;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyCheckInDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyGroupDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyMemberCardDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyMemberListRowDO;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyCheckInMapper;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyGroupMapper;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyMemberCardMapper;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductDO;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductFamilyCardPlanDO;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductFamilyCardPlanMapper;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductMapper;
import com.stufamily.backend.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FamilyApplicationServiceTest {

    @Mock
    private FamilyGroupMapper familyGroupMapper;
    @Mock
    private FamilyMemberCardMapper familyMemberCardMapper;
    @Mock
    private FamilyCheckInMapper familyCheckInMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductFamilyCardPlanMapper familyCardPlanMapper;

    private FamilyApplicationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new FamilyApplicationService(
            familyGroupMapper, familyMemberCardMapper, familyCheckInMapper, productMapper, familyCardPlanMapper);
    }

    @Test
    void shouldAddCheckInWhenFamilyCardIsActiveAtCheckedTime() {
        FamilyGroupDO group = activeGroup();
        group.setGroupNo("FG001");
        group.setActivatedAt(OffsetDateTime.parse("2026-03-01T00:00:00+08:00"));
        group.setExpireAt(OffsetDateTime.parse("2026-12-31T23:59:59+08:00"));
        when(familyGroupMapper.selectOne(any())).thenReturn(group);
        when(familyCheckInMapper.insert(any(FamilyCheckInDO.class))).thenReturn(1);

        FamilyCheckInView view = service.addCheckIn(new AddFamilyCheckInCommand(
            1L,
            null,
            null,
            new BigDecimal("31.2304160"),
            new BigDecimal("121.4737010"),
            "上海市黄浦区人民广场",
            OffsetDateTime.parse("2026-03-31T09:00:00+08:00")
        ));
        assertEquals("FG001", view.groupNo());
        assertEquals(1L, view.checkinUserId());
        assertEquals(new BigDecimal("31.2304160"), view.latitude());
    }

    @Test
    void shouldRejectCheckInWhenFamilyCardNotActiveAtCheckedTime() {
        when(familyGroupMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.addCheckIn(new AddFamilyCheckInCommand(
            1L,
            null,
            null,
            new BigDecimal("31.2304160"),
            new BigDecimal("121.4737010"),
            "上海市黄浦区人民广场",
            OffsetDateTime.parse("2027-01-01T09:00:00+08:00")
        )));
    }

    @Test
    void shouldRejectCheckInWhenRateLimitedWithinFiveMinutes() {
        FamilyCheckInDO latest = new FamilyCheckInDO();
        latest.setCreatedAt(OffsetDateTime.now().minusMinutes(2));
        when(familyCheckInMapper.selectOne(any())).thenReturn(latest);

        assertThrows(BusinessException.class, () -> service.addCheckIn(new AddFamilyCheckInCommand(
            1L,
            null,
            null,
            new BigDecimal("31.2304160"),
            new BigDecimal("121.4737010"),
            "addr",
            OffsetDateTime.parse("2026-03-31T09:00:00+08:00")
        )));
    }

    @Test
    void shouldAddCheckInWithFamilyMemberId() {
        FamilyGroupDO group = activeGroup();
        group.setGroupNo("FG001");
        group.setActivatedAt(OffsetDateTime.parse("2026-03-01T00:00:00+08:00"));
        group.setExpireAt(OffsetDateTime.parse("2026-12-31T23:59:59+08:00"));
        when(familyGroupMapper.selectById(99L)).thenReturn(group);
        when(familyGroupMapper.selectOne(any())).thenReturn(group);
        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setId(1001L);
        member.setGroupId(99L);
        member.setStatus("ACTIVE");
        member.setJoinedAt(OffsetDateTime.parse("2026-03-01T10:00:00+08:00"));
        when(familyMemberCardMapper.selectById(1001L)).thenReturn(member);
        when(familyCheckInMapper.insert(any(FamilyCheckInDO.class))).thenReturn(1);

        FamilyCheckInView view = service.addCheckIn(new AddFamilyCheckInCommand(
            1L,
            "FG001",
            1001L,
            new BigDecimal("31.2304160"),
            new BigDecimal("121.4737010"),
            "addr",
            OffsetDateTime.parse("2026-03-31T09:00:00+08:00")
        ));
        assertEquals(1001L, view.familyMemberId());
    }

    @Test
    void shouldAddAndSearchMember() {
        FamilyGroupDO group = activeGroup();
        when(familyGroupMapper.selectOne(any())).thenReturn(group);
        when(familyMemberCardMapper.selectOne(any())).thenReturn(null);
        when(familyMemberCardMapper.insert(any(FamilyMemberCardDO.class))).thenAnswer(invocation -> {
            FamilyMemberCardDO member = invocation.getArgument(0);
            member.setId(1L);
            return 1;
        });
        FamilyMemberListRowDO insertedRow = new FamilyMemberListRowDO();
        insertedRow.setMemberNo("M1");
        insertedRow.setMemberName("Alice");
        insertedRow.setStudentOrCardNo("S001");
        insertedRow.setPhone("13800000000");
        insertedRow.setJoinedAt(OffsetDateTime.parse("2026-03-24T10:00:00+08:00"));
        insertedRow.setStatus("ACTIVE");
        insertedRow.setFamilyGroupExpireAt(group.getExpireAt());
        insertedRow.setWechatAvatarUrl("https://example.com/avatar.png");
        when(familyMemberCardMapper.selectPageByGroupAndKeyword(any(), any(), any(), any(), any()))
            .thenReturn(List.of(insertedRow));
        when(familyGroupMapper.updateById(any(FamilyGroupDO.class))).thenReturn(1);

        var member = service.addMember(new AddFamilyMemberCommand(
            1L, null, "Alice", "S001", "13800000000", OffsetDateTime.parse("2026-03-24T10:00:00+08:00")));
        assertEquals("ACTIVE", member.status());

        FamilyMemberCardDO stored = new FamilyMemberCardDO();
        stored.setMemberNo(member.memberNo());
        stored.setMemberName("Alice");
        stored.setStudentOrCardNo("S001");
        stored.setPhone("13800000000");
        stored.setJoinedAt(OffsetDateTime.parse("2026-03-24T10:00:00+08:00"));
        stored.setStatus("ACTIVE");
        FamilyMemberListRowDO row = new FamilyMemberListRowDO();
        row.setMemberNo(stored.getMemberNo());
        row.setMemberName(stored.getMemberName());
        row.setStudentOrCardNo(stored.getStudentOrCardNo());
        row.setPhone(stored.getPhone());
        row.setJoinedAt(stored.getJoinedAt());
        row.setStatus(stored.getStatus());
        row.setFamilyGroupExpireAt(group.getExpireAt());
        row.setWechatAvatarUrl("https://example.com/avatar.png");
        when(familyMemberCardMapper.countByOwnerAndKeyword(any(), any())).thenReturn(1L);
        when(familyMemberCardMapper.selectPageByOwnerAndKeyword(any(), any(), any(), any()))
            .thenReturn(List.of(row));
        assertEquals(1, service.searchMembers(1L, null, "Ali", 1, 10).items().size());
    }

    @Test
    void shouldCancelMemberCard() {
        FamilyGroupDO group = activeGroup();
        group.setOwnerUserId(2L);
        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setId(10L);
        member.setGroupId(99L);
        member.setMemberNo("M1");
        member.setStatus("ACTIVE");
        when(familyMemberCardMapper.selectOne(any())).thenReturn(member);
        when(familyGroupMapper.selectById(99L)).thenReturn(group);
        when(familyMemberCardMapper.updateById(any(FamilyMemberCardDO.class))).thenReturn(1);

        service.cancelExpiredCard(2L, "M1");
        assertEquals("CANCELLED", member.getStatus());
    }

    @Test
    void shouldRejectDuplicateCard() {
        when(familyGroupMapper.selectOne(any())).thenReturn(activeGroup());
        when(familyMemberCardMapper.selectOne(any())).thenReturn(new FamilyMemberCardDO());
        assertThrows(BusinessException.class,
            () -> service.addMember(new AddFamilyMemberCommand(3L, null, "Tom2", "S003", "13800000001", OffsetDateTime.now())));
    }

    @Test
    void shouldRejectWhenExceedMaxMemberCount() {
        FamilyGroupDO group = activeGroup();
        group.setCurrentMembers(5);
        group.setMaxMembers(5);
        when(familyGroupMapper.selectOne(any())).thenReturn(group);
        assertThrows(BusinessException.class,
            () -> service.addMember(new AddFamilyMemberCommand(4L, null, "Overflow", "S999", "13800000002", OffsetDateTime.now())));
    }

    @Test
    void shouldThrowWhenCancelOnMissingGroup() {
        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setGroupId(111L);
        when(familyMemberCardMapper.selectOne(any())).thenReturn(member);
        when(familyGroupMapper.selectById(111L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.cancelExpiredCard(100L, "M0"));
    }

    @Test
    void shouldReturnGroupQuotaWhenActiveGroupExists() {
        FamilyGroupDO group = activeGroup();
        group.setGroupNo("FG0001");
        group.setFamilyCardProductId(101L);
        group.setFamilyCardPlanId(1001L);
        group.setMaxMembers(5);
        group.setCurrentMembers(2);
        ProductDO product = new ProductDO();
        product.setId(101L);
        product.setProductType("FAMILY_CARD");
        product.setTitle("家庭卡");
        ProductFamilyCardPlanDO plan = new ProductFamilyCardPlanDO();
        plan.setId(1001L);
        plan.setDurationType("SEMESTER");
        plan.setDurationMonths(6);
        when(familyGroupMapper.selectList(any())).thenReturn(List.of(group));
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product));
        when(familyCardPlanMapper.selectBatchIds(any())).thenReturn(List.of(plan));

        FamilyGroupQuotaView quota = service.getCurrentGroupQuota(1L);
        assertEquals(true, quota.hasActiveGroup());
        assertEquals(1, quota.groups().size());
        assertEquals("FG0001", quota.groups().get(0).groupNo());
        assertEquals(5, quota.groups().get(0).maxMembers());
        assertEquals(2, quota.groups().get(0).currentMembers());
        assertEquals(3, quota.groups().get(0).availableMembers());
        assertEquals("FAMILY_CARD", quota.groups().get(0).productType());
        assertEquals("家庭卡", quota.groups().get(0).productTitle());
        assertEquals("SEMESTER", quota.groups().get(0).durationType());
        assertEquals(6, quota.groups().get(0).durationMonths());
    }

    @Test
    void shouldReturnEmptyQuotaWhenNoActiveGroup() {
        when(familyGroupMapper.selectList(any())).thenReturn(List.of());

        FamilyGroupQuotaView quota = service.getCurrentGroupQuota(1L);
        assertEquals(false, quota.hasActiveGroup());
        assertEquals(0, quota.groups().size());
    }

    private FamilyGroupDO activeGroup() {
        FamilyGroupDO group = new FamilyGroupDO();
        group.setId(99L);
        group.setOwnerUserId(1L);
        group.setCurrentMembers(0);
        group.setMaxMembers(5);
        group.setStatus("ACTIVE");
        group.setExpireAt(OffsetDateTime.now().plusMonths(1));
        return group;
    }
}

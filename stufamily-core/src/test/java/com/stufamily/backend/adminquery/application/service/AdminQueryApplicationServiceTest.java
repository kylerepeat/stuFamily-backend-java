package com.stufamily.backend.adminquery.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyGroupDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyCheckInDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyMemberCardDO;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyCheckInMapper;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyGroupMapper;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyMemberCardMapper;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.mapper.SysUserMapper;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.MonthlyAmountRowDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderMainDO;
import com.stufamily.backend.order.infrastructure.persistence.mapper.OrderMainMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.PaymentRefundMapper;
import com.stufamily.backend.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AdminQueryApplicationServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private OrderMainMapper orderMainMapper;
    @Mock
    private PaymentRefundMapper paymentRefundMapper;
    @Mock
    private FamilyCheckInMapper familyCheckInMapper;
    @Mock
    private FamilyGroupMapper familyGroupMapper;
    @Mock
    private FamilyMemberCardMapper familyMemberCardMapper;

    private AdminQueryApplicationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AdminQueryApplicationService(
            sysUserMapper, orderMainMapper, paymentRefundMapper, familyCheckInMapper, familyGroupMapper, familyMemberCardMapper
        );
    }

    @Test
    void shouldReturnFilterOptions() {
        var options = service.listFilterOptions();
        assertFalse(options.productPublishStatuses().isEmpty());
        assertEquals("ON_SHELF", options.productPublishStatuses().get(1).value());
        assertEquals("FAMILY_CARD", options.orderTypes().get(0).value());
    }

    @Test
    void shouldListWechatUsers() {
        SysUserDO user = new SysUserDO();
        user.setId(1L);
        user.setUserNo("U1");
        user.setUserType("WECHAT");
        user.setStatus("ACTIVE");
        user.setOpenid("openid-1");
        user.setNickname("nick-1");
        when(sysUserMapper.selectCount(any())).thenReturn(1L);
        when(sysUserMapper.selectList(any())).thenReturn(List.of(user));

        var users = service.listWechatUsers(null, "ACTIVE", 1, 10);
        assertEquals(1, users.items().size());
        assertEquals("openid-1", users.items().get(0).openid());
        assertEquals(1, users.total());
    }

    @Test
    void shouldListOrdersWithWechatUser() {
        OrderMainDO order = new OrderMainDO();
        order.setId(100L);
        order.setOrderNo("ORD100");
        order.setBuyerUserId(1L);
        order.setOrderType("FAMILY_CARD");
        order.setOrderStatus("PAID");
        order.setPayableAmountCents(19900L);
        order.setCurrency("CNY");
        when(orderMainMapper.selectCount(any())).thenReturn(1L);
        when(orderMainMapper.selectList(any())).thenReturn(List.of(order));

        SysUserDO user = new SysUserDO();
        user.setId(1L);
        user.setOpenid("openid-1");
        user.setNickname("nick-1");
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(user));

        var orders = service.listOrders("PAID", "FAMILY_CARD", null, 1, 10);
        assertFalse(orders.items().isEmpty());
        assertEquals("ORD100", orders.items().get(0).orderNo());
        assertEquals("openid-1", orders.items().get(0).buyerOpenid());
        assertEquals(1, orders.total());
    }

    @Test
    void shouldListFamilyCardsWithWechatUser() {
        FamilyGroupDO group = new FamilyGroupDO();
        group.setId(10L);
        group.setGroupNo("G001");
        group.setOwnerUserId(1L);
        group.setStatus("ACTIVE");
        group.setMaxMembers(5);
        group.setCurrentMembers(2);
        when(familyGroupMapper.selectCount(any())).thenReturn(1L);
        when(familyGroupMapper.selectList(any())).thenReturn(List.of(group));

        SysUserDO owner = new SysUserDO();
        owner.setId(1L);
        owner.setOpenid("openid-owner");
        owner.setNickname("owner");
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(owner));

        var familyCards = service.listFamilyCards("ACTIVE", null, 1, 10);
        assertFalse(familyCards.items().isEmpty());
        assertEquals("G001", familyCards.items().get(0).groupNo());
        assertEquals("openid-owner", familyCards.items().get(0).ownerOpenid());
        assertEquals(1, familyCards.total());
    }

    @Test
    void shouldListFamilyMembersByKeyword() {
        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setId(1L);
        member.setMemberNo("M001");
        member.setMemberName("Tom");
        member.setStudentOrCardNo("S001");
        member.setPhone("13800000000");
        member.setStatus("ACTIVE");
        member.setGroupId(10L);
        when(familyMemberCardMapper.selectCount(any())).thenReturn(1L);
        when(familyMemberCardMapper.selectList(any())).thenReturn(List.of(member));

        FamilyGroupDO group = new FamilyGroupDO();
        group.setId(10L);
        group.setGroupNo("G001");
        group.setOwnerUserId(1L);
        when(familyGroupMapper.selectBatchIds(any())).thenReturn(List.of(group));

        SysUserDO owner = new SysUserDO();
        owner.setId(1L);
        owner.setNickname("owner");
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(owner));

        var result = service.listFamilyMembers("Tom", 1, 10);
        assertEquals(1, result.total());
        assertEquals("Tom", result.items().get(0).memberName());
        assertEquals("G001", result.items().get(0).groupNo());
        assertEquals("owner", result.items().get(0).ownerNickname());
    }

    @Test
    void shouldListFamilyCheckInsByMemberAndWechatUser() {
        FamilyCheckInDO checkIn = new FamilyCheckInDO();
        checkIn.setId(1L);
        checkIn.setCheckinNo("CK001");
        checkIn.setGroupId(10L);
        checkIn.setOwnerUserId(1L);
        checkIn.setFamilyMemberId(1001L);
        checkIn.setLatitude(new BigDecimal("31.2304160"));
        checkIn.setLongitude(new BigDecimal("121.4737010"));
        checkIn.setAddressText("addr");
        checkIn.setCheckedInAt(OffsetDateTime.parse("2026-03-31T09:00:00+08:00"));
        checkIn.setCreatedAt(OffsetDateTime.parse("2026-03-31T09:00:05+08:00"));
        when(familyCheckInMapper.selectCount(any())).thenReturn(1L);
        when(familyCheckInMapper.selectList(any())).thenReturn(List.of(checkIn));

        FamilyGroupDO group = new FamilyGroupDO();
        group.setId(10L);
        group.setGroupNo("FG001");
        when(familyGroupMapper.selectBatchIds(any())).thenReturn(List.of(group));

        SysUserDO user = new SysUserDO();
        user.setId(1L);
        user.setOpenid("openid-1");
        user.setNickname("nick-1");
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(user));

        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setId(1001L);
        member.setMemberNo("M001");
        member.setMemberName("Tom");
        when(familyMemberCardMapper.selectBatchIds(any())).thenReturn(List.of(member));

        var result = service.listFamilyCheckIns(1001L, 1L, 1, 10);
        assertEquals(1, result.total());
        assertEquals("CK001", result.items().get(0).checkinNo());
        assertEquals("FG001", result.items().get(0).groupNo());
        assertEquals("nick-1", result.items().get(0).ownerNickname());
        assertEquals("Tom", result.items().get(0).familyMemberName());
    }

    @Test
    void shouldDeriveWechatUserIdFromMemberWhenWechatUserIdIsMissing() {
        FamilyCheckInDO checkIn = new FamilyCheckInDO();
        checkIn.setId(2L);
        checkIn.setCheckinNo("CK002");
        checkIn.setGroupId(10L);
        checkIn.setOwnerUserId(8L);
        checkIn.setFamilyMemberId(1001L);
        checkIn.setLatitude(new BigDecimal("31.2304160"));
        checkIn.setLongitude(new BigDecimal("121.4737010"));
        checkIn.setAddressText("addr-2");
        checkIn.setCheckedInAt(OffsetDateTime.parse("2026-03-31T10:00:00+08:00"));
        checkIn.setCreatedAt(OffsetDateTime.parse("2026-03-31T10:00:05+08:00"));
        when(familyCheckInMapper.selectCount(any())).thenReturn(1L);
        when(familyCheckInMapper.selectList(any())).thenReturn(List.of(checkIn));

        FamilyGroupDO group = new FamilyGroupDO();
        group.setId(10L);
        group.setGroupNo("FG001");
        when(familyGroupMapper.selectBatchIds(any())).thenReturn(List.of(group));

        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setId(1001L);
        member.setMemberNo("M001");
        member.setMemberName("Tom");
        member.setAddedByUserId(8L);
        when(familyMemberCardMapper.selectById(1001L)).thenReturn(member);
        when(familyMemberCardMapper.selectBatchIds(any())).thenReturn(List.of(member));

        SysUserDO owner = new SysUserDO();
        owner.setId(8L);
        owner.setOpenid("openid-8");
        owner.setNickname("nick-8");
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(owner));

        var result = service.listFamilyCheckIns(1001L, null, 1, 10);

        verify(familyMemberCardMapper).selectById(1001L);
        assertEquals(1, result.total());
        assertEquals(8L, result.items().get(0).ownerUserId());
        assertEquals("nick-8", result.items().get(0).ownerNickname());
    }

    @Test
    void shouldUseOwnerScopeWhenWechatUserIdDerivedFromFamilyMember() {
        FamilyMemberCardDO memberForResolve = new FamilyMemberCardDO();
        memberForResolve.setId(1001L);
        memberForResolve.setAddedByUserId(8L);
        when(familyMemberCardMapper.selectById(1001L)).thenReturn(memberForResolve);

        FamilyCheckInDO checkIn = new FamilyCheckInDO();
        checkIn.setId(9L);
        checkIn.setCheckinNo("CK009");
        checkIn.setGroupId(10L);
        checkIn.setOwnerUserId(8L);
        checkIn.setFamilyMemberId(2001L);
        checkIn.setCreatedAt(OffsetDateTime.parse("2026-03-31T12:00:05+08:00"));
        when(familyCheckInMapper.selectCount(any())).thenReturn(1L);
        when(familyCheckInMapper.selectList(any())).thenReturn(List.of(checkIn));

        FamilyGroupDO group = new FamilyGroupDO();
        group.setId(10L);
        group.setGroupNo("FG001");
        when(familyGroupMapper.selectBatchIds(any())).thenReturn(List.of(group));

        FamilyMemberCardDO memberDetail = new FamilyMemberCardDO();
        memberDetail.setId(2001L);
        memberDetail.setMemberNo("M2001");
        memberDetail.setMemberName("Rose");
        when(familyMemberCardMapper.selectBatchIds(any())).thenReturn(List.of(memberDetail));

        SysUserDO owner = new SysUserDO();
        owner.setId(8L);
        owner.setOpenid("openid-8");
        owner.setNickname("nick-8");
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(owner));

        var result = service.listFamilyCheckIns(1001L, null, 1, 10);
        assertEquals(1, result.total());
        assertEquals("CK009", result.items().get(0).checkinNo());
        assertEquals(8L, result.items().get(0).ownerUserId());
    }

    @Test
    void shouldNotBackfillOwnerWhenCheckInOwnerUserIdIsNull() {
        FamilyCheckInDO checkIn = new FamilyCheckInDO();
        checkIn.setId(3L);
        checkIn.setCheckinNo("CK003");
        checkIn.setGroupId(10L);
        checkIn.setOwnerUserId(null);
        checkIn.setFamilyMemberId(1002L);
        checkIn.setCreatedAt(OffsetDateTime.parse("2026-03-31T11:00:05+08:00"));
        when(familyCheckInMapper.selectCount(any())).thenReturn(1L);
        when(familyCheckInMapper.selectList(any())).thenReturn(List.of(checkIn));

        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setId(1002L);
        member.setAddedByUserId(8L);
        member.setMemberNo("M002");
        member.setMemberName("Jerry");
        when(familyMemberCardMapper.selectList(any())).thenReturn(List.of(member));
        when(familyMemberCardMapper.selectBatchIds(any())).thenReturn(List.of(member));

        FamilyGroupDO group = new FamilyGroupDO();
        group.setId(10L);
        group.setGroupNo("FG001");
        when(familyGroupMapper.selectBatchIds(any())).thenReturn(List.of(group));

        SysUserDO owner = new SysUserDO();
        owner.setId(8L);
        owner.setOpenid("openid-8");
        owner.setNickname("nick-8");
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(owner));

        var result = service.listFamilyCheckIns(null, 8L, 1, 10);
        assertEquals(1, result.total());
        assertEquals("CK003", result.items().get(0).checkinNo());
        assertEquals(null, result.items().get(0).ownerUserId());
    }

    @Test
    void shouldReturnMonthlyIncomeStats() {
        MonthlyAmountRowDO total = new MonthlyAmountRowDO();
        total.setMonth("2026-01");
        total.setAmountCents(10000L);
        MonthlyAmountRowDO refund = new MonthlyAmountRowDO();
        refund.setMonth("2026-01");
        refund.setAmountCents(1000L);

        when(orderMainMapper.selectMonthlyPaidIncomeStats("2026-01", "2026-03", "FAMILY_CARD", 1L))
            .thenReturn(List.of(total));
        when(paymentRefundMapper.selectMonthlyRefundIncomeStats("2026-01", "2026-03", "FAMILY_CARD", 1L))
            .thenReturn(List.of(refund));

        var result = service.monthlyIncomeStats("2026-01", "2026-03", "FAMILY_CARD", 1L);
        assertEquals(1, result.monthlyTotalIncome().size());
        assertEquals("2026-01", result.monthlyTotalIncome().get(0).month());
        assertEquals(10000L, result.totalIncomeCents());
        assertEquals(1000L, result.totalRefundCents());
        assertEquals(9000L, result.netIncomeCents());
    }

    @Test
    void shouldRejectInvalidMonthRangeInMonthlyIncomeStats() {
        assertThrows(BusinessException.class,
            () -> service.monthlyIncomeStats("2026-02", "2026-01", "FAMILY_CARD", 1L));
    }
}

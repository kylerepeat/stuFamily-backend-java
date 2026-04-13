package com.stufamily.backend.adminquery.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stufamily.backend.adminquery.application.dto.AdminFamilyCardWithWechatUserView;
import com.stufamily.backend.adminquery.application.dto.AdminFamilyCheckInView;
import com.stufamily.backend.adminquery.application.dto.AdminFamilyMemberWithWechatUserView;
import com.stufamily.backend.adminquery.application.dto.AdminFilterOptionsView;
import com.stufamily.backend.adminquery.application.dto.AdminMonthlyAmountView;
import com.stufamily.backend.adminquery.application.dto.AdminMonthlyIncomeStatsView;
import com.stufamily.backend.adminquery.application.dto.AdminOrderWithWechatUserView;
import com.stufamily.backend.adminquery.application.dto.AdminSelectOptionView;
import com.stufamily.backend.adminquery.application.dto.AdminWechatUserView;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyCheckInDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyGroupDO;
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
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminQueryApplicationService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int LOOKUP_USER_LIMIT = 500;
    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final SysUserMapper sysUserMapper;
    private final OrderMainMapper orderMainMapper;
    private final PaymentRefundMapper paymentRefundMapper;
    private final FamilyCheckInMapper familyCheckInMapper;
    private final FamilyGroupMapper familyGroupMapper;
    private final FamilyMemberCardMapper familyMemberCardMapper;

    public AdminQueryApplicationService(SysUserMapper sysUserMapper, OrderMainMapper orderMainMapper,
                                        PaymentRefundMapper paymentRefundMapper,
                                        FamilyCheckInMapper familyCheckInMapper,
                                        FamilyGroupMapper familyGroupMapper,
                                        FamilyMemberCardMapper familyMemberCardMapper) {
        this.sysUserMapper = sysUserMapper;
        this.orderMainMapper = orderMainMapper;
        this.paymentRefundMapper = paymentRefundMapper;
        this.familyCheckInMapper = familyCheckInMapper;
        this.familyGroupMapper = familyGroupMapper;
        this.familyMemberCardMapper = familyMemberCardMapper;
    }

    public PageResult<AdminWechatUserView> listWechatUsers(
        String keyword, String status, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;

        long total = sysUserMapper.selectCount(buildWechatUserQuery(keyword, status));
        List<AdminWechatUserView> items = sysUserMapper.selectList(
                buildWechatUserQuery(keyword, status)
                    .orderByDesc(SysUserDO::getId)
                    .last("limit " + normalizedPageSize + " offset " + offset))
            .stream()
            .map(this::toWechatUserView)
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    public AdminFilterOptionsView listFilterOptions() {
        return new AdminFilterOptionsView(
            List.of(
                new AdminSelectOptionView("DRAFT", "草稿"),
                new AdminSelectOptionView("ON_SHELF", "上架"),
                new AdminSelectOptionView("OFF_SHELF", "下架")
            ),
            List.of(
                new AdminSelectOptionView("ACTIVE", "启用"),
                new AdminSelectOptionView("DISABLED", "停用"),
                new AdminSelectOptionView("LOCKED", "锁定")
            ),
            List.of(
                new AdminSelectOptionView("PENDING_PAYMENT", "待支付"),
                new AdminSelectOptionView("PAID", "已支付"),
                new AdminSelectOptionView("CANCELLED", "已取消"),
                new AdminSelectOptionView("EXPIRED", "已过期"),
                new AdminSelectOptionView("REFUNDED", "已退款")
            ),
            List.of(
                new AdminSelectOptionView("FAMILY_CARD", "家庭卡"),
                new AdminSelectOptionView("VALUE_ADDED_SERVICE", "增值服务")
            ),
            List.of(
                new AdminSelectOptionView("ACTIVE", "生效中"),
                new AdminSelectOptionView("CLOSED", "已停用")
            )
        );
    }

    public PageResult<AdminOrderWithWechatUserView> listOrders(
        String orderStatus, String orderType, String keyword, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;

        long total = orderMainMapper.selectCount(buildOrderQuery(orderStatus, orderType, keyword));
        List<OrderMainDO> orders = orderMainMapper.selectList(
            buildOrderQuery(orderStatus, orderType, keyword)
                .orderByDesc(OrderMainDO::getCreatedAt, OrderMainDO::getId)
                .last("limit " + normalizedPageSize + " offset " + offset)
        );
        Map<Long, SysUserDO> userMap = loadUserMap(orders.stream().map(OrderMainDO::getBuyerUserId).collect(Collectors.toSet()));
        List<AdminOrderWithWechatUserView> items = orders.stream()
            .map(order -> {
                SysUserDO user = userMap.get(order.getBuyerUserId());
                return new AdminOrderWithWechatUserView(
                    order.getId(),
                    order.getOrderNo(),
                    order.getBuyerUserId(),
                    order.getOrderType(),
                    order.getOrderStatus(),
                    order.getPayableAmountCents(),
                    order.getCurrency(),
                    order.getCreatedAt(),
                    order.getPaidAt(),
                    user == null ? null : user.getOpenid(),
                    user == null ? null : user.getNickname(),
                    user == null ? null : user.getAvatarUrl()
                );
            })
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    public PageResult<AdminFamilyCardWithWechatUserView> listFamilyCards(
        String familyStatus, String keyword, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;

        long total = familyGroupMapper.selectCount(buildFamilyCardQuery(familyStatus, keyword));
        List<FamilyGroupDO> familyGroups = familyGroupMapper.selectList(
            buildFamilyCardQuery(familyStatus, keyword)
                .orderByDesc(FamilyGroupDO::getCreatedAt, FamilyGroupDO::getId)
                .last("limit " + normalizedPageSize + " offset " + offset)
        );
        Map<Long, SysUserDO> userMap = loadUserMap(familyGroups.stream().map(FamilyGroupDO::getOwnerUserId).collect(Collectors.toSet()));
        List<AdminFamilyCardWithWechatUserView> items = familyGroups.stream()
            .map(group -> {
                SysUserDO owner = userMap.get(group.getOwnerUserId());
                return new AdminFamilyCardWithWechatUserView(
                    group.getId(),
                    group.getGroupNo(),
                    group.getSourceOrderId(),
                    group.getOwnerUserId(),
                    group.getMaxMembers(),
                    group.getCurrentMembers(),
                    group.getStatus(),
                    group.getActivatedAt(),
                    group.getExpireAt(),
                    group.getCreatedAt(),
                    owner == null ? null : owner.getOpenid(),
                    owner == null ? null : owner.getNickname(),
                    owner == null ? null : owner.getAvatarUrl()
                );
            })
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    public PageResult<AdminFamilyMemberWithWechatUserView> listFamilyMembers(
        String keyword, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;

        LambdaQueryWrapper<FamilyMemberCardDO> countQuery = buildFamilyMemberQuery(keyword);
        LambdaQueryWrapper<FamilyMemberCardDO> listQuery = buildFamilyMemberQuery(keyword);
        long total = familyMemberCardMapper.selectCount(countQuery);
        List<FamilyMemberCardDO> members = familyMemberCardMapper.selectList(
            listQuery.orderByDesc(FamilyMemberCardDO::getJoinedAt, FamilyMemberCardDO::getId)
                .last("limit " + normalizedPageSize + " offset " + offset)
        );

        Set<Long> groupIds = members.stream().map(FamilyMemberCardDO::getGroupId).collect(Collectors.toSet());
        Map<Long, FamilyGroupDO> groupMap = loadGroupMap(groupIds);
        Map<Long, SysUserDO> ownerMap = loadUserMap(groupMap.values().stream()
            .map(FamilyGroupDO::getOwnerUserId)
            .collect(Collectors.toSet()));

        List<AdminFamilyMemberWithWechatUserView> items = members.stream().map(member -> {
            FamilyGroupDO group = groupMap.get(member.getGroupId());
            SysUserDO owner = group == null ? null : ownerMap.get(group.getOwnerUserId());
            return new AdminFamilyMemberWithWechatUserView(
                member.getId(),
                member.getMemberNo(),
                member.getMemberName(),
                member.getStudentOrCardNo(),
                member.getPhone(),
                member.getStatus(),
                member.getJoinedAt(),
                member.getGroupId(),
                group == null ? null : group.getGroupNo(),
                group == null ? null : group.getOwnerUserId(),
                owner == null ? null : owner.getNickname()
            );
        }).toList();

        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    public PageResult<AdminFamilyCheckInView> listFamilyCheckIns(
        Long familyMemberId, Long wechatUserId, Integer pageNo, Integer pageSize) {
        validatePositiveId(familyMemberId, "family_member_id");
        validatePositiveId(wechatUserId, "wechat_user_id");

        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;

        Long effectiveWechatUserId = resolveEffectiveWechatUserId(familyMemberId, wechatUserId);
        boolean keepMemberFilter = familyMemberId != null && wechatUserId != null;

        LambdaQueryWrapper<FamilyCheckInDO> countQuery =
            buildFamilyCheckInQuery(keepMemberFilter ? familyMemberId : null, effectiveWechatUserId);
        LambdaQueryWrapper<FamilyCheckInDO> listQuery =
            buildFamilyCheckInQuery(keepMemberFilter ? familyMemberId : null, effectiveWechatUserId);
        long total = familyCheckInMapper.selectCount(countQuery);
        List<FamilyCheckInDO> checkIns = familyCheckInMapper.selectList(
            listQuery.orderByDesc(FamilyCheckInDO::getCreatedAt, FamilyCheckInDO::getId)
                .last("limit " + normalizedPageSize + " offset " + offset)
        );

        Map<Long, FamilyGroupDO> groupMap = loadGroupMap(checkIns.stream()
            .map(FamilyCheckInDO::getGroupId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet()));
        Map<Long, SysUserDO> ownerMap = loadUserMap(checkIns.stream()
            .map(FamilyCheckInDO::getOwnerUserId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet()));
        Map<Long, FamilyMemberCardDO> memberMap = loadMemberMap(checkIns.stream()
            .map(FamilyCheckInDO::getFamilyMemberId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet()));

        List<AdminFamilyCheckInView> items = checkIns.stream().map(checkIn -> {
            FamilyGroupDO group = groupMap.get(checkIn.getGroupId());
            SysUserDO owner = ownerMap.get(checkIn.getOwnerUserId());
            FamilyMemberCardDO member = memberMap.get(checkIn.getFamilyMemberId());
            return new AdminFamilyCheckInView(
                checkIn.getId(),
                checkIn.getCheckinNo(),
                checkIn.getGroupId(),
                group == null ? null : group.getGroupNo(),
                checkIn.getOwnerUserId(),
                owner == null ? null : owner.getOpenid(),
                owner == null ? null : owner.getNickname(),
                checkIn.getFamilyMemberId(),
                member == null ? null : member.getMemberNo(),
                member == null ? null : member.getMemberName(),
                checkIn.getLatitude(),
                checkIn.getLongitude(),
                checkIn.getAddressText(),
                checkIn.getCheckedInAt(),
                checkIn.getCreatedAt()
            );
        }).toList();

        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    private Long resolveEffectiveWechatUserId(Long familyMemberId, Long wechatUserId) {
        if (wechatUserId != null) {
            return wechatUserId;
        }
        if (familyMemberId == null) {
            return null;
        }
        FamilyMemberCardDO member = familyMemberCardMapper.selectById(familyMemberId);
        if (member == null) {
            return null;
        }
        return member.getAddedByUserId();
    }

    public AdminMonthlyIncomeStatsView monthlyIncomeStats(
        String startMonth, String endMonth, String productType, Long productId) {
        String normalizedStartMonth = normalizeMonth(startMonth, "start_month");
        String normalizedEndMonth = normalizeMonth(endMonth, "end_month");
        validateMonthRange(normalizedStartMonth, normalizedEndMonth);
        String normalizedProductType = normalizeProductType(productType);
        if (productId != null && productId <= 0L) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "product_id must be greater than zero");
        }

        List<MonthlyAmountRowDO> totalIncomeRows = orderMainMapper.selectMonthlyPaidIncomeStats(
            normalizedStartMonth, normalizedEndMonth, normalizedProductType, productId
        );
        List<MonthlyAmountRowDO> refundIncomeRows = paymentRefundMapper.selectMonthlyRefundIncomeStats(
            normalizedStartMonth, normalizedEndMonth, normalizedProductType, productId
        );

        List<AdminMonthlyAmountView> monthlyTotalIncome = toMonthlyAmountViews(totalIncomeRows);
        List<AdminMonthlyAmountView> monthlyRefundIncome = toMonthlyAmountViews(refundIncomeRows);
        long totalIncomeCents = sumAmount(totalIncomeRows);
        long totalRefundCents = sumAmount(refundIncomeRows);
        return new AdminMonthlyIncomeStatsView(
            monthlyTotalIncome,
            monthlyRefundIncome,
            totalIncomeCents,
            totalRefundCents,
            totalIncomeCents - totalRefundCents
        );
    }

    private LambdaQueryWrapper<SysUserDO> buildWechatUserQuery(String keyword, String status) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<SysUserDO>()
            .isNotNull(SysUserDO::getOpenid)
            .in(SysUserDO::getUserType, List.of("WECHAT", "HYBRID"));
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysUserDO::getStatus, status.trim().toUpperCase());
        }
        appendUserKeywordFilter(wrapper, keyword);
        return wrapper;
    }

    private LambdaQueryWrapper<OrderMainDO> buildOrderQuery(String orderStatus, String orderType, String keyword) {
        LambdaQueryWrapper<OrderMainDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(orderStatus)) {
            wrapper.eq(OrderMainDO::getOrderStatus, orderStatus.trim().toUpperCase());
        }
        if (StringUtils.hasText(orderType)) {
            wrapper.eq(OrderMainDO::getOrderType, orderType.trim().toUpperCase());
        }
        appendOrderKeywordFilter(wrapper, keyword);
        return wrapper;
    }

    private LambdaQueryWrapper<FamilyGroupDO> buildFamilyCardQuery(String familyStatus, String keyword) {
        LambdaQueryWrapper<FamilyGroupDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(familyStatus)) {
            wrapper.eq(FamilyGroupDO::getStatus, familyStatus.trim().toUpperCase());
        }
        appendFamilyKeywordFilter(wrapper, keyword);
        return wrapper;
    }

    private void appendUserKeywordFilter(LambdaQueryWrapper<SysUserDO> wrapper, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        wrapper.and(w -> w.like(SysUserDO::getOpenid, keyword)
            .or().like(SysUserDO::getNickname, keyword)
            .or().like(SysUserDO::getPhone, keyword)
            .or().like(SysUserDO::getUserNo, keyword));
    }

    private void appendOrderKeywordFilter(LambdaQueryWrapper<OrderMainDO> wrapper, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        Set<Long> matchedUserIds = lookupUserIdsByKeyword(keyword);
        if (matchedUserIds.isEmpty()) {
            wrapper.like(OrderMainDO::getOrderNo, keyword);
            return;
        }
        wrapper.and(w -> w.like(OrderMainDO::getOrderNo, keyword)
            .or().in(OrderMainDO::getBuyerUserId, matchedUserIds));
    }

    private void appendFamilyKeywordFilter(LambdaQueryWrapper<FamilyGroupDO> wrapper, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        Set<Long> matchedUserIds = lookupUserIdsByKeyword(keyword);
        if (matchedUserIds.isEmpty()) {
            wrapper.like(FamilyGroupDO::getGroupNo, keyword);
            return;
        }
        wrapper.and(w -> w.like(FamilyGroupDO::getGroupNo, keyword)
            .or().in(FamilyGroupDO::getOwnerUserId, matchedUserIds));
    }

    private Set<Long> lookupUserIdsByKeyword(String keyword) {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUserDO>()
                .select(SysUserDO::getId)
                .isNotNull(SysUserDO::getOpenid)
                .and(w -> w.like(SysUserDO::getOpenid, keyword)
                    .or().like(SysUserDO::getNickname, keyword)
                    .or().like(SysUserDO::getPhone, keyword)
                    .or().like(SysUserDO::getUserNo, keyword))
                .last("limit " + LOOKUP_USER_LIMIT))
            .stream()
            .map(SysUserDO::getId)
            .collect(Collectors.toSet());
    }

    private Set<Long> lookupUserIdsByNickname(String keyword) {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUserDO>()
                .like(SysUserDO::getNickname, keyword)
                .last("limit " + LOOKUP_USER_LIMIT))
            .stream()
            .map(SysUserDO::getId)
            .collect(Collectors.toSet());
    }

    private Set<Long> lookupGroupIdsByOwnerIds(Set<Long> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return Collections.emptySet();
        }
        return familyGroupMapper.selectList(new LambdaQueryWrapper<FamilyGroupDO>()
                .in(FamilyGroupDO::getOwnerUserId, ownerIds)
                .last("limit " + LOOKUP_USER_LIMIT))
            .stream()
            .map(FamilyGroupDO::getId)
            .collect(Collectors.toSet());
    }

    private Set<Long> lookupGroupIdsByGroupNo(String keyword) {
        return familyGroupMapper.selectList(new LambdaQueryWrapper<FamilyGroupDO>()
                .like(FamilyGroupDO::getGroupNo, keyword)
                .last("limit " + LOOKUP_USER_LIMIT))
            .stream()
            .map(FamilyGroupDO::getId)
            .collect(Collectors.toSet());
    }

    private LambdaQueryWrapper<FamilyMemberCardDO> buildFamilyMemberQuery(String keyword) {
        LambdaQueryWrapper<FamilyMemberCardDO> wrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.hasText(keyword)) {
            return wrapper;
        }

        Set<Long> matchedGroupIds = new HashSet<>(lookupGroupIdsByGroupNo(keyword));
        matchedGroupIds.addAll(lookupGroupIdsByOwnerIds(lookupUserIdsByNickname(keyword)));

        wrapper.and(w -> w.like(FamilyMemberCardDO::getPhone, keyword)
            .or().like(FamilyMemberCardDO::getMemberName, keyword)
            .or().like(FamilyMemberCardDO::getStudentOrCardNo, keyword)
            .or().like(FamilyMemberCardDO::getMemberNo, keyword));

        if (!matchedGroupIds.isEmpty()) {
            wrapper.or(w -> w.in(FamilyMemberCardDO::getGroupId, matchedGroupIds));
        }
        return wrapper;
    }

    private LambdaQueryWrapper<FamilyCheckInDO> buildFamilyCheckInQuery(Long familyMemberId, Long wechatUserId) {
        LambdaQueryWrapper<FamilyCheckInDO> wrapper = new LambdaQueryWrapper<>();
        if (familyMemberId != null) {
            wrapper.eq(FamilyCheckInDO::getFamilyMemberId, familyMemberId);
        }
        if (wechatUserId != null) {
            wrapper.eq(FamilyCheckInDO::getOwnerUserId, wechatUserId);
        }
        return wrapper;
    }

    private Map<Long, SysUserDO> loadUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(SysUserDO::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, FamilyGroupDO> loadGroupMap(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return familyGroupMapper.selectBatchIds(groupIds).stream()
            .collect(Collectors.toMap(FamilyGroupDO::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, FamilyMemberCardDO> loadMemberMap(Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return familyMemberCardMapper.selectBatchIds(memberIds).stream()
            .collect(Collectors.toMap(FamilyMemberCardDO::getId, Function.identity(), (a, b) -> a));
    }

    private AdminWechatUserView toWechatUserView(SysUserDO user) {
        return new AdminWechatUserView(
            user.getId(),
            user.getUserNo(),
            user.getUserType(),
            user.getStatus(),
            user.getOpenid(),
            user.getNickname(),
            user.getAvatarUrl(),
            user.getPhone(),
            user.getLastLoginAt(),
            user.getCreatedAt()
        );
    }

    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo <= 0) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private List<AdminMonthlyAmountView> toMonthlyAmountViews(List<MonthlyAmountRowDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
            .map(row -> new AdminMonthlyAmountView(row.getMonth(), row.getAmountCents() == null ? 0L : row.getAmountCents()))
            .toList();
    }

    private long sumAmount(List<MonthlyAmountRowDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0L;
        }
        return rows.stream()
            .map(MonthlyAmountRowDO::getAmountCents)
            .filter(amount -> amount != null && amount > 0)
            .mapToLong(Long::longValue)
            .sum();
    }

    private String normalizeMonth(String month, String fieldName) {
        if (!StringUtils.hasText(month)) {
            return null;
        }
        String normalized = month.trim();
        if (!MONTH_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, fieldName + " must be in yyyy-MM format");
        }
        return normalized;
    }

    private void validateMonthRange(String startMonth, String endMonth) {
        if ((startMonth == null) != (endMonth == null)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "start_month and end_month must both be provided");
        }
        if (startMonth != null && endMonth != null && startMonth.compareTo(endMonth) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "start_month cannot be greater than end_month");
        }
    }

    private String normalizeProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            return null;
        }
        String normalized = productType.trim().toUpperCase();
        if (!"FAMILY_CARD".equals(normalized) && !"VALUE_ADDED_SERVICE".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "invalid product_type");
        }
        return normalized;
    }

    private void validatePositiveId(Long value, String fieldName) {
        if (value != null && value <= 0L) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, fieldName + " must be greater than zero");
        }
    }
}

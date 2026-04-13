package com.stufamily.backend.family.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stufamily.backend.family.application.command.AddFamilyCheckInCommand;
import com.stufamily.backend.family.application.command.AddFamilyMemberCommand;
import com.stufamily.backend.family.application.dto.FamilyCheckInView;
import com.stufamily.backend.family.application.dto.FamilyGroupQuotaView;
import com.stufamily.backend.family.application.dto.FamilyMemberView;
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
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FamilyApplicationService {
    private static final Duration CHECK_IN_RATE_LIMIT_WINDOW = Duration.ofMinutes(5);
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final FamilyGroupMapper familyGroupMapper;
    private final FamilyMemberCardMapper familyMemberCardMapper;
    private final FamilyCheckInMapper familyCheckInMapper;
    private final ProductMapper productMapper;
    private final ProductFamilyCardPlanMapper familyCardPlanMapper;

    public FamilyApplicationService(FamilyGroupMapper familyGroupMapper, FamilyMemberCardMapper familyMemberCardMapper,
                                    FamilyCheckInMapper familyCheckInMapper,
                                    ProductMapper productMapper, ProductFamilyCardPlanMapper familyCardPlanMapper) {
        this.familyGroupMapper = familyGroupMapper;
        this.familyMemberCardMapper = familyMemberCardMapper;
        this.familyCheckInMapper = familyCheckInMapper;
        this.productMapper = productMapper;
        this.familyCardPlanMapper = familyCardPlanMapper;
    }

    @Transactional
    public FamilyCheckInView addCheckIn(AddFamilyCheckInCommand command) {
        validateLocation(command.latitude(), command.longitude());
        if (command.checkedInAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "checkedInAt is required");
        }
        if (!StringUtils.hasText(command.addressText())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "addressText is required");
        }
        OffsetDateTime now = OffsetDateTime.now();
        enforceCheckInRateLimit(command.ownerUserId(), now);
        FamilyMemberCardDO member = null;
        FamilyGroupDO group;
        if (command.familyMemberId() != null) {
            member = requireFamilyMemberForCheckIn(command.ownerUserId(), command.familyMemberId(), command.checkedInAt());
            group = requireActiveGroupAtById(command.ownerUserId(), member.getGroupId(), command.checkedInAt());
            if (StringUtils.hasText(command.groupNo()) && !command.groupNo().trim().equals(group.getGroupNo())) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "groupNo does not match familyMemberId");
            }
        } else {
            group = requireActiveGroupAt(command.ownerUserId(), command.groupNo(), command.checkedInAt());
        }
        FamilyCheckInDO checkIn = new FamilyCheckInDO();
        checkIn.setCheckinNo("CK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
        checkIn.setGroupId(group.getId());
        checkIn.setOwnerUserId(command.ownerUserId());
        checkIn.setFamilyMemberId(member == null ? null : member.getId());
        checkIn.setLatitude(command.latitude());
        checkIn.setLongitude(command.longitude());
        checkIn.setAddressText(command.addressText().trim());
        checkIn.setCheckedInAt(command.checkedInAt());
        checkIn.setCreatedAt(now);
        checkIn.setUpdatedAt(now);
        familyCheckInMapper.insert(checkIn);
        return new FamilyCheckInView(
            checkIn.getCheckinNo(),
            group.getGroupNo(),
            checkIn.getOwnerUserId(),
            checkIn.getFamilyMemberId(),
            checkIn.getLatitude(),
            checkIn.getLongitude(),
            checkIn.getAddressText(),
            checkIn.getCheckedInAt()
        );
    }

    private void enforceCheckInRateLimit(Long ownerUserId, OffsetDateTime now) {
        if (ownerUserId == null || ownerUserId <= 0) {
            return;
        }
        FamilyCheckInDO latest = familyCheckInMapper.selectOne(
            new LambdaQueryWrapper<FamilyCheckInDO>()
                .eq(FamilyCheckInDO::getOwnerUserId, ownerUserId)
                .orderByDesc(FamilyCheckInDO::getCreatedAt, FamilyCheckInDO::getId)
                .last("limit 1")
        );
        if (latest == null || latest.getCreatedAt() == null) {
            return;
        }
        OffsetDateTime nextAllowedAt = latest.getCreatedAt().plus(CHECK_IN_RATE_LIMIT_WINDOW);
        if (now.isBefore(nextAllowedAt)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "5分钟内只能打卡一次");
        }
    }

    @Transactional
    public FamilyMemberView addMember(AddFamilyMemberCommand command) {
        FamilyGroupDO group = requireActiveGroup(command.ownerUserId(), command.groupNo());
        if (group.getCurrentMembers() >= group.getMaxMembers()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "family member count exceeds purchased plan");
        }
        FamilyMemberCardDO exists = familyMemberCardMapper.selectOne(
            new LambdaQueryWrapper<FamilyMemberCardDO>()
                .eq(FamilyMemberCardDO::getGroupId, group.getId())
                .eq(FamilyMemberCardDO::getStudentOrCardNo, command.studentOrCardNo())
        );
        if (exists != null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "member card already exists");
        }
        OffsetDateTime now = OffsetDateTime.now();
        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setGroupId(group.getId());
        member.setMemberNo("M" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT));
        member.setMemberName(command.memberName());
        member.setStudentOrCardNo(command.studentOrCardNo());
        member.setPhone(command.phone());
        member.setCardReceivedDate(command.joinedAt().toLocalDate());
        member.setAddedByUserId(command.ownerUserId());
        member.setStatus("ACTIVE");
        member.setJoinedAt(command.joinedAt());
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        familyMemberCardMapper.insert(member);

        group.setCurrentMembers(group.getCurrentMembers() + 1);
        group.setUpdatedAt(now);
        familyGroupMapper.updateById(group);

        List<FamilyMemberListRowDO> rows = familyMemberCardMapper.selectPageByGroupAndKeyword(
            group.getId(), command.ownerUserId(), member.getMemberNo(), 1, 0);
        if (!rows.isEmpty()) {
            return toView(rows.get(0));
        }
        return toView(member, group);
    }

    @Transactional(readOnly = true)
    public PageResult<FamilyMemberView> searchMembers(Long ownerUserId, String groupNo, String keyword, Integer pageNo,
                                                      Integer pageSize) {
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;

        if (StringUtils.hasText(groupNo)) {
            FamilyGroupDO group = findActiveGroup(ownerUserId, groupNo);
            if (group == null) {
                return PageResult.of(List.of(), 0, normalizedPageNo, normalizedPageSize);
            }
            long total = familyMemberCardMapper.countByGroupAndKeyword(group.getId(), ownerUserId, keyword);
            List<FamilyMemberView> items = familyMemberCardMapper.selectPageByGroupAndKeyword(
                    group.getId(), ownerUserId, keyword, normalizedPageSize, offset)
                .stream()
                .map(this::toView)
                .toList();
            return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
        }

        long total = familyMemberCardMapper.countByOwnerAndKeyword(ownerUserId, keyword);
        List<FamilyMemberView> items = familyMemberCardMapper.selectPageByOwnerAndKeyword(
                ownerUserId, keyword, normalizedPageSize, offset)
            .stream()
            .map(this::toView)
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    @Transactional(readOnly = true)
    public FamilyGroupQuotaView getCurrentGroupQuota(Long ownerUserId) {
        List<FamilyGroupDO> groups = findActiveGroups(ownerUserId);
        if (groups.isEmpty()) {
            return FamilyGroupQuotaView.empty();
        }
        List<Long> productIds = groups.stream()
            .map(FamilyGroupDO::getFamilyCardProductId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
        List<Long> planIds = groups.stream()
            .map(FamilyGroupDO::getFamilyCardPlanId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();

        Map<Long, ProductDO> productMap = productIds.isEmpty()
            ? Collections.emptyMap()
            : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(ProductDO::getId, Function.identity(), (a, b) -> a));
        Map<Long, ProductFamilyCardPlanDO> planMap = planIds.isEmpty()
            ? Collections.emptyMap()
            : familyCardPlanMapper.selectBatchIds(planIds).stream()
                .collect(Collectors.toMap(ProductFamilyCardPlanDO::getId, Function.identity(), (a, b) -> a));

        List<FamilyGroupQuotaView.GroupQuotaView> groupViews = groups.stream()
            .map(group -> {
                int maxMembers = group.getMaxMembers() == null ? 0 : group.getMaxMembers();
                int currentMembers = group.getCurrentMembers() == null ? 0 : group.getCurrentMembers();
                int availableMembers = Math.max(maxMembers - currentMembers, 0);
                ProductDO product = productMap.get(group.getFamilyCardProductId());
                ProductFamilyCardPlanDO plan = planMap.get(group.getFamilyCardPlanId());
                return new FamilyGroupQuotaView.GroupQuotaView(
                    group.getGroupNo(),
                    group.getStatus(),
                    group.getExpireAt(),
                    maxMembers,
                    currentMembers,
                    availableMembers,
                    product == null ? "FAMILY_CARD" : product.getProductType(),
                    group.getFamilyCardProductId(),
                    product == null ? null : product.getTitle(),
                    group.getFamilyCardPlanId(),
                    plan == null ? null : plan.getDurationType(),
                    plan == null ? null : plan.getDurationMonths()
                );
            })
            .toList();

        return new FamilyGroupQuotaView(true, groupViews);
    }

    @Transactional
    public void cancelExpiredCard(Long ownerUserId, String memberNo) {
        FamilyMemberCardDO member = familyMemberCardMapper.selectOne(
            new LambdaQueryWrapper<FamilyMemberCardDO>().eq(FamilyMemberCardDO::getMemberNo, memberNo)
        );
        if (member == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "member not found");
        }
        FamilyGroupDO group = familyGroupMapper.selectById(member.getGroupId());
        if (group == null || !ownerUserId.equals(group.getOwnerUserId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "family group not found");
        }
        if ("CANCELLED".equals(member.getStatus())) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        member.setStatus("CANCELLED");
        member.setCancelledAt(now);
        member.setUpdatedAt(now);
        familyMemberCardMapper.updateById(member);
    }

    private FamilyGroupDO requireActiveGroup(Long ownerUserId) {
        FamilyGroupDO group = findActiveGroup(ownerUserId);
        if (group == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "active family card not found");
        }
        return group;
    }

    private FamilyGroupDO requireActiveGroup(Long ownerUserId, String groupNo) {
        FamilyGroupDO group = findActiveGroup(ownerUserId, groupNo);
        if (group == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "active family card not found");
        }
        return group;
    }

    private FamilyGroupDO requireActiveGroupAt(Long ownerUserId, String groupNo, OffsetDateTime checkedInAt) {
        FamilyGroupDO group = findActiveGroupAt(ownerUserId, groupNo, checkedInAt);
        if (group == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "family card is not active at checkedInAt");
        }
        return group;
    }

    private FamilyGroupDO requireActiveGroupAtById(Long ownerUserId, Long groupId, OffsetDateTime checkedInAt) {
        FamilyGroupDO group = findActiveGroupAtById(ownerUserId, groupId, checkedInAt);
        if (group == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "family card is not active at checkedInAt");
        }
        return group;
    }

    private FamilyMemberCardDO requireFamilyMemberForCheckIn(Long ownerUserId, Long familyMemberId, OffsetDateTime checkedInAt) {
        if (familyMemberId == null || familyMemberId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "familyMemberId is invalid");
        }
        FamilyMemberCardDO member = familyMemberCardMapper.selectById(familyMemberId);
        if (member == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "family member not found");
        }
        if (!"ACTIVE".equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "family member is not active");
        }
        if (member.getJoinedAt() != null && member.getJoinedAt().isAfter(checkedInAt)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "family member is not active at checkedInAt");
        }
        FamilyGroupDO group = familyGroupMapper.selectById(member.getGroupId());
        if (group == null || !ownerUserId.equals(group.getOwnerUserId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "family member does not belong to current user");
        }
        return member;
    }

    private FamilyGroupDO findActiveGroup(Long ownerUserId) {
        return findActiveGroup(ownerUserId, null);
    }

    private FamilyGroupDO findActiveGroup(Long ownerUserId, String groupNo) {
        LambdaQueryWrapper<FamilyGroupDO> wrapper = new LambdaQueryWrapper<FamilyGroupDO>()
            .eq(FamilyGroupDO::getOwnerUserId, ownerUserId)
            .eq(FamilyGroupDO::getStatus, "ACTIVE")
            .gt(FamilyGroupDO::getExpireAt, OffsetDateTime.now());
        if (StringUtils.hasText(groupNo)) {
            wrapper.eq(FamilyGroupDO::getGroupNo, groupNo.trim());
            wrapper.last("limit 1");
            return familyGroupMapper.selectOne(wrapper);
        }
        wrapper.orderByDesc(FamilyGroupDO::getId).last("limit 1");
        return familyGroupMapper.selectOne(wrapper);
    }

    private FamilyGroupDO findActiveGroupAt(Long ownerUserId, String groupNo, OffsetDateTime checkedInAt) {
        LambdaQueryWrapper<FamilyGroupDO> wrapper = new LambdaQueryWrapper<FamilyGroupDO>()
            .eq(FamilyGroupDO::getOwnerUserId, ownerUserId)
            .eq(FamilyGroupDO::getStatus, "ACTIVE")
            .le(FamilyGroupDO::getActivatedAt, checkedInAt)
            .ge(FamilyGroupDO::getExpireAt, checkedInAt);
        if (StringUtils.hasText(groupNo)) {
            wrapper.eq(FamilyGroupDO::getGroupNo, groupNo.trim());
            wrapper.last("limit 1");
            return familyGroupMapper.selectOne(wrapper);
        }
        wrapper.orderByDesc(FamilyGroupDO::getActivatedAt).orderByDesc(FamilyGroupDO::getId).last("limit 1");
        return familyGroupMapper.selectOne(wrapper);
    }

    private FamilyGroupDO findActiveGroupAtById(Long ownerUserId, Long groupId, OffsetDateTime checkedInAt) {
        if (groupId == null || groupId <= 0) {
            return null;
        }
        return familyGroupMapper.selectOne(
            new LambdaQueryWrapper<FamilyGroupDO>()
                .eq(FamilyGroupDO::getId, groupId)
                .eq(FamilyGroupDO::getOwnerUserId, ownerUserId)
                .eq(FamilyGroupDO::getStatus, "ACTIVE")
                .le(FamilyGroupDO::getActivatedAt, checkedInAt)
                .ge(FamilyGroupDO::getExpireAt, checkedInAt)
                .last("limit 1")
        );
    }

    private List<FamilyGroupDO> findActiveGroups(Long ownerUserId) {
        return familyGroupMapper.selectList(
            new LambdaQueryWrapper<FamilyGroupDO>()
                .eq(FamilyGroupDO::getOwnerUserId, ownerUserId)
                .eq(FamilyGroupDO::getStatus, "ACTIVE")
                .gt(FamilyGroupDO::getExpireAt, OffsetDateTime.now())
                .orderByDesc(FamilyGroupDO::getActivatedAt)
                .orderByDesc(FamilyGroupDO::getId)
        );
    }

    private FamilyMemberView toView(FamilyMemberCardDO member) {
        return toView(member, null);
    }

    private FamilyMemberView toView(FamilyMemberCardDO member, FamilyGroupDO group) {
        return new FamilyMemberView(
            member.getMemberNo(),
            member.getMemberName(),
            member.getStudentOrCardNo(),
            member.getPhone(),
            member.getJoinedAt(),
            member.getStatus(),
            group == null ? null : group.getExpireAt(),
            null
        );
    }

    private FamilyMemberView toView(FamilyMemberListRowDO row) {
        return new FamilyMemberView(
            row.getMemberNo(),
            row.getMemberName(),
            row.getStudentOrCardNo(),
            row.getPhone(),
            row.getJoinedAt(),
            row.getStatus(),
            row.getFamilyGroupExpireAt(),
            row.getWechatAvatarUrl()
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

    private void validateLocation(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "latitude and longitude are required");
        }
        if (latitude.compareTo(new BigDecimal("-90")) < 0 || latitude.compareTo(new BigDecimal("90")) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "latitude out of range");
        }
        if (longitude.compareTo(new BigDecimal("-180")) < 0 || longitude.compareTo(new BigDecimal("180")) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "longitude out of range");
        }
    }
}

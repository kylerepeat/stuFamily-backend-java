package com.stufamily.backend.home.application.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stufamily.backend.home.application.command.AdminBannerUpdateCommand;
import com.stufamily.backend.home.application.command.AdminNoticeCreateCommand;
import com.stufamily.backend.home.application.command.AdminProductUpdateCommand;
import com.stufamily.backend.home.application.command.AdminReplyParentMessageCommand;
import com.stufamily.backend.home.application.command.AdminSiteProfileUpdateCommand;
import com.stufamily.backend.home.application.command.CreateParentMessageCommand;
import com.stufamily.backend.home.application.dto.AdminHomeNoticeView;
import com.stufamily.backend.home.application.dto.AdminParentMessageDetailView;
import com.stufamily.backend.home.application.dto.AdminParentMessageNodeView;
import com.stufamily.backend.home.application.dto.AdminParentMessageView;
import com.stufamily.backend.home.application.dto.AdminHomepageBannerView;
import com.stufamily.backend.home.application.dto.AdminProductDetailView;
import com.stufamily.backend.home.application.dto.AdminSiteProfileView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.home.application.dto.HomeProductDetailView;
import com.stufamily.backend.home.application.dto.HomeProductView;
import com.stufamily.backend.home.application.dto.HomePageView;
import com.stufamily.backend.home.application.dto.ParentMessageReplyView;
import com.stufamily.backend.home.application.dto.ParentMessageView;
import com.stufamily.backend.home.infrastructure.persistence.dataobject.HomepageBannerDO;
import com.stufamily.backend.home.infrastructure.persistence.dataobject.HomeNoticeDO;
import com.stufamily.backend.home.infrastructure.persistence.dataobject.ParentMessageDO;
import com.stufamily.backend.home.infrastructure.persistence.dataobject.SiteProfileDO;
import com.stufamily.backend.home.infrastructure.persistence.mapper.HomepageBannerMapper;
import com.stufamily.backend.home.infrastructure.persistence.mapper.HomeNoticeMapper;
import com.stufamily.backend.home.infrastructure.persistence.mapper.ParentMessageMapper;
import com.stufamily.backend.home.infrastructure.persistence.mapper.SiteProfileMapper;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.mapper.SysUserMapper;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyGroupDO;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyGroupMapper;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductDO;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductFamilyCardPlanDO;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductValueAddedSkuDO;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductFamilyCardPlanMapper;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductMapper;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductValueAddedSkuMapper;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HomeApplicationService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final HomepageBannerMapper homepageBannerMapper;
    private final HomeNoticeMapper homeNoticeMapper;
    private final SiteProfileMapper siteProfileMapper;
    private final ParentMessageMapper parentMessageMapper;
    private final SysUserMapper sysUserMapper;
    private final FamilyGroupMapper familyGroupMapper;
    private final ProductMapper productMapper;
    private final ProductFamilyCardPlanMapper familyCardPlanMapper;
    private final ProductValueAddedSkuMapper valueAddedSkuMapper;

    public HomeApplicationService(HomepageBannerMapper homepageBannerMapper, HomeNoticeMapper homeNoticeMapper,
                                  SiteProfileMapper siteProfileMapper,
                                  ParentMessageMapper parentMessageMapper, SysUserMapper sysUserMapper,
                                  FamilyGroupMapper familyGroupMapper, ProductMapper productMapper,
                                  ProductFamilyCardPlanMapper familyCardPlanMapper,
                                  ProductValueAddedSkuMapper valueAddedSkuMapper) {
        this.homepageBannerMapper = homepageBannerMapper;
        this.homeNoticeMapper = homeNoticeMapper;
        this.siteProfileMapper = siteProfileMapper;
        this.parentMessageMapper = parentMessageMapper;
        this.sysUserMapper = sysUserMapper;
        this.familyGroupMapper = familyGroupMapper;
        this.productMapper = productMapper;
        this.familyCardPlanMapper = familyCardPlanMapper;
        this.valueAddedSkuMapper = valueAddedSkuMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<ParentMessageView> listMyParentMessages(Long userId, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        LambdaQueryWrapper<ParentMessageDO> countQuery = new LambdaQueryWrapper<ParentMessageDO>()
            .eq(ParentMessageDO::getUserId, userId)
            .eq(ParentMessageDO::getDeleted, false)
            .eq(ParentMessageDO::getSenderType, "USER")
            .isNull(ParentMessageDO::getParentId);
        long total = parentMessageMapper.selectCount(countQuery);
        if (total <= 0) {
            return PageResult.of(List.of(), 0, normalizedPageNo, normalizedPageSize);
        }

        List<ParentMessageDO> roots = parentMessageMapper.selectList(
            new LambdaQueryWrapper<ParentMessageDO>()
                .eq(ParentMessageDO::getUserId, userId)
                .eq(ParentMessageDO::getDeleted, false)
                .eq(ParentMessageDO::getSenderType, "USER")
                .isNull(ParentMessageDO::getParentId)
                .orderByDesc(ParentMessageDO::getCreatedAt, ParentMessageDO::getId)
                .last("limit " + normalizedPageSize + " offset " + offset)
        );
        if (roots.isEmpty()) {
            return PageResult.of(List.of(), total, normalizedPageNo, normalizedPageSize);
        }

        Set<Long> rootIds = roots.stream().map(ParentMessageDO::getId).collect(Collectors.toSet());
        Map<Long, List<ParentMessageReplyView>> repliesByRootId = parentMessageMapper.selectList(
                new LambdaQueryWrapper<ParentMessageDO>()
                    .eq(ParentMessageDO::getDeleted, false)
                    .in(ParentMessageDO::getRootId, rootIds)
                    .isNotNull(ParentMessageDO::getParentId)
                    .orderByAsc(ParentMessageDO::getCreatedAt, ParentMessageDO::getId)
            ).stream()
            .collect(Collectors.groupingBy(
                ParentMessageDO::getRootId,
                Collectors.mapping(this::toParentMessageReplyView, Collectors.toList())
            ));

        List<ParentMessageView> items = roots.stream()
            .map(root -> toParentMessageView(root, repliesByRootId.getOrDefault(root.getId(), List.of())))
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    @Transactional
    public ParentMessageView createParentMessage(CreateParentMessageCommand command) {
        if (command.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "user not authenticated");
        }
        if (!StringUtils.hasText(command.content())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "content is required");
        }
        String content = command.content().trim();
        if (content.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "content length must be <= 500");
        }
        SysUserDO user = sysUserMapper.selectById(command.userId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "user not found");
        }
        OffsetDateTime now = OffsetDateTime.now();
        ParentMessageDO message = new ParentMessageDO();
        message.setUserId(command.userId());
        message.setSenderType("USER");
        message.setParentId(null);
        message.setRootId(null);
        message.setNicknameSnapshot(resolveNickname(user));
        message.setAvatarSnapshot(user.getAvatarUrl());
        message.setContent(content);
        message.setStatus("APPROVED");
        message.setViewed(false);
        message.setViewedAt(null);
        message.setRepliedAt(null);
        message.setClosed(false);
        message.setDeleted(false);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        parentMessageMapper.insert(message);
        message.setRootId(message.getId());
        parentMessageMapper.updateById(message);
        return toParentMessageView(message);
    }

    public PageResult<AdminParentMessageView> listAdminParentMessages(Boolean viewed, Boolean replied, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;

        LambdaQueryWrapper<ParentMessageDO> countQuery = buildAdminRootMessageQuery(viewed, replied);
        LambdaQueryWrapper<ParentMessageDO> listQuery = buildAdminRootMessageQuery(viewed, replied);

        long total = parentMessageMapper.selectCount(countQuery);
        List<AdminParentMessageView> items = parentMessageMapper.selectList(
                listQuery.orderByDesc(ParentMessageDO::getCreatedAt, ParentMessageDO::getId)
                    .last("limit " + normalizedPageSize + " offset " + offset)
            ).stream()
            .map(this::toAdminParentMessageView)
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    @Transactional
    public AdminParentMessageDetailView getAdminParentMessageDetail(Long messageId) {
        ParentMessageDO root = requireRootMessage(messageId);
        if (!Boolean.TRUE.equals(root.getViewed())) {
            root.setViewed(true);
            root.setViewedAt(OffsetDateTime.now());
            parentMessageMapper.updateById(root);
        }
        return buildAdminParentMessageDetail(root);
    }

    @Transactional
    public AdminParentMessageDetailView replyAdminParentMessage(
        Long messageId, AdminReplyParentMessageCommand command, Long operatorUserId) {
        if (!StringUtils.hasText(command.content())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "reply content is required");
        }
        String content = command.content().trim();
        if (content.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "reply content length must be <= 500");
        }
        ParentMessageDO root = requireRootMessage(messageId);
        if (Boolean.TRUE.equals(root.getClosed())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "message is closed");
        }

        ParentMessageDO latestNode = parentMessageMapper.selectOne(
            new LambdaQueryWrapper<ParentMessageDO>()
                .eq(ParentMessageDO::getRootId, root.getId())
                .eq(ParentMessageDO::getDeleted, false)
                .orderByDesc(ParentMessageDO::getId)
                .last("limit 1")
        );
        Long parentId = latestNode == null ? root.getId() : latestNode.getId();
        OffsetDateTime now = OffsetDateTime.now();

        ParentMessageDO reply = new ParentMessageDO();
        reply.setUserId(root.getUserId());
        reply.setSenderType("ADMIN");
        reply.setParentId(parentId);
        reply.setRootId(root.getId());
        reply.setNicknameSnapshot("ADMIN");
        reply.setAvatarSnapshot(null);
        reply.setContent(content);
        reply.setStatus("APPROVED");
        reply.setViewed(true);
        reply.setViewedAt(now);
        reply.setRepliedAt(null);
        reply.setClosed(false);
        reply.setDeleted(false);
        reply.setCreatedAt(now);
        reply.setUpdatedAt(now);
        parentMessageMapper.insert(reply);

        root.setViewed(true);
        root.setViewedAt(root.getViewedAt() == null ? now : root.getViewedAt());
        root.setRepliedAt(now);
        root.setUpdatedAt(now);
        parentMessageMapper.updateById(root);
        return buildAdminParentMessageDetail(root);
    }

    @Transactional
    public void closeAdminParentMessage(Long messageId, Long operatorUserId) {
        ParentMessageDO root = requireRootMessage(messageId);
        root.setClosed(true);
        parentMessageMapper.updateById(root);
    }

    @Transactional
    public void deleteAdminParentMessage(Long messageId, Long operatorUserId) {
        ParentMessageDO root = requireRootMessage(messageId);
        root.setDeleted(true);
        parentMessageMapper.updateById(root);
        parentMessageMapper.update(null, new LambdaUpdateWrapper<ParentMessageDO>()
            .set(ParentMessageDO::getDeleted, true)
            .eq(ParentMessageDO::getRootId, root.getId()));
    }

    public HomePageView loadHomePage() {
        OffsetDateTime now = OffsetDateTime.now();
        List<HomePageView.BannerView> banners = homepageBannerMapper.selectList(
                new LambdaQueryWrapper<HomepageBannerDO>()
                    .eq(HomepageBannerDO::getEnabled, true)
                    .and(w -> w.isNull(HomepageBannerDO::getStartAt).or().le(HomepageBannerDO::getStartAt, now))
                    .and(w -> w.isNull(HomepageBannerDO::getEndAt).or().ge(HomepageBannerDO::getEndAt, now))
                    .orderByAsc(HomepageBannerDO::getSortOrder, HomepageBannerDO::getId)
            ).stream()
            .map(b -> new HomePageView.BannerView(b.getId(), b.getTitle(), b.getImageUrl()))
            .toList();

        SiteProfileDO profileDO = siteProfileMapper.selectOne(
            new LambdaQueryWrapper<SiteProfileDO>()
                .eq(SiteProfileDO::getActive, true)
                .orderByDesc(SiteProfileDO::getId)
                .last("limit 1")
        );
        HomePageView.SiteProfileView profile = new HomePageView.SiteProfileView(
            profileDO == null ? "" : profileDO.getCommunityName(),
            profileDO == null ? "" : profileDO.getIntroText(),
            profileDO == null ? "" : profileDO.getContactPerson(),
            profileDO == null ? "" : profileDO.getContactPhone(),
            profileDO == null ? "" : profileDO.getContactWechat(),
            profileDO == null ? "" : profileDO.getContactWechatQrUrl(),
            profileDO == null ? "" : profileDO.getAddressText(),
            profileDO == null ? null : profileDO.getLatitude(),
            profileDO == null ? null : profileDO.getLongitude()
        );
        String bannerSlogan = profileDO == null ? "" : profileDO.getBannerSlogan();
        List<HomePageView.NoticeView> notices = homeNoticeMapper.selectList(
                new LambdaQueryWrapper<HomeNoticeDO>()
                    .eq(HomeNoticeDO::getEnabled, true)
                    .and(w -> w.isNull(HomeNoticeDO::getStartAt).or().le(HomeNoticeDO::getStartAt, now))
                    .and(w -> w.isNull(HomeNoticeDO::getEndAt).or().ge(HomeNoticeDO::getEndAt, now))
                    .orderByDesc(HomeNoticeDO::getSortOrder, HomeNoticeDO::getId)
            ).stream()
            .map(noticeDO -> new HomePageView.NoticeView(
                safeString(noticeDO.getTitle()),
                safeString(noticeDO.getContent())
            ))
            .toList();
        return new HomePageView(banners, bannerSlogan, profile, notices);
    }

    public PageResult<AdminHomepageBannerView> listAdminBanners(Integer pageNo, Integer pageSize) {
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        long total = homepageBannerMapper.selectCount(new LambdaQueryWrapper<>());
        List<AdminHomepageBannerView> items = homepageBannerMapper.selectList(
                new LambdaQueryWrapper<HomepageBannerDO>()
                    .orderByAsc(HomepageBannerDO::getSortOrder, HomepageBannerDO::getId)
                    .last("limit " + normalizedPageSize + " offset " + offset)
            ).stream()
            .map(this::toAdminBannerView)
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    public AdminHomepageBannerView updateAdminBanner(Long bannerId, AdminBannerUpdateCommand command, Long operatorUserId) {
        if (command.startAt() != null && command.endAt() != null && command.endAt().isBefore(command.startAt())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "banner end_at cannot be before start_at");
        }
        HomepageBannerDO exist = homepageBannerMapper.selectById(bannerId);
        if (exist == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "banner not found");
        }
        exist.setTitle(command.title());
        exist.setImageUrl(command.imageUrl());
        exist.setLinkType(normalizeLinkType(command.linkType()));
        exist.setLinkTarget(command.linkTarget());
        exist.setSortOrder(command.sortOrder());
        exist.setEnabled(command.enabled());
        exist.setStartAt(command.startAt());
        exist.setEndAt(command.endAt());
        exist.setUpdatedBy(null);
        homepageBannerMapper.updateById(exist);
        return toAdminBannerView(exist);
    }

    public void deleteAdminBanner(Long bannerId) {
        if (homepageBannerMapper.deleteById(bannerId) == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "banner not found");
        }
    }

    public PageResult<AdminHomeNoticeView> listAdminNotices(Integer pageNo, Integer pageSize) {
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        long total = homeNoticeMapper.selectCount(new LambdaQueryWrapper<>());
        List<AdminHomeNoticeView> items = homeNoticeMapper.selectList(
                new LambdaQueryWrapper<HomeNoticeDO>()
                    .orderByDesc(HomeNoticeDO::getSortOrder, HomeNoticeDO::getId)
                    .last("limit " + normalizedPageSize + " offset " + offset)
            ).stream()
            .map(this::toAdminNoticeView)
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    @Transactional
    public AdminHomeNoticeView createAdminNotice(AdminNoticeCreateCommand command, Long operatorUserId) {
        if (!StringUtils.hasText(command.title())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "title is required");
        }
        String title = command.title().trim();
        if (title.length() > 50) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "title length must be <= 50");
        }
        String content = normalizeNullable(command.content());
        if (content != null && content.length() > 2000) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "content length must be <= 2000");
        }
        if (command.startAt() != null && command.endAt() != null && command.endAt().isBefore(command.startAt())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "notice end_at cannot be before start_at");
        }
        OffsetDateTime now = OffsetDateTime.now();
        HomeNoticeDO notice = new HomeNoticeDO();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setEnabled(command.enabled() == null || command.enabled());
        notice.setSortOrder(command.sortOrder() == null ? 0 : command.sortOrder());
        notice.setStartAt(command.startAt());
        notice.setEndAt(command.endAt());
        notice.setCreatedBy(operatorUserId);
        notice.setUpdatedBy(operatorUserId);
        notice.setCreatedAt(now);
        notice.setUpdatedAt(now);
        homeNoticeMapper.insert(notice);
        return toAdminNoticeView(notice);
    }

    @Transactional
    public void deleteAdminNotice(Long noticeId) {
        if (homeNoticeMapper.deleteById(noticeId) == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "notice not found");
        }
    }

    public PageResult<AdminSiteProfileView> listAdminSiteProfiles(Integer pageNo, Integer pageSize) {
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        long total = siteProfileMapper.selectCount(new LambdaQueryWrapper<>());
        List<AdminSiteProfileView> items = siteProfileMapper.selectList(
                new LambdaQueryWrapper<SiteProfileDO>()
                    .orderByDesc(SiteProfileDO::getActive, SiteProfileDO::getId)
                    .last("limit " + normalizedPageSize + " offset " + offset)
            ).stream()
            .map(this::toAdminSiteProfileView)
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    public AdminSiteProfileView getAdminSiteProfile(Long siteProfileId) {
        SiteProfileDO siteProfile = siteProfileMapper.selectById(siteProfileId);
        if (siteProfile == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "site profile not found");
        }
        return toAdminSiteProfileView(siteProfile);
    }

    public AdminSiteProfileView updateAdminSiteProfile(Long siteProfileId, AdminSiteProfileUpdateCommand command, Long operatorUserId) {
        SiteProfileDO siteProfile = siteProfileMapper.selectById(siteProfileId);
        if (siteProfile == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "site profile not found");
        }
        boolean active = Boolean.TRUE.equals(command.active());
        if (active) {
            siteProfileMapper.update(null, new LambdaUpdateWrapper<SiteProfileDO>()
                .set(SiteProfileDO::getActive, false)
                .eq(SiteProfileDO::getActive, true)
                .ne(SiteProfileDO::getId, siteProfileId));
        }
        siteProfile.setCommunityName(command.communityName());
        siteProfile.setBannerSlogan(command.bannerSlogan());
        siteProfile.setIntroText(command.introText());
        siteProfile.setContactPerson(command.contactPerson());
        siteProfile.setContactPhone(command.contactPhone());
        siteProfile.setContactWechat(command.contactWechat());
        siteProfile.setContactWechatQrUrl(command.contactWechatQrUrl());
        siteProfile.setAddressText(command.addressText());
        siteProfile.setLatitude(command.latitude());
        siteProfile.setLongitude(command.longitude());
        siteProfile.setActive(active);
        siteProfile.setUpdatedBy(null);
        siteProfileMapper.updateById(siteProfile);
        return toAdminSiteProfileView(siteProfile);
    }

    public void deleteAdminSiteProfile(Long siteProfileId) {
        SiteProfileDO siteProfile = siteProfileMapper.selectById(siteProfileId);
        if (siteProfile == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "site profile not found");
        }
        if (Boolean.TRUE.equals(siteProfile.getActive())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "active site profile cannot be deleted");
        }
        Long totalProfiles = siteProfileMapper.selectCount(new LambdaQueryWrapper<SiteProfileDO>());
        if (totalProfiles != null && totalProfiles <= 1) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "at least one site profile must remain");
        }
        siteProfileMapper.deleteById(siteProfileId);
    }

    public List<HomeProductView> loadProducts(LocalDate saleStartAt, LocalDate saleEndAt) {
        if (saleStartAt == null || saleEndAt == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "sale_start_at and sale_end_at are required");
        }
        if (saleStartAt != null && saleEndAt != null && saleStartAt.isAfter(saleEndAt)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "sale_start_at cannot be after sale_end_at");
        }
        OffsetDateTime queryStartAt = toStartOfDay(saleStartAt);
        OffsetDateTime queryEndExclusive = toStartOfDay(saleEndAt.plusDays(1));
        LambdaQueryWrapper<ProductDO> productQuery = new LambdaQueryWrapper<ProductDO>()
            .eq(ProductDO::getPublishStatus, "ON_SHELF")
            .eq(ProductDO::getDeleted, false);
        productQuery.and(w -> w.isNull(ProductDO::getSaleStartAt).or().lt(ProductDO::getSaleStartAt, queryEndExclusive));
        productQuery.and(w -> w.isNull(ProductDO::getSaleEndAt).or().ge(ProductDO::getSaleEndAt, queryStartAt));
        List<ProductDO> products = productMapper.selectList(
            productQuery.orderByDesc(ProductDO::getTop, ProductDO::getDisplayPriority, ProductDO::getId)
        );
        List<HomeProductView> productViews = new ArrayList<>();
        for (ProductDO product : products) {
            long price = resolveDisplayPrice(product.getId(), product.getProductType());
            productViews.add(new HomeProductView(
                product.getId(),
                product.getProductType(),
                product.getTitle(),
                price,
                Boolean.TRUE.equals(product.getTop()),
                product.getPublishStatus()
            ));
        }

        productViews = productViews.stream()
            .sorted(Comparator.comparing(HomeProductView::top).reversed())
            .toList();
        return productViews;
    }

    public PageResult<HomeProductView> loadAdminProducts(
        LocalDate saleStartAt, LocalDate saleEndAt, String publishStatus, Integer pageNo, Integer pageSize) {
        if (saleStartAt == null || saleEndAt == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "sale_start_at and sale_end_at are required");
        }
        if (saleStartAt.isAfter(saleEndAt)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "sale_start_at cannot be after sale_end_at");
        }
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;

        OffsetDateTime queryStartAt = toStartOfDay(saleStartAt);
        OffsetDateTime queryEndExclusive = toStartOfDay(saleEndAt.plusDays(1));
        LambdaQueryWrapper<ProductDO> countQuery = buildAdminProductQuery(queryStartAt, queryEndExclusive, publishStatus);
        LambdaQueryWrapper<ProductDO> listQuery = buildAdminProductQuery(queryStartAt, queryEndExclusive, publishStatus);

        long total = productMapper.selectCount(countQuery);
        List<ProductDO> products = productMapper.selectList(
            listQuery.orderByDesc(ProductDO::getTop, ProductDO::getDisplayPriority, ProductDO::getId)
                .last("limit " + normalizedPageSize + " offset " + offset)
        );
        List<HomeProductView> items = products.stream()
            .map(product -> new HomeProductView(
                product.getId(),
                product.getProductType(),
                product.getTitle(),
                resolveDisplayPrice(product.getId(), product.getProductType()),
                Boolean.TRUE.equals(product.getTop()),
                product.getPublishStatus()
            ))
            .sorted(Comparator.comparing(HomeProductView::top).reversed())
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    public HomeProductDetailView loadProductDetail(Long productId) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || Boolean.TRUE.equals(product.getDeleted())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "product not found");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (!"ON_SHELF".equals(product.getPublishStatus()) || !isInSaleWindow(product, now)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "product not found");
        }
        List<HomeProductDetailView.FamilyCardPlanView> familyCardPlans = List.of();
        List<HomeProductDetailView.ValueAddedSkuView> valueAddedSkus = List.of();
        if ("FAMILY_CARD".equals(product.getProductType())) {
            familyCardPlans = familyCardPlanMapper.selectList(
                    new LambdaQueryWrapper<ProductFamilyCardPlanDO>()
                        .eq(ProductFamilyCardPlanDO::getProductId, productId)
                        .orderByAsc(ProductFamilyCardPlanDO::getDurationMonths, ProductFamilyCardPlanDO::getId)
                ).stream()
                .map(plan -> new HomeProductDetailView.FamilyCardPlanView(
                    plan.getId(),
                    plan.getDurationType(),
                    plan.getDurationMonths() == null ? 0 : plan.getDurationMonths(),
                    plan.getPriceCents() == null ? 0L : plan.getPriceCents(),
                    plan.getMaxFamilyMembers() == null ? 0 : plan.getMaxFamilyMembers(),
                    Boolean.TRUE.equals(plan.getEnabled())
                ))
                .toList();
        } else if ("VALUE_ADDED_SERVICE".equals(product.getProductType())) {
            valueAddedSkus = valueAddedSkuMapper.selectList(
                    new LambdaQueryWrapper<ProductValueAddedSkuDO>()
                        .eq(ProductValueAddedSkuDO::getProductId, productId)
                        .orderByAsc(ProductValueAddedSkuDO::getId)
                ).stream()
                .map(sku -> new HomeProductDetailView.ValueAddedSkuView(
                    sku.getId(),
                    sku.getTitle(),
                    sku.getPriceCents() == null ? 0L : sku.getPriceCents(),
                    Boolean.TRUE.equals(sku.getEnabled())
                ))
                .toList();
        }
        return new HomeProductDetailView(
            product.getId(),
            product.getProductNo(),
            product.getProductType(),
            product.getTitle(),
            product.getSubtitle(),
            product.getDetailContent(),
            parseImageUrls(product.getImageUrls()),
            product.getContactName(),
            product.getContactPhone(),
            product.getServiceStartAt(),
            product.getServiceEndAt(),
            product.getSaleStartAt(),
            product.getSaleEndAt(),
            product.getPublishStatus(),
            Boolean.TRUE.equals(product.getDeleted()),
            Boolean.TRUE.equals(product.getTop()),
            product.getDisplayPriority() == null ? 0 : product.getDisplayPriority(),
            product.getListVisibilityRuleId(),
            product.getDetailVisibilityRuleId(),
            product.getCategoryId(),
            familyCardPlans,
            valueAddedSkus
        );
    }

    public AdminProductDetailView getAdminProductDetail(Long productId) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || Boolean.TRUE.equals(product.getDeleted())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "product not found");
        }
        return toAdminProductDetail(product);
    }

    @Transactional
    public AdminProductDetailView createAdminProduct(AdminProductUpdateCommand command, Long operatorUserId) {
        String productType = normalizeProductType(command.productType());
        validateProductType(productType);
        validateProductTimeRange(command.serviceStartAt(), command.serviceEndAt(), "service");
        validateProductTimeRange(command.saleStartAt(), command.saleEndAt(), "sale");

        ProductDO product = new ProductDO();
        product.setProductNo(generateProductNo());
        product.setProductType(productType);
        product.setTitle(command.title());
        product.setSubtitle(command.subtitle());
        product.setDetailContent(command.detailContent());
        product.setImageUrls(toJsonString(command.imageUrls()));
        product.setContactName(command.contactName());
        product.setContactPhone(command.contactPhone());
        product.setServiceStartAt(command.serviceStartAt());
        product.setServiceEndAt(command.serviceEndAt());
        product.setSaleStartAt(command.saleStartAt());
        product.setSaleEndAt(command.saleEndAt());
        product.setPublishStatus(normalizePublishStatus(command.publishStatus()));
        product.setDeleted(false);
        product.setTop(Boolean.TRUE.equals(command.top()));
        product.setDisplayPriority(command.displayPriority() == null ? 0 : command.displayPriority());
        product.setCreatedBy(null);
        product.setUpdatedBy(null);
        productMapper.insert(product);

        if ("FAMILY_CARD".equals(productType)) {
            if (command.familyCardPlans() == null || command.familyCardPlans().isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "family card plans are required");
            }
            replaceFamilyCardPlans(product.getId(), command.familyCardPlans());
        } else {
            if (command.valueAddedSkus() == null || command.valueAddedSkus().isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "value added skus are required");
            }
            replaceValueAddedSkus(product.getId(), command.valueAddedSkus());
        }
        return getAdminProductDetail(product.getId());
    }

    @Transactional
    public AdminProductDetailView updateAdminProduct(Long productId, AdminProductUpdateCommand command, Long operatorUserId) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || Boolean.TRUE.equals(product.getDeleted())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "product not found");
        }
        String productType = normalizeProductType(command.productType());
        if (!product.getProductType().equals(productType)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "product_type cannot be changed");
        }
        validateProductTimeRange(command.serviceStartAt(), command.serviceEndAt(), "service");
        validateProductTimeRange(command.saleStartAt(), command.saleEndAt(), "sale");

        product.setTitle(command.title());
        product.setSubtitle(command.subtitle());
        product.setDetailContent(command.detailContent());
        product.setImageUrls(toJsonString(command.imageUrls()));
        product.setContactName(command.contactName());
        product.setContactPhone(command.contactPhone());
        product.setServiceStartAt(command.serviceStartAt());
        product.setServiceEndAt(command.serviceEndAt());
        product.setSaleStartAt(command.saleStartAt());
        product.setSaleEndAt(command.saleEndAt());
        product.setPublishStatus(normalizePublishStatus(command.publishStatus()));
        product.setTop(Boolean.TRUE.equals(command.top()));
        product.setDisplayPriority(command.displayPriority() == null ? 0 : command.displayPriority());
        product.setUpdatedBy(null);
        productMapper.updateById(product);

        if ("FAMILY_CARD".equals(productType) && command.familyCardPlans() != null) {
            replaceFamilyCardPlans(productId, command.familyCardPlans());
        }
        if ("VALUE_ADDED_SERVICE".equals(productType) && command.valueAddedSkus() != null) {
            replaceValueAddedSkus(productId, command.valueAddedSkus());
        }
        return getAdminProductDetail(productId);
    }

    @Transactional
    public AdminProductDetailView onShelfAdminProduct(Long productId, Long operatorUserId) {
        return changeAdminProductPublishStatus(productId, "ON_SHELF", operatorUserId);
    }

    @Transactional
    public AdminProductDetailView offShelfAdminProduct(Long productId, Long operatorUserId) {
        return changeAdminProductPublishStatus(productId, "OFF_SHELF", operatorUserId);
    }

    private AdminProductDetailView changeAdminProductPublishStatus(Long productId, String publishStatus, Long operatorUserId) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || Boolean.TRUE.equals(product.getDeleted())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "product not found");
        }
        product.setPublishStatus(publishStatus);
        product.setUpdatedBy(null);
        productMapper.updateById(product);
        return toAdminProductDetail(product);
    }

    private void replaceFamilyCardPlans(Long productId, List<AdminProductUpdateCommand.FamilyCardPlanCommand> plans) {
        List<ProductFamilyCardPlanDO> existingPlans = familyCardPlanMapper.selectList(
            new LambdaQueryWrapper<ProductFamilyCardPlanDO>()
                .eq(ProductFamilyCardPlanDO::getProductId, productId)
        );
        Map<Long, ProductFamilyCardPlanDO> existingById = existingPlans.stream()
            .filter(plan -> plan.getId() != null)
            .collect(Collectors.toMap(ProductFamilyCardPlanDO::getId, p -> p));
        HashSet<Long> handledPlanIds = new HashSet<>();

        for (AdminProductUpdateCommand.FamilyCardPlanCommand plan : plans) {
            if (plan.id() != null) {
                ProductFamilyCardPlanDO existed = existingById.get(plan.id());
                if (existed == null) {
                    throw new BusinessException(ErrorCode.INVALID_PARAM, "family card plan id not found for current product");
                }
                existed.setDurationType(plan.durationType());
                existed.setDurationMonths(plan.durationMonths());
                existed.setPriceCents(plan.priceCents());
                existed.setMaxFamilyMembers(plan.maxFamilyMembers());
                existed.setEnabled(plan.enabled() == null || plan.enabled());
                familyCardPlanMapper.updateById(existed);
                handledPlanIds.add(existed.getId());
            } else {
                ProductFamilyCardPlanDO planDO = new ProductFamilyCardPlanDO();
                planDO.setProductId(productId);
                planDO.setDurationType(plan.durationType());
                planDO.setDurationMonths(plan.durationMonths());
                planDO.setPriceCents(plan.priceCents());
                planDO.setMaxFamilyMembers(plan.maxFamilyMembers());
                planDO.setEnabled(plan.enabled() == null || plan.enabled());
                familyCardPlanMapper.insert(planDO);
                if (planDO.getId() != null) {
                    handledPlanIds.add(planDO.getId());
                }
            }
        }

        for (ProductFamilyCardPlanDO existed : existingPlans) {
            if (existed.getId() == null || handledPlanIds.contains(existed.getId())) {
                continue;
            }
            boolean referenced = familyGroupMapper.selectCount(
                new LambdaQueryWrapper<FamilyGroupDO>()
                    .eq(FamilyGroupDO::getFamilyCardPlanId, existed.getId())
            ) > 0;
            if (!referenced) {
                familyCardPlanMapper.deleteById(existed.getId());
            }
        }
    }

    private void replaceValueAddedSkus(Long productId, List<AdminProductUpdateCommand.ValueAddedSkuCommand> skus) {
        valueAddedSkuMapper.delete(new LambdaQueryWrapper<ProductValueAddedSkuDO>()
            .eq(ProductValueAddedSkuDO::getProductId, productId));
        for (AdminProductUpdateCommand.ValueAddedSkuCommand sku : skus) {
            ProductValueAddedSkuDO skuDO = new ProductValueAddedSkuDO();
            skuDO.setProductId(productId);
            skuDO.setSkuNo(generateSkuNo(productId));
            skuDO.setTitle(sku.title());
            skuDO.setUnitName("item");
            skuDO.setPriceCents(sku.priceCents());
            skuDO.setMaxPurchaseQty(1);
            skuDO.setServiceNotice(null);
            skuDO.setEnabled(sku.enabled() == null || sku.enabled());
            valueAddedSkuMapper.insert(skuDO);
        }
    }

    private AdminProductDetailView toAdminProductDetail(ProductDO product) {
        List<AdminProductDetailView.FamilyCardPlanView> familyCardPlans = List.of();
        List<AdminProductDetailView.ValueAddedSkuView> valueAddedSkus = List.of();
        if ("FAMILY_CARD".equals(product.getProductType())) {
            familyCardPlans = familyCardPlanMapper.selectList(
                    new LambdaQueryWrapper<ProductFamilyCardPlanDO>()
                        .eq(ProductFamilyCardPlanDO::getProductId, product.getId())
                        .orderByAsc(ProductFamilyCardPlanDO::getDurationMonths, ProductFamilyCardPlanDO::getId)
                ).stream()
                .map(plan -> new AdminProductDetailView.FamilyCardPlanView(
                    plan.getId(),
                    plan.getDurationType(),
                    plan.getDurationMonths() == null ? 0 : plan.getDurationMonths(),
                    plan.getPriceCents() == null ? 0L : plan.getPriceCents(),
                    plan.getMaxFamilyMembers() == null ? 0 : plan.getMaxFamilyMembers(),
                    Boolean.TRUE.equals(plan.getEnabled())
                ))
                .toList();
        } else if ("VALUE_ADDED_SERVICE".equals(product.getProductType())) {
            valueAddedSkus = valueAddedSkuMapper.selectList(
                    new LambdaQueryWrapper<ProductValueAddedSkuDO>()
                        .eq(ProductValueAddedSkuDO::getProductId, product.getId())
                        .orderByAsc(ProductValueAddedSkuDO::getId)
                ).stream()
                .map(sku -> new AdminProductDetailView.ValueAddedSkuView(
                    sku.getId(),
                    sku.getTitle(),
                    sku.getPriceCents() == null ? 0L : sku.getPriceCents(),
                    Boolean.TRUE.equals(sku.getEnabled())
                ))
                .toList();
        }
        return new AdminProductDetailView(
            product.getId(),
            product.getProductNo(),
            product.getProductType(),
            product.getTitle(),
            product.getSubtitle(),
            product.getDetailContent(),
            parseImageUrls(product.getImageUrls()),
            product.getContactName(),
            product.getContactPhone(),
            product.getServiceStartAt(),
            product.getServiceEndAt(),
            product.getSaleStartAt(),
            product.getSaleEndAt(),
            product.getPublishStatus(),
            Boolean.TRUE.equals(product.getDeleted()),
            Boolean.TRUE.equals(product.getTop()),
            product.getDisplayPriority() == null ? 0 : product.getDisplayPriority(),
            product.getListVisibilityRuleId(),
            product.getDetailVisibilityRuleId(),
            product.getCategoryId(),
            familyCardPlans,
            valueAddedSkus
        );
    }

    private void validateProductType(String productType) {
        if (!"FAMILY_CARD".equals(productType) && !"VALUE_ADDED_SERVICE".equals(productType)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "invalid product_type");
        }
    }

    private String normalizeProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "product_type is required");
        }
        return productType.trim().toUpperCase();
    }

    private String normalizePublishStatus(String publishStatus) {
        if (!StringUtils.hasText(publishStatus)) {
            return "DRAFT";
        }
        String normalized = publishStatus.trim().toUpperCase();
        if (!"DRAFT".equals(normalized) && !"ON_SHELF".equals(normalized) && !"OFF_SHELF".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "invalid publish_status");
        }
        return normalized;
    }

    private LambdaQueryWrapper<ProductDO> buildAdminProductQuery(
        OffsetDateTime queryStartAt, OffsetDateTime queryEndExclusive, String publishStatus) {
        LambdaQueryWrapper<ProductDO> query = new LambdaQueryWrapper<ProductDO>()
            .eq(ProductDO::getDeleted, false);
        String normalizedStatus = normalizePublishStatusForFilter(publishStatus);
        if (normalizedStatus != null) {
            query.eq(ProductDO::getPublishStatus, normalizedStatus);
        }
        query.and(w -> w.isNull(ProductDO::getSaleStartAt).or().lt(ProductDO::getSaleStartAt, queryEndExclusive));
        query.and(w -> w.isNull(ProductDO::getSaleEndAt).or().ge(ProductDO::getSaleEndAt, queryStartAt));
        return query;
    }

    private String normalizePublishStatusForFilter(String publishStatus) {
        if (!StringUtils.hasText(publishStatus)) {
            return null;
        }
        String normalized = publishStatus.trim().toUpperCase();
        if (!"DRAFT".equals(normalized) && !"ON_SHELF".equals(normalized) && !"OFF_SHELF".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "invalid publish_status");
        }
        return normalized;
    }

    private void validateProductTimeRange(OffsetDateTime startAt, OffsetDateTime endAt, String fieldPrefix) {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, fieldPrefix + " end_at cannot be before start_at");
        }
    }

    private String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private String generateProductNo() {
        long now = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "PRD" + now + random;
    }

    private String generateSkuNo(Long productId) {
        long now = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "SKU" + productId + now + random;
    }

    private List<String> parseImageUrls(String imageUrlsJson) {
        if (!StringUtils.hasText(imageUrlsJson)) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(imageUrlsJson, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private OffsetDateTime toStartOfDay(LocalDate date) {
        return date.atStartOfDay(DEFAULT_ZONE).toOffsetDateTime();
    }

    private AdminHomeNoticeView toAdminNoticeView(HomeNoticeDO noticeDO) {
        return new AdminHomeNoticeView(
            noticeDO.getId(),
            noticeDO.getTitle(),
            noticeDO.getContent(),
            Boolean.TRUE.equals(noticeDO.getEnabled()),
            noticeDO.getSortOrder() == null ? 0 : noticeDO.getSortOrder(),
            noticeDO.getStartAt(),
            noticeDO.getEndAt(),
            noticeDO.getCreatedAt(),
            noticeDO.getUpdatedAt()
        );
    }

    private AdminHomepageBannerView toAdminBannerView(HomepageBannerDO bannerDO) {
        return new AdminHomepageBannerView(
            bannerDO.getId(),
            bannerDO.getTitle(),
            bannerDO.getImageUrl(),
            bannerDO.getLinkType(),
            bannerDO.getLinkTarget(),
            bannerDO.getSortOrder(),
            bannerDO.getEnabled(),
            bannerDO.getStartAt(),
            bannerDO.getEndAt()
        );
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private LambdaQueryWrapper<ParentMessageDO> buildAdminRootMessageQuery(Boolean viewed, Boolean replied) {
        LambdaQueryWrapper<ParentMessageDO> query = new LambdaQueryWrapper<ParentMessageDO>()
            .eq(ParentMessageDO::getDeleted, false)
            .eq(ParentMessageDO::getSenderType, "USER")
            .isNull(ParentMessageDO::getParentId);
        if (viewed != null) {
            query.eq(ParentMessageDO::getViewed, viewed);
        }
        if (replied != null) {
            if (replied) {
                query.isNotNull(ParentMessageDO::getRepliedAt);
            } else {
                query.isNull(ParentMessageDO::getRepliedAt);
            }
        }
        return query;
    }

    private ParentMessageDO requireRootMessage(Long messageId) {
        ParentMessageDO message = parentMessageMapper.selectById(messageId);
        if (message == null || Boolean.TRUE.equals(message.getDeleted())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "message not found");
        }
        Long rootId = message.getRootId() == null ? message.getId() : message.getRootId();
        ParentMessageDO root = parentMessageMapper.selectById(rootId);
        if (root == null || Boolean.TRUE.equals(root.getDeleted())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "message not found");
        }
        if (!"USER".equals(root.getSenderType()) || root.getParentId() != null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "message root not found");
        }
        return root;
    }

    private AdminParentMessageDetailView buildAdminParentMessageDetail(ParentMessageDO root) {
        List<AdminParentMessageNodeView> nodes = parentMessageMapper.selectList(
                new LambdaQueryWrapper<ParentMessageDO>()
                    .eq(ParentMessageDO::getDeleted, false)
                    .eq(ParentMessageDO::getRootId, root.getId())
                    .orderByAsc(ParentMessageDO::getCreatedAt, ParentMessageDO::getId)
            ).stream()
            .map(this::toAdminParentMessageNodeView)
            .toList();
        return new AdminParentMessageDetailView(toAdminParentMessageView(root), nodes);
    }

    private AdminParentMessageView toAdminParentMessageView(ParentMessageDO messageDO) {
        return new AdminParentMessageView(
            messageDO.getId(),
            messageDO.getUserId(),
            messageDO.getNicknameSnapshot(),
            messageDO.getAvatarSnapshot(),
            messageDO.getContent(),
            Boolean.TRUE.equals(messageDO.getViewed()),
            messageDO.getRepliedAt() != null,
            Boolean.TRUE.equals(messageDO.getClosed()),
            messageDO.getCreatedAt(),
            messageDO.getViewedAt(),
            messageDO.getRepliedAt()
        );
    }

    private AdminParentMessageNodeView toAdminParentMessageNodeView(ParentMessageDO messageDO) {
        return new AdminParentMessageNodeView(
            messageDO.getId(),
            messageDO.getParentId(),
            messageDO.getRootId(),
            messageDO.getUserId(),
            messageDO.getSenderType(),
            messageDO.getNicknameSnapshot(),
            messageDO.getAvatarSnapshot(),
            messageDO.getContent(),
            messageDO.getCreatedAt()
        );
    }

    private ParentMessageView toParentMessageView(ParentMessageDO messageDO) {
        return toParentMessageView(messageDO, List.of());
    }

    private ParentMessageView toParentMessageView(ParentMessageDO messageDO, List<ParentMessageReplyView> replies) {
        return new ParentMessageView(
            messageDO.getId(),
            messageDO.getNicknameSnapshot(),
            messageDO.getAvatarSnapshot(),
            messageDO.getContent(),
            messageDO.getCreatedAt(),
            replies == null ? List.of() : replies
        );
    }

    private ParentMessageReplyView toParentMessageReplyView(ParentMessageDO messageDO) {
        return new ParentMessageReplyView(
            messageDO.getId(),
            messageDO.getSenderType(),
            messageDO.getNicknameSnapshot(),
            messageDO.getAvatarSnapshot(),
            messageDO.getContent(),
            messageDO.getCreatedAt()
        );
    }

    private String resolveNickname(SysUserDO user) {
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return "USER" + user.getId();
    }

    private AdminSiteProfileView toAdminSiteProfileView(SiteProfileDO siteProfileDO) {
        return new AdminSiteProfileView(
            siteProfileDO.getId(),
            siteProfileDO.getCommunityName(),
            siteProfileDO.getBannerSlogan(),
            siteProfileDO.getIntroText(),
            siteProfileDO.getContactPerson(),
            siteProfileDO.getContactPhone(),
            siteProfileDO.getContactWechat(),
            siteProfileDO.getContactWechatQrUrl(),
            siteProfileDO.getAddressText(),
            siteProfileDO.getLatitude(),
            siteProfileDO.getLongitude(),
            siteProfileDO.getActive()
        );
    }

    private String normalizeLinkType(String linkType) {
        if (!StringUtils.hasText(linkType)) {
            return "NONE";
        }
        return linkType.trim().toUpperCase();
    }

    private boolean isInSaleWindow(ProductDO product, OffsetDateTime now) {
        OffsetDateTime saleStartAt = product.getSaleStartAt();
        if (saleStartAt != null && now.isBefore(saleStartAt)) {
            return false;
        }
        OffsetDateTime saleEndAt = product.getSaleEndAt();
        return saleEndAt == null || !now.isAfter(saleEndAt);
    }

    private long resolveDisplayPrice(Long productId, String productType) {
        if ("FAMILY_CARD".equals(productType)) {
            return familyCardPlanMapper.selectList(
                    new LambdaQueryWrapper<ProductFamilyCardPlanDO>()
                        .eq(ProductFamilyCardPlanDO::getProductId, productId)
                        .eq(ProductFamilyCardPlanDO::getEnabled, true)
                ).stream()
                .map(ProductFamilyCardPlanDO::getPriceCents)
                .min(Long::compareTo)
                .orElse(0L);
        }
        if ("VALUE_ADDED_SERVICE".equals(productType)) {
            return valueAddedSkuMapper.selectList(
                    new LambdaQueryWrapper<ProductValueAddedSkuDO>()
                        .eq(ProductValueAddedSkuDO::getProductId, productId)
                        .eq(ProductValueAddedSkuDO::getEnabled, true)
                ).stream()
                .map(ProductValueAddedSkuDO::getPriceCents)
                .min(Long::compareTo)
                .orElse(0L);
        }
        return 0L;
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
}


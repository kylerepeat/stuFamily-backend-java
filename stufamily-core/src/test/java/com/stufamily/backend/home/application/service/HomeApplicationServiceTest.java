package com.stufamily.backend.home.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stufamily.backend.home.application.command.CreateParentMessageCommand;
import com.stufamily.backend.home.application.dto.ParentMessageView;
import com.stufamily.backend.home.infrastructure.persistence.dataobject.HomepageBannerDO;
import com.stufamily.backend.home.infrastructure.persistence.dataobject.HomeNoticeDO;
import com.stufamily.backend.home.infrastructure.persistence.dataobject.ParentMessageDO;
import com.stufamily.backend.home.infrastructure.persistence.dataobject.SiteProfileDO;
import com.stufamily.backend.home.infrastructure.persistence.mapper.HomepageBannerMapper;
import com.stufamily.backend.home.infrastructure.persistence.mapper.HomeNoticeMapper;
import com.stufamily.backend.home.infrastructure.persistence.mapper.ParentMessageMapper;
import com.stufamily.backend.home.infrastructure.persistence.mapper.SiteProfileMapper;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyGroupMapper;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.mapper.SysUserMapper;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductDO;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductFamilyCardPlanDO;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductValueAddedSkuDO;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductFamilyCardPlanMapper;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductMapper;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductValueAddedSkuMapper;
import com.stufamily.backend.shared.exception.BusinessException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class HomeApplicationServiceTest {

    @Mock
    private HomepageBannerMapper homepageBannerMapper;
    @Mock
    private HomeNoticeMapper homeNoticeMapper;
    @Mock
    private SiteProfileMapper siteProfileMapper;
    @Mock
    private ParentMessageMapper parentMessageMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private FamilyGroupMapper familyGroupMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductFamilyCardPlanMapper familyCardPlanMapper;
    @Mock
    private ProductValueAddedSkuMapper valueAddedSkuMapper;

    private HomeApplicationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new HomeApplicationService(homepageBannerMapper, homeNoticeMapper, siteProfileMapper,
            parentMessageMapper, sysUserMapper,
            familyGroupMapper, productMapper,
            familyCardPlanMapper,
            valueAddedSkuMapper);
    }

    @Test
    void shouldReturnHomepageDataFromDatabase() {
        HomepageBannerDO banner = new HomepageBannerDO();
        banner.setId(1L);
        banner.setTitle("banner");
        banner.setImageUrl("/img.png");
        when(homepageBannerMapper.selectList(any())).thenReturn(List.of(banner));

        SiteProfileDO profile = new SiteProfileDO();
        profile.setCommunityName("community");
        profile.setIntroText("intro");
        profile.setContactPhone("138");
        profile.setBannerSlogan("welcome");
        profile.setAddressText("address");
        when(siteProfileMapper.selectOne(any())).thenReturn(profile);
        HomeNoticeDO notice = new HomeNoticeDO();
        notice.setTitle("通知标题");
        notice.setContent("通知内容");
        when(homeNoticeMapper.selectList(any())).thenReturn(List.of(notice));

        ProductDO familyProduct = new ProductDO();
        familyProduct.setId(100L);
        familyProduct.setProductType("FAMILY_CARD");
        familyProduct.setTitle("family");
        familyProduct.setProductNo("P1");
        familyProduct.setImageUrls("[\"https://example.com/a.png\"]");
        familyProduct.setTop(true);
        familyProduct.setPublishStatus("ON_SHELF");
        familyProduct.setDeleted(false);
        familyProduct.setSaleStartAt(OffsetDateTime.parse("2026-03-01T00:00:00+08:00"));
        familyProduct.setSaleEndAt(OffsetDateTime.parse("2026-03-31T23:59:59+08:00"));
        ProductDO valueAddedProduct = new ProductDO();
        valueAddedProduct.setId(101L);
        valueAddedProduct.setProductType("VALUE_ADDED_SERVICE");
        valueAddedProduct.setTitle("pickup");
        valueAddedProduct.setTop(false);
        valueAddedProduct.setPublishStatus("ON_SHELF");
        valueAddedProduct.setDeleted(false);
        valueAddedProduct.setSaleStartAt(OffsetDateTime.parse("2026-03-05T00:00:00+08:00"));
        valueAddedProduct.setSaleEndAt(OffsetDateTime.parse("2026-03-20T23:59:59+08:00"));
        when(productMapper.selectList(any())).thenReturn(List.of(familyProduct, valueAddedProduct));

        ProductFamilyCardPlanDO plan = new ProductFamilyCardPlanDO();
        plan.setPriceCents(199900L);
        when(familyCardPlanMapper.selectList(any())).thenReturn(List.of(plan), List.of(plan));
        ProductValueAddedSkuDO sku = new ProductValueAddedSkuDO();
        sku.setPriceCents(12900L);
        when(valueAddedSkuMapper.selectList(any())).thenReturn(List.of(sku));
        when(productMapper.selectById(100L)).thenReturn(familyProduct);
        when(productMapper.selectById(101L)).thenReturn(valueAddedProduct);

        var home = service.loadHomePage();
        assertEquals("community", home.siteProfile().communityName());
        assertEquals("welcome", home.bannerSlogan());
        assertEquals("通知标题", home.notices().get(0).title());
        assertEquals("通知内容", home.notices().get(0).content());
        assertFalse(home.banners().isEmpty());
        var products = service.loadProducts(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        assertEquals(2, products.size());
        assertEquals(199900L, products.get(0).priceCents());
        assertEquals(12900L, products.get(1).priceCents());

        var detail = service.loadProductDetail(100L);
        assertEquals("P1", detail.productNo());
        assertEquals(1, detail.imageUrls().size());
        assertEquals("FAMILY_CARD", detail.type());
    }

    @Test
    void shouldRejectDetailWhenOutOfSaleWindow() {
        ProductDO product = new ProductDO();
        product.setId(200L);
        product.setPublishStatus("ON_SHELF");
        product.setDeleted(false);
        product.setSaleStartAt(OffsetDateTime.parse("2099-01-01T00:00:00+08:00"));
        product.setSaleEndAt(OffsetDateTime.parse("2099-12-31T23:59:59+08:00"));
        when(productMapper.selectById(200L)).thenReturn(product);

        assertThrows(BusinessException.class, () -> service.loadProductDetail(200L));
    }

    @Test
    void shouldRejectDeleteActiveSiteProfile() {
        SiteProfileDO active = new SiteProfileDO();
        active.setId(1L);
        active.setActive(true);
        when(siteProfileMapper.selectById(1L)).thenReturn(active);

        assertThrows(BusinessException.class, () -> service.deleteAdminSiteProfile(1L));
    }

    @Test
    void shouldRejectDeleteWhenOnlyOneSiteProfileLeft() {
        SiteProfileDO profile = new SiteProfileDO();
        profile.setId(2L);
        profile.setActive(false);
        when(siteProfileMapper.selectById(2L)).thenReturn(profile);
        when(siteProfileMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.deleteAdminSiteProfile(2L));
    }

    @Test
    void shouldCreateParentMessageWithoutReview() {
        SysUserDO user = new SysUserDO();
        user.setId(8L);
        user.setNickname("ParentA");
        user.setAvatarUrl("https://example.com/avatar.png");
        when(sysUserMapper.selectById(8L)).thenReturn(user);

        var view = service.createParentMessage(new CreateParentMessageCommand(8L, "  message content  "));

        assertEquals("ParentA", view.nickname());
        assertEquals("message content", view.content());
        verify(parentMessageMapper).insert(any(ParentMessageDO.class));
    }

    @Test
    void shouldListOnlyMyParentMessages() {
        ParentMessageDO mine = new ParentMessageDO();
        mine.setId(10L);
        mine.setUserId(8L);
        mine.setNicknameSnapshot("ParentA");
        mine.setContent("my message");

        ParentMessageDO reply = new ParentMessageDO();
        reply.setId(11L);
        reply.setRootId(10L);
        reply.setParentId(10L);
        reply.setSenderType("ADMIN");
        reply.setNicknameSnapshot("ADMIN");
        reply.setContent("admin reply");

        when(parentMessageMapper.selectCount(any())).thenReturn(1L);
        when(parentMessageMapper.selectList(any())).thenReturn(List.of(mine), List.of(reply));

        var page = service.listMyParentMessages(8L, 1, 10);
        assertEquals(1, page.total());
        assertEquals(1, page.items().size());
        ParentMessageView first = page.items().get(0);
        assertEquals("my message", first.content());
        assertEquals("ADMIN", first.replies().get(0).senderType());
    }

    @Test
    void shouldCreateAdminNotice() {
        var view = service.createAdminNotice(
            new com.stufamily.backend.home.application.command.AdminNoticeCreateCommand(
                "系统通知",
                null,
                true,
                10,
                null,
                null
            ),
            1L
        );

        assertEquals("系统通知", view.title());
        assertEquals(null, view.content());
        verify(homeNoticeMapper).insert(any(HomeNoticeDO.class));
    }
}


package com.stufamily.backend.adminapi.controller;

import com.stufamily.backend.adminapi.request.AdminBannerUpdateRequest;
import com.stufamily.backend.adminapi.request.AdminNoticeCreateRequest;
import com.stufamily.backend.adminapi.request.AdminReplyParentMessageRequest;
import com.stufamily.backend.adminapi.request.AdminSiteProfileUpdateRequest;
import com.stufamily.backend.home.application.command.AdminBannerUpdateCommand;
import com.stufamily.backend.home.application.command.AdminNoticeCreateCommand;
import com.stufamily.backend.home.application.command.AdminReplyParentMessageCommand;
import com.stufamily.backend.home.application.command.AdminSiteProfileUpdateCommand;
import com.stufamily.backend.home.application.dto.AdminHomeNoticeView;
import com.stufamily.backend.home.application.dto.AdminHomepageBannerView;
import com.stufamily.backend.home.application.dto.AdminParentMessageDetailView;
import com.stufamily.backend.home.application.dto.AdminParentMessageView;
import com.stufamily.backend.home.application.dto.AdminSiteProfileView;
import com.stufamily.backend.home.application.service.HomeApplicationService;
import com.stufamily.backend.shared.api.ApiResponse;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/home")
public class AdminHomeController {

    private final HomeApplicationService homeApplicationService;

    public AdminHomeController(HomeApplicationService homeApplicationService) {
        this.homeApplicationService = homeApplicationService;
    }

    @GetMapping("/banners")
    public ApiResponse<PageResult<AdminHomepageBannerView>> listBanners(
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(homeApplicationService.listAdminBanners(pageNo, pageSize));
    }

    @PutMapping("/banners/{bannerId}")
    public ApiResponse<AdminHomepageBannerView> updateBanner(
        @PathVariable("bannerId") Long bannerId,
        @Valid @RequestBody AdminBannerUpdateRequest request) {
        Long userId = CurrentUser.requireUserId();
        return ApiResponse.success(homeApplicationService.updateAdminBanner(
            bannerId,
            new AdminBannerUpdateCommand(
                request.title(),
                request.imageUrl(),
                request.linkType(),
                request.linkTarget(),
                request.sortOrder(),
                request.enabled(),
                request.startAt(),
                request.endAt()
            ),
            userId
        ));
    }

    @DeleteMapping("/banners/{bannerId}")
    public ApiResponse<Void> deleteBanner(@PathVariable("bannerId") Long bannerId) {
        homeApplicationService.deleteAdminBanner(bannerId);
        return ApiResponse.ok();
    }

    @GetMapping("/notices")
    public ApiResponse<PageResult<AdminHomeNoticeView>> listNotices(
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(homeApplicationService.listAdminNotices(pageNo, pageSize));
    }

    @PostMapping("/notices")
    public ApiResponse<AdminHomeNoticeView> createNotice(@Valid @RequestBody AdminNoticeCreateRequest request) {
        Long userId = CurrentUser.requireUserId();
        return ApiResponse.success(homeApplicationService.createAdminNotice(
            new AdminNoticeCreateCommand(
                request.title(),
                request.content(),
                request.enabled(),
                request.sortOrder(),
                request.startAt(),
                request.endAt()
            ),
            userId
        ));
    }

    @DeleteMapping("/notices/{noticeId}")
    public ApiResponse<Void> deleteNotice(@PathVariable("noticeId") Long noticeId) {
        homeApplicationService.deleteAdminNotice(noticeId);
        return ApiResponse.ok();
    }

    @GetMapping("/site-profiles")
    public ApiResponse<PageResult<AdminSiteProfileView>> listSiteProfiles(
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(homeApplicationService.listAdminSiteProfiles(pageNo, pageSize));
    }

    @GetMapping("/site-profiles/{siteProfileId}")
    public ApiResponse<AdminSiteProfileView> getSiteProfile(@PathVariable("siteProfileId") Long siteProfileId) {
        return ApiResponse.success(homeApplicationService.getAdminSiteProfile(siteProfileId));
    }

    @PutMapping("/site-profiles/{siteProfileId}")
    public ApiResponse<AdminSiteProfileView> updateSiteProfile(
        @PathVariable("siteProfileId") Long siteProfileId,
        @Valid @RequestBody AdminSiteProfileUpdateRequest request) {
        Long userId = CurrentUser.requireUserId();
        return ApiResponse.success(homeApplicationService.updateAdminSiteProfile(
            siteProfileId,
            new AdminSiteProfileUpdateCommand(
                request.communityName(),
                request.bannerSlogan(),
                request.introText(),
                request.contactPerson(),
                request.contactPhone(),
                request.contactWechat(),
                request.contactWechatQrUrl(),
                request.addressText(),
                request.latitude(),
                request.longitude(),
                request.active()
            ),
            userId
        ));
    }

    @DeleteMapping("/site-profiles/{siteProfileId}")
    public ApiResponse<Void> deleteSiteProfile(@PathVariable("siteProfileId") Long siteProfileId) {
        homeApplicationService.deleteAdminSiteProfile(siteProfileId);
        return ApiResponse.ok();
    }

    @GetMapping("/messages")
    public ApiResponse<PageResult<AdminParentMessageView>> listMessages(
        @RequestParam(name = "viewed", required = false) Boolean viewed,
        @RequestParam(name = "replied", required = false) Boolean replied,
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(homeApplicationService.listAdminParentMessages(viewed, replied, pageNo, pageSize));
    }

    @GetMapping("/messages/{messageId}")
    public ApiResponse<AdminParentMessageDetailView> getMessageDetail(@PathVariable("messageId") Long messageId) {
        return ApiResponse.success(homeApplicationService.getAdminParentMessageDetail(messageId));
    }

    @PostMapping("/messages/{messageId}/reply")
    public ApiResponse<AdminParentMessageDetailView> replyMessage(
        @PathVariable("messageId") Long messageId,
        @Valid @RequestBody AdminReplyParentMessageRequest request) {
        Long adminUserId = CurrentUser.requireUserId();
        return ApiResponse.success(homeApplicationService.replyAdminParentMessage(
            messageId, new AdminReplyParentMessageCommand(request.content()), adminUserId));
    }

    @PostMapping("/messages/{messageId}/close")
    public ApiResponse<Void> closeMessage(@PathVariable("messageId") Long messageId) {
        Long adminUserId = CurrentUser.requireUserId();
        homeApplicationService.closeAdminParentMessage(messageId, adminUserId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(@PathVariable("messageId") Long messageId) {
        Long adminUserId = CurrentUser.requireUserId();
        homeApplicationService.deleteAdminParentMessage(messageId, adminUserId);
        return ApiResponse.ok();
    }
}

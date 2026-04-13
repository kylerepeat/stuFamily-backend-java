package com.stufamily.backend.weixinapi.controller;

import com.stufamily.backend.family.application.command.AddFamilyMemberCommand;
import com.stufamily.backend.family.application.command.AddFamilyCheckInCommand;
import com.stufamily.backend.family.application.dto.FamilyCheckInView;
import com.stufamily.backend.family.application.dto.FamilyGroupQuotaView;
import com.stufamily.backend.family.application.dto.FamilyMemberView;
import com.stufamily.backend.family.application.service.FamilyApplicationService;
import com.stufamily.backend.shared.api.ApiResponse;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.security.CurrentUser;
import com.stufamily.backend.weixinapi.request.AddFamilyCheckInRequest;
import com.stufamily.backend.weixinapi.request.AddFamilyMemberRequest;
import jakarta.validation.Valid;
import java.time.ZoneOffset;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weixin/family")
public class WeixinFamilyController {
    private static final ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.ofHours(8);

    private final FamilyApplicationService familyApplicationService;

    public WeixinFamilyController(FamilyApplicationService familyApplicationService) {
        this.familyApplicationService = familyApplicationService;
    }

    @PostMapping("/members")
    public ApiResponse<FamilyMemberView> add(@Valid @RequestBody AddFamilyMemberRequest request) {
        Long userId = CurrentUser.requireUserId();
        return ApiResponse.success(familyApplicationService.addMember(
            new AddFamilyMemberCommand(userId, request.groupNo(), request.memberName(), request.studentOrCardNo(), request.phone(),
                request.joinedAt().atOffset(DEFAULT_ZONE_OFFSET)))
        );
    }

    @PostMapping("/check-ins")
    public ApiResponse<FamilyCheckInView> addCheckIn(@Valid @RequestBody AddFamilyCheckInRequest request) {
        Long userId = CurrentUser.requireUserId();
        return ApiResponse.success(familyApplicationService.addCheckIn(
            new AddFamilyCheckInCommand(
                userId,
                request.groupNo(),
                request.familyMemberId(),
                request.latitude(),
                request.longitude(),
                request.addressText(),
                request.checkedInAt().atOffset(DEFAULT_ZONE_OFFSET)
            )
        ));
    }

    @GetMapping("/members")
    public ApiResponse<PageResult<FamilyMemberView>> search(
        @RequestParam(name = "group_no", required = false) String groupNo,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        Long userId = CurrentUser.requireUserId();
        return ApiResponse.success(familyApplicationService.searchMembers(userId, groupNo, keyword, pageNo, pageSize));
    }

    @DeleteMapping("/members/{memberNo}")
    public ApiResponse<Void> cancel(@PathVariable("memberNo") String memberNo) {
        Long userId = CurrentUser.requireUserId();
        familyApplicationService.cancelExpiredCard(userId, memberNo);
        return ApiResponse.ok();
    }

    @GetMapping("/group/quota")
    public ApiResponse<FamilyGroupQuotaView> groupQuota() {
        Long userId = CurrentUser.requireUserId();
        return ApiResponse.success(familyApplicationService.getCurrentGroupQuota(userId));
    }
}

package com.stufamily.backend.weixinapi.controller;

import com.stufamily.backend.home.application.command.CreateParentMessageCommand;
import com.stufamily.backend.home.application.dto.HomeProductDetailView;
import com.stufamily.backend.home.application.dto.HomeProductView;
import com.stufamily.backend.home.application.dto.HomePageView;
import com.stufamily.backend.home.application.dto.ParentMessageView;
import com.stufamily.backend.home.application.service.HomeApplicationService;
import com.stufamily.backend.shared.api.ApiResponse;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.security.CurrentUser;
import com.stufamily.backend.weixinapi.request.CreateParentMessageRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weixin/home")
public class WeixinHomeController {

    private final HomeApplicationService homeApplicationService;

    public WeixinHomeController(HomeApplicationService homeApplicationService) {
        this.homeApplicationService = homeApplicationService;
    }

    @GetMapping("/index")
    public ApiResponse<HomePageView> index() {
        return ApiResponse.success(homeApplicationService.loadHomePage());
    }

    @GetMapping("/products")
    public ApiResponse<List<HomeProductView>> products(
        @RequestParam(name = "sale_start_at", required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate saleStartAt,
        @RequestParam(name = "sale_end_at", required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate saleEndAt) {
        return ApiResponse.success(homeApplicationService.loadProducts(saleStartAt, saleEndAt));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<HomeProductDetailView> productDetail(@PathVariable("productId") Long productId) {
        return ApiResponse.success(homeApplicationService.loadProductDetail(productId));
    }

    @PostMapping("/messages")
    public ApiResponse<ParentMessageView> createMessage(@Valid @RequestBody CreateParentMessageRequest request) {
        Long userId = CurrentUser.requireUserId();
        return ApiResponse.success(homeApplicationService.createParentMessage(
            new CreateParentMessageCommand(userId, request.content()))
        );
    }

    @GetMapping("/messages/mine")
    public ApiResponse<PageResult<ParentMessageView>> myMessages(
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        Long userId = CurrentUser.requireUserId();
        return ApiResponse.success(homeApplicationService.listMyParentMessages(userId, pageNo, pageSize));
    }
}

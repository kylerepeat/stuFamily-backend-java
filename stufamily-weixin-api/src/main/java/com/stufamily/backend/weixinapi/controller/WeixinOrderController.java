package com.stufamily.backend.weixinapi.controller;

import com.stufamily.backend.identity.application.service.AuthApplicationService;
import com.stufamily.backend.order.application.command.CreateOrderCommand;
import com.stufamily.backend.order.application.command.SubmitServiceReviewCommand;
import com.stufamily.backend.order.application.dto.OrderCreateResult;
import com.stufamily.backend.order.application.dto.PurchasedProductView;
import com.stufamily.backend.order.application.service.OrderApplicationService;
import com.stufamily.backend.shared.api.ApiResponse;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.security.CurrentUser;
import com.stufamily.backend.weixinapi.request.CreateOrderRequest;
import com.stufamily.backend.weixinapi.request.SubmitServiceReviewRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weixin/orders")
public class WeixinOrderController {

    private final OrderApplicationService orderApplicationService;
    private final AuthApplicationService authApplicationService;

    public WeixinOrderController(OrderApplicationService orderApplicationService,
                                 AuthApplicationService authApplicationService) {
        this.orderApplicationService = orderApplicationService;
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/create")
    public ApiResponse<OrderCreateResult> create(@Valid @RequestBody CreateOrderRequest request, HttpServletRequest rawRequest) {
        Long userId = CurrentUser.requireUserId();
        String openid = authApplicationService.requireOpenid(userId);
        OrderCreateResult result = orderApplicationService.createOrder(
            new CreateOrderCommand(
                userId,
                request.productType(),
                request.productId(),
                request.skuId(),
                request.durationType(),
                request.cardApplyDate(),
                request.applicantName(),
                request.applicantStudentOrCardNo(),
                request.applicantPhone(),
                request.amountCents(),
                rawRequest.getRemoteAddr()
            ),
            openid
        );
        return ApiResponse.success(result);
    }

    @GetMapping("/{orderNo}/status")
    public ApiResponse<String> status(@PathVariable("orderNo") String orderNo) {
        return ApiResponse.success(orderApplicationService.findOrderStatus(orderNo));
    }

    @GetMapping("/purchased-products")
    public ApiResponse<PageResult<PurchasedProductView>> listPurchasedProducts(
        @RequestParam(name = "product_type", required = false) String productType,
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        Long userId = CurrentUser.requireUserId();
        return ApiResponse.success(orderApplicationService.listPurchasedProducts(userId, productType, pageNo, pageSize));
    }

    @PostMapping("/{orderNo}/review")
    public ApiResponse<Void> submitReview(@PathVariable("orderNo") String orderNo,
                                          @Valid @RequestBody SubmitServiceReviewRequest request) {
        Long userId = CurrentUser.requireUserId();
        orderApplicationService.submitServiceReview(
            new SubmitServiceReviewCommand(userId, orderNo, request.stars(), request.content())
        );
        return ApiResponse.ok();
    }
}

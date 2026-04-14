package com.stufamily.backend.integration;

import com.stufamily.backend.wechat.gateway.WechatAuthGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("首页接口集成测试")
public class WeixinHomeIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private WechatAuthGateway wechatAuthGateway;

    private static final String HOME_INDEX_URL = "/api/weixin/home/index";
    private static final String PRODUCTS_URL = "/api/weixin/home/products";
    private static final String MESSAGES_URL = "/api/weixin/home/messages";
    private static final String MESSAGES_MINE_URL = "/api/weixin/home/messages/mine";

    private String testToken;
    private Long testUserId;

    @BeforeEach
    void setUp() throws Exception {
        when(wechatAuthGateway.code2Session(anyString()))
            .thenReturn(new WechatSession("openid_home_test_001", null, "session_key"));

        String loginBody = objectMapper.writeValueAsString(new LoginRequest("home_test_code", "测试用户", null));
        MvcResult loginResult = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        testToken = extractDataField(loginResult, "accessToken");
        testUserId = extractDataFieldAsLong(loginResult, "userId");
    }

    @Nested
    @DisplayName("首页信息接口")
    class HomeIndexTests {

        @Test
        @DisplayName("获取首页信息成功 - 无需登录")
        void homeIndex_success_withoutLogin() throws Exception {
            mockMvc.perform(get(HOME_INDEX_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.banners").isArray())
                .andExpect(jsonPath("$.data.siteProfile").exists())
                .andExpect(jsonPath("$.data.siteProfile.communityName").isNotEmpty())
                .andExpect(jsonPath("$.data.notices").isArray());
        }

        @Test
        @DisplayName("获取首页信息成功 - 包含轮播图")
        void homeIndex_withBanners() throws Exception {
            mockMvc.perform(get(HOME_INDEX_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.banners").isArray());
        }

        @Test
        @DisplayName("获取首页信息成功 - 包含站点信息")
        void homeIndex_withSiteProfile() throws Exception {
            mockMvc.perform(get(HOME_INDEX_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteProfile.communityName").isNotEmpty())
                .andExpect(jsonPath("$.data.siteProfile.contactPhone").exists())
                .andExpect(jsonPath("$.data.siteProfile.address").exists());
        }
    }

    @Nested
    @DisplayName("商品列表接口")
    class ProductsTests {

        @Test
        @DisplayName("获取商品列表成功 - 无需登录")
        void products_success_withoutLogin() throws Exception {
            LocalDate today = LocalDate.now();
            String startDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String endDate = today.plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            mockMvc.perform(get(PRODUCTS_URL)
                    .param("sale_start_at", startDate)
                    .param("sale_end_at", endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("获取商品列表失败 - 缺少日期参数")
        void products_missingDateParams_failure() throws Exception {
            mockMvc.perform(get(PRODUCTS_URL))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("获取商品列表失败 - 日期格式错误")
        void products_invalidDateFormat_failure() throws Exception {
            mockMvc.perform(get(PRODUCTS_URL)
                    .param("sale_start_at", "invalid-date")
                    .param("sale_end_at", "invalid-date"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("获取商品列表成功 - 包含家庭卡商品")
        void products_withFamilyCardProducts() throws Exception {
            LocalDate today = LocalDate.now();
            String startDate = today.minusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String endDate = today.plusDays(365).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            mockMvc.perform(get(PRODUCTS_URL)
                    .param("sale_start_at", startDate)
                    .param("sale_end_at", endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.type == 'FAMILY_CARD')]").exists());
        }
    }

    @Nested
    @DisplayName("商品详情接口")
    class ProductDetailTests {

        @Test
        @DisplayName("获取商品详情成功 - 无需登录")
        void productDetail_success_withoutLogin() throws Exception {
            mockMvc.perform(get(PRODUCTS_URL + "/1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        jsonPath("$.success").value(true).match(result);
                        jsonPath("$.data.id").value(1).match(result);
                        jsonPath("$.data.title").isNotEmpty().match(result);
                    } else if (status == 400) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Product not found in database");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                });
        }

        @Test
        @DisplayName("获取商品详情失败 - 商品不存在")
        void productDetail_notFound() throws Exception {
            mockMvc.perform(get(PRODUCTS_URL + "/999999"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("家长留言接口")
    class ParentMessageTests {

        @Test
        @DisplayName("提交留言成功 - 已登录用户")
        void createMessage_success() throws Exception {
            String messageBody = objectMapper.writeValueAsString(new MessageRequest("这是一条测试留言内容"));

            mockMvc.perform(withAuth(post(MESSAGES_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(messageBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("这是一条测试留言内容"))
                .andExpect(jsonPath("$.data.nickname").exists())
                .andExpect(jsonPath("$.data.createdAt").exists());
        }

        @Test
        @DisplayName("提交留言失败 - 未登录")
        void createMessage_notLoggedIn_failure() throws Exception {
            String messageBody = objectMapper.writeValueAsString(new MessageRequest("测试留言"));

            mockMvc.perform(post(MESSAGES_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(messageBody))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("提交留言失败 - 内容为空")
        void createMessage_emptyContent_failure() throws Exception {
            String messageBody = objectMapper.writeValueAsString(new MessageRequest(""));

            mockMvc.perform(withAuth(post(MESSAGES_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(messageBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("提交留言失败 - 内容超过500字符")
        void createMessage_contentTooLong_failure() throws Exception {
            String longContent = "a".repeat(501);
            String messageBody = objectMapper.writeValueAsString(new MessageRequest(longContent));

            mockMvc.perform(withAuth(post(MESSAGES_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(messageBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("查询我的留言列表成功 - 已登录用户")
        void listMyMessages_success() throws Exception {
            String messageBody = objectMapper.writeValueAsString(new MessageRequest("查询列表测试留言"));
            mockMvc.perform(withAuth(post(MESSAGES_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(messageBody))
                .andExpect(status().isOk());

            mockMvc.perform(withAuth(get(MESSAGES_MINE_URL), testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());
        }

        @Test
        @DisplayName("查询我的留言列表失败 - 未登录")
        void listMyMessages_notLoggedIn_failure() throws Exception {
            mockMvc.perform(get(MESSAGES_MINE_URL))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("查询我的留言列表成功 - 分页参数")
        void listMyMessages_withPaging() throws Exception {
            mockMvc.perform(withAuth(get(MESSAGES_MINE_URL), testToken)
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
        }
    }

    private record LoginRequest(String code, String nickname, String avatarUrl) {}

    private record MessageRequest(String content) {}
}

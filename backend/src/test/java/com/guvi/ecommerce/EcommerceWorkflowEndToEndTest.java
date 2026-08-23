package com.guvi.ecommerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guvi.ecommerce.entity.User;
import com.guvi.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end coverage of the user-facing workflows over the real security filter
 * chain and an in-memory database. Razorpay's hosted checkout is the only step
 * excluded — it requires a live API key — so this stops at the checkout boundary.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EcommerceWorkflowEndToEndTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long productId;

    @BeforeEach
    void reset() {
        cartItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        productId = productRepository.save(com.guvi.ecommerce.entity.Product.builder()
                .name("Wireless Mouse").description("Ergonomic")
                .price(BigDecimal.valueOf(799)).stock(5).category("electronics")
                .build()).getId();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"name\":\"Buyer\",\"email\":\"%s\",\"password\":\"password123\"}", email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String createAdminAndGetToken() throws Exception {
        userRepository.save(User.builder().name("Admin").email("admin@test.com")
                .password(passwordEncoder.encode("password123")).role(User.Role.ADMIN).build());
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    // ---------- auth ----------

    @Test
    void register_thenLogin_issuesUsableToken() throws Exception {
        registerAndGetToken("buyer@test.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"buyer@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returns400WithMessage() throws Exception {
        registerAndGetToken("dup@test.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Buyer\",\"email\":\"dup@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void login_wrongPassword_returns401WithMessage() throws Exception {
        registerAndGetToken("buyer@test.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"buyer@test.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginResponse_neverContainsPasswordHash() throws Exception {
        registerAndGetToken("buyer@test.com");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"buyer@test.com\",\"password\":\"password123\"}"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("password");
    }

    // ---------- browsing & role-based access ----------

    @Test
    void products_areBrowsableAndSearchableWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/products/search").param("query", "mouse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Wireless Mouse"));
    }

    @Test
    void createProduct_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"price\":10,\"stock\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required. Please log in."));
    }

    @Test
    void expiredOrTamperedToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_asCustomer_isForbidden() throws Exception {
        String token = registerAndGetToken("buyer@test.com");

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"price\":10,\"stock\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUpdateAndDeleteProducts() throws Exception {
        String token = createAdminAndGetToken();

        MvcResult created = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Keyboard\",\"price\":1499,\"stock\":3}"))
                .andExpect(status().isOk())
                .andReturn();
        long newId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/products/" + newId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Keyboard Pro\",\"price\":1799,\"stock\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Keyboard Pro"));

        mockMvc.perform(delete("/api/products/" + newId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + newId))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminOrdersEndpoint_isForbiddenToCustomers() throws Exception {
        String token = registerAndGetToken("buyer@test.com");

        mockMvc.perform(get("/api/orders/admin/all").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/orders/admin/all")
                        .header("Authorization", "Bearer " + createAdminAndGetToken()))
                .andExpect(status().isOk());
    }

    // ---------- cart workflow ----------

    @Test
    void cartWorkflow_addUpdateRemove_keepsTotalsConsistent() throws Exception {
        String token = registerAndGetToken("buyer@test.com");

        MvcResult added = mockMvc.perform(post("/api/cart")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"productId\":%d,\"quantity\":2}", productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.subtotal").value(1598))
                .andReturn();
        long itemId = objectMapper.readTree(added.getResponse().getContentAsString()).get("id").asLong();

        // adding the same product again accumulates rather than duplicating the line
        mockMvc.perform(post("/api/cart")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"productId\":%d,\"quantity\":1}", productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3));

        mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subtotal").value(2397));

        mockMvc.perform(put("/api/cart/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(799));

        mockMvc.perform(delete("/api/cart/" + itemId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cartResponse_neverExposesTheOwningUser() throws Exception {
        String token = registerAndGetToken("buyer@test.com");
        mockMvc.perform(post("/api/cart")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"productId\":%d,\"quantity\":1}", productId)));

        MvcResult result = mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("password");
        JsonNode firstItem = objectMapper.readTree(body).get(0);
        assertThat(firstItem.has("user")).isFalse();
    }

    @Test
    void addToCart_beyondStock_isRejectedWithReadableMessage() throws Exception {
        String token = registerAndGetToken("buyer@test.com");

        mockMvc.perform(post("/api/cart")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"productId\":%d,\"quantity\":99}", productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("in stock")));
    }

    @Test
    void oneCustomerCannotTouchAnotherCustomersCartItem() throws Exception {
        String ownerToken = registerAndGetToken("owner@test.com");
        MvcResult added = mockMvc.perform(post("/api/cart")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"productId\":%d,\"quantity\":1}", productId)))
                .andReturn();
        long itemId = objectMapper.readTree(added.getResponse().getContentAsString()).get("id").asLong();

        String intruderToken = registerAndGetToken("intruder@test.com");

        mockMvc.perform(delete("/api/cart/" + itemId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkout_withEmptyCart_isRejected() throws Exception {
        String token = registerAndGetToken("buyer@test.com");

        mockMvc.perform(post("/api/orders/checkout").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cart is empty"));
    }

    // ---------- profile ----------

    @Test
    void profile_canBeReadAndUpdated() throws Exception {
        String token = registerAndGetToken("buyer@test.com");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("buyer@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Buyer\",\"phone\":\"9876543210\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Buyer"))
                .andExpect(jsonPath("$.phone").value("9876543210"));

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.name").value("Updated Buyer"));
    }

    @Test
    void profile_passwordChange_takesEffectOnNextLogin() throws Exception {
        String token = registerAndGetToken("buyer@test.com");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Buyer\",\"currentPassword\":\"password123\",\"newPassword\":\"newpassword456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"buyer@test.com\",\"password\":\"newpassword456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"buyer@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profile_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- order serialization ----------

    /**
     * Regression guard for the bidirectional Order/OrderItem cycle. This persists a
     * real order and reads it back through the API, so the entities arrive as live
     * Hibernate objects with a lazily-proxied user — the exact shape that used to
     * blow the stack during JSON serialization.
     */
    @Test
    void persistedOrder_isReadableByOwnerAndAdminWithoutRecursion() throws Exception {
        String token = registerAndGetToken("buyer@test.com");
        User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();
        com.guvi.ecommerce.entity.Product product = productRepository.findById(productId).orElseThrow();

        com.guvi.ecommerce.entity.Order order = orderRepository.save(
                com.guvi.ecommerce.entity.Order.builder()
                        .user(buyer)
                        .totalAmount(BigDecimal.valueOf(1598))
                        .status(com.guvi.ecommerce.entity.Order.OrderStatus.PAID)
                        .razorpayOrderId("order_LIVE1")
                        .razorpayPaymentId("pay_LIVE1")
                        .build());
        order.setItems(java.util.List.of(com.guvi.ecommerce.entity.OrderItem.builder()
                .order(order).product(product).quantity(2).price(BigDecimal.valueOf(799))
                .build()));
        orderRepository.save(order);

        // the customer's own history
        MvcResult mine = mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].totalAmount").value(1598))
                .andExpect(jsonPath("$[0].items.length()").value(1))
                .andExpect(jsonPath("$[0].items[0].product.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$[0].items[0].order").doesNotExist())
                .andReturn();
        assertThat(mine.getResponse().getContentAsString()).doesNotContain("password");

        // the admin view, which additionally renders the buyer's email
        MvcResult all = mockMvc.perform(get("/api/orders/admin/all")
                        .header("Authorization", "Bearer " + createAdminAndGetToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].user.email").value("buyer@test.com"))
                .andExpect(jsonPath("$[0].user.password").doesNotExist())
                .andReturn();
        assertThat(all.getResponse().getContentAsString()).doesNotContain("password");
    }

    @Test
    void adminCanAdvanceOrderStatus_andRejectsUnknownValues() throws Exception {
        User buyer = userRepository.save(User.builder().name("Buyer").email("buyer@test.com")
                .password(passwordEncoder.encode("password123")).role(User.Role.CUSTOMER).build());
        com.guvi.ecommerce.entity.Order order = orderRepository.save(
                com.guvi.ecommerce.entity.Order.builder()
                        .user(buyer).totalAmount(BigDecimal.TEN)
                        .status(com.guvi.ecommerce.entity.Order.OrderStatus.CREATED)
                        .razorpayOrderId("order_LIVE2").build());

        String adminToken = createAdminAndGetToken();

        mockMvc.perform(put("/api/orders/admin/" + order.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(put("/api/orders/admin/" + order.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "TELEPORTED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Unknown order status")));
    }
}

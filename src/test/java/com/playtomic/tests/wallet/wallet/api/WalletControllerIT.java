package com.playtomic.tests.wallet.wallet.api;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.playtomic.tests.wallet.wallet.model.Wallet;
import com.playtomic.tests.wallet.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class WalletControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRepository walletRepository;

    @AfterEach
    public void tearDown() {
        walletRepository.deleteAll();
    }

    @Test
    public void whenCreatingWalletWithAUserID_thenReturnsTheWallet() throws Exception {
        UUID userId = UUID.randomUUID();

        ResultActions response = mockMvc.perform(post("/v1/wallet/")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":\"" + userId + "\"}"));

        response.andExpect(status().isCreated());
        response.andExpect(jsonPath("$.id", is(notNullValue())));
        response.andExpect(jsonPath("$.userId", is(userId.toString())));
        response.andExpect(jsonPath("$.amount", is(0)));
        String jsonResponse = response.andReturn().getResponse().getContentAsString();
        String walletIdString = JsonPath.parse(jsonResponse).read("$.id", String.class);
        UUID walletId = UUID.fromString(walletIdString);
        Wallet actual = walletRepository.findById(walletId).orElseThrow();
        assertEquals(userId, actual.getUserId());
        assertEquals(0, BigDecimal.valueOf(0).compareTo(actual.getAmount()));
    }

    @Test
    public void whenCreatingWalletAnErrorIsProduced_thenReturnsError() throws Exception {
        UUID userId = UUID.randomUUID();
        walletRepository.save(aNewWalletWithUserIdAndAmount(userId, new BigDecimal(0)));

        ResultActions response = mockMvc.perform(post("/v1/wallet/")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":\"" + userId + "\"}"));

        response.andExpect(status().isBadRequest());
        response.andExpect(jsonPath("$.error",
            is("Failed to create wallet, maybe the user has already a Wallet")));
    }

    @Test
    public void whenCreatingWalletWithoutAUserID_thenReturnsMissingBody() throws Exception {
        ResultActions response = mockMvc.perform(post("/v1/wallet/")
            .contentType(MediaType.APPLICATION_JSON));

        response.andExpect(status().isBadRequest());
        response.andExpect(jsonPath("$.error", is("Request body is missing or malformed")));
    }

    @Test
    public void whenCreatingWalletWithEmptyUserId_thenReturnsInvalidUserId() throws Exception {
        ResultActions response = mockMvc.perform(post("/v1/wallet/")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":\"\"}"));

        response.andExpect(status().isBadRequest());
        response.andExpect(jsonPath("$.userId", is("userID should be a valid UUID")));
    }

    @Test
    public void whenCreatingWalletWithUserIdNotUUID_thenReturnsInvalidUserId() throws Exception {
        ResultActions response = mockMvc.perform(post("/v1/wallet/")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":\"\"}"));

        response.andExpect(status().isBadRequest());
        response.andExpect(jsonPath("$.userId", is("userID should be a valid UUID")));
    }

    private static Wallet aNewWalletWithUserIdAndAmount(UUID userId, BigDecimal amount) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setAmount(amount);
        return wallet;
    }
}

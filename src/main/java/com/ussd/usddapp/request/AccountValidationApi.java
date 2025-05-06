package com.ussd.usddapp.request;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ussd.usddapp.dto.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;

@Slf4j
public class AccountValidationApi {

    private static final String API_URL = "https://52.49.107.237:8110/api/v1/web";
    private static final String API_KEY = "your-secure-api-key-here";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static AccountValidationResponse validateAccount(AccountValidationRequest request) throws IOException {
        log.info("Sending account validation request for account: {}", request.getAccount());

        String json = objectMapper.writeValueAsString(request);
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);
        Request httpRequest = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("X-API-Key", API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                log.error("Account validation failed: HTTP {}", response.code());
                throw new IOException("Unexpected code " + response.code());
            }

            String responseBody = response.body().string();
            log.debug("Account validation response: {}", responseBody);
            return objectMapper.readValue(responseBody, AccountValidationResponse.class);
        }
    }

}

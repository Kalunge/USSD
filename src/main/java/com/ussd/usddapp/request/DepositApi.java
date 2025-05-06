package com.ussd.usddapp.request;

import com.fasterxml.jackson.databind.*;
import com.ussd.usddapp.dto.*;
import lombok.extern.slf4j.*;
import okhttp3.*;

import java.io.*;


@Slf4j
public class DepositApi {

    private static final String API_URL = "https://52.49.107.237:8110/api/v1/web";
    private static final String API_KEY = "your-secure-api-key-here";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static DepositResponse performDeposit(DepositRequest request) throws IOException {
        log.info("Sending deposit request for account: {}, amount: {}", request.getAccount1(), request.getAmount());

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
                log.error("Deposit failed: HTTP {}", response.code());
                throw new IOException("Unexpected code " + response.code());
            }

            String responseBody = response.body().string();
            log.debug("Deposit response: {}", responseBody);
            return objectMapper.readValue(responseBody, DepositResponse.class);
        }
    }
}

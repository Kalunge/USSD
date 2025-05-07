package com.ussd.usddapp.request;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.ussd.usddapp.dto.*;
import lombok.*;
import lombok.extern.slf4j.*;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.*;

import java.io.*;

@Slf4j
@Component
public class MobileMoneyValidationApi {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.url}")
    private String apiUrl;

    public MobileMoneyValidationResponse validateMobileAccount(MobileMoneyValidationRequest request) throws IOException {
        log.info("Sending mobile money validation request - phoneNo: {}, providerId: {}",
                request.getPhoneNo(), request.getProviderId());

        OkHttpClient client = OkHttpClientCertUtil.createUnsafeOkHttpClient();
        String json = objectMapper.writeValueAsString(request);
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);
        Request httpRequest = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (response.body() == null) {
                log.error("Mobile money validation failed: Empty response body");
                throw new IOException("Empty response body");
            }

            String responseBody = response.body().string();
            log.info("Validation response: {}", responseBody);

            if (!response.isSuccessful()) {
                log.error("Mobile money validation failed: HTTP {}", response.code());
                throw new IOException("Unexpected code " + response.code());
            }

            return objectMapper.readValue(responseBody, MobileMoneyValidationResponse.class);
        } catch (IOException e) {
            log.error("Error during mobile money validation: {}", e.getMessage(), e);
            throw e;
        }
    }
}
package com.ussd.usddapp.request;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ussd.usddapp.dto.MobileMoneyRequest;
import com.ussd.usddapp.dto.MobileMoneyResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class MobileMoneyApi {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.url}")
    private String apiUrl;


    public MobileMoneyResponse performMobileMoneyOperation(MobileMoneyRequest request) throws IOException {
        log.info("Sending mobile money request - type: {}, phoneNo: {}, amount: {}",
                request.getType(), request.getPhoneNo(), request.getBillAmount());

        OkHttpClient client = OkHttpClientCertUtil.createUnsafeOkHttpClient();
        String json = objectMapper.writeValueAsString(request);
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);
        Request httpRequest = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                log.error("Mobile money operation failed: HTTP {}", response.code());
                throw new IOException("Unexpected code " + response.code());
            }

            String responseBody = response.body().string();
            log.debug("Mobile money response: {}", responseBody);
            return objectMapper.readValue(responseBody, MobileMoneyResponse.class);
        }
    }
}
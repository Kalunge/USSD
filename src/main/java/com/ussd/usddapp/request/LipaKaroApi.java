package com.ussd.usddapp.request;

import com.fasterxml.jackson.databind.*;
import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.dto.lipakaro.*;
import lombok.extern.slf4j.*;
import okhttp3.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;

import static com.ussd.usddapp.request.OkHttpClientCertUtil.createUnsafeOkHttpClient;

@Slf4j
@Component
public class LipaKaroApi {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.url}")
    private String apiUrl;

    public LipaKaroResponse payFees(LipaKaroRequest request) throws IOException {
        log.info("Sending deposit request for account: {}, amount: {}", request.getAccount(), request.getAmount());

        OkHttpClient client = createUnsafeOkHttpClient();
        String json = objectMapper.writeValueAsString(request);
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);
        Request httpRequest = new Request.Builder().url(apiUrl).post(body).addHeader("Content-Type", "application/json").build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                log.error("Deposit failed: HTTP {}", response.code());
                throw new IOException("Unexpected code " + response.code());
            }

            String responseBody = response.body().string();
            log.info("lipa karo response: {}", responseBody);
            return objectMapper.readValue(responseBody, LipaKaroResponse.class);
        }
    }
}

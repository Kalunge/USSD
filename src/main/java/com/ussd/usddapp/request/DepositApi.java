package com.ussd.usddapp.request;

import com.fasterxml.jackson.databind.*;
import com.ussd.usddapp.dto.*;
import io.lettuce.core.dynamic.annotation.*;
import lombok.extern.slf4j.*;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.*;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;


@Slf4j
@Component
public class DepositApi {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.url}")
    private String apiUrl;

    private static OkHttpClient createUnsafeOkHttpClient() {
        try {
            log.info("Setting up unsafe OkHttpClient to bypass SSL validation");
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            log.debug("Skipping client certificate validation");
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            log.debug("Skipping server certificate validation");
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> {
                        log.debug("Accepting hostname: {}", hostname);
                        return true;
                    })
                    .build();
        } catch (Exception e) {
            log.error("Failed to create unsafe OkHttpClient: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create OkHttpClient with disabled SSL validation", e);
        }
    }

    public  DepositResponse performDeposit(DepositRequest request) throws IOException {
        log.info("Sending deposit request for account: {}, amount: {}", request.getAccount1(), request.getAmount());

        OkHttpClient client = createUnsafeOkHttpClient();
        String json = objectMapper.writeValueAsString(request);
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);
        Request httpRequest = new Request.Builder()
                .url(apiUrl)
                .post(body)
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

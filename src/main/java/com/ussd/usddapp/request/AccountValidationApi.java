package com.ussd.usddapp.request;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ussd.usddapp.dto.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.*;
import java.security.cert.*;

@Slf4j
@Component
public class AccountValidationApi {


    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.key}")
    private String apiKey;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

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

    public AccountValidationResponse validateAccount(AccountValidationRequest request) throws IOException {
        log.info("Sending account validation request for account: {}", request.getAccount());
        request.setApiKey(apiKey);

        String json = objectMapper.writeValueAsString(request);
        OkHttpClient client = createUnsafeOkHttpClient();

        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);
        Request httpRequest = new Request.Builder()
                .url(apiUrl)
                .post(body)
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

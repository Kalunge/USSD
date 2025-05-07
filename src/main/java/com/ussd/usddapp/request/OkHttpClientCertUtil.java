package com.ussd.usddapp.request;

import lombok.extern.slf4j.*;
import okhttp3.*;

import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;

@Slf4j
public class OkHttpClientCertUtil {
    static OkHttpClient createUnsafeOkHttpClient() {
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
}

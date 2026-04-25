package com.lvatong.lft.service;

import com.lvatong.lft.config.XfyunConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class XfyunAuthService {

    private final XfyunConfig xfyunConfig;

    private static final String HMAC_SHA256 = "HmacSHA256";

    public AuthResult generateAuthUrl(String dialect) {
        try {
            String host = "iat-api.xfyun.cn";
            String path = "/v2/iat";

            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = sdf.format(new Date());

            String origin = "host: " + host + "\ndate: " + date + "\nGET " + path + " HTTP/1.1";

            String signature = hmacSha256(xfyunConfig.getApiSecret(), origin);

            String authorization = String.format(
                    "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                    xfyunConfig.getApiKey(), "hmac-sha256", "host date request-line", signature
            );

            String authorizationBase64 = Base64.getEncoder().encodeToString(
                    authorization.getBytes(StandardCharsets.UTF_8)
            );

            String dialectParam = mapDialect(dialect);

            String url = xfyunConfig.getIatUrl()
                    + "?authorization=" + URLEncoder.encode(authorizationBase64, StandardCharsets.UTF_8)
                    + "&date=" + URLEncoder.encode(date, StandardCharsets.UTF_8)
                    + "&host=" + URLEncoder.encode(host, StandardCharsets.UTF_8)
                    + "&dialect_param=" + dialectParam;

            return new AuthResult(url, xfyunConfig.getAuthTtl());
        } catch (Exception e) {
            log.error("Failed to generate xfyun auth url", e);
            throw new RuntimeException("生成语音识别鉴权失败", e);
        }
    }

    private String mapDialect(String dialect) {
        if (dialect == null || dialect.isBlank()) {
            return "mandarin";
        }
        return switch (dialect.toLowerCase()) {
            case "cantonese" -> "cantonese";
            case "henanese" -> "henanese";
            default -> "mandarin";
        };
    }

    private String hmacSha256(String key, String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    public record AuthResult(String url, int expiresIn) {}
}

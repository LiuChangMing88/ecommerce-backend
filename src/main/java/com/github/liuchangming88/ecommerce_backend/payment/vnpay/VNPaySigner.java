package com.github.liuchangming88.ecommerce_backend.payment.vnpay;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public final class VNPaySigner {

    private VNPaySigner() {}

    public static String computeSignature(Map<String,String> params, String secret) {
        Map<String,String> sorted = new TreeMap<>(params);
        sorted.remove("vnp_SecureHash");
        sorted.remove("vnp_SecureHashType");

        String data = buildDataString(sorted);
        return hmac512(secret, data);
    }

    public static boolean verify(Map<String,String> params, String secret) {
        String provided = params.get("vnp_SecureHash");
        if (provided == null) return false;
        Map<String,String> sorted = new TreeMap<>(params);
        sorted.remove("vnp_SecureHash");
        sorted.remove("vnp_SecureHashType");
        String data = buildDataString(sorted);
        String expected = hmac512(secret, data);
        return expected.equalsIgnoreCase(provided);
    }

    public static String buildQueryStringWithHash(Map<String,String> params, String secret) {
        String signature = computeSignature(params, secret);
        return buildDataString(new TreeMap<>(params)) +
                "&vnp_SecureHash=" + signature +
                "&vnp_SecureHashType=HmacSHA512";
    }

    private static String buildDataString(Map<String,String> sorted) {
        return sorted.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .reduce((a,b) -> a + "&" + b)
                .orElse("");
    }

    private static String hmac512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute HMAC", e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
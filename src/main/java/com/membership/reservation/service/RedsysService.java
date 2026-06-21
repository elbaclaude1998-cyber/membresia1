package com.membership.reservation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.membership.reservation.dto.ReservationDtos.RedsysForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Integración Redsys (redirección) con firma HMAC_SHA256_V1:
 *  1. Se construyen los DS_MERCHANT_* en JSON y se codifican en Base64 (Ds_MerchantParameters).
 *  2. Se deriva una clave por pedido cifrando el número de pedido con 3DES (clave del comercio).
 *  3. La firma es HMAC-SHA256(Ds_MerchantParameters) con esa clave derivada, en Base64.
 */
@Service
public class RedsysService {

    private static final String SIGNATURE_VERSION = "HMAC_SHA256_V1";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String url;
    private final String merchantCode;
    private final String terminal;
    private final String currency;
    private final String transactionType;
    private final byte[] secretKey;
    private final String merchantUrl;
    private final String urlOk;
    private final String urlKo;

    public RedsysService(
            @Value("${redsys.url:https://sis-t.redsys.es:25443/sis/realizarPago}") String url,
            @Value("${redsys.merchant-code:999008881}") String merchantCode,
            @Value("${redsys.terminal:1}") String terminal,
            @Value("${redsys.currency:978}") String currency,
            @Value("${redsys.transaction-type:0}") String transactionType,
            @Value("${redsys.secret-key:sq7HjrUOBfKmC576ILgskD5srU870gJ7}") String secretKeyBase64,
            @Value("${redsys.merchant-url:http://localhost:8080/redsys/callback}") String merchantUrl,
            @Value("${redsys.url-ok:http://localhost:5173/reserva/exito}") String urlOk,
            @Value("${redsys.url-ko:http://localhost:5173/reserva/error}") String urlKo) {
        this.url = url;
        this.merchantCode = merchantCode;
        this.terminal = terminal;
        this.currency = currency;
        this.transactionType = transactionType;
        this.secretKey = Base64.getDecoder().decode(secretKeyBase64);
        this.merchantUrl = merchantUrl;
        this.urlOk = urlOk;
        this.urlKo = urlKo;
    }

    /** Resultado decodificado de una notificación de Redsys. */
    public record CallbackResult(String order, String responseCode, boolean authorized) { }

    /** Construye Ds_MerchantParameters + Ds_Signature para enviar al TPV. */
    public RedsysForm buildPaymentForm(String order, int amountCents, String paymentMethod) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("DS_MERCHANT_AMOUNT", String.valueOf(amountCents));
        params.put("DS_MERCHANT_ORDER", order);
        params.put("DS_MERCHANT_MERCHANTCODE", merchantCode);
        params.put("DS_MERCHANT_CURRENCY", currency);
        params.put("DS_MERCHANT_TRANSACTIONTYPE", transactionType);
        params.put("DS_MERCHANT_TERMINAL", terminal);
        params.put("DS_MERCHANT_MERCHANTURL", merchantUrl);
        params.put("DS_MERCHANT_URLOK", urlOk);
        params.put("DS_MERCHANT_URLKO", urlKo);
        params.put("DS_MERCHANT_PRODUCTDESCRIPTION", "Reserva");
        // "z" = Bizum, "C" = tarjeta.
        String methods = "bizum".equalsIgnoreCase(paymentMethod) ? "z" : "C";
        params.put("DS_MERCHANT_PAYMETHODS", methods);

        String merchantParameters = encodeParameters(params);
        String signature = sign(merchantParameters, order, false);
        return new RedsysForm(url, SIGNATURE_VERSION, merchantParameters, signature, methods);
    }

    /** Verifica la firma de una notificación y extrae order + código de respuesta. */
    public CallbackResult verifyCallback(String merchantParameters, String receivedSignature) {
        try {
            byte[] decoded = base64TolerantDecode(merchantParameters);
            JsonNode node = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            String order = node.get("Ds_Order").asText();
            String responseCode = node.has("Ds_Response") ? node.get("Ds_Response").asText() : "";

            String expected = sign(merchantParameters, order, true);
            if (!constantTimeEquals(normalize(expected), normalize(receivedSignature))) {
                throw new SecurityException("Firma Redsys inválida");
            }
            boolean authorized = isAuthorized(responseCode);
            return new CallbackResult(order, responseCode, authorized);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Notificación Redsys ilegible", e);
        }
    }

    // ---- criptografía ----

    private String encodeParameters(Map<String, String> params) {
        try {
            String json = objectMapper.writeValueAsString(params);
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String sign(String merchantParameters, String order, boolean urlSafe) {
        try {
            byte[] derivedKey = encrypt3DES(order, secretKey);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(derivedKey, "HmacSHA256"));
            byte[] result = mac.doFinal(merchantParameters.getBytes(StandardCharsets.UTF_8));
            return urlSafe
                    ? Base64.getUrlEncoder().encodeToString(result)
                    : Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("Error firmando Redsys", e);
        }
    }

    private byte[] encrypt3DES(String order, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DESede"), new IvParameterSpec(new byte[8]));
        byte[] orderBytes = order.getBytes(StandardCharsets.UTF_8);
        int blockLen = ((orderBytes.length + 7) / 8) * 8;
        byte[] padded = Arrays.copyOf(orderBytes, blockLen);
        return cipher.doFinal(padded);
    }

    private static boolean isAuthorized(String responseCode) {
        try {
            int code = Integer.parseInt(responseCode);
            return code >= 0 && code <= 99;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static byte[] base64TolerantDecode(String data) {
        String s = data.replace('-', '+').replace('_', '/');
        switch (s.length() % 4) {
            case 2 -> s += "==";
            case 3 -> s += "=";
            default -> { }
        }
        return Base64.getDecoder().decode(s);
    }

    private static String normalize(String base64) {
        return base64.replace('+', '-').replace('/', '_').replaceAll("=+$", "");
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}

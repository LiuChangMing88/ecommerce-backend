package com.github.liuchangming88.ecommerce_backend.payment.vnpay;


import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
class VNPaySignerTest {

    @Test
    void computeSignature_orderIndependent_and_ignoresExistingHashFields() {
        Map<String,String> a = new HashMap<>();
        a.put("vnp_TxnRef","REF1");
        a.put("vnp_Amount","1000");
        a.put("vnp_SecureHash","shouldBeIgnored");
        a.put("vnp_SecureHashType","HmacSHA512");
        Map<String,String> b = new LinkedHashMap<>();
        b.put("vnp_Amount","1000");
        b.put("vnp_TxnRef","REF1");
        String s1 = VNPaySigner.computeSignature(a,"secret");
        String s2 = VNPaySigner.computeSignature(b,"secret");
        assertThat(s1).isEqualTo(s2);
    }

    @Test
    void verify_trueWithCorrectSignature_falseWhenTamperedOrMissing() {
        Map<String,String> p = new HashMap<>();
        p.put("vnp_TxnRef","REF2");
        p.put("vnp_Amount","500");
        String sig = VNPaySigner.computeSignature(p,"secret");
        Map<String,String> withSig = new HashMap<>(p);
        withSig.put("vnp_SecureHash", sig.toUpperCase()); // case-insensitive
        assertThat(VNPaySigner.verify(withSig,"secret")).isTrue();

        Map<String,String> tampered = new HashMap<>(withSig);
        tampered.put("vnp_Amount","501");
        assertThat(VNPaySigner.verify(tampered,"secret")).isFalse();

        Map<String,String> missing = new HashMap<>(p);
        assertThat(VNPaySigner.verify(missing,"secret")).isFalse();
    }

    @Test
    void buildQueryStringWithHash_encodesAndAddsHashAndType() {
        Map<String,String> p = new HashMap<>();
        p.put("vnp_TxnRef","R 3");
        p.put("vnp_Desc","A B+C"); // test space and plus
        p.put("empty","");
        p.put("nullVal", null);
        String qs = VNPaySigner.buildQueryStringWithHash(p,"secret");
        assertThat(qs).contains("vnp_TxnRef=R%203");
        assertThat(qs).contains("vnp_Desc=A%20B%2BC");
        assertThat(qs).doesNotContain("empty=");
        assertThat(qs).doesNotContain("nullVal=");
        assertThat(qs).contains("vnp_SecureHash=");
        assertThat(qs).contains("vnp_SecureHashType=HmacSHA512");
    }
}

package com.github.liuchangming88.ecommerce_backend.api.controller.payments;

import com.github.liuchangming88.ecommerce_backend.model.LocalOrder;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalOrderRepository;
import com.github.liuchangming88.ecommerce_backend.payment.PaymentRepository;
import com.github.liuchangming88.ecommerce_backend.payment.dto.IpnResponse;
import com.github.liuchangming88.ecommerce_backend.payment.dto.PayResponse;
import com.github.liuchangming88.ecommerce_backend.payment.vnpay.VNPayIpnService;
import com.github.liuchangming88.ecommerce_backend.payment.vnpay.VNPayPaymentService;
import com.github.liuchangming88.ecommerce_backend.payment.vnpay.VNPayProperties;
import com.github.liuchangming88.ecommerce_backend.payment.vnpay.VNPaySigner;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments/vnpay")
public class VNPayController {

    private final VNPayPaymentService paymentService;
    private final VNPayIpnService ipnService;
    private final VNPayProperties props;
    private final LocalOrderRepository localOrderRepository;

    public VNPayController(VNPayPaymentService paymentService,
                           VNPayIpnService ipnService,
                           VNPayProperties props,
                           LocalOrderRepository localOrderRepository) {
        this.paymentService = paymentService;
        this.ipnService = ipnService;
        this.props = props;
        this.localOrderRepository = localOrderRepository;
    }

    @GetMapping("/pay/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PayResponse> pay(@PathVariable Long orderId,
                           @AuthenticationPrincipal LocalUser localUser,
                           HttpServletRequest request) {
        LocalOrder order = localOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        // To extract the client's IP, as VNPay requires the client's IP address to be present
        String ip = extractClientIp(request);
        PayResponse payResponse = paymentService.initiateOrReusePayment(order, ip, localUser.getId());
        return new ResponseEntity<>(payResponse, HttpStatus.OK);
    }

    @GetMapping("/ipn")
    @Transactional
    public ResponseEntity<IpnResponse> ipn(@RequestParam Map<String,String> allParams) {
        return new ResponseEntity<>(ipnService.processIpnRequestAndReturnToVNPayServer(allParams), HttpStatus.OK);
    }

    @GetMapping("/return")
    public ResponseEntity<Map<String,String>> returnPage(@RequestParam Map<String,String> allParams) {
        boolean sigOk = VNPaySigner.verify(allParams, props.getHashSecret());
        return new ResponseEntity<>(Map.of(
                "signatureValid", Boolean.toString(sigOk),
                "vnp_ResponseCode", allParams.getOrDefault("vnp_ResponseCode",""),
                "vnp_TxnRef", allParams.getOrDefault("vnp_TxnRef","")
        ), HttpStatus.OK);
    }

    // Extract the IP address of the request
    private String extractClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
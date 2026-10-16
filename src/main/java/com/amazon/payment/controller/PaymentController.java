package com.amazon.payment.controller;



import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.amazon.payment.entity.PaymentEntity;
import com.amazon.payment.repository.PaymentRepository;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<PaymentEntity> getPaymentByOrderId(
            @PathVariable Long orderId) {

        Optional<PaymentEntity> payment =
                paymentRepository.findByOrderId(orderId);

        return payment
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
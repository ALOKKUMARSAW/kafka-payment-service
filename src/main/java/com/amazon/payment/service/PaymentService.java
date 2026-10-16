package com.amazon.payment.service;




import java.math.BigDecimal;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amazon.payment.entity.PaymentEntity;
import com.amazon.payment.event.OrderCreatedEvent;
import com.amazon.payment.event.PaymentEvent;
import com.amazon.payment.kafka.KafkaPaymentProducer;
import com.amazon.payment.repository.PaymentRepository;


@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaPaymentProducer kafkaPaymentProducer;

    public PaymentService(
            PaymentRepository paymentRepository,
            KafkaPaymentProducer kafkaPaymentProducer) {

        this.paymentRepository = paymentRepository;
        this.kafkaPaymentProducer = kafkaPaymentProducer;
    }

    @Transactional
    public void processPayment(OrderCreatedEvent event) {

        // 1. Idempotency check
        if (paymentRepository.findByOrderId(event.getOrderId())
                .isPresent()) {
            System.out.println(
                    "Payment already processed for order: "
                    + event.getOrderId());
            return;
        }

        // 2. Prepare payment
        PaymentEntity payment = new PaymentEntity();

        payment.setOrderId(event.getOrderId());
        payment.setCustomerId(event.getCustomerId());
        payment.setAmount(event.getAmount());
        payment.setPaymentMethod("UPI");
        payment.setEventId(
                event.getEventId() != null
                        ? event.getEventId()
                        : UUID.randomUUID().toString());

        // 3. Simulate payment result
        boolean success =
                event.getCustomerId() != null
                && event.getCustomerId() != 102L
                && event.getAmount() != null
                && event.getAmount().compareTo(BigDecimal.ZERO) > 0;

        if (success) {
            payment.setPaymentStatus("SUCCESS");
            payment.setReason(null);
        } else {
            payment.setPaymentStatus("FAILED");
            payment.setReason("PAYMENT_DECLINED");
        }

        // 4. Save payment
        PaymentEntity savedPayment =
                paymentRepository.saveAndFlush(payment);

        // 5. Build payment outcome event
        PaymentEvent paymentEvent = new PaymentEvent();

        paymentEvent.setEventType(
                success ? "PAYMENT_SUCCESS" : "PAYMENT_FAILED");

        paymentEvent.setOrderId(savedPayment.getOrderId());
        paymentEvent.setCustomerId(savedPayment.getCustomerId());
        paymentEvent.setAmount(savedPayment.getAmount());
        paymentEvent.setPaymentId(
                "TXN-" + savedPayment.getPaymentId());
        paymentEvent.setPaymentMethod(
                savedPayment.getPaymentMethod());
        paymentEvent.setPaymentStatus(
                savedPayment.getPaymentStatus());
        paymentEvent.setReason(savedPayment.getReason());

        // 6. Publish result
        kafkaPaymentProducer.sendPaymentEvent(paymentEvent);
    }
}
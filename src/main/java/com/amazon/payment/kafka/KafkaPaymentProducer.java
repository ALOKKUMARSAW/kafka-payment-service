package com.amazon.payment.kafka;




import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.amazon.payment.event.PaymentEvent;


@Service
public class KafkaPaymentProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public KafkaPaymentProducer(
            KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendPaymentEvent(PaymentEvent event) {

        String topic;

        if ("PAYMENT_SUCCESS".equals(event.getEventType())) {
            topic = "payment-success";
        } else {
            topic = "payment-failed";
        }

        kafkaTemplate.send(
                topic,
                event.getOrderId().toString(),
                event
        );
    }
}
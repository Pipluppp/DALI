package com.dali.ecommerce.controller;

import com.dali.ecommerce.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public WebhookController(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/maya")
    public ResponseEntity<Void> handleMayaWebhook(@RequestBody String payload) {
        System.out.println("Received Maya Webhook: " + payload);

        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String event = rootNode.path("event").asText();
            JsonNode dataNode = rootNode.path("data");

            // The requestReferenceNumber is what we set as our order ID.
            String requestReferenceNumber = dataNode.path("requestReferenceNumber").asText();
            // The "id" is the unique checkout ID from Maya.
            String mayaCheckoutId = dataNode.path("id").asText();


            if (requestReferenceNumber != null && !requestReferenceNumber.isEmpty()) {
                Integer orderId = Integer.parseInt(requestReferenceNumber);

                if ("CHECKOUT_SUCCESS".equals(event)) {
                    orderService.processSuccessfulPayment(orderId, mayaCheckoutId);
                } else if ("CHECKOUT_FAILURE".equals(event)) {
                    orderService.failOrderPayment(orderId);
                }
                // We can also handle other events like PAYMENT_SUCCESS, PAYMENT_FAILED, etc.
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error processing Maya webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
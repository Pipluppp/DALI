package com.dali.ecommerce.payment.maya;

import com.dali.ecommerce.order.Order;
import com.dali.ecommerce.order.OrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MayaService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final String mayaCheckoutUrl;
    private final String mayaPublicApiKey;
    private final String appBaseUrl;

    public MayaService(ObjectMapper objectMapper,
                       @Value("${maya.api.url.checkout}") String mayaCheckoutUrl,
                       @Value("${maya.api.key.public}") String mayaPublicApiKey,
                       @Value("${app.base.url}") String appBaseUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.mayaCheckoutUrl = mayaCheckoutUrl;
        this.mayaPublicApiKey = mayaPublicApiKey;
        this.appBaseUrl = appBaseUrl;
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public CheckoutResponse createCheckout(Order order) throws Exception {
        CheckoutRequest checkoutRequest = buildCheckoutRequest(order);

        String requestBody = objectMapper.writeValueAsString(checkoutRequest);
        String encodedKey = Base64.getEncoder().encodeToString((mayaPublicApiKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mayaCheckoutUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + encodedKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), CheckoutResponse.class);
        } else {
            System.err.println("Maya API Error: " + response.body());
            throw new RuntimeException("Failed to create Maya checkout. Status code: " + response.statusCode() + " Body: " + response.body());
        }
    }

    private CheckoutRequest buildCheckoutRequest(Order order) {
        // Create Total Amount
        TotalAmount totalAmount = new TotalAmount();
        totalAmount.setValue(roundToTwoDecimals(order.getTotalPrice()));
        totalAmount.setCurrency("PHP");

        // Create Buyer
        Buyer buyer = new Buyer();
        buyer.setFirstName(order.getAccount().getFirstName());
        buyer.setLastName(order.getAccount().getLastName());
        Contact contact = new Contact();
        contact.setPhone(order.getAccount().getPhoneNumber());
        contact.setEmail(order.getAccount().getEmail());
        buyer.setContact(contact);

        // Create Items
        List<Item> items = order.getOrderItems().stream()
                .map(this::convertOrderItemToMayaItem)
                .collect(Collectors.toList());

        // Create Redirect URLs
        RedirectUrl redirectUrl = new RedirectUrl();
        redirectUrl.setSuccess(appBaseUrl + "/payment/callback/success?orderId=" + order.getOrderId());
        redirectUrl.setFailure(appBaseUrl + "/payment/callback/failure?orderId=" + order.getOrderId());
        redirectUrl.setCancel(appBaseUrl + "/payment/callback/cancel?orderId=" + order.getOrderId());

        // Assemble Checkout Request
        CheckoutRequest request = new CheckoutRequest();
        request.setTotalAmount(totalAmount);
        request.setBuyer(buyer);
        request.setItems(items);
        request.setRedirectUrl(redirectUrl);
        request.setRequestReferenceNumber(order.getOrderId().toString());

        return request;
    }

    private Item convertOrderItemToMayaItem(OrderItem orderItem) {
        Item item = new Item();
        item.setName(orderItem.getProduct().getName());
        item.setQuantity(String.valueOf(orderItem.getQuantity()));
        item.setCode(orderItem.getProduct().getId().toString());
        item.setDescription(orderItem.getProduct().getCategory());

        TotalAmount itemTotal = new TotalAmount();
        itemTotal.setValue(roundToTwoDecimals(orderItem.getSubtotal()));
        itemTotal.setCurrency("PHP");
        item.setTotalAmount(itemTotal);

        return item;
    }
}
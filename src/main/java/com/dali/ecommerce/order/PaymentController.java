package com.dali.ecommerce.order;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payment/callback")
public class PaymentController {

    private final OrderService orderService;

    public PaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/success")
    public String handleSuccess(@RequestParam("orderId") Integer orderId,
                                SessionStatus sessionStatus, Model model, RedirectAttributes redirectAttributes) {

        System.out.println("Payment callback success for Order ID: " + orderId);

        try {
            orderService.confirmPaymentOnSuccessRedirect(orderId);
            System.out.println("Successfully processed payment confirmation for Order ID: " + orderId);

            // Clear the checkout session attributes.
            sessionStatus.setComplete();

            // Prepare the model for the success page.
            model.addAttribute("orderId", orderId);
            return "payment-success";

        } catch (Exception e) {
            // If something goes wrong (e.g., out of stock), redirect with an error.
            System.err.println("Error processing payment success for order " + orderId + ": " + e.getMessage());
            e.printStackTrace();

            // Fail the payment in the DB to be safe
            orderService.failOrderPayment(orderId);
            sessionStatus.setComplete();

            redirectAttributes.addFlashAttribute("errorMessage", "Payment was confirmed, but we couldn't finalize your order: " + e.getMessage());
            return "redirect:/checkout/payment";
        }
    }

    @GetMapping("/failure")
    public String handleFailure(@RequestParam("orderId") Integer orderId, SessionStatus sessionStatus, RedirectAttributes redirectAttributes) {
        System.out.println("Payment callback failure for Order ID: " + orderId);
        orderService.failOrderPayment(orderId);
        sessionStatus.setComplete();
        redirectAttributes.addFlashAttribute("errorMessage", "Your payment failed. Please try again or select a different payment method.");
        return "redirect:/checkout/payment";
    }

    @GetMapping("/cancel")
    public String handleCancel(@RequestParam("orderId") Integer orderId, SessionStatus sessionStatus, RedirectAttributes redirectAttributes) {
        System.out.println("Payment callback cancelled for Order ID: " + orderId);
        orderService.failOrderPayment(orderId);
        sessionStatus.setComplete();
        redirectAttributes.addFlashAttribute("errorMessage", "Payment was cancelled. You can try again anytime.");
        return "redirect:/checkout/payment";
    }
}
package com.dali.ecommerce.order;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/order/{id}")
    public String orderDetail(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        Order order = orderService.findOrderById(id);

        // Security check: ensure the logged-in user owns this order
        if (authentication == null || !order.getAccount().getEmail().equals(authentication.getName())) {
            // Redirect to profile or an access-denied page if the user is not the owner
            return "redirect:/profile?error=access_denied";
        }

        model.addAttribute("order", order);
        return "order-detail";
    }

    @PostMapping("/order/{id}/cancel")
    public String cancelOrder(@PathVariable("id") Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " has been successfully cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not cancel order: " + e.getMessage());
        }
        return "redirect:/order/" + id;
    }
}
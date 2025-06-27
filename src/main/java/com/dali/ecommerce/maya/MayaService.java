package com.dali.ecommerce.maya;

import com.dali.ecommerce.maya.CheckoutResponse;
import com.dali.ecommerce.model.Order;

public interface MayaService {
    CheckoutResponse createCheckout(Order order) throws Exception;
}
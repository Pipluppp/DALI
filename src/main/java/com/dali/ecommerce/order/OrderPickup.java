package com.dali.ecommerce.order;

import com.dali.ecommerce.store.Store;
import jakarta.persistence.*;

@Entity
@Table(name = "order_pickups")
public class OrderPickup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_pickup_id")
    private Integer orderPickupId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // Getters and Setters
    public Integer getOrderPickupId() {
        return orderPickupId;
    }

    public void setOrderPickupId(Integer orderPickupId) {
        this.orderPickupId = orderPickupId;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }
}
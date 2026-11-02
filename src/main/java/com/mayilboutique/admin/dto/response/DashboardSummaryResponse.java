package com.mayilboutique.admin.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.PaymentStatus;

public class DashboardSummaryResponse {

    private long orders;
    private BigDecimal sales;
    private long customers;
    private long products;
    private long lowStock;
    private List<OrderSummaryResponse> recentOrders;

    public DashboardSummaryResponse() {
    }

    public DashboardSummaryResponse(long orders, BigDecimal sales, long customers, long products, long lowStock,
                                    List<OrderSummaryResponse> recentOrders) {
        this.orders = orders;
        this.sales = sales;
        this.customers = customers;
        this.products = products;
        this.lowStock = lowStock;
        this.recentOrders = recentOrders;
    }

    public long getOrders() {
        return orders;
    }

    public BigDecimal getSales() {
        return sales;
    }

    public long getCustomers() {
        return customers;
    }

    public long getProducts() {
        return products;
    }

    public long getLowStock() {
        return lowStock;
    }

    public List<OrderSummaryResponse> getRecentOrders() {
        return recentOrders;
    }

    public void setOrders(long orders) {
        this.orders = orders;
    }

    public void setSales(BigDecimal sales) {
        this.sales = sales;
    }

    public void setCustomers(long customers) {
        this.customers = customers;
    }

    public void setProducts(long products) {
        this.products = products;
    }

    public void setLowStock(long lowStock) {
        this.lowStock = lowStock;
    }

    public void setRecentOrders(List<OrderSummaryResponse> recentOrders) {
        this.recentOrders = recentOrders;
    }
}

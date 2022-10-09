package model.item;

public class CustomerOrder {
    public String orderId;
    public String customerId;
    public String productId;
    public String orderDateTime;
    public String paymentMethod;
    public int isRefund;
    public double orderTotal;
    public String sex;
    public int age;
    public int birthMonth;
    public int birthDay;
    public int openDays;
    public int totalPoints;
    public String province;
    public String membership;

    public CustomerOrder(Order order, Customer customer) {
        this.orderId = order.orderId;
        this.customerId = order.customerId;
        this.productId = order.productId;
        this.orderDateTime = order.orderDateTime;
        this.paymentMethod = order.paymentMethod;
        this.isRefund = order.isRefund;
        this.orderTotal = order.orderTotal;
        this.sex = customer.sex;
        this.age = customer.age;
        this.birthMonth = customer.birthMonth;
        this.birthDay = customer.birthDay;
        this.openDays = customer.openDays;
        this.totalPoints = customer.totalPoints;
        this.province = customer.province;
        this.membership = customer.membership;
    }
}

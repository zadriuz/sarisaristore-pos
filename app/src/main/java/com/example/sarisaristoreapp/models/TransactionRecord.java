package com.example.sarisaristoreapp.models;

public class TransactionRecord {
    private String transactionId;
    private String purseType;
    private String transactionDate;
    private double totalAmount;
    private String paymentMethod;
    private double cashGiven;
    private double changeAmount;
    private String deviceInfo;
    private double totalProfit; // <-- THIS FIXES YOUR RED UNDERLINE

    // Empty constructor required for Firebase
    public TransactionRecord() {}

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getPurseType() { return purseType; }
    public void setPurseType(String purseType) { this.purseType = purseType; }

    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getCashGiven() { return cashGiven; }
    public void setCashGiven(double cashGiven) { this.cashGiven = cashGiven; }

    public double getChangeAmount() { return changeAmount; }
    public void setChangeAmount(double changeAmount) { this.changeAmount = changeAmount; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public double getTotalProfit() { return totalProfit; }
    public void setTotalProfit(double totalProfit) { this.totalProfit = totalProfit; }
}
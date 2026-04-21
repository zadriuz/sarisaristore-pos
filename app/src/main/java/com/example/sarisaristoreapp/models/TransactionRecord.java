package com.example.sarisaristoreapp.models;

public class TransactionRecord {
    private String transactionId;
    private String purseType;
    private double totalAmount;
    private String transactionDate;

    // NEW TRACKING VARIABLES
    private String paymentMethod;
    private double cashGiven;
    private double changeAmount;
    private String deviceInfo;
    public TransactionRecord() {
        // Firebase Empty Constructor
    }

    public TransactionRecord(String transactionId, String purseType, double totalAmount, String transactionDate, String paymentMethod, double cashGiven, double changeAmount, String deviceInfo) {
        this.transactionId = transactionId;
        this.purseType = purseType;
        this.totalAmount = totalAmount;
        this.transactionDate = transactionDate;
        this.paymentMethod = paymentMethod;
        this.cashGiven = cashGiven;
        this.changeAmount = changeAmount;
        this.deviceInfo = deviceInfo;
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getPurseType() { return purseType; }
    public void setPurseType(String purseType) { this.purseType = purseType; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public double getCashGiven() { return cashGiven; }
    public void setCashGiven(double cashGiven) { this.cashGiven = cashGiven; }
    public double getChangeAmount() { return changeAmount; }
    public void setChangeAmount(double changeAmount) { this.changeAmount = changeAmount; }
    public String getDeviceInfo(){ return deviceInfo; }
    public void setDeviceInfo(String deviceInfo){ this.deviceInfo = deviceInfo; }
}
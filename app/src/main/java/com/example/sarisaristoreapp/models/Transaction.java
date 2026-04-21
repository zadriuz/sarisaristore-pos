package com.example.sarisaristoreapp.models;

public class Transaction {
    private int transactionID;
    private long timeStamp;
    private int userID;
    private String purseType;
    private double totalAmountDue;
    private double cashTendered;

    private double changeGiven;

    public Transaction() {
    }

    public double getChangeGiven() {
        return changeGiven;
    }

    public void setChangeGiven(double changeGiven) {
        this.changeGiven = changeGiven;
    }

    public double getCashTendered() {
        return cashTendered;
    }

    public void setCashTendered(double cashTendered) {
        this.cashTendered = cashTendered;
    }

    public double getTotalAmountDue() {
        return totalAmountDue;
    }

    public void setTotalAmountDue(double totalAmountDue) {
        this.totalAmountDue = totalAmountDue;
    }

    public String getPurseType() {
        return purseType;
    }

    public void setPurseType(String purseType) {
        this.purseType = purseType;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(int transactionID) {
        this.transactionID = transactionID;
    }

    public Transaction(double changeGiven, double cashTendered, double totalAmountDue, String purseType, int userID, long timeStamp, int transactionID) {
        this.changeGiven = changeGiven;
        this.cashTendered = cashTendered;
        this.totalAmountDue = totalAmountDue;
        this.purseType = purseType;
        this.userID = userID;
        this.timeStamp = timeStamp;
        this.transactionID = transactionID;
    }
}

package com.example.sarisaristoreapp.models;

public class Product {
    private int productID;
    private int currentQuantity;
    private double costPrice;
    private double sellingPrice;
    private String barcode;
    private String productName;
    private String category;

    public Product(){

    }

    public Product(int productID, int currentQuantity, double costPrice, double sellingPrice, String barcode, String productName, String category){
        this.productID = productID;
        this.currentQuantity = currentQuantity;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.barcode = barcode;
        this.productName = productName;
        this.category = category;
    }

    public String getProductName(){
        return productName;
    }

    public void setProductName(String productName){
        this.productName = productName;
    }

    public String getBarcode(){
        return barcode;
    }

    public void setBarcode(String barcode){
        this.barcode = barcode;
    }

    public double getSellingPrice(){
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice){
        this.sellingPrice = sellingPrice;
    }

    public double getCostPrice(){
        return costPrice;
    }

    public void setCostPrice(double costPrice){
        this.costPrice = costPrice;
    }

    public int getCurrentQuantity(){
        return currentQuantity;
    }

    public void setCurrentQuantity(int currentQuantity){
        this.currentQuantity = currentQuantity;
    }

    public int getProductID(){
        return productID;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public void setProductID(int productID){
        this.productID = productID;
    }
}

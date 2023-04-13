package com.example.aiinvoice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceItem {

    private String referenceNumber;
    private String description;
    private double quantity;
    private double price;

    public InvoiceItem(String referenceNumber, String description, double quantity, double price) {
        this.referenceNumber = referenceNumber;
        this.description = description;
        this.quantity = quantity;
        this.price = price;
    }

    @Override
    public String toString() {
        return "InvoiceItem{" +
                "description='" + description + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}


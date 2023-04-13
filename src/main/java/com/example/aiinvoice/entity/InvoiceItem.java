package com.example.aiinvoice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem {

    private String description;
    private double quantity;
    private double price;

    @Override
    public String toString() {
        return "InvoiceItem{" +
                "description='" + description + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}


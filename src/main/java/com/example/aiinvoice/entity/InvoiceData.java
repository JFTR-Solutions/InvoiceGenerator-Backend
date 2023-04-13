package com.example.aiinvoice.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class InvoiceData {

    List<InvoiceItem> invoiceItems;
    double subTotal;

    public InvoiceData() {
       invoiceItems = new ArrayList<>();
    }

    public void addInvoiceItem(String referenceNumber, String description, double quantity, double price) {
        if (invoiceItems == null) {
            invoiceItems = new ArrayList<>();
        }

        // Check if the item is already present in the list
        for (InvoiceItem item : invoiceItems) {
            if (item.getDescription().equals(description) && item.getQuantity() == quantity && item.getPrice() == price) {
                return;
            }
        }

        // If the item is not already present in the list, add it
        invoiceItems.add(new InvoiceItem(referenceNumber, description, quantity, price));
    }

}

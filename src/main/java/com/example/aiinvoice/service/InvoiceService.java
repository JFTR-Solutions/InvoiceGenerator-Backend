package com.example.aiinvoice.service;


import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzedDocument;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentField;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentFieldType;
import com.example.aiinvoice.entity.InvoiceData;
import com.example.aiinvoice.entity.InvoiceItem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InvoiceService {


    public InvoiceData mergeInvoicesData(InvoiceData invoiceData1, InvoiceData invoiceData2) {
        InvoiceData mergedInvoiceData = new InvoiceData();

        // Add invoice items from both invoices
        mergedInvoiceData.getInvoiceItems().addAll(invoiceData1.getInvoiceItems());
        mergedInvoiceData.getInvoiceItems().addAll(invoiceData2.getInvoiceItems());

        // Set your reference for each invoice item

        double combinedSubtotal = 0;
        for (InvoiceItem item : mergedInvoiceData.getInvoiceItems()) {
            combinedSubtotal += item.getPrice() * item.getQuantity();
        }
        mergedInvoiceData.setSubTotal(Math.round(combinedSubtotal * 100.0) / 100.0);

        return mergedInvoiceData;
    }

    public InvoiceData extractInvoiceData(AnalyzeResult analyzeResult, String referenceNumber) {
        InvoiceData invoiceData = new InvoiceData();

        List<AnalyzedDocument> analyzedDocuments = analyzeResult.getDocuments();
        if (analyzedDocuments == null) {
            return invoiceData;
        }

        for (AnalyzedDocument analyzedDocument : analyzedDocuments) {
            Map<String, DocumentField> fields = analyzedDocument.getFields();

            // Extracting items
            DocumentField invoiceItemsField = fields.get("Items");
            if (invoiceItemsField != null && DocumentFieldType.LIST == invoiceItemsField.getType()) {
                List<DocumentField> itemList = invoiceItemsField.getValueAsList();
                for (DocumentField item : itemList) {
                    if (DocumentFieldType.MAP == item.getType()) {
                        Map<String, DocumentField> itemData = item.getValueAsMap();

                        String description = "";
                        double price = 0.0;
                        double quantity = 0.0;

                        // Extracting description
                        DocumentField descriptionField = itemData.get("Description");
                        if (descriptionField != null) {
                            description = descriptionField.getContent();
                        }

                        // Check if the description contains "Discount"
                        description = description.toLowerCase();
                        if (!description.contains("discount") && ((!description.contains("tracking"))) && (!description.contains("freight"))) {
                            // Extracting quantity
                            DocumentField quantityField = itemData.get("Quantity");
                            if (quantityField != null) {
                                String stringWithNumbersAndFirstComma = quantityField.getContent().replaceFirst("(?<=\\d),(?=\\d)", ".");
                                String stringWithNumbersAndDotOnly = stringWithNumbersAndFirstComma.replaceAll("[^\\d.]", "");
                                quantity = Double.parseDouble(stringWithNumbersAndDotOnly);
                            }

                            // Extracting price
                            DocumentField priceField = itemData.get("UnitPrice");
                            if (priceField != null) {
                                String stringWithNumbersAndCommas = priceField.getContent().replaceAll("[^\\d,\\.]", "");
                                String stringWithSingleDecimalPoint = stringWithNumbersAndCommas.replaceAll("(?<=[\\d,])\\.(?=\\d{3})", "").replaceFirst("(?<=\\d),(?=\\d)", ".");
                                price = Double.parseDouble(stringWithSingleDecimalPoint);
                            }

                            invoiceData.addInvoiceItem(referenceNumber, description, quantity, price);
                        }
                    }
                }
            }

            // Extracting subtotal
            DocumentField subtotalField = fields.get("SubTotal");
            System.out.println(subtotalField.getContent());
            if (subtotalField != null) {
                String stringWithNumbersAndCommas = subtotalField.getContent().replaceAll("[^\\d,]", "");
                String stringWithSingleDecimalPoint = stringWithNumbersAndCommas.replaceAll("(?<=\\d),(?=\\d{3})", "").replaceFirst(",", ".");
                invoiceData.setSubTotal(Double.parseDouble(stringWithSingleDecimalPoint));
            }
        }

        return invoiceData;
    }
}



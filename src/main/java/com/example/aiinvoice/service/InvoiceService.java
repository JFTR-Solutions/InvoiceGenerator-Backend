package com.example.aiinvoice.service;


import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzedDocument;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentField;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentFieldType;
import com.example.aiinvoice.entity.InvoiceItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceService {


    public List<InvoiceItem> extractInvoiceData(AnalyzeResult analyzeResult, String referenceNumber) {
        List<InvoiceItem> invoiceItemList = new ArrayList<>();

        List<AnalyzedDocument> analyzedDocuments = analyzeResult.getDocuments();
        if (analyzedDocuments == null) {
            return invoiceItemList;
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
                        if (!description.contains("discount") && ((!description.contains("tracking"))) && (!description.contains("freight")) && (!description.contains("administration"))) {
                            // Extracting quantity
                            DocumentField quantityField = itemData.get("Quantity");
                            if (quantityField != null) {
                                String stringWithNumbersAndFirstComma = quantityField.getContent().replaceFirst("(?<=\\d),(?=\\d)", ".");
                                String stringWithNumbersAndDotOnly = stringWithNumbersAndFirstComma.replaceAll("[^\\d.]", "");
                                quantity = Double.parseDouble(stringWithNumbersAndDotOnly);
                            }

                            DocumentField priceField = itemData.get("UnitPrice");
                            if (priceField != null) {
                                String stringWithNumbersAndCommas = priceField.getContent().replaceAll("[^\\d,\\.]", "");
                                String stringWithOnlyDecimalPoint = stringWithNumbersAndCommas.replaceAll(",", ""); // Remove all commas
                                int decimalPointIndex = stringWithOnlyDecimalPoint.lastIndexOf('.'); // Find the last decimal point

                                // Remove all other decimal points except the last one
                                if (decimalPointIndex > 0) {
                                    stringWithOnlyDecimalPoint = stringWithOnlyDecimalPoint.substring(0, decimalPointIndex).replaceAll("\\.", "") + stringWithOnlyDecimalPoint.substring(decimalPointIndex);
                                }

                                price = Double.parseDouble(stringWithOnlyDecimalPoint);
                            }

                            invoiceItemList.add(new InvoiceItem(referenceNumber, description, quantity, price));
                        }
                    }
                }
            }

        }

        return invoiceItemList;
    }
}



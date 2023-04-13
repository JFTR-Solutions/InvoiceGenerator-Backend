package com.example.aiinvoice.api;

import com.azure.ai.formrecognizer.documentanalysis.models.*;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.example.aiinvoice.entity.InvoiceData;
import com.example.aiinvoice.entity.InvoiceItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class FormRecognizer {
    //use your `key` and `endpoint` environment variables
    private static final String key = System.getenv("FR_KEY");
    private static final String endpoint = System.getenv("FR_ENDPOINT");

    private DocumentAnalysisClient client;

    public FormRecognizer() {
        // create your `DocumentAnalysisClient` instance and `AzureKeyCredential` variable
        this.client = new DocumentAnalysisClientBuilder()
                .credential(new AzureKeyCredential(key))
                .endpoint(endpoint)
                .buildClient();
    }

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceData> extractInvoices(@RequestParam("files") List<MultipartFile> files) {
        String modelId = "prebuilt-invoice";
        InvoiceData combinedInvoiceData = new InvoiceData();

        for (MultipartFile file : files) {
            try {
                byte[] fileBytes = file.getBytes();
                // Extract reference number from file name
                String fileName = file.getOriginalFilename();
                Pattern pattern = Pattern.compile("(\\d{3})-(\\w)-(\\d{6})");
                Matcher matcher = pattern.matcher(fileName);
                String referenceNumber = null;
                if (matcher.find()) {
                    referenceNumber = matcher.group(0);
                }
                // Analyze invoice
                SyncPoller<OperationResult, AnalyzeResult> analyzeInvoicePoller =
                        client.beginAnalyzeDocument(modelId, BinaryData.fromBytes(fileBytes));
                AnalyzeResult analyzeTaxResult = analyzeInvoicePoller.getFinalResult();
                InvoiceData invoiceData = extractInvoiceData(analyzeTaxResult, referenceNumber);
                combinedInvoiceData = mergeInvoicesData(combinedInvoiceData, invoiceData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ResponseEntity.ok(combinedInvoiceData);
    }

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

    @PostMapping("/invoice")
    public ResponseEntity<InvoiceData> extractInvoice(@RequestParam("file") MultipartFile file) {
        String invoiceModelId = "prebuilt-invoice";

        // Extract file name
        String fileName = file.getOriginalFilename();

        try {
            byte[] fileBytes = file.getBytes();
            // Analyze invoice using the invoice model
            SyncPoller<OperationResult, AnalyzeResult> invoicePoller =
                    client.beginAnalyzeDocument(invoiceModelId, BinaryData.fromBytes(fileBytes));
            AnalyzeResult invoiceResult = invoicePoller.getFinalResult();
            InvoiceData invoiceData = extractInvoiceData(invoiceResult, fileName);

            return ResponseEntity.ok(invoiceData);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
            if (subtotalField != null) {
                String stringWithNumbersAndCommas = subtotalField.getContent().replaceAll("[^\\d,]", "");
                String stringWithSingleDecimalPoint = stringWithNumbersAndCommas.replaceAll("(?<=\\d),(?=\\d{3})", "").replaceFirst(",", ".");
                invoiceData.setSubTotal(Double.parseDouble(stringWithSingleDecimalPoint));
            }
        }

        return invoiceData;
    }
}


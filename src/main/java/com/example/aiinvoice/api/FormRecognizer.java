package com.example.aiinvoice.api;

import com.azure.ai.formrecognizer.*;

import com.azure.ai.formrecognizer.documentanalysis.models.*;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.example.aiinvoice.entity.InvoiceData;
import com.example.aiinvoice.entity.InvoiceItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Arrays;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
                SyncPoller<OperationResult, AnalyzeResult> analyzeW2Poller =
                        client.beginAnalyzeDocument(modelId, BinaryData.fromBytes(fileBytes));

                AnalyzeResult analyzeTaxResult = analyzeW2Poller.getFinalResult();
                InvoiceData invoiceData = extractInvoiceData(analyzeTaxResult);
                combinedInvoiceData = mergeInvoiceData(combinedInvoiceData, invoiceData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ResponseEntity.ok(combinedInvoiceData);
    }

    private InvoiceData mergeInvoiceData(InvoiceData invoiceData1, InvoiceData invoiceData2) {
        InvoiceData mergedInvoiceData = new InvoiceData();

        // Add invoice items from both invoices
        mergedInvoiceData.getInvoiceItems().addAll(invoiceData1.getInvoiceItems());
        mergedInvoiceData.getInvoiceItems().addAll(invoiceData2.getInvoiceItems());

        double combinedSubtotal = 0;
        for (InvoiceItem item : mergedInvoiceData.getInvoiceItems()) {
            combinedSubtotal += item.getPrice() * item.getQuantity();
        }
        mergedInvoiceData.setSubTotal(Math.round(combinedSubtotal * 100.0) / 100.0);

        return mergedInvoiceData;
    }

    @PostMapping("/invoice")
    public ResponseEntity<InvoiceData> extractInvoice(@RequestBody byte[] fileBytes) {
        String modelId = "prebuilt-invoice";

        SyncPoller<OperationResult, AnalyzeResult> analyzeW2Poller =
                client.beginAnalyzeDocument(modelId, BinaryData.fromBytes(fileBytes));

        AnalyzeResult analyzeTaxResult = analyzeW2Poller.getFinalResult();
        InvoiceData invoiceData = extractInvoiceData(analyzeTaxResult);
        return ResponseEntity.ok(invoiceData);

    }

    private InvoiceData extractInvoiceData(AnalyzeResult analyzeResult) {
        InvoiceData invoiceData = new InvoiceData();
        Pattern pattern = Pattern.compile("\\b\\d{3}-[A-Z]-\\d{6}\\b");

        List<AnalyzedDocument> analyzedDocuments = analyzeResult.getDocuments();
        if (analyzedDocuments == null) {
            return invoiceData;
        }

        for (AnalyzedDocument analyzedDocument : analyzedDocuments) {
            Map<String, DocumentField> fields = analyzedDocument.getFields();

            //TODO: Extracting your reference
/*            System.out.println("Field keys: " + fields.keySet());
            // Extracting your reference
            DocumentField yourReferenceField = fields.get("Your reference");
            if (yourReferenceField != null) {
                String content = yourReferenceField.getContent();
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    String yourReference = matcher.group();
                    invoiceData.setInvoiceNumber(yourReference);
                }
            }*/

/*            // Extracting customer name
            DocumentField customerNameField = fields.get("CustomerName");
            if (customerNameField != null) {
                String stringWithLettersAndSpacesOnly = customerNameField.getContent().replaceAll("[^\\p{L}\\s]", "");
                invoiceData.setCustomerName(stringWithLettersAndSpacesOnly);
            }*/


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


                            invoiceData.addInvoiceItem(description, quantity, price);
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



/*    public static void main(final String[] args) {

        FormRecognizer formRecognizer = new FormRecognizer();

        // sample document
        String w2FilePath = "path/to/your/w2.png";

        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(Paths.get(w2FilePath));
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
            return;
        }

        ResponseEntity<InvoiceData> response = formRecognizer.extractW2Data(fileBytes);

        if (response.getStatusCode().is2xxSuccessful()) {
            InvoiceData analyzeTaxResult = response.getBody();
            // Process the AnalyzeResult object as before
            // ...
        } else {
            System.err.println("Failed to analyze the document.");
        }
    }*/

package com.example.aiinvoice.api;

import com.azure.ai.formrecognizer.documentanalysis.models.*;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.example.aiinvoice.entity.InvoiceData;
import com.example.aiinvoice.entity.InvoiceItem;
import com.example.aiinvoice.service.InvoiceExportService;
import com.example.aiinvoice.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@CrossOrigin
@RestController
public class FormRecognizer {

    @Autowired
    InvoiceService invoiceService;
    InvoiceExportService invoiceExportService;

    public FormRecognizer(InvoiceService invoiceService, InvoiceExportService invoiceExportService) {
        this.invoiceService = invoiceService;
        this.invoiceExportService =invoiceExportService;
    }

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
    public ResponseEntity<InvoiceData> extractInvoices(@RequestParam("files") List<MultipartFile> files) throws IOException {
        String modelId = "prebuilt-invoice";
        InvoiceData combinedInvoiceData = new InvoiceData();

        for (MultipartFile file : files) {
            try {
                byte[] fileBytes = file.getBytes();
                // Extract reference number from file name
                String fileName = file.getOriginalFilename();
                assert fileName != null;
                int startIndex = fileName.indexOf("[PONR-") + 6;
                int endIndex = fileName.indexOf("]",startIndex);
                String referenceNumber = fileName.substring(startIndex, endIndex);

                // Analyze invoice
                SyncPoller<OperationResult, AnalyzeResult> analyzeInvoicePoller =
                        client.beginAnalyzeDocument(modelId, BinaryData.fromBytes(fileBytes));
                AnalyzeResult analyzeTaxResult = analyzeInvoicePoller.getFinalResult();
                InvoiceData invoiceData = invoiceService.extractInvoiceData(analyzeTaxResult, referenceNumber);
                combinedInvoiceData = invoiceService.mergeInvoicesData(combinedInvoiceData, invoiceData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        invoiceExportService.createInvoiceExcel(combinedInvoiceData);
        return ResponseEntity.ok(combinedInvoiceData);
    }



    @PostMapping("/invoice")
    public ResponseEntity<InvoiceData> extractInvoice(@RequestParam("file") MultipartFile file) {
        String invoiceModelId = "prebuilt-invoice";

        // Extract file name
        String fileName = file.getOriginalFilename();
        assert fileName != null;
        int startIndex = fileName.indexOf("[PONR-") + 6;
        int endIndex = fileName.indexOf("]",startIndex);
        String referenceNumber = fileName.substring(startIndex, endIndex);


        try {
            byte[] fileBytes = file.getBytes();
            // Analyze invoice using the invoice model
            SyncPoller<OperationResult, AnalyzeResult> invoicePoller =
                    client.beginAnalyzeDocument(invoiceModelId, BinaryData.fromBytes(fileBytes));
            AnalyzeResult invoiceResult = invoicePoller.getFinalResult();
            InvoiceData invoiceData = invoiceService.extractInvoiceData(invoiceResult, referenceNumber);

            return ResponseEntity.ok(invoiceData);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}

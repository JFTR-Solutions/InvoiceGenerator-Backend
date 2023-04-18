package com.example.aiinvoice.api;

import com.azure.ai.formrecognizer.documentanalysis.models.*;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.example.aiinvoice.entity.InvoiceItem;
import com.example.aiinvoice.service.APIService;
import com.example.aiinvoice.service.InvoiceExportService;
import com.example.aiinvoice.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin
@RestController
public class FormRecognizer {


    @Autowired
    InvoiceService invoiceService;
    @Autowired
    APIService apiService;
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
    public ResponseEntity<List<InvoiceItem>> extractInvoices(@RequestParam("files") List<MultipartFile> files) throws IOException {
       /* String modelId = "prebuilt-invoice";
        List<InvoiceItem> combinedInvoiceList = new ArrayList<>();


        for (MultipartFile file : files) {
            try {
                byte[] fileBytes = file.getBytes();
                // Extract reference number from file name
                String fileName = file.getOriginalFilename();
                assert fileName != null;
                int startIndex = fileName.indexOf("[PONR-") + 6;
                int endIndex = fileName.indexOf("]", startIndex);
                String referenceNumber = fileName.substring(startIndex, endIndex);

                // Analyze invoice
                SyncPoller<OperationResult, AnalyzeResult> analyzeInvoicePoller =
                        client.beginAnalyzeDocument(modelId, BinaryData.fromBytes(fileBytes));
                AnalyzeResult invoiceResult = analyzeInvoicePoller.getFinalResult();
                List<InvoiceItem> invoiceItemList = invoiceService.extractInvoiceData(invoiceResult,referenceNumber);
                combinedInvoiceList.addAll(invoiceItemList);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }*/
        return ResponseEntity.ok(apiService.processInvoices(files));
    }

    @PostMapping("/invoices/byte")
    public ResponseEntity<byte[]> InvoicesToByte(@RequestParam("files") List<MultipartFile> files, @RequestParam("dispatchNumber") String dispatchNumber) throws IOException {
        System.out.println("InvoicesToByte endpoint called");
        String modelId = "prebuilt-invoice";
        List<InvoiceItem> combinedInvoiceList = new ArrayList<>();
        InvoiceExportService invoiceExportService = new InvoiceExportService();

        for (MultipartFile file : files) {
            try {
                System.out.println("Processing file: " + file.getOriginalFilename());

                byte[] fileBytes = file.getBytes();
                // Extract reference number from file name
                String fileName = file.getOriginalFilename();
                assert fileName != null;
                int startIndex = fileName.indexOf("[PONR-") + 6;
                int endIndex = fileName.indexOf("]", startIndex);
                String referenceNumber = fileName.substring(startIndex, endIndex);

                System.out.println("Reference number: " + referenceNumber);

                // Analyze invoice
                SyncPoller<OperationResult, AnalyzeResult> analyzeInvoicePoller =
                        client.beginAnalyzeDocument(modelId, BinaryData.fromBytes(fileBytes));
                AnalyzeResult analyzeTaxResult = analyzeInvoicePoller.getFinalResult();

                System.out.println("Analyze result: " + analyzeTaxResult);
                combinedInvoiceList.addAll(invoiceService.extractInvoiceData(analyzeTaxResult, referenceNumber));

            } catch (Exception e) {
                System.out.println("xxxx" + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }
        System.out.println("Generating Excel file");
        byte[] byteArr = invoiceExportService.createInvoiceExcel(combinedInvoiceList, dispatchNumber);
        System.out.println("Excel file generated");
        return ResponseEntity.ok(byteArr);
    }

}

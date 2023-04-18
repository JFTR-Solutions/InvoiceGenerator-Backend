package com.example.aiinvoice.service;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;
import com.azure.ai.formrecognizer.documentanalysis.models.OperationResult;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.example.aiinvoice.entity.InvoiceItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class APIService {

    @Autowired
    InvoiceService invoiceService;


    //use your `key` and `endpoint` environment variables
    private static final String key = System.getenv("FR_KEY");
    private static final String endpoint = System.getenv("FR_ENDPOINT");

    private DocumentAnalysisClient client;

    public APIService() {
        // create your `DocumentAnalysisClient` instance and `AzureKeyCredential` variable
        this.client = new DocumentAnalysisClientBuilder()
                .credential(new AzureKeyCredential(key))
                .endpoint(endpoint)
                .buildClient();
    }

    public List<InvoiceItem> processInvoices(List<MultipartFile> files) {
        String modelId = "prebuilt-invoice";
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
        }


        return combinedInvoiceList;
    }


}

package com.example.aiinvoice.api;

import com.example.aiinvoice.entity.InvoiceItem;
import com.example.aiinvoice.service.APIService;
import com.example.aiinvoice.service.InvoiceExportService;
import com.example.aiinvoice.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;

@CrossOrigin
@RestController
public class FormRecognizer {


    @Autowired
    InvoiceService invoiceService;
    @Autowired
    APIService apiService;
    @Autowired
    InvoiceExportService invoiceExportService;

    @PostMapping("/invoices")
    public ResponseEntity<List<InvoiceItem>> extractInvoices(@RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(apiService.processInvoices(files));
    }

    @PostMapping("/invoices/byte")
    public ResponseEntity<byte[]> InvoicesToByte(@RequestParam("files") List<MultipartFile> files, @RequestParam("dispatchNumber") String dispatchNumber) throws IOException {
        return ResponseEntity.ok(invoiceExportService.createInvoiceExcel(apiService.processInvoices(files), dispatchNumber));
    }


    @PostMapping("/invoices/test")
    public ResponseEntity<String> invoiceTest(@RequestParam("files") List<MultipartFile> files) throws IOException {
        String testdata = "[{\"referenceNumber\":\"240-S-218544\",\"description\":\"igs-gg\",\"quantity\":3,\"price\":175},{\"referenceNumber\":\"240-S-218544\",\"description\":\"igs-gg\",\"quantity\":1,\"price\":1248},{\"referenceNumber\":\"240-S-221321\",\"description\":\"bm015 base module for pd527 valve cont hs: 85423111 coo: denmark\",\"quantity\":3,\"price\":124}]";
        return ResponseEntity.ok(testdata);
    }

}

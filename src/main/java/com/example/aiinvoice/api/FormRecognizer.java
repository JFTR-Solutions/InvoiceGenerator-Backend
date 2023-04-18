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
        byte[] byteArr = invoiceExportService.createInvoiceExcel(apiService.processInvoices(files), dispatchNumber);

        // Set the desired file name
        String fileName = "invoice_" + dispatchNumber + ".xlsx";

        // Set the headers, including the "Content-Disposition" header with the desired file name
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(fileName).build());

        return ResponseEntity.ok().headers(headers).body(byteArr);
    }

}

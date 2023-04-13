package com.example.aiinvoice.service;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.example.aiinvoice.entity.InvoiceData;
import com.example.aiinvoice.entity.InvoiceItem;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class InvoiceExportService {

            public void createInvoiceExcel(InvoiceData invoiceData) throws IOException {

                // Create a workbook object
                Workbook workbook = new XSSFWorkbook();

                // Create a sheet with name "Invoice"
                Sheet sheet = workbook.createSheet("Invoice");

                // Create a header row
                Row headerRow = sheet.createRow(0);
                Cell descriptionHeaderCell = headerRow.createCell(0);
                descriptionHeaderCell.setCellValue("Description");
                Cell quantityHeaderCell = headerRow.createCell(1);
                quantityHeaderCell.setCellValue("Quantity");
                Cell priceHeaderCell = headerRow.createCell(2);
                priceHeaderCell.setCellValue("Price");
                Cell referenceNumberHeaderCell = headerRow.createCell(3);
                referenceNumberHeaderCell.setCellValue("Reference Number");

                // Create rows for each invoice item
                int rowIndex = 1;
                for (InvoiceItem invoiceItem : invoiceData.getInvoiceItems()) {
                    Row row = sheet.createRow(rowIndex++);
                    Cell descriptionCell = row.createCell(0);
                    descriptionCell.setCellValue(invoiceItem.getDescription());
                    Cell quantityCell = row.createCell(1);
                    quantityCell.setCellValue(invoiceItem.getQuantity());
                    Cell priceCell = row.createCell(2);
                    priceCell.setCellValue(invoiceItem.getPrice());
                    Cell referenceNumberCell = row.createCell(3);
                    referenceNumberCell.setCellValue(invoiceItem.getReferenceNumber());
                }

                // Create a subtotal row
                Row subTotalRow = sheet.createRow(rowIndex++);
                Cell subTotalHeaderCell = subTotalRow.createCell(0);
                subTotalHeaderCell.setCellValue("Subtotal");
                Cell subTotalCell = subTotalRow.createCell(2);
                subTotalCell.setCellValue(invoiceData.getSubTotal());

                // Auto-size the columns
                for (int i = 0; i < 4; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Write the workbook to a file
                try (FileOutputStream outputStream = new FileOutputStream("Invoice.xlsx")) {
                    workbook.write(outputStream);
                }
            }

    public byte[] createInvoiceExcelByte(InvoiceData invoiceData) throws IOException {
        // Create a workbook object
        Workbook workbook = new XSSFWorkbook();

        // Create a sheet with name "Invoice"
        Sheet sheet = workbook.createSheet("Invoice");

        // Create a header row
        Row headerRow = sheet.createRow(0);
        Cell descriptionHeaderCell = headerRow.createCell(0);
        descriptionHeaderCell.setCellValue("Description");
        Cell quantityHeaderCell = headerRow.createCell(1);
        quantityHeaderCell.setCellValue("Quantity");
        Cell priceHeaderCell = headerRow.createCell(2);
        priceHeaderCell.setCellValue("Price");
        Cell referenceNumberHeaderCell = headerRow.createCell(3);
        referenceNumberHeaderCell.setCellValue("Reference Number");

        // Create rows for each invoice item
        int rowIndex = 1;
        for (InvoiceItem invoiceItem : invoiceData.getInvoiceItems()) {
            Row row = sheet.createRow(rowIndex++);
            Cell descriptionCell = row.createCell(0);
            descriptionCell.setCellValue(invoiceItem.getDescription());
            Cell quantityCell = row.createCell(1);
            quantityCell.setCellValue(invoiceItem.getQuantity());
            Cell priceCell = row.createCell(2);
            priceCell.setCellValue(invoiceItem.getPrice());
            Cell referenceNumberCell = row.createCell(3);
            referenceNumberCell.setCellValue(invoiceItem.getReferenceNumber());
        }

        // Create a subtotal row
        Row subTotalRow = sheet.createRow(rowIndex++);
        Cell subTotalHeaderCell = subTotalRow.createCell(0);
        subTotalHeaderCell.setCellValue("Subtotal");
        Cell subTotalCell = subTotalRow.createCell(2);
        subTotalCell.setCellValue(invoiceData.getSubTotal());

        // Auto-size the columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write the workbook to a byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        workbook.write(byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // Close the workbook and the output stream
        workbook.close();
        byteArrayOutputStream.close();

        return byteArray;
    }


}
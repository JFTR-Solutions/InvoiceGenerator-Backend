package com.example.aiinvoice.service;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.example.aiinvoice.entity.InvoiceData;
import com.example.aiinvoice.entity.InvoiceItem;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class InvoiceExportService {

            public void createInvoiceExcelTEST(InvoiceData invoiceData) throws IOException {

                // Open the existing workbook file
                FileInputStream fileInputStream = new FileInputStream("swift_invoice_template.xlsx");
                Workbook workbook = WorkbookFactory.create(fileInputStream);

                // Create a workbook object
                //Workbook workbook = new XSSFWorkbook();

                // Create a sheet with name "Invoice"
                Sheet sheet = workbook.getSheet("Invoice");


                // Create rows for each invoice item
                int rowIndex = sheet.getLastRowNum() + 1;

                for (InvoiceItem invoiceItem : invoiceData.getInvoiceItems()) {
                    Row row = sheet.createRow(rowIndex++);
                    Cell descriptionCell = row.createCell(2);
                    descriptionCell.setCellValue(invoiceItem.getDescription());
                    Cell quantityCell = row.createCell(0);
                    quantityCell.setCellValue(invoiceItem.getQuantity());
                    Cell priceCell = row.createCell(4);
                    priceCell.setCellValue(invoiceItem.getPrice());
                    Cell referenceNumberCell = row.createCell(7);
                    referenceNumberCell.setCellValue(invoiceItem.getReferenceNumber());
                }

                // Create a subtotal row
                Row subTotalRow = sheet.createRow(rowIndex++);
                Cell subTotalHeaderCell = subTotalRow.createCell(0);
                subTotalHeaderCell.setCellValue("Subtotal");
                Cell subTotalCell = subTotalRow.createCell(4);
                subTotalCell.setCellValue(invoiceData.getSubTotal());

                // Auto-size the columns
                /*for (int i = 0; i < 4; i++) {
                    sheet.autoSizeColumn(i);
                }*/

                // Write the workbook to a file
                try (FileOutputStream outputStream = new FileOutputStream("Invoice.xlsx")) {
                    workbook.write(outputStream);
                }
            }

    public byte[] createInvoiceExcel(InvoiceData invoiceData) throws IOException {
            // Load the template file
            String templatePath = "swift_invoice_template.xlsx";
            FileInputStream inputStream = new FileInputStream(templatePath);
            Workbook workbook = new XSSFWorkbook(inputStream);

            // Get the "Invoice" sheet
              Sheet sheet = workbook.getSheetAt(0);

            //Set invoice data in table
            int rowIndex = 17;
            int quantityIndex = 0;
            for (InvoiceItem invoiceItem : invoiceData.getInvoiceItems()) {
                Row row = sheet.createRow(rowIndex);
                Cell descriptionCell = row.createCell(2);
                descriptionCell.setCellValue(invoiceItem.getDescription());
                Cell quantityCell = row.createCell(0);
                quantityCell.setCellValue(invoiceItem.getQuantity());
                quantityIndex += invoiceItem.getQuantity();
                Cell priceCell = row.createCell(4);
                priceCell.setCellValue(invoiceItem.getPrice());
                Cell referenceNumberCell = row.createCell(7);
                referenceNumberCell.setCellValue(invoiceItem.getReferenceNumber());


                rowIndex++;
            }
            CellStyle styleLine = workbook.createCellStyle();
            styleLine.setBorderBottom(BorderStyle.MEDIUM);
            Row tableFooter = sheet.createRow(rowIndex);
             for (int i = 0; i < 8; i++) {
                Cell cell = tableFooter.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellStyle(styleLine);
             }

            //Subtotal row
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle style = workbook.createCellStyle();
            style.setFont(boldFont);
            rowIndex++;
            Row subTotalRow = sheet.createRow(rowIndex);
            Cell subQuantityCell = subTotalRow.createCell(0);
            subQuantityCell.setCellStyle(style);
            subQuantityCell.setCellValue(quantityIndex);
            Cell subTotalCell = subTotalRow.createCell(4);
            subTotalCell.setCellStyle(style);
            subTotalCell.setCellValue(invoiceData.getSubTotal());


            // Auto-size the columns
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write the workbook to a byte array
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            workbook.write(byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            // Close the workbook and the input/output streams
            workbook.close();
            inputStream.close();
            byteArrayOutputStream.close();

            return byteArray;
        }


}
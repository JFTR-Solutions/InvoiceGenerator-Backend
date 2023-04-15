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
                FileInputStream fileInputStream = new FileInputStream("swift_invoice_template2.xlsx");
                Workbook workbook = WorkbookFactory.create(fileInputStream);

                // Create a workbook object
                //Workbook workbook = new XSSFWorkbook();

                // Create a sheet with name "Invoice"
                Sheet sheet = workbook.getSheet("Invoice");

                CellStyle wrapTextStyle = workbook.createCellStyle();
                wrapTextStyle.setWrapText(true);
                // Create rows for each invoice item
                int rowIndex = sheet.getLastRowNum() + 1;

                for (InvoiceItem invoiceItem : invoiceData.getInvoiceItems()) {
                    Row row = sheet.createRow(rowIndex++);
                    Cell descriptionCell = row.createCell(0);
                    descriptionCell.setCellValue(invoiceItem.getDescription());
                    descriptionCell.setCellStyle(wrapTextStyle);
                    Cell quantityCell = row.createCell(1);
                    quantityCell.setCellValue(invoiceItem.getQuantity());
                    Cell priceCell = row.createCell(2);
                    priceCell.setCellValue(invoiceItem.getPrice());
                    Cell referenceNumberCell = row.createCell(4);
                    referenceNumberCell.setCellValue(invoiceItem.getReferenceNumber());

                    sheet.setColumnWidth(0, 10000);
                }

                // Create a subtotal row
                Row subTotalRow = sheet.createRow(rowIndex++);
                Cell subTotalHeaderCell = subTotalRow.createCell(0);
                subTotalHeaderCell.setCellValue("Subtotal");
                Cell subTotalCell = subTotalRow.createCell(3);
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
            String templatePath = "swift_invoice_template2.xlsx";
            FileInputStream inputStream = new FileInputStream(templatePath);
            Workbook workbook = new XSSFWorkbook(inputStream);

            // Get the "Invoice" sheet
              Sheet sheet = workbook.getSheetAt(0);

            CellStyle wrapTextStyle = workbook.createCellStyle();
            wrapTextStyle.setWrapText(true);
            //Set invoice data in table
            int rowIndex = 17;
            int quantityIndex = 1;
            for (InvoiceItem invoiceItem : invoiceData.getInvoiceItems()) {

                Row row = sheet.createRow(rowIndex);
                Cell descriptionCell = row.createCell(0);
                descriptionCell.setCellValue(invoiceItem.getDescription());
                descriptionCell.setCellStyle(wrapTextStyle);
                Cell quantityCell = row.createCell(1);
                quantityCell.setCellValue(invoiceItem.getQuantity());
                sheet.setColumnWidth(0, 10000);
                quantityIndex += invoiceItem.getQuantity();
                Cell priceCell = row.createCell(2);
                priceCell.setCellValue(invoiceItem.getPrice());
                Cell currencyCell = row.createCell(3);
                currencyCell.setCellValue("EUR");
                Cell referenceNumberCell = row.createCell(4);
                referenceNumberCell.setCellValue(invoiceItem.getReferenceNumber());


                rowIndex++;
            }
            CellStyle styleLine = workbook.createCellStyle();
            styleLine.setBorderBottom(BorderStyle.MEDIUM);
            Row tableFooter = sheet.createRow(rowIndex);
             for (int i = 0; i < 5; i++) {
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
            Cell subQuantityCell = subTotalRow.createCell(1);
            subQuantityCell.setCellStyle(style);
            subQuantityCell.setCellValue(quantityIndex);
            Cell subTotalCell = subTotalRow.createCell(2);
            subTotalCell.setCellStyle(style);
            subTotalCell.setCellValue(invoiceData.getSubTotal());
            Cell subCurrencyCell = subTotalRow.createCell(3);
            subCurrencyCell.setCellStyle(style);
            subCurrencyCell.setCellValue("EUR");

            // Auto-size the columns
            for (int i = 1; i < 6; i++) {
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
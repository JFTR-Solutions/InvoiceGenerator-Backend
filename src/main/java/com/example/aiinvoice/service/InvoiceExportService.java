package com.example.aiinvoice.service;

import java.io.*;
import java.time.LocalDate;

import com.example.aiinvoice.entity.InvoiceData;
import com.example.aiinvoice.entity.InvoiceItem;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class InvoiceExportService {

    public byte[] createInvoiceExcel(InvoiceData invoiceData, String dispNr) throws IOException {
        // Load the template file
        System.out.println("Entering createInvoiceExcel method");
        try {
            System.out.println("Entering createInvoiceExcel method");

            // Use class loader to get the file from resources
            String templatePath = "src/main/resources/swift_invoice_template3.xlsx";
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(templatePath);
            if (inputStream == null) {
                throw new RuntimeException("Cannot find the file: " + templatePath);
            }

            Workbook workbook = new XSSFWorkbook(inputStream);


            // Get the "Invoice" sheet
            Sheet sheet = workbook.getSheetAt(0);
            // Merge cells between C8-E13

            CellStyle wrapTextStyle = workbook.createCellStyle();
            wrapTextStyle.setWrapText(true);

            //Set invoice number

            Row dispatchNumberRow = sheet.getRow(2);
            Cell dispatchNumberCell = dispatchNumberRow.createCell(5);
            dispatchNumberCell.setCellValue("Our ref: " + dispNr);

            Row dateRow = sheet.getRow(1);
            Cell dateCell = dateRow.createCell(5);
            LocalDate date = LocalDate.now();
            String month = date.getMonth().toString().substring(0, 1) + date.getMonth().toString().substring(1).toLowerCase();
            dateCell.setCellValue("Date: " + month + " " + dayOfMonthSuffix(date.getDayOfMonth()) + ", " + date.getYear());


            //Set invoice data in table
            int rowIndex = 17;
            int quantityIndex = 1;
            int totalQuantity = 0;
            double subtotal = 0;

            for (InvoiceItem invoiceItem : invoiceData.getInvoiceItems()) {
                System.out.println("Processing invoice data row: " + invoiceItem);
                Row row = sheet.createRow(rowIndex);
                Cell descriptionCell = row.createCell(0);

                descriptionCell.setCellValue(invoiceItem.getDescription());
                descriptionCell.setCellStyle(wrapTextStyle);
                sheet.setColumnWidth(0, 10000);

                Cell quantityCell = row.createCell(1);
                quantityCell.setCellValue(invoiceItem.getQuantity());
                quantityIndex += invoiceItem.getQuantity();

                Cell priceCell = row.createCell(2);
                priceCell.setCellValue(invoiceItem.getPrice());

                double totalForRow = invoiceItem.getQuantity() * invoiceItem.getPrice();

                Cell totalCell = row.createCell(3);
                totalCell.setCellValue(totalForRow);

                Cell currencyCell = row.createCell(4);
                currencyCell.setCellValue("EUR");
                Cell referenceNumberCell = row.createCell(5);

                referenceNumberCell.setCellValue(invoiceItem.getReferenceNumber());

                totalQuantity += invoiceItem.getQuantity();
                subtotal += invoiceItem.getQuantity() * invoiceItem.getPrice();

                rowIndex++;
            }
            CellStyle styleLine = workbook.createCellStyle();
            styleLine.setBorderBottom(BorderStyle.MEDIUM);
            Row tableFooter = sheet.createRow(rowIndex);
            for (int i = 0; i < 6; i++) {
                Cell cell = tableFooter.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellStyle(styleLine);
            }

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle style = workbook.createCellStyle();
            style.setFont(boldFont);
            rowIndex++;

            Row subTotalRow = sheet.createRow(rowIndex);

            Cell total = subTotalRow.createCell(0);
            total.setCellValue("Total");
            total.setCellStyle(style);

            Cell subQuantityCell = subTotalRow.createCell(1);


            subQuantityCell.setCellStyle(style);
            subQuantityCell.setCellValue(totalQuantity); // Use the updated totalQuantity value
            Cell subTotalCell = subTotalRow.createCell(3);
            subTotalCell.setCellStyle(style);
            subTotalCell.setCellValue(subtotal); // Use the updated subtotal value
            Cell subCurrencyCell = subTotalRow.createCell(4);
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
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }


    private String dayOfMonthSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return day + "th";
        }
        switch (day % 10) {
            case 1:
                return day + "st";
            case 2:
                return day + "nd";
            case 3:
                return day + "rd";
            default:
                return day + "th";
        }
    }


}
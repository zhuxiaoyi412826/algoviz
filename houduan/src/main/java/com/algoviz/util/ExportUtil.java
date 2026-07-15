package com.algoviz.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExportUtil {
    public static byte[] exportToExcel(List<String> headers, List<List<Object>> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Data");
            
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
            }
            
            for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<Object> rowData = data.get(rowIndex);
                for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    Object value = rowData.get(colIndex);
                    if (value == null) {
                        cell.setCellValue("");
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            }
            
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public static byte[] exportToCsv(List<String> headers, List<List<Object>> data) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.join(",", headers)).append("\n");
        
        for (List<Object> rowData : data) {
            StringBuilder row = new StringBuilder();
            for (int i = 0; i < rowData.size(); i++) {
                Object value = rowData.get(i);
                if (i > 0) row.append(",");
                if (value != null) {
                    String strValue = value.toString();
                    if (strValue.contains(",") || strValue.contains("\"") || strValue.contains("\n")) {
                        strValue = "\"" + strValue.replace("\"", "\"\"") + "\"";
                    }
                    row.append(strValue);
                }
            }
            sb.append(row).append("\n");
        }
        
        return sb.toString().getBytes("UTF-8");
    }

    public static byte[] exportToJson(List<String> headers, List<List<Object>> data) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
            if (rowIndex > 0) sb.append(",");
            sb.append("{");
            
            List<Object> rowData = data.get(rowIndex);
            for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                if (colIndex > 0) sb.append(",");
                sb.append("\"").append(headers.get(colIndex)).append("\":");
                
                Object value = rowData.get(colIndex);
                if (value == null) {
                    sb.append("null");
                } else if (value instanceof Number || value instanceof Boolean) {
                    sb.append(value);
                } else {
                    sb.append("\"").append(escapeJson(value.toString())).append("\"");
                }
            }
            
            sb.append("}");
        }
        
        sb.append("]");
        return sb.toString().getBytes("UTF-8");
    }

    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public static <T> List<List<Object>> convertToDataList(List<T> list, String[] fields, Map<String, String> fieldLabels) {
        return null;
    }
}

package com.flt.fltspring.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TableDataTransformer {
    // Common patterns in aircraft logbooks
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{1,2}[:|.]\\d{2}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?$");

    public List<TableRow> transformData(List<TableRow> rows) {
        log.info("Starting transformation of {} rows", rows.size());
        List<TableRow> transformed = rows.stream()
                .map(this::transformRow)
                .collect(Collectors.toList());
        log.info("Completed transformation. Output rows: {}", transformed.size());
        return transformed;
    }

    private TableRow transformRow(TableRow row) {
        log.debug("Transforming row {}, isHeader: {}", row.getRowIndex(), row.isHeader());
        Map<Integer, String> cleanedData = new HashMap<>();

        row.getColumnData().forEach((column, value) -> {
            String cleanedValue = cleanValue(value, row.isHeader());
            log.debug("Column {}: '{}' -> '{}'", column, value, cleanedValue);
            if (cleanedValue != null && !cleanedValue.trim().isEmpty()) {
                cleanedData.put(column, cleanedValue);
            }
        });

        TableRow transformedRow = new TableRow(row.getRowIndex(), cleanedData, row.isHeader());
        log.debug("Transformed row result: {}", transformedRow);
        return transformedRow;
    }

    private String cleanValue(String value, boolean isHeader) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        // Don't transform header values
        if (isHeader) {
            return value.trim();
        }

        String cleaned = value.trim();

        // Handle specific patterns
        if (TIME_PATTERN.matcher(cleaned).matches()) {
            // Standardize time format (e.g., "1:30" -> "1.5")
            cleaned = standardizeTime(cleaned);
        } else if (DATE_PATTERN.matcher(cleaned).matches()) {
            // Standardize date format
            cleaned = standardizeDate(cleaned);
        }

        // Remove multiple spaces
        cleaned = cleaned.replaceAll("\\s+", " ");

        // Remove common OCR artifacts
        cleaned = cleaned.replaceAll("[\\p{Punct}&&[^./]]", "");

        return cleaned;
    }

    private String standardizeTime(String time) {
        try {
            String[] parts = time.split("[:|.]");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            double decimal = hours + (minutes / 60.0);
            return String.format("%.1f", decimal);
        } catch (Exception e) {
            log.warn("Failed to standardize time value: {}", time, e);
            return time;
        }
    }

    private String standardizeDate(String date) {
        try {
            String[] parts = date.split("/");
            // Assuming MM/DD format if only two parts
            if (parts.length == 2) {
                return String.format("%02d/%02d",
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]));
            }
            // MM/DD/YY format
            return String.format("%02d/%02d/%s",
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    parts[2]);
        } catch (Exception e) {
            log.warn("Failed to standardize date value: {}", date, e);
            return date;
        }
    }
}
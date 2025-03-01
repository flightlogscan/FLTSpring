package com.flt.fltspring.service;

import com.flt.fltspring.config.ColumnConfig;
import com.flt.fltspring.model.TableRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TableDataTransformerService {

    private final ColumnConfig[] columnConfigs;

    public TableDataTransformerService(ColumnConfig[] columnConfigs) {
        this.columnConfigs = columnConfigs;
    }

    // Characters commonly misidentified as numbers
    private static final Map<String, String> NUMERIC_REPLACEMENTS = new HashMap<>() {{
        put("O", "0");
        put("o", "0");
        put("l", "1");
        put("I", "1");
        put("i", "1");
        put("/", "1");
        put("\\", "1");
        put("S", "5");
        put("s", "5");
        put("Z", "2");
        put("z", "2");
        put("b", "6");
        put("B", "8");
    }};

    // Characters commonly misidentified as letters
    private static final Map<String, String> AIRPORT_CODE_REPLACEMENTS = new HashMap<>() {{
        put("0", "O");
        put("1", "I");
        put("2", "Z");
        put("5", "S");
        put("8", "B");
    }};

    private static final Pattern AIRPORT_CODE_PATTERN = Pattern.compile("^[A-Z]{3,4}$");

    public List<TableRow> transformData(List<TableRow> rows) {
        if (rows.isEmpty()) {
            return rows;
        }

        // The first row should be our header row with properly duplicated headers
        TableRow headerRow = rows.get(0);

        // Build type map from the headers we have
        Map<Integer, String> columnTypes = buildColumnTypeMap(headerRow);

        // Transform only the data rows, preserve header row
        List<TableRow> transformedRows = new ArrayList<>();
        transformedRows.add(headerRow);  // Keep header row as-is

        // Transform subsequent rows
        for (int i = 1; i < rows.size(); i++) {
            transformedRows.add(transformRow(rows.get(i), columnTypes));
        }

        return transformedRows;
    }

    private Map<Integer, String> buildColumnTypeMap(TableRow headerRow) {
        Map<Integer, String> typeMap = new HashMap<>();

        // For each header, including duplicated ones
        headerRow.getColumnData().forEach((index, headerValue) -> {
            // First try exact match
            for (ColumnConfig config : columnConfigs) {
                if (headerValue.equals(config.getFieldName())) {
                    typeMap.put(index, config.getType());
                    log.info("Mapped column {} ({}) to type {} [exact match]",
                            index, headerValue, config.getType());
                    return;  // Found exact match, no need to continue
                }
            }

            // If no exact match, find the best partial match
            String bestMatch = null;
            String bestType = "STRING";  // Default to STRING if no match found
            int bestMatchLength = 0;

            for (ColumnConfig config : columnConfigs) {
                // If header contains config field name and it's longer than our current best match
                if (headerValue.contains(config.getFieldName()) &&
                        config.getFieldName().length() > bestMatchLength) {
                    bestMatch = config.getFieldName();
                    bestType = config.getType();
                    bestMatchLength = config.getFieldName().length();
                }
            }

            if (bestMatch != null) {
                typeMap.put(index, bestType);
                log.info("Mapped column {} ({}) to type {} [partial match with {}]",
                        index, headerValue, bestType, bestMatch);
            } else {
                log.warn("No match found for header {}, defaulting to STRING", headerValue);
                typeMap.put(index, "STRING");
            }
        });

        return typeMap;
    }

    private TableRow transformRow(TableRow row, Map<Integer, String> columnTypes) {
        if (row.isHeader()) {
            return row;
        }

        Map<Integer, String> transformedData = new HashMap<>();

        row.getColumnData().forEach((index, value) -> {
            String type = columnTypes.getOrDefault(index, "STRING");
            String transformedValue = transformValue(value, type);
            log.info("Transformed value at index {} from '{}' to '{}' using type {}",
                    index, value, transformedValue, type);
            transformedData.put(index, transformedValue);
        });

        return new TableRow(row.getRowIndex(), transformedData, false);
    }

    private String transformValue(String value, String columnType) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        value = value.trim();

        switch (columnType) {
            case "INTEGER":
                return transformInteger(value);
            case "AIRPORT_CODE":
                return transformAirportCode(value);
            case "STRING":
            default:
                return value;
        }
    }

    private String transformInteger(String value) {
        return transformWithReplacements(value, NUMERIC_REPLACEMENTS, "[^0-9]", "0");
    }

    private String transformAirportCode(String value) {
        // First convert to uppercase
        String transformed = value.toUpperCase();
        
        // Apply replacements
        transformed = transformWithReplacements(transformed, AIRPORT_CODE_REPLACEMENTS, null, null);
        
        // If it already matches airport code pattern, return as is
        if (AIRPORT_CODE_PATTERN.matcher(transformed).matches()) {
            return transformed;
        }
        
        // Otherwise, remove any remaining numbers and non-letters
        transformed = transformed.replaceAll("[^A-Z]", "");
        
        // Take first 4 characters (or less if shorter)
        return transformed.length() > 4 ? transformed.substring(0, 4) : transformed;
    }
    
    /**
     * Applies character replacements and filtering to transform values
     * @param value Original string value
     * @param replacements Map of character replacements to apply
     * @param filterPattern Regular expression pattern to filter out characters (can be null)
     * @param defaultValue Default value if result is empty (can be null)
     * @return Transformed string
     */
    private String transformWithReplacements(String value, Map<String, String> replacements, 
                                            String filterPattern, String defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue != null ? defaultValue : "";
        }
        
        // Apply character replacements
        String transformed = value;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            transformed = transformed.replace(replacement.getKey(), replacement.getValue());
        }
        
        // Apply filter pattern if provided
        if (filterPattern != null) {
            transformed = transformed.replaceAll(filterPattern, "");
            
            // Return default if empty
            if (transformed.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
        }
        
        return transformed;
    }
}
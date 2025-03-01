package com.flt.fltspring.service;

import com.flt.fltspring.config.ColumnConfig;
import com.flt.fltspring.model.TableRow;
import com.flt.fltspring.service.transform.AirportCodeTransformer;
import com.flt.fltspring.service.transform.ContextValidator;
import com.flt.fltspring.service.transform.HeaderMatcher;
import com.flt.fltspring.service.transform.TextTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableDataTransformerService {

    private final ColumnConfig[] columnConfigs;
    private final TextTransformer textTransformer;
    private final AirportCodeTransformer airportCodeTransformer;
    private final HeaderMatcher headerMatcher;
    private final ContextValidator contextValidator;

    /**
     * Transform table data by cleaning and converting values based on
     * column types and domain-specific rules for flight logbooks
     */
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
            // Clean and normalize the header first
            String normalizedHeader = textTransformer.normalizeHeaderValue(headerValue);

            // First try exact match with normalized header
            for (ColumnConfig config : columnConfigs) {
                if (normalizedHeader.equals(config.getFieldName()) ||
                        headerValue.equals(config.getFieldName())) {
                    typeMap.put(index, config.getType());
                    log.info("Mapped column {} ({}) to type {} [exact match]",
                            index, headerValue, config.getType());
                    return;  // Found exact match, no need to continue
                }
            }

            // Try fuzzy matching with common flight logbook headers
            String matchedCommonHeader = headerMatcher.findClosestMatch(normalizedHeader, null);
            if (matchedCommonHeader != null) {
                // If we matched a common header, look up its type in our configs
                for (ColumnConfig config : columnConfigs) {
                    if (matchedCommonHeader.equals(config.getFieldName())) {
                        typeMap.put(index, config.getType());
                        log.info("Mapped column {} ({}) to type {} [fuzzy match with {}]",
                                index, headerValue, config.getType(), matchedCommonHeader);
                        return;
                    }
                }
            }

            // If no exact match, find the best partial match
            String bestMatch = null;
            String bestType = "STRING";  // Default to STRING if no match found
            int bestMatchLength = 0;

            for (ColumnConfig config : columnConfigs) {
                // Try to match with normalized header first
                boolean normalizedContains = normalizedHeader.contains(config.getFieldName());
                boolean originalContains = headerValue.contains(config.getFieldName());

                // If header contains config field name and it's longer than our current best match
                if ((normalizedContains || originalContains) &&
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
                // Type inference based on content patterns
                if (headerMatcher.inferAirportCodeType(headerValue)) {
                    typeMap.put(index, "AIRPORT_CODE");
                    log.info("Mapped column {} ({}) to AIRPORT_CODE [inferred from name]",
                            index, headerValue);
                } else if (headerMatcher.inferNumericType(headerValue)) {
                    typeMap.put(index, "INTEGER");
                    log.info("Mapped column {} ({}) to INTEGER [inferred from name]",
                            index, headerValue);
                } else {
                    log.warn("No match found for header {}, defaulting to STRING", headerValue);
                    typeMap.put(index, "STRING");
                }
            }
        });

        return typeMap;
    }

    private TableRow transformRow(TableRow row, Map<Integer, String> columnTypes) {
        if (row.isHeader()) {
            return row;
        }

        Map<Integer, String> transformedData = new HashMap<>();
        Map<Integer, String> originalData = row.getColumnData();

        // First pass: do standard transformations on all fields
        originalData.forEach((index, value) -> {
            String type = columnTypes.getOrDefault(index, "STRING");
            String transformedValue = transformValue(value, type);
            transformedData.put(index, transformedValue);

            if (log.isDebugEnabled() && !value.equals(transformedValue)) {
                log.debug("Transformed value at index {} from '{}' to '{}' using type {}",
                        index, value, transformedValue, type);
            }
        });

        // Second pass: context-aware transformations for special cases
        contextValidator.validateRowContext(transformedData, columnTypes);

        // Preserve parent headers from original row
        return TableRow.builder()
                .rowIndex(row.getRowIndex())
                .columnData(transformedData)
                .isHeader(false)
                .parentHeaders(row.getParentHeaders())
                .build();
    }

    private String transformValue(String value, String columnType) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        value = value.trim();

        switch (columnType) {
            case "INTEGER":
                return textTransformer.transformInteger(value);
            case "AIRPORT_CODE":
                return airportCodeTransformer.transformAirportCode(value);
            case "STRING":
            default:
                return value;
        }
    }
}
package com.flt.fltspring.service;

import com.flt.fltspring.config.CharacterReplacementConfig;
import com.flt.fltspring.config.ColumnConfig;
import com.flt.fltspring.model.bizlogic.TableRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TableDataTransformerService {

    // Used to correct common header errors
    private static final Map<String, String> HEADER_CANONICAL_MAP = Map.of(
            "ENGINE LAND", "SINGLE-ENGINE LAND",
            "SINGLE-ENGINE LAND", "SINGLE-ENGINE LAND",
            "MULTI-ENGINE LAND", "MULTI-ENGINE LAND",
            "MULTI- ENGINE LAND", "MULTI-ENGINE LAND",
            "AIRCRAFT CATEGORY SINGLE-", "SINGLE-ENGINE LAND"
    );

    private final ColumnConfig[] columnConfigs;
    private final CharacterReplacementConfig replacementConfig;

    /**
     * Normalize header and data rows in one pass.
     */
    public List<TableRow> transformData(List<TableRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        // Infer column types from the first (header) row
        TableRow headerRow = rows.get(0);
        Map<Integer, String> columnTypeMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.getColumnData().entrySet()) {
            String rawHeader = Optional.ofNullable(entry.getValue())
                    .map(String::trim)
                    .orElse("")
                    .toUpperCase();
            String canonicalHeader = HEADER_CANONICAL_MAP.getOrDefault(rawHeader, rawHeader);
            String type = Arrays.stream(columnConfigs)
                    .filter(cfg -> cfg.getFieldName().equalsIgnoreCase(canonicalHeader))
                    .map(ColumnConfig::getType)
                    .findFirst()
                    .orElse("STRING");
            columnTypeMap.put(entry.getKey(), type);
        }

        List<TableRow> normalizedRows = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            TableRow original = rows.get(i);
            boolean isHeader = (i == 0);

            Map<Integer, String> normalizedData = new LinkedHashMap<>();
            for (Map.Entry<Integer, String> cell : original.getColumnData().entrySet()) {
                int colIndex = cell.getKey();
                String rawValue = cell.getValue();
                String columnType = columnTypeMap.getOrDefault(colIndex, "STRING");
                String normalizedValue = normalizeCell(rawValue, columnType, isHeader);
                normalizedData.put(colIndex, normalizedValue);
            }

            normalizedRows.add(TableRow.builder()
                    .rowIndex(original.getRowIndex())
                    .columnData(normalizedData)
                    .isHeader(isHeader)
                    .parentHeaders(original.getParentHeaders())
                    .build());
        }

        return normalizedRows;
    }

    /**
     * Trim and apply header or type-based replacements.
     * TODO: I don't like that clients have to do this
     */
    private String normalizeCell(String rawValue, String columnType, boolean isHeader) {
        if (rawValue == null) {
            return null;
        }
        String value = rawValue.trim();

        if (isHeader) {
            String headerKey = value.toUpperCase();
            value = HEADER_CANONICAL_MAP.getOrDefault(headerKey, value);
            for (Map.Entry<String, String> repl : replacementConfig.getStringReplacements().entrySet()) {
                value = value.replace(repl.getKey(), repl.getValue());
            }
            return value;
        }

        // For data rows, apply type-specific replacements
        Map<String, String> replacements;
        if ("INTEGER".equalsIgnoreCase(columnType)) {
            replacements = replacementConfig.getNumericReplacements();
        } else if ("AIRPORT_CODE".equalsIgnoreCase(columnType)) {
            replacements = replacementConfig.getAirportCodeReplacements();
        } else if ("DATE".equalsIgnoreCase(columnType)) {
            replacements = replacementConfig.getDateReplacements();
        } else {
            replacements = replacementConfig.getStringReplacements();
        }
        for (Map.Entry<String, String> repl : replacements.entrySet()) {
            value = value.replace(repl.getKey(), repl.getValue());
        }
        return value;
    }
}

package com.flt.fltspring.service;

import com.flt.fltspring.config.CharacterReplacementConfig;
import com.flt.fltspring.config.ColumnConfig;
import com.flt.fltspring.model.bizlogic.TableRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Transformer: processes header and data rows.
 */
@Service
@RequiredArgsConstructor
public class TableDataTransformerService {

    private final ColumnConfig[] columnConfigs;
    private final CharacterReplacementConfig replacementConfig;

    public List<TableRow> transformData(List<TableRow> rows) {
        if (rows.isEmpty()) return rows;

        // Determine type for each column from header row
        TableRow headerRow = rows.getFirst();
        Map<Integer, String> columnTypes = headerRow.getColumnData().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> findTypeForHeader(e.getValue())
                ));

        List<TableRow> result = new ArrayList<>();
        // Process header row
        result.add(processRow(headerRow, columnTypes, true));
        // Process data rows
        for (int i = 1; i < rows.size(); i++) {
            result.add(processRow(rows.get(i), columnTypes, false));
        }
        return result;
    }

    private TableRow processRow(TableRow row, Map<Integer, String> types, boolean isHeader) {
        Map<Integer, String> transformed = row.getColumnData().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> applyReplacements(e.getValue(), types.getOrDefault(e.getKey(), "STRING"), isHeader)
                ));

        return TableRow.builder()
                .rowIndex(row.getRowIndex())
                .columnData(transformed)
                .isHeader(isHeader)
                .parentHeaders(row.getParentHeaders())
                .build();
    }

    private String findTypeForHeader(String header) {
        if (header == null) return "STRING";
        String trimmed = header.trim();
        return java.util.Arrays.stream(columnConfigs)
                .filter(cfg -> cfg.getFieldName().equalsIgnoreCase(trimmed))
                .map(ColumnConfig::getType)
                .findFirst()
                .orElse("STRING");
    }

    private String applyReplacements(String value, String type, boolean isHeader) {
        if (value == null) return null;
        String result = value.trim();

        Map<String, String> replMap;
        if (isHeader) {
            // Use header/string replacements
            replMap = replacementConfig.getStringReplacements();
        } else {
            switch (type) {
                case "INTEGER":
                    replMap = replacementConfig.getNumericReplacements();
                    break;
                case "AIRPORT_CODE":
                    replMap = replacementConfig.getAirportCodeReplacements();
                    break;
                default:
                    // No replacements for other data fields
                    replMap = Collections.emptyMap();
            }
        }
        // Apply replacements
        for (Map.Entry<String, String> entry : replMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
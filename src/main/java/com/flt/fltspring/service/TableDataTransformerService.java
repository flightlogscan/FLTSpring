package com.flt.fltspring.service;

import com.flt.fltspring.config.CharacterReplacementConfig;
import com.flt.fltspring.config.ColumnConfig;
import com.flt.fltspring.config.ColumnType;
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

    private static final Map<String, String> HEADER_CANONICAL_MAP = Map.ofEntries(
            Map.entry("ENGINE LAND", "SINGLE-ENGINE LAND"),
            Map.entry("SINGLE-ENGINE LAND", "SINGLE-ENGINE LAND"),
            Map.entry("MULTI-ENGINE LAND", "MULTI-ENGINE LAND"),
            Map.entry("MULTI- ENGINE LAND", "MULTI-ENGINE LAND"),
            Map.entry("AIRCRAFT CATEGORY SINGLE-", "SINGLE-ENGINE LAND")
    );

    private final ColumnConfig[] columnConfigs;
    private final CharacterReplacementConfig replacementConfig;

    public List<TableRow> transformData(final List<TableRow> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        final TableRow headerRow = rows.getFirst();
        final Map<Integer, ColumnType> columnTypeMap = buildColumnTypeMap(headerRow);

        final List<TableRow> normalizedRows = new ArrayList<>(rows.size());

        for (final TableRow row : rows) {
            final boolean isHeader = row == headerRow;
            final Map<Integer, String> normalizedData = new LinkedHashMap<>();

            for (Map.Entry<Integer, String> cell : row.getColumnData().entrySet()) {
                final int colIndex = cell.getKey();
                final String rawValue = cell.getValue();
                final ColumnType columnType = columnTypeMap.getOrDefault(colIndex, ColumnType.STRING);
                final String normalizedValue = normalizeCell(rawValue, columnType, isHeader);
                normalizedData.put(colIndex, normalizedValue);
            }

            normalizedRows.add(TableRow.builder()
                    .rowIndex(row.getRowIndex())
                    .columnData(normalizedData)
                    .isHeader(isHeader)
                    .parentHeaders(row.getParentHeaders())
                    .build());
        }

        return normalizedRows;
    }

    private Map<Integer, ColumnType> buildColumnTypeMap(TableRow headerRow) {
        final Map<Integer, ColumnType> columnTypeMap = new LinkedHashMap<>();

        for (final Map.Entry<Integer, String> entry : headerRow.getColumnData().entrySet()) {
            final String canonicalHeader = Optional.ofNullable(entry.getValue())
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(header -> HEADER_CANONICAL_MAP.getOrDefault(header, header))
                    .orElse("");

            // Determine the cell's data type from the header
            final ColumnType type = Arrays.stream(columnConfigs)
                    .filter(cfg -> cfg.getFieldName().equalsIgnoreCase(canonicalHeader))
                    .map(ColumnConfig::getType)
                    .findFirst()
                    .orElse(ColumnType.STRING);

            columnTypeMap.put(entry.getKey(), type);
        }

        return columnTypeMap;
    }

    private String normalizeCell(String rawValue, ColumnType columnType, boolean isHeader) {
        if (rawValue == null) return null;

        String value = rawValue.trim();

        if (isHeader) {
            value = HEADER_CANONICAL_MAP.getOrDefault(value.toUpperCase(), value);
            for (Map.Entry<String, String> repl : replacementConfig.getStringReplacements().entrySet()) {
                value = value.replace(repl.getKey(), repl.getValue());
            }
            return value;
        }

        final Map<String, String> replacements = switch (columnType) {
            case INTEGER -> replacementConfig.getNumericReplacements();
            case AIRPORT_CODE -> replacementConfig.getAirportCodeReplacements();
            case DATE -> replacementConfig.getDateReplacements();
            case STRING -> replacementConfig.getStringReplacements();
        };

        for (Map.Entry<String, String> repl : replacements.entrySet()) {
            value = value.replace(repl.getKey(), repl.getValue());
        }

        return value;
    }
}
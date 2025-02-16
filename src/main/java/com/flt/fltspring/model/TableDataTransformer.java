package com.flt.fltspring.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@Service
public class TableDataTransformer {
    private static final Map<String, String> CHARACTER_REPLACEMENTS = new HashMap<>() {{
        put("/", "1");
        put("\\", "1");
        put("o", "0");
        put("O", "0");
        put("l", "1");
        put("I", "1");
        put("S", "5");
        put("s", "5");
        put("Z", "2");
        put("z", "2");
        put("b", "6");
        put("B", "8");
    }};

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
            String cleanedValue = row.isHeader() ? value.trim() : replaceCharacters(value);
            if (cleanedValue != null && !cleanedValue.trim().isEmpty()) {
                cleanedData.put(column, cleanedValue);
            }
        });

        TableRow transformedRow = new TableRow(row.getRowIndex(), cleanedData, row.isHeader());
        log.debug("Transformed row result: {}", transformedRow);
        return transformedRow;
    }

    private String replaceCharacters(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        String result = value.trim();
        for (Map.Entry<String, String> replacement : CHARACTER_REPLACEMENTS.entrySet()) {
            if (result.contains(replacement.getKey())) {
                result = result.replace(replacement.getKey(), replacement.getValue());
            }
        }
        return result;
    }
}
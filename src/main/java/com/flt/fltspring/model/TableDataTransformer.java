package com.flt.fltspring.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private DocumentAnalysisService.ColumnConfig[] columnConfigs;

    private Map<String, String> columnTypeMap;

    @Autowired
    public void init() {
        columnTypeMap = new HashMap<>();
        for (DocumentAnalysisService.ColumnConfig config : columnConfigs) {
            columnTypeMap.put(config.getFieldName(), config.getType());
        }
    }

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
            String cleanedValue;
            if (row.isHeader()) {
                cleanedValue = value.trim();
            } else {
                String columnType = getColumnType(value, row.getColumnData());
                cleanedValue = shouldTransform(columnType) ? replaceCharacters(value) : value.trim();
            }
            if (cleanedValue != null && !cleanedValue.trim().isEmpty()) {
                cleanedData.put(column, cleanedValue);
            }
        });

        return new TableRow(row.getRowIndex(), cleanedData, row.isHeader());
    }

    private String getColumnType(String value, Map<Integer, String> columnData) {
        return columnData.entrySet().stream()
                .filter(e -> e.getValue().equals(value))
                .findFirst()
                .map(e -> columnTypeMap.getOrDefault(value, "STRING"))
                .orElse("STRING");
    }

    private boolean shouldTransform(String columnType) {
        return "INTEGER".equals(columnType);
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
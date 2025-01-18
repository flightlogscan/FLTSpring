package com.flt.fltspring.model;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TableDataTransformer {
    public List<TableRow> transformData(List<TableRow> rows) {
        return rows.stream()
                .map(this::transformRow)
                .collect(Collectors.toList());
    }

    private TableRow transformRow(TableRow row) {
        Map<Integer, String> cleanedData = new HashMap<>();

        row.getColumnData().forEach((column, value) -> {
            String cleanedValue = cleanValue(value);
            cleanedData.put(column, cleanedValue);
        });

        return new TableRow(row.getRowIndex(), cleanedData, row.isHeader());
    }

    private String cleanValue(String value) {
        // Add your cleaning logic here
        // e.g., convert letters to numbers, format dates, etc.
        return value;
    }
}

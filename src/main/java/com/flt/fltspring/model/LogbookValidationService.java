package com.flt.fltspring.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LogbookValidationService {

    @Autowired
    private LogbookTemplateService templateService;

    @Autowired
    private DocumentAnalysisService.ColumnConfig[] columnConfigs;

    private Map<String, DocumentAnalysisService.ColumnConfig> configMap;
    private Map<String, String> headerMap;
    private Set<String> parentHeaders;

    @Autowired
    public void init() {
        this.configMap = Arrays.stream(columnConfigs)
                .collect(Collectors.toMap(
                        config -> config.getFieldName().toLowerCase(),
                        config -> config
                ));

        this.headerMap = new HashMap<>();
        headerMap.put("engine land", "SINGLE-ENGINE LAND");
        headerMap.put("multi- engine land", "MULTI-ENGINE LAND");
        headerMap.put("date", "DATE");
        headerMap.put("aircraft type", "AIRCRAFT TYPE");
        headerMap.put("aircraft ident", "AIRCRAFT IDENT");
        headerMap.put("from", "FROM");
        headerMap.put("to", "TO");
        headerMap.put("nr inst. app.", "NR INST. APP.");
        headerMap.put("remarks and endorsements", "REMARKS AND ENDORSEMENTS");
        headerMap.put("nr t/o", "NR T/O");
        headerMap.put("nr ldg", "NR LDG");

        this.parentHeaders = Arrays.stream(columnConfigs)
                .filter(config -> config.getColumnCount() > 1)
                .map(DocumentAnalysisService.ColumnConfig::getFieldName)
                .collect(Collectors.toSet());
    }

    public List<TableRow> validateAndCorrect(List<TableRow> scannedRows, LogbookType type) {
        log.info("Starting validation for {} rows with type {}", scannedRows.size(), type);

        TableRow headerRow = scannedRows.stream()
                .filter(TableRow::isHeader)
                .findFirst()
                .orElse(null);

        TableRow finalHeaderRow = headerRow;
        List<TableRow> dataRows = scannedRows.stream()
                .filter(row -> !row.equals(finalHeaderRow))
                .map(row -> new TableRow(row.getRowIndex(), row.getColumnData(), false))
                .collect(Collectors.toList());

        if (headerRow != null) {
            Map<Integer, String> consolidatedHeaders = new HashMap<>();
            headerRow.getColumnData().forEach((column, value) -> {
                String normalized = value.toLowerCase().trim();
                String canonicalHeader = headerMap.getOrDefault(normalized, value);
                consolidatedHeaders.put(column, canonicalHeader);
            });

            headerRow = new TableRow(0, consolidatedHeaders, true);
        }

        List<TableRow> result = new ArrayList<>();
        if (headerRow != null) {
            result.add(headerRow);
        }
        result.addAll(dataRows);

        return result;
    }

    private Map<Integer, String> expandHeaders(Map<Integer, String> headers, Set<Integer> dataColumns) {
        Map<Integer, String> expanded = new HashMap<>(headers);
        List<Integer> sortedKeys = new ArrayList<>(headers.keySet());
        Collections.sort(sortedKeys);

        String currentParentHeader = null;
        for (int i = 0; i < sortedKeys.size(); i++) {
            int currentKey = sortedKeys.get(i);
            String currentValue = headers.get(currentKey);

            if (isParentHeader(currentValue)) {
                currentParentHeader = currentValue;
                continue;
            }

            int nextKey = (i + 1 < sortedKeys.size()) ? sortedKeys.get(i + 1) : Integer.MAX_VALUE;
            String headerToUse = currentParentHeader != null ? currentParentHeader : currentValue;

            for (int col = currentKey + 1; col < nextKey; col++) {
                if (!headers.containsKey(col) && dataColumns.contains(col)) {
                    expanded.put(col, headerToUse);
                }
            }
        }

        return expanded;
    }

    private boolean isParentHeader(String header) {
        return header != null && parentHeaders.contains(header);
    }
}
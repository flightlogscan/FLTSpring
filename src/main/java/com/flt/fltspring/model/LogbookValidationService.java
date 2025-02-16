package com.flt.fltspring.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogbookValidationService {
    private final LogbookTemplateService templateService;

    public List<TableRow> validateAndCorrect(List<TableRow> scannedRows, LogbookType type) {
        log.info("Starting validation for {} rows with type {}", scannedRows.size(), type);

        final LogbookTemplate template = templateService.getTemplate(type);
        if (template == null) {
            log.warn("No template found for type {}. Returning original rows.", type);
            return scannedRows;
        }

        // Find header row
        final TableRow headerRow = scannedRows.stream()
                .filter(TableRow::isHeader)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No header row found"));

        log.info("Found header row: {}", headerRow);

        // Create corrected header row while preserving non-template columns
        final Map<Integer, String> correctedHeaders = new HashMap<>(headerRow.getColumnData());
        template.getExpectedHeaders().forEach((index, def) -> {
            if (def.isRequired() || headerRow.getColumnData().containsKey(index)) {
                correctedHeaders.put(index, def.getExpectedName());
                log.debug("Corrected header column {}: {}", index, def.getExpectedName());
            }
        });

        // Find all columns that contain data in non-header rows
        final Set<Integer> dataColumns = scannedRows.stream()
                .filter(row -> !row.isHeader())
                .flatMap(row -> row.getColumnData().keySet().stream())
                .collect(Collectors.toSet());

        // Duplicate headers for columns that should share the same header
        final Map<Integer, String> expandedHeaders = new HashMap<>(correctedHeaders);
        final List<Integer> headerKeys = new ArrayList<>(correctedHeaders.keySet());
        Collections.sort(headerKeys);

        for (int i = 0; i < headerKeys.size(); i++) {
            int currentKey = headerKeys.get(i);
            final String currentValue = correctedHeaders.get(currentKey);

            // Find the next header key
            final int nextKey = (i + 1 < headerKeys.size()) ? headerKeys.get(i + 1) : Integer.MAX_VALUE;

            // Add the same header value to all columns between current and next header
            for (int col = currentKey + 1; col < nextKey; col++) {
                if (!correctedHeaders.containsKey(col) && dataColumns.contains(col)) {
                    expandedHeaders.put(col, currentValue);
                    log.debug("Duplicated header '{}' to column {}", currentValue, col);
                }
            }
        }

        // Create new header row with expanded headers
        final TableRow correctedHeaderRow = new TableRow(
                headerRow.getRowIndex(),
                expandedHeaders,
                true
        );

        log.info("Created corrected header row: {}", correctedHeaderRow);

        // Return corrected data rows with original data since transformations are handled elsewhere
        final List<TableRow> correctedDataRows = scannedRows.stream()
                .filter(row -> !row.isHeader())
                .toList();

        log.info("Processed {} data rows", correctedDataRows.size());

        // Combine corrected header and data rows
        final List<TableRow> result = new ArrayList<>();
        result.add(correctedHeaderRow);
        result.addAll(correctedDataRows);

        log.info("Validation complete. Final row count: {}", result.size());
        return result;
    }
}
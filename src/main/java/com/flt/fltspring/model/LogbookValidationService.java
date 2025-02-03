package com.flt.fltspring.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogbookValidationService {
    private final LogbookTemplateService templateService;
    private static final Map<Character, String> REPLACEMENTS = new HashMap<>();

    static {
        REPLACEMENTS.put('/', "1");
        REPLACEMENTS.put('\\', "1");
        REPLACEMENTS.put('o', "0");
        REPLACEMENTS.put('O', "0");
        REPLACEMENTS.put('l', "1");
        REPLACEMENTS.put('I', "1");
        REPLACEMENTS.put('S', "5");
        REPLACEMENTS.put('s', "5");
        REPLACEMENTS.put('Z', "2");
        REPLACEMENTS.put('z', "2");
        REPLACEMENTS.put('b', "6");
        REPLACEMENTS.put('B', "8");
    }

    public List<TableRow> validateAndCorrect(List<TableRow> scannedRows, LogbookType type) {
        log.info("Starting validation for {} rows with type {}", scannedRows.size(), type);

        LogbookTemplate template = templateService.getTemplate(type);
        if (template == null) {
            log.warn("No template found for type {}. Returning original rows.", type);
            return scannedRows;
        }

        // Find header row
        TableRow headerRow = scannedRows.stream()
                .filter(TableRow::isHeader)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No header row found"));

        log.info("Found header row: {}", headerRow);

        // Create corrected header row while preserving non-template columns
        Map<Integer, String> correctedHeaders = new HashMap<>(headerRow.getColumnData());
        template.getExpectedHeaders().forEach((index, def) -> {
            if (def.isRequired() || headerRow.getColumnData().containsKey(index)) {
                correctedHeaders.put(index, def.getExpectedName());
                log.debug("Corrected header column {}: {}", index, def.getExpectedName());
            }
        });

        // Create new header row
        TableRow correctedHeaderRow = new TableRow(
                headerRow.getRowIndex(),
                correctedHeaders,
                true
        );

        log.info("Created corrected header row: {}", correctedHeaderRow);

        // Validate and correct data rows
        List<TableRow> correctedDataRows = scannedRows.stream()
                .filter(row -> !row.isHeader())
                .map(row -> correctDataRow(row, template))
                .collect(Collectors.toList());

        log.info("Corrected {} data rows", correctedDataRows.size());

        // Combine corrected header and data rows
        List<TableRow> result = new ArrayList<>();
        result.add(correctedHeaderRow);
        result.addAll(correctedDataRows);

        log.info("Validation complete. Final row count: {}", result.size());
        return result;
    }

    private TableRow correctDataRow(TableRow row, LogbookTemplate template) {
        log.debug("Correcting data row {}", row.getRowIndex());

        // Start with all existing data
        Map<Integer, String> correctedData = new HashMap<>(row.getColumnData());

        // Apply corrections only to template-defined columns
        template.getExpectedHeaders().forEach((index, def) -> {
            String value = row.getColumnData().get(index);
            if (value != null) {
                String corrected = correctValue(value, def.getDataType());
                correctedData.put(index, corrected);
                log.debug("Corrected column {} from '{}' to '{}'", index, value, corrected);
            }
        });

        TableRow correctedRow = new TableRow(row.getRowIndex(), correctedData, false);
        log.debug("Corrected row result: {}", correctedRow);
        return correctedRow;
    }

    private String correctValue(String value, LogbookTemplate.DataType type) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        try {
            switch (type) {
                case NUMBER:
                    return correctNumberValue(value.trim());
                case DATE:
                    return correctDateValue(value.trim());
                case TIME:
                    return correctTimeValue(value.trim());
                default:
                    return value.trim();
            }
        } catch (Exception e) {
            log.warn("Failed to correct value '{}' of type {}: {}", value, type, e.getMessage());
            return value;
        }
    }

    private String correctNumberValue(String value) {
        StringBuilder corrected = new StringBuilder();
        for (char c : value.toCharArray()) {
            corrected.append(REPLACEMENTS.getOrDefault(c, String.valueOf(c)));
        }
        return corrected.toString().replaceAll("[^0-9.]", "");
    }

    private String correctDateValue(String value) {
        try {
            String[] parts = value.split("/");
            if (parts.length >= 2) {
                int month = Integer.parseInt(parts[0]);
                int day = Integer.parseInt(parts[1]);

                // Basic validation
                month = Math.min(Math.max(month, 1), 12);
                day = Math.min(Math.max(day, 1), 31);

                return String.format("%02d/%02d", month, day);
            }
        } catch (Exception e) {
            log.warn("Failed to correct date value: {}", value);
        }
        return value;
    }

    private String correctTimeValue(String value) {
        try {
            // Remove any non-numeric characters except : and .
            value = value.replaceAll("[^0-9:.]", "");

            String[] parts = value.split("[:|.]");
            if (parts.length == 2) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);

                // Basic validation
                hours = Math.min(Math.max(hours, 0), 23);
                minutes = Math.min(Math.max(minutes, 0), 59);

                return String.format("%d.%02d", hours, minutes);
            }
        } catch (Exception e) {
            log.warn("Failed to correct time value: {}", value);
        }
        return value;
    }
}
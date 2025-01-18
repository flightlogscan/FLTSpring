package com.flt.fltspring.model;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Service to validate and correct scanned data
@Service
@RequiredArgsConstructor
public class LogbookValidationService {
    private final LogbookTemplateService templateService;

    public List<TableRow> validateAndCorrect(List<TableRow> scannedRows, LogbookType type) {
        LogbookTemplate template = templateService.getTemplate(type);
        if (template == null) {
            return scannedRows; // Return original if no template exists
        }

        // Find header row
        TableRow headerRow = scannedRows.stream()
                .filter(TableRow::isHeader)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No header row found"));

        // Create corrected header row based on template
        Map<Integer, String> correctedHeaders = new HashMap<>();
        template.getExpectedHeaders().forEach((index, def) -> {
            String scannedHeader = headerRow.getColumnData().get(index);
            if (scannedHeader != null) {
                // Compare scanned header with template
                if (headerMatches(scannedHeader, def.getExpectedName())) {
                    correctedHeaders.put(index, def.getExpectedName());
                } else {
                    correctedHeaders.put(index, def.getExpectedName()); // Use template header
                }
            } else if (def.isRequired()) {
                correctedHeaders.put(index, def.getExpectedName());
            }
        });

        // Create new header row
        TableRow correctedHeaderRow = new TableRow(
                headerRow.getRowIndex(),
                correctedHeaders,
                true
        );

        // Validate and correct data rows
        List<TableRow> correctedDataRows = scannedRows.stream()
                .filter(row -> !row.isHeader())
                .map(row -> correctDataRow(row, template))
                .collect(Collectors.toList());

        // Combine corrected header and data rows
        List<TableRow> result = new ArrayList<>();
        result.add(correctedHeaderRow);
        result.addAll(correctedDataRows);
        return result;
    }

    private boolean headerMatches(String scanned, String expected) {
        // Implement fuzzy matching logic
        // Could use Levenshtein distance or other similarity metrics
        return scanned.equalsIgnoreCase(expected) ||
                scanned.replace(" ", "").equalsIgnoreCase(expected.replace(" ", ""));
    }

    private TableRow correctDataRow(TableRow row, LogbookTemplate template) {
        Map<Integer, String> correctedData = new HashMap<>();

        template.getExpectedHeaders().forEach((index, def) -> {
            String value = row.getColumnData().get(index);
            if (value != null) {
                correctedData.put(index, correctValue(value, def.getDataType()));
            }
        });

        return new TableRow(row.getRowIndex(), correctedData, false);
    }

    private String correctValue(String value, LogbookTemplate.DataType type) {
        switch (type) {
            case NUMBER:
                return correctNumberValue(value);
            case DATE:
                return correctDateValue(value);
            case TIME:
                return correctTimeValue(value);
            default:
                return value;
        }
    }

    private String correctNumberValue(String value) {
        // Implement number correction logic
        // e.g., convert "O" to "0", "l" to "1", etc.
        return value.replace("O", "0")
                .replace("l", "1")
                .replace("I", "1");
    }

    private String correctDateValue(String value) {
        // Implement date format correction logic
        return value; // Placeholder
    }

    private String correctTimeValue(String value) {
        // Implement time format correction logic
        return value; // Placeholder
    }
}

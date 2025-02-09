package com.flt.fltspring.model;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.DocumentTable;
import com.azure.ai.documentintelligence.models.DocumentTableCell;
import com.azure.ai.documentintelligence.models.DocumentTableCellKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAnalysisService {
    private final LogbookValidationService validationService;
    private final TableDataTransformer transformer;

    private static final List<String> UNWANTED_STRINGS = List.of(
            "I certify that",
            "TOTALS",
            "AMT. FORWARDED"
    );

    private static final Map<String, String> CHARACTER_REPLACEMENTS = new HashMap<String, String>() {{
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

    public TableResponseDTO analyzeDocument(AnalyzeResult analyzeResult, LogbookType logbookType) {
        log.info("Starting document analysis for logbook type: {}", logbookType);
        List<TableRow> tableRows = convertToTableRows(analyzeResult);
        return processTableRows(tableRows, logbookType);
    }

    public TableResponseDTO processTableRows(List<TableRow> tableRows, LogbookType logbookType) {
        log.info("Processing {} table rows", tableRows.size());
        log.info("Input rows: {}", tableRows);
        List<TableRow> transformedRows = transformer.transformData(tableRows);
        log.info("After transformation: {} rows", transformedRows.size());
        log.info("Transformed rows: {}", transformedRows);
        List<TableRow> validatedRows = validationService.validateAndCorrect(transformedRows, logbookType);
        log.info("After validation: {} rows", validatedRows.size());
        log.info("Validated rows: {}", validatedRows);
        return mapToResponse(validatedRows);
    }

    private List<TableRow> convertToTableRows(AnalyzeResult analyzeResult) {
        final List<TableRow> allTableRows = new ArrayList<>();

        if (analyzeResult.getTables() == null) {
            log.warn("No tables found in analyze result");
            return allTableRows;
        }

        log.info("Found {} tables in analyze result", analyzeResult.getTables().size());

        int columnOffset = 0;
        int tableIndex = 0;
        for (DocumentTable documentTable : analyzeResult.getTables()) {
            log.info("====== Starting Table Processing ======");
            log.info("Raw table data: {}", documentTable);
            log.info("Processing table {} - Rows: {}, Columns: {}",
                    tableIndex++, documentTable.getRowCount(), documentTable.getColumnCount());

            // Create a single map for all rows in this table
            Map<Integer, Map<Integer, String>> consolidatedRows = new HashMap<>();
            Map<Integer, Boolean> headerRows = new HashMap<>();

            // First pass: collect all cells for each row
            for (DocumentTableCell cell : documentTable.getCells()) {
                int rowIndex = cell.getRowIndex();
                int columnIndex = columnOffset + cell.getColumnIndex();
                String content = cell.getContent();
                boolean isHeader = DocumentTableCellKind.COLUMN_HEADER.equals(cell.getKind());

                log.info("Processing cell - Row: {}, Col: {}, Content: {}, IsHeader: {}",
                        rowIndex, columnIndex, content, isHeader);

                // Don't skip header rows anymore
                if (isHeader) {
                    headerRows.put(rowIndex, true);
                }

                // Get or create the row's data map
                Map<Integer, String> rowContent = consolidatedRows.computeIfAbsent(rowIndex, k -> new HashMap<>());

                // Always add content, even if empty
                if (content != null && !content.trim().isEmpty()) {
                    rowContent.put(columnIndex, content.trim());
                    log.info("Added content at row {} column {}: {}",
                            rowIndex, columnIndex, content.trim());
                }
            }

            log.info("Consolidated rows before filtering: {}", consolidatedRows);

            // Second pass: create TableRow objects for non-empty rows
            consolidatedRows.forEach((rowIndex, content) -> {
                // Create row for all non-empty content, including headers
                if (!isRowEmpty(content) && !containsUnwantedStrings(content)) {
                    log.info("Creating TableRow for row {} with content: {}", rowIndex, content);
                    TableRow tableRow = new TableRow(
                            rowIndex,
                            new HashMap<>(content),
                            headerRows.getOrDefault(rowIndex, false)
                    );
                    allTableRows.add(tableRow);
                } else {
                    log.info("Skipping row {} - Empty: {}, Unwanted: {}",
                            rowIndex, isRowEmpty(content), containsUnwantedStrings(content));
                }
            });

            columnOffset += documentTable.getColumnCount();
            log.info("Column offset after table {}: {}", tableIndex - 1, columnOffset);
        }

        List<TableRow> sortedRows = allTableRows.stream()
                .sorted(Comparator.comparingInt(TableRow::getRowIndex))
                .collect(Collectors.toList());

        log.info("Final row count: {}", sortedRows.size());
        sortedRows.forEach(row ->
                log.info("Row {}: Header={}, Content={}", row.getRowIndex(), row.isHeader(), row.getColumnData()));

        return sortedRows;
    }

    private TableResponseDTO mapToResponse(List<TableRow> rows) {
        log.info("Mapping {} rows to response", rows.size());
        TableResponseDTO response = new TableResponseDTO();
        List<RowDTO> dtos = rows.stream()
                .map(this::convertToRowDTO)
                .filter(dto -> dto != null)  // Only filter null DTOs, keep empty ones
                .collect(Collectors.toList());
        log.info("Converted to {} DTOs", dtos.size());
        log.info("Final DTOs: {}", dtos);
        response.setRows(dtos);
        return response;
    }

    private RowDTO convertToRowDTO(TableRow row) {
        if (row == null) {
            log.info("Skipping null row");
            return null;
        }

        log.info("Converting row to DTO: {}", row);
        RowDTO dto = new RowDTO();
        dto.setRowIndex(row.getRowIndex());

        Map<Integer, String> formattedContent = new HashMap<>();
        row.getColumnData().forEach((key, value) -> {
            if (value != null && !value.trim().isEmpty()) {
                String refinedValue = row.isHeader() ? value : refineCharacters(value);
                formattedContent.put(key, refinedValue);
                log.info("Processed content - Column: {}, Original: '{}', Refined: '{}'",
                        key, value, refinedValue);
            }
        });

        dto.setContent(formattedContent);
        dto.setHeader(row.isHeader());
        log.info("Created DTO: {}", dto);
        return dto;
    }

    private boolean isRowEmpty(Map<Integer, String> rowData) {
        boolean isEmpty = rowData.values().stream()
                .allMatch(value -> value == null || value.trim().isEmpty());
        log.info("Row emptiness check: {} -> {}", rowData, isEmpty);
        return isEmpty;
    }

    private boolean containsUnwantedStrings(Map<Integer, String> rowData) {
        boolean containsUnwanted = rowData.values().stream()
                .anyMatch(value -> UNWANTED_STRINGS.stream()
                        .anyMatch(unwanted ->
                                value != null && value.toLowerCase().contains(unwanted.toLowerCase())));
        if (containsUnwanted) {
            log.info("Found unwanted strings in row: {}", rowData);
        }
        return containsUnwanted;
    }

    private String refineCharacters(String input) {
        String result = input;
        for (Map.Entry<String, String> replacement : CHARACTER_REPLACEMENTS.entrySet()) {
            if (result.contains(replacement.getKey())) {
                log.info("Replacing '{}' with '{}' in '{}'",
                        replacement.getKey(), replacement.getValue(), result);
                result = result.replace(replacement.getKey(), replacement.getValue());
            }
        }
        return result;
    }
}
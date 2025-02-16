package com.flt.fltspring.model;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.DocumentTableCell;
import com.azure.ai.documentintelligence.models.DocumentTableCellKind;
import com.flt.fltspring.ImageAnalyzerDummyRestController.DummyAnalyzeResult;
import com.flt.fltspring.ImageAnalyzerDummyRestController.DummyCell;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAnalysisService {
    private final LogbookValidationService validationService;
    private final TableDataTransformer transformer;
    private List<TableRow> rows;

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ColumnConfig {
        private String fieldName;
        private int columnCount;
        private String type;
    }

    private static final List<ColumnConfig> COLUMN_CONFIGS = List.of(
            new ColumnConfig("DATE", 1, "STRING"),
            new ColumnConfig("AIRCRAFT TYPE", 1, "STRING"),
            new ColumnConfig("AIRCRAFT IDENT", 1, "STRING"),
            new ColumnConfig("FROM", 1, "STRING"),
            new ColumnConfig("TO", 1, "STRING"),
            new ColumnConfig("NR INST. APP.", 1, "STRING"),
            new ColumnConfig("REMARKS AND ENDORSEMENTS", 1, "STRING"),
            new ColumnConfig("NR T/O", 1, "INTEGER"),
            new ColumnConfig("NR LDG", 1, "INTEGER"),
            new ColumnConfig("SINGLE-ENGINE LAND (DAY)", 1, "INTEGER"),
            new ColumnConfig("SINGLE-ENGINE LAND (NIGHT)", 1, "INTEGER"),
            new ColumnConfig("MULTI-ENGINE LAND", 2, "INTEGER"),
            new ColumnConfig("AND CLASS", 4, "STRING"),
            new ColumnConfig("NIGHT", 2, "INTEGER"),
            new ColumnConfig("ACTUAL INSTRUMENT", 2, "INTEGER"),
            new ColumnConfig("SIMULATED INSTRUMENT (HOOD)", 2, "INTEGER"),
            new ColumnConfig("FLIGHT SIMULATOR", 2, "INTEGER"),
            new ColumnConfig("CROSS COUNTRY", 2, "INTEGER"),
            new ColumnConfig("AS FLIGHT INSTRUCTOR", 2, "INTEGER"),
            new ColumnConfig("DUAL RECEIVED", 2, "INTEGER"),
            new ColumnConfig("PILOT IN COMMAND (INCL. SOLO)", 2, "INTEGER"),
            new ColumnConfig("TOTAL DURATION OF FLIGHT", 2, "INTEGER")
    );

    private static final Map<Integer, String> COLUMN_TYPES = new HashMap<>();

    // Initialize column types based on configuration
    static {
        // Map for tracking current column index for each field
        Map<String, Integer> startingColumns = new HashMap<>();
        int currentColumn = 0;

        for (ColumnConfig config : COLUMN_CONFIGS) {
            startingColumns.put(config.getFieldName(), currentColumn);
            for (int i = 0; i < config.getColumnCount(); i++) {
                COLUMN_TYPES.put(currentColumn + i, config.getType());
            }
            currentColumn += config.getColumnCount();
        }

        log.info("Initialized column types mapping: {}", COLUMN_TYPES);
    }

    public TableResponseDTO analyzeDocument(AnalyzeResult analyzeResult, LogbookType logbookType) {
        log.info("Starting document analysis for logbook type: {}", logbookType);
        List<TableRow> tableRows = convertToTableRows(analyzeResult);
        return processTableRows(tableRows, logbookType);
    }

    public TableResponseDTO analyzeDummyDocument(DummyAnalyzeResult dummyResult, LogbookType logbookType) {
        log.info("Starting dummy document analysis for logbook type: {}", logbookType);
        List<TableRow> tableRows = convertToTableRows(dummyResult);
        return processTableRows(tableRows, logbookType);
    }

    public TableResponseDTO processTableRows(List<TableRow> tableRows, LogbookType logbookType) {
        log.info("Processing {} table rows", tableRows.size());
        log.info("Input rows: {}", tableRows);
        this.rows = tableRows;
        List<TableRow> transformedRows = transformer.transformData(tableRows);
        log.info("After transformation: {} rows", transformedRows.size());
        log.info("Transformed rows: {}", transformedRows);
        List<TableRow> validatedRows = validationService.validateAndCorrect(transformedRows, logbookType);
        log.info("After validation: {} rows", validatedRows.size());
        log.info("Validated rows: {}", validatedRows);
        return mapToResponse(validatedRows);
    }

    // Interface to abstract table cell properties
    private interface TableCell {
        int getRowIndex();
        int getColumnIndex();
        String getContent();
        boolean isHeader();
    }

    // Adapter for DocumentTableCell
    private static class DocumentTableCellAdapter implements TableCell {
        private final DocumentTableCell cell;

        public DocumentTableCellAdapter(DocumentTableCell cell) {
            this.cell = cell;
        }

        @Override
        public int getRowIndex() {
            return cell.getRowIndex();
        }

        @Override
        public int getColumnIndex() {
            return cell.getColumnIndex();
        }

        @Override
        public String getContent() {
            return cell.getContent();
        }

        @Override
        public boolean isHeader() {
            return DocumentTableCellKind.COLUMN_HEADER.equals(cell.getKind());
        }
    }

    // Adapter for DummyCell
    private static class DummyCellAdapter implements TableCell {
        private final DummyCell cell;

        public DummyCellAdapter(DummyCell cell) {
            this.cell = cell;
        }

        @Override
        public int getRowIndex() {
            return cell.getRowIndex();
        }

        @Override
        public int getColumnIndex() {
            return cell.getColumnIndex();
        }

        @Override
        public String getContent() {
            return cell.getContent();
        }

        @Override
        public boolean isHeader() {
            return "columnHeader".equals(cell.getKind());
        }
    }

    private List<TableRow> convertToTableRows(AnalyzeResult analyzeResult) {
        if (analyzeResult.getTables() == null) {
            log.warn("No tables found in analyze result");
            return new ArrayList<>();
        }

        List<TableStructure> tableStructures = analyzeResult.getTables().stream()
                .map(table -> new TableStructure(
                        table.getRowCount(),
                        table.getColumnCount(),
                        table.getCells().stream()
                                .map(DocumentTableCellAdapter::new)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return processTableStructures(tableStructures);
    }

    private List<TableRow> convertToTableRows(DummyAnalyzeResult dummyResult) {
        if (dummyResult.getTables() == null) {
            log.warn("No tables found in dummy analyze result");
            return new ArrayList<>();
        }

        List<TableStructure> tableStructures = dummyResult.getTables().stream()
                .map(table -> new TableStructure(
                        table.getRowCount(),
                        table.getColumnCount(),
                        table.getCells().stream()
                                .map(DummyCellAdapter::new)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return processTableStructures(tableStructures);
    }

    private static class TableStructure {
        private final int rowCount;
        private final int columnCount;
        private final List<TableCell> cells;

        public TableStructure(int rowCount, int columnCount, List<TableCell> cells) {
            this.rowCount = rowCount;
            this.columnCount = columnCount;
            this.cells = cells;
        }
    }

    private boolean shouldSkipTable(TableStructure table) {
        // Check if any cell in the table contains unwanted strings
        return table.cells.stream()
                .map(TableCell::getContent)
                .anyMatch(content ->
                        content != null && UNWANTED_STRINGS.stream()
                                .anyMatch(unwanted ->
                                        content.toLowerCase().contains(unwanted.toLowerCase())));
    }

    private List<TableRow> processTableStructures(List<TableStructure> tableStructures) {
        final List<TableRow> allTableRows = new ArrayList<>();
        final Map<Integer, Map<Integer, String>> consolidatedHeaders = new HashMap<>();

        log.info("Found {} tables to process", tableStructures.size());

        int columnOffset = 0;
        int tableIndex = 0;

        for (TableStructure table : tableStructures) {
            if (shouldSkipTable(table)) {
                log.info("Skipping table {} as it contains unwanted strings", tableIndex);
                tableIndex++;
                continue;
            }

            log.info("====== Starting Table Processing ======");
            log.info("Processing table {} - Rows: {}, Columns: {}",
                    tableIndex++, table.rowCount, table.columnCount);

            // Create a single map for all rows in this table
            Map<Integer, Map<Integer, String>> consolidatedRows = new HashMap<>();
            Map<Integer, Boolean> headerRows = new HashMap<>();

            // First pass: collect all cells for each row
            for (TableCell cell : table.cells) {
                int rowIndex = cell.getRowIndex();
                int columnIndex = columnOffset + cell.getColumnIndex();
                String content = cell.getContent();
                boolean isHeader = cell.isHeader();

                log.info("Processing cell - Row: {}, Col: {}, Content: {}, IsHeader: {}",
                        rowIndex, columnIndex, content, isHeader);

                // Special handling for headers - consolidate them into row 0
                if (isHeader) {
                    Map<Integer, String> headerContent = consolidatedHeaders.computeIfAbsent(0, k -> new HashMap<>());
                    if (content != null && !content.trim().isEmpty()) {
                        headerContent.put(columnIndex, content.trim());
                        log.info("Added header content at column {}: {}", columnIndex, content.trim());
                    }
                    continue; // Skip adding headers to regular rows
                }

                Map<Integer, String> rowContent = consolidatedRows.computeIfAbsent(rowIndex, k -> new HashMap<>());

                if (content != null && !content.trim().isEmpty()) {
                    rowContent.put(columnIndex, content.trim());
                    log.info("Added content at row {} column {}: {}",
                            rowIndex, columnIndex, content.trim());
                }
            }

            log.info("Consolidated rows before filtering: {}", consolidatedRows);

            // Second pass: create TableRow objects for non-empty rows
            consolidatedRows.forEach((rowIndex, content) -> {
                if (!isRowEmpty(content) && !containsUnwantedStrings(content)) {
                    log.info("Creating TableRow for row {} with content: {}", rowIndex, content);
                    TableRow tableRow = new TableRow(
                            rowIndex,
                            new HashMap<>(content),
                            false // All non-header content goes here
                    );
                    allTableRows.add(tableRow);
                } else {
                    log.info("Skipping row {} - Empty: {}, Unwanted: {}",
                            rowIndex, isRowEmpty(content), containsUnwantedStrings(content));
                }
            });

            columnOffset += table.columnCount;
            log.info("Column offset after table {}: {}", tableIndex - 1, columnOffset);
        }

        // Add consolidated headers as first row if they exist
        if (!consolidatedHeaders.isEmpty()) {
            Map<Integer, String> headerContent = consolidatedHeaders.get(0);
            if (!headerContent.isEmpty()) {
                TableRow headerRow = new TableRow(
                        0,
                        new HashMap<>(headerContent),
                        true
                );
                allTableRows.add(0, headerRow);
                log.info("Added consolidated header row: {}", headerRow);
            }
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
                .filter(dto -> dto != null)
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

        if (row.isHeader()) {
            // For header rows, duplicate headers for related columns
            Map<Integer, String> headerContent = row.getColumnData();

            // Get all data columns from non-header rows to ensure headers cover all data columns
            Set<Integer> dataColumns = rows.stream()
                    .filter(r -> !r.isHeader())
                    .flatMap(r -> r.getColumnData().keySet().stream())
                    .collect(Collectors.toSet());

            // Sort header keys to process them in order
            List<Integer> sortedHeaderKeys = new ArrayList<>(headerContent.keySet());
            Collections.sort(sortedHeaderKeys);

            for (int i = 0; i < sortedHeaderKeys.size(); i++) {
                int currentKey = sortedHeaderKeys.get(i);
                String currentValue = headerContent.get(currentKey);

                if (currentValue != null && !currentValue.trim().isEmpty()) {
                    formattedContent.put(currentKey, currentValue);

                    // Find next header column
                    int nextHeaderKey = (i + 1 < sortedHeaderKeys.size())
                            ? sortedHeaderKeys.get(i + 1)
                            : Integer.MAX_VALUE;

                    // Add same header for all columns with data until next header
                    for (int col = currentKey + 1; col < nextHeaderKey; col++) {
                        if (dataColumns.contains(col)) {
                            formattedContent.put(col, currentValue);
                            log.debug("Duplicated header '{}' to column {}", currentValue, col);
                        }
                    }
                }
            }
        } else {
            // For data rows, just add refined values
            row.getColumnData().forEach((key, value) -> {
                if (value != null && !value.trim().isEmpty()) {
                    formattedContent.put(key, refineCharacters(value, key));
                }
            });
        }

        dto.setContent(formattedContent);
        dto.setHeader(row.isHeader());
        log.info("Created DTO: {}", dto);
        return dto;
    }

    private String findHeaderKey(Integer columnIndex) {
        // Find the last header key that's less than or equal to this column index
        return COLUMN_CONFIGS.stream()
                .map(config -> config.getFieldName())
                .map(String::valueOf)
                .filter(key -> {
                    int headerIndex = Integer.parseInt(key);
                    return headerIndex <= columnIndex;
                })
                .max(Comparator.comparingInt(Integer::parseInt))
                .orElse(String.valueOf(columnIndex));
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

    private String refineCharacters(String input, Integer columnIndex) {
        String columnType = COLUMN_TYPES.getOrDefault(columnIndex, "STRING");

        // Only apply character replacements for INTEGER type columns
        if (!"INTEGER".equals(columnType)) {
            return input;
        }

        String result = input;
        for (Map.Entry<String, String> replacement : CHARACTER_REPLACEMENTS.entrySet()) {
            if (result.contains(replacement.getKey())) {
                log.info("Replacing '{}' with '{}' in '{}' for INTEGER column {}",
                        replacement.getKey(), replacement.getValue(), result, columnIndex);
                result = result.replace(replacement.getKey(), replacement.getValue());
            }
        }
        return result;
    }
}
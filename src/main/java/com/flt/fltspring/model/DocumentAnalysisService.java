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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    @Autowired
    private ColumnConfig[] columnConfigs;

    private List<TableRow> rows;
    private Map<String, ColumnConfig> configMap;

    private static final List<String> UNWANTED_STRINGS = List.of(
            "I certify that",
            "TOTALS",
            "AMT. FORWARDED"
    );

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnConfig {
        private String fieldName;
        private int columnCount;
        private String type;
    }

    @Autowired
    public void init() {
        this.configMap = new HashMap<>();
        for (ColumnConfig config : columnConfigs) {
            configMap.put(config.getFieldName().toUpperCase(), config);
            if (config.getFieldName().contains("(")) {
                String simpleName = config.getFieldName().substring(0, config.getFieldName().indexOf("(")).trim();
                configMap.put(simpleName.toUpperCase(), config);
            }
        }
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

    private interface TableCell {
        int getRowIndex();
        int getColumnIndex();
        String getContent();
        boolean isHeader();
    }

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

    private List<TableRow> convertToTableRows(final AnalyzeResult analyzeResult) {
        if (analyzeResult.getTables() == null) {
            log.warn("No tables found in analyze result");
            return new ArrayList<>();
        }

        final List<TableStructure> tableStructures = analyzeResult.getTables().stream()
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

        final List<TableStructure> tableStructures = dummyResult.getTables().stream()
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
        return table.cells.stream()
                .map(TableCell::getContent)
                .anyMatch(content ->
                        content != null && UNWANTED_STRINGS.stream()
                                .anyMatch(unwanted ->
                                        content.toLowerCase().contains(unwanted.toLowerCase())));
    }

    private List<TableRow> processTableStructures(List<TableStructure> tableStructures) {
        final List<TableRow> allTableRows = new ArrayList<>();
        log.info("Found {} tables to process", tableStructures.size());

        Map<Integer, String> consolidatedHeaders = new HashMap<>();
        Map<Integer, String> consolidatedData = new HashMap<>();

        int tableIndex = 0;
        int columnOffset = 0;

        for (TableStructure table : tableStructures) {
            if (shouldSkipTable(table)) {
                log.info("Skipping table {} as it contains unwanted strings", tableIndex);
                tableIndex++;
                continue;
            }

            log.info("====== Starting Table Processing ======");
            log.info("Processing table {} - Rows: {}, Columns: {}", tableIndex++, table.rowCount, table.columnCount);

            // Group cells by row
            Map<Integer, List<TableCell>> rowGroups = table.cells.stream()
                    .collect(Collectors.groupingBy(TableCell::getRowIndex));

            // Find the header row index
            int headerRowIndex = rowGroups.entrySet().stream()
                    .filter(entry -> entry.getValue().stream()
                            .anyMatch(cell -> cell.isHeader() ||
                                    (cell.getContent() != null && isKnownHeader(cell.getContent().trim()))))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(-1);

            rowGroups.entrySet().forEach(entry -> {
                    log.info("Key for entry: {}", entry.getKey());
            entry.getValue().stream().forEach(inside -> {
                log.info(inside.getContent());
                log.info("Is header: {}", inside.isHeader());
            });
            });

            // Process header row first
            if (headerRowIndex >= 0) {
                List<TableCell> headerCells = rowGroups.get(headerRowIndex);
                for (TableCell cell : headerCells) {
                    String content = cell.getContent();
                    if (content != null && !content.trim().isEmpty()) {
                        String normalizedContent = normalizeHeaderContent(content.trim());
                        log.info("Normalized content: {}", normalizedContent);
                        if (configMap.containsKey(normalizedContent)) {
                            log.info("Contained key: {}", normalizedContent);
                            ColumnConfig config = configMap.get(normalizedContent);
                            consolidatedHeaders.put(cell.getColumnIndex() + columnOffset, config.getFieldName());

                            if (config.getColumnCount() > 1) {
                                for (int i = 1; i < config.getColumnCount(); i++) {
                                    consolidatedHeaders.put(cell.getColumnIndex() + columnOffset + i, config.getFieldName());
                                }
                            }
                        }
                    }
                }
            }

            // Process data rows
            int finalColumnOffset = columnOffset;
            rowGroups.forEach((rowIndex, cells) -> {
                if (rowIndex != headerRowIndex) {
                    for (TableCell cell : cells) {
                        String content = cell.getContent();
                        if (content != null && !content.trim().isEmpty()) {
                            consolidatedData.put(cell.getColumnIndex() + finalColumnOffset, content.trim());
                        }
                    }
                }
            });

            columnOffset += table.columnCount;
        }

        // Create final TableRow objects
        if (!consolidatedHeaders.isEmpty()) {
            allTableRows.add(new TableRow(0, new HashMap<>(consolidatedHeaders), true));
        }
        if (!consolidatedData.isEmpty()) {
            allTableRows.add(new TableRow(1, new HashMap<>(consolidatedData), false));
        }

        return allTableRows;
    }

    private String normalizeHeaderContent(String content) {
        if (content == null) return "";

        String normalized = content.toUpperCase().trim();

        // Handle known header variations
        if (normalized.equals("FROM/TO") || normalized.equals("ROUTE OF FLIGHT")) {
            return "FROM";
        }
        if (normalized.equals("AIRCRAFT CATEGORY") || normalized.equals("CATEGORY AND CLASS")) {
            return "SINGLE-ENGINE LAND";
        }

        // Remove parenthetical clauses
        if (normalized.contains("(")) {
            normalized = normalized.substring(0, normalized.indexOf("(")).trim();
        }

        return normalized;
    }

    private boolean isKnownHeader(String content) {
        log.info("Checking for known header against content: {}", content);
        return configMap.containsKey(normalizeHeaderContent(content));
    }

    private TableResponseDTO mapToResponse(List<TableRow> rows) {
        log.info("Mapping {} rows to response", rows.size());
        final TableResponseDTO response = new TableResponseDTO();
        final List<RowDTO> dtos = rows.stream()
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
        final RowDTO dto = new RowDTO();
        dto.setRowIndex(row.getRowIndex());
        dto.setContent(row.getColumnData());
        dto.setHeader(row.isHeader());
        log.info("Created DTO: {}", dto);
        return dto;
    }
}
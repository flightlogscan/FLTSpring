package com.flt.fltspring.service;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.flt.fltspring.model.DocumentTableCellAdapter;
import com.flt.fltspring.model.RowDTO;
import com.flt.fltspring.model.TableResponseDTO;
import com.flt.fltspring.model.TableRow;
import com.flt.fltspring.model.dummy.DummyAnalyzeResult;
import com.flt.fltspring.model.dummy.DummyCellAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.flt.fltspring.model.TableCell;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAnalysisService {
    private final LogbookValidationService validationService;
    private final TableDataTransformerService transformer;

    private static final List<String> UNWANTED_STRINGS = List.of(
            "I certify that",
            "TOTALS",
            "AMT. FORWARDED"
    );

    public TableResponseDTO analyzeDocument(AnalyzeResult analyzeResult) {
        List<TableRow> tableRows = convertToTableRows(analyzeResult);
        return processTableRows(tableRows);
    }

    public TableResponseDTO analyzeDummyDocument(DummyAnalyzeResult dummyResult) {
        List<TableRow> tableRows = convertToTableRows(dummyResult);
        return processTableRows(tableRows);
    }

    public TableResponseDTO processTableRows(List<TableRow> tableRows) {
        log.info("Processing {} table rows", tableRows.size());
        log.info("Input rows: {}", tableRows);
        List<TableRow> transformedRows = transformer.transformData(tableRows);
        log.info("After transformation: {} rows", transformedRows.size());
        log.info("Transformed rows: {}", transformedRows);
        List<TableRow> validatedRows = validationService.validateAndCorrect(transformedRows);
        log.info("After validation: {} rows", validatedRows.size());
        log.info("Validated rows: {}", validatedRows);
        return mapToResponse(validatedRows);
    }



    private List<TableRow> convertToTableRows(final AnalyzeResult analyzeResult) {
        if (analyzeResult.getTables() == null) {
            log.warn("No tables found in analyze result");
            return new ArrayList<>();
        }

        final List<TableStructure> tableStructures = analyzeResult.getTables().stream()
                .map(table -> new TableStructure(
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
                        table.getColumnCount(),
                        table.getCells().stream()
                                .map(DummyCellAdapter::new)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return processTableStructures(tableStructures);
    }

    private static class TableStructure {
        private final int columnCount;
        private final List<TableCell> cells;

        public TableStructure(int columnCount, List<TableCell> cells) {
            this.columnCount = columnCount;
            this.cells = cells;
        }
    }
    private String cleanContent(String content) {
        if (content == null) return null;
        return content.replaceAll("\\r?\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
    private boolean shouldSkipTable(TableStructure table, List<TableStructure> allTables) {
        if (allTables.size() <= 2) {
            return false;
        }
        return table.cells.stream()
                .map(TableCell::getContent)
                .anyMatch(content ->
                        content != null && UNWANTED_STRINGS.stream()
                                .anyMatch(unwanted ->
                                        content.toLowerCase().contains(unwanted.toLowerCase())));
    }

    private boolean shouldSkipRow(List<TableCell> rowCells) {
        return rowCells.stream()
                .map(TableCell::getContent)
                .anyMatch(content ->
                        content != null && UNWANTED_STRINGS.stream()
                                .anyMatch(unwanted ->
                                        content.toLowerCase().contains(unwanted.toLowerCase())));
    }

    private List<TableRow> processTableStructures(List<TableStructure> tableStructures) {
        final List<TableRow> allTableRows = new ArrayList<>();
        Map<Integer, String> consolidatedHeaders = new HashMap<>();
        Map<Integer, Map<Integer, String>> dataRowsByIndex = new HashMap<>(); // Key is row index, value is the row data
        Set<Integer> allDataColumns = new HashSet<>();  // Track all column indices that have data

        int columnOffset = 0;

        for (TableStructure table : tableStructures) {
            if (shouldSkipTable(table, tableStructures)) {
                continue;
            }

            // Group cells by row
            Map<Integer, List<TableCell>> rowGroups = table.cells.stream()
                    .collect(Collectors.groupingBy(TableCell::getRowIndex));

            List<Integer> rowIndices = new ArrayList<>(rowGroups.keySet());
            Collections.sort(rowIndices);

            if (!rowIndices.isEmpty()) {
                Map<Integer, String> row0Headers = new HashMap<>();
                Map<Integer, String> row1Headers = new HashMap<>();

                // Process row 0 headers
                if (rowGroups.containsKey(0)) {
                    List<TableCell> cells = rowGroups.get(0).stream()
                            .sorted(Comparator.comparingInt(TableCell::getColumnIndex))
                            .collect(Collectors.toList());

                    for (TableCell cell : cells) {
                        if (cell.getContent() != null && !cell.getContent().trim().isEmpty()) {
                            int globalIndex = cell.getColumnIndex() + columnOffset;
                            row0Headers.put(globalIndex, cleanContent(cell.getContent()));
                            log.info("Row 0 header at index {}: {}", globalIndex, cleanContent(cell.getContent()));
                        }
                    }
                }

                // Process row 1 headers
                if (rowGroups.containsKey(1)) {
                    List<TableCell> cells = rowGroups.get(1).stream()
                            .sorted(Comparator.comparingInt(TableCell::getColumnIndex))
                            .collect(Collectors.toList());

                    for (TableCell cell : cells) {
                        if (cell.getContent() != null && !cell.getContent().trim().isEmpty()) {
                            int globalIndex = cell.getColumnIndex() + columnOffset;
                            row1Headers.put(globalIndex, cleanContent(cell.getContent()));
                            log.info("Row 1 header at index {}: {}", globalIndex, cleanContent(cell.getContent()));
                        }
                    }
                }

                // Process data rows to collect all used column indices
                for (int rowIndex : rowIndices) {
                    if (rowIndex > 1) {
                        List<TableCell> cells = rowGroups.get(rowIndex).stream()
                                .sorted(Comparator.comparingInt(TableCell::getColumnIndex))
                                .collect(Collectors.toList());

                        if (!shouldSkipRow(cells)) {
                            Map<Integer, String> rowData = dataRowsByIndex.computeIfAbsent(rowIndex, k -> new HashMap<>());

                            for (TableCell cell : cells) {
                                if (cell.getContent() != null && !cell.getContent().trim().isEmpty()) {
                                    int globalIndex = cell.getColumnIndex() + columnOffset;
                                    String content = cleanContent(cell.getContent());
                                    rowData.put(globalIndex, content);
                                    allDataColumns.add(globalIndex);  // Track this column index
                                    log.info("Data from row {} at index {}: {}", rowIndex, globalIndex, content);
                                }
                            }
                        }
                    }
                }

                // Process headers with knowledge of all used data columns
                // First process row 0 headers to establish base headers
                for (Map.Entry<Integer, String> entry : row0Headers.entrySet()) {
                    int index = entry.getKey();
                    String header = entry.getValue();
                    consolidatedHeaders.put(index, header);
                    // Duplicate header if next column has data and isn't a row 1 header
                    if (allDataColumns.contains(index + 1) && !row1Headers.containsKey(index + 1)) {
                        consolidatedHeaders.put(index + 1, header);
                    }
                }

                // Then process row 1 headers which override row 0 headers for their specific positions
                for (Map.Entry<Integer, String> entry : row1Headers.entrySet()) {
                    int index = entry.getKey();
                    String header = entry.getValue();
                    consolidatedHeaders.put(index, header);
                    // Duplicate row 1 headers if next column has data and isn't another row 1 header position
                    if (allDataColumns.contains(index + 1) && !row1Headers.containsKey(index + 1)) {
                        consolidatedHeaders.put(index + 1, header);
                    }
                }
            }

            columnOffset += table.columnCount;
        }

        log.info("Final consolidated headers: {}", consolidatedHeaders);

        if (!consolidatedHeaders.isEmpty()) {
            allTableRows.add(new TableRow(0, new HashMap<>(consolidatedHeaders), true));
        }

        // Add each data row while preserving row order
        List<Integer> sortedRowIndices = new ArrayList<>(dataRowsByIndex.keySet());
        Collections.sort(sortedRowIndices);

        int rowIndex = 1;
        for (Integer originalRowIndex : sortedRowIndices) {
            Map<Integer, String> rowData = dataRowsByIndex.get(originalRowIndex);
            if (!rowData.isEmpty()) {  // Only add non-empty rows
                log.info("Final consolidated data: {}", rowData);
                allTableRows.add(new TableRow(rowIndex++, rowData, false));
            }
        }

        return allTableRows;
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
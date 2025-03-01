package com.flt.fltspring.service;

import com.flt.fltspring.model.TableCell;
import com.flt.fltspring.model.TableRow;
import com.flt.fltspring.model.TableStructure;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class TableProcessorService {

    private static final List<String> UNWANTED_STRINGS = List.of(
            "I certify that",
            "TOTALS",
            "AMT. FORWARDED"
    );

    public List<TableRow> processTables(List<TableStructure> tableStructures) {
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
            Map<Integer, List<TableCell>> rowGroups = table.getCells().stream()
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
                            .toList();

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
                            .toList();

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

// Process headers with knowledge of all used data columns and columnSpan information
                // Map to track which columns are covered by a spanning header
                Set<Integer> columnsWithSpannedHeader = new HashSet<>();
                
                // First process row 0 headers to establish base headers
                for (TableCell cell : rowGroups.getOrDefault(0, Collections.emptyList())) {
                    if (cell.getContent() != null && !cell.getContent().trim().isEmpty()) {
                        int startIndex = cell.getColumnIndex() + columnOffset;
                        int columnSpan = cell.getColumnSpan();
                        String header = cleanContent(cell.getContent());
                        
                        // Apply the header to all columns it spans
                        for (int i = 0; i < columnSpan; i++) {
                            int spanIndex = startIndex + i;
                            // Only set if row1 doesn't have a header here
                            if (!row1Headers.containsKey(spanIndex)) {
                                consolidatedHeaders.put(spanIndex, header);
                                columnsWithSpannedHeader.add(spanIndex);
                                log.info("Row 0 header spans column {}: {}", spanIndex, header);
                            }
                        }
                    }
                }

                // Then process row 1 headers which override row 0 headers for their specific positions
                for (TableCell cell : rowGroups.getOrDefault(1, Collections.emptyList())) {
                    if (cell.getContent() != null && !cell.getContent().trim().isEmpty()) {
                        int startIndex = cell.getColumnIndex() + columnOffset;
                        int columnSpan = cell.getColumnSpan();
                        String header = cleanContent(cell.getContent());
                        
                        // Apply the header to all columns it spans
                        for (int i = 0; i < columnSpan; i++) {
                            int spanIndex = startIndex + i;
                            consolidatedHeaders.put(spanIndex, header);
                            columnsWithSpannedHeader.add(spanIndex);
                            log.info("Row 1 header spans column {}: {}", spanIndex, header);
                        }
                    }
                }
            }

            columnOffset += table.getColumnCount();
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
        return table.getCells().stream()
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
}

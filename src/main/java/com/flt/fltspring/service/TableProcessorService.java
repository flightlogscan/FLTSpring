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

        // Track parent-child header relationships
        Map<String, Set<String>> headerHierarchy = new HashMap<>(); // Maps parent headers to their child headers
        Map<Integer, String> columnToParentHeader = new HashMap<>(); // Maps column index to its parent header

        int columnOffset = 0;
        
        for (TableStructure table : tableStructures) {
            log.info("Processing table from page {}", table.getPageNumber());
            if (shouldSkipTable(table, tableStructures)) {
                log.info("Skipping table from page {}", table.getPageNumber());
                continue;
            }

            // Group cells by row
            Map<Integer, List<TableCell>> rowGroups = table.getCells().stream()
                    .collect(Collectors.groupingBy(TableCell::getRowIndex));

            List<Integer> rowIndices = new ArrayList<>(rowGroups.keySet());
            Collections.sort(rowIndices);

            if (!rowIndices.isEmpty()) {
                // First, collect all row 0 headers (potential parent headers)
                Map<Integer, TableCell> row0HeaderCells = new HashMap<>();
                if (rowGroups.containsKey(0)) {
                    for (TableCell cell : rowGroups.get(0)) {
                        if (cell.getContent() != null && !cell.getContent().trim().isEmpty()) {
                            int startIdx = cell.getColumnIndex() + columnOffset;
                            row0HeaderCells.put(startIdx, cell);
                            String headerContent = cleanContent(cell.getContent());

                            // Initialize the set of child headers for this parent
                            headerHierarchy.putIfAbsent(headerContent, new HashSet<>());

                            // For headers that span multiple columns, mark all those columns
                            for (int i = 0; i < cell.getColumnSpan(); i++) {
                                int colIdx = startIdx + i;
                                columnToParentHeader.put(colIdx, headerContent);
                                log.info("Column {} has parent header: {}", colIdx, headerContent);
                            }
                        }
                    }
                }

                // Then, process row 1 headers and associate them with their parent headers
                Map<Integer, String> row1Headers = new HashMap<>();
                if (rowGroups.containsKey(1)) {
                    for (TableCell cell : rowGroups.get(1)) {
                        if (cell.getContent() != null && !cell.getContent().trim().isEmpty()) {
                            int startIdx = cell.getColumnIndex() + columnOffset;
                            String childHeader = cleanContent(cell.getContent());
                            row1Headers.put(startIdx, childHeader);

                            // Find the parent header for this column
                            String parentHeader = columnToParentHeader.get(startIdx);
                            if (parentHeader != null) {
                                // Associate this child header with its parent
                                headerHierarchy.get(parentHeader).add(childHeader);
                                log.info("Child header '{}' is under parent header '{}'", childHeader, parentHeader);
                            }

                            // For spanning headers, mark all spanned columns
                            for (int i = 0; i < cell.getColumnSpan(); i++) {
                                int colIdx = startIdx + i;
                                consolidatedHeaders.put(colIdx, childHeader);
                            }
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

                // Apply row 0 headers to consolidated headers where row 1 headers don't exist
                for (Map.Entry<Integer, TableCell> entry : row0HeaderCells.entrySet()) {
                    int startIndex = entry.getKey();
                    TableCell cell = entry.getValue();
                    String header = cleanContent(cell.getContent());

                    for (int i = 0; i < cell.getColumnSpan(); i++) {
                        int colIdx = startIndex + i;
                        // Only set if row 1 doesn't already have a header for this column
                        if (!row1Headers.containsKey(colIdx)) {
                            consolidatedHeaders.put(colIdx, header);
                            log.info("Row 0 header applied to column {}: {}", colIdx, header);
                        }
                    }
                }
            }

            columnOffset += table.getColumnCount();
        }

        log.info("Final consolidated headers: {}", consolidatedHeaders);
        log.info("Header hierarchy: {}", headerHierarchy);

        // Create a comprehensive parent headers map for each column
        Map<Integer, String> finalParentHeadersMap = new HashMap<>();

        // First, process direct parent-child relationships from the hierarchy
        for (Map.Entry<String, Set<String>> hierarchyEntry : headerHierarchy.entrySet()) {
            String parentHeader = hierarchyEntry.getKey();
            Set<String> childHeaders = hierarchyEntry.getValue();

            // Find all columns with these child headers
            for (Map.Entry<Integer, String> headerEntry : consolidatedHeaders.entrySet()) {
                int colIdx = headerEntry.getKey();
                String header = headerEntry.getValue();

                if (childHeaders.contains(header)) {
                    finalParentHeadersMap.put(colIdx, parentHeader);
                    log.info("Column {} with header '{}' has parent '{}'", colIdx, header, parentHeader);
                }
            }
        }

        // Then, fill in the direct parent mappings for any remaining columns
        for (Map.Entry<Integer, String> entry : consolidatedHeaders.entrySet()) {
            int colIdx = entry.getKey();

            // Only add if we don't already have a parent mapping
            if (!finalParentHeadersMap.containsKey(colIdx) && columnToParentHeader.containsKey(colIdx)) {
                finalParentHeadersMap.put(colIdx, columnToParentHeader.get(colIdx));
                log.info("Column {} with header '{}' has direct parent '{}'",
                        colIdx, consolidatedHeaders.get(colIdx), columnToParentHeader.get(colIdx));
            }
        }

        // Build a map from child header to parent header for easier lookup
        Map<String, String> childToParentMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : headerHierarchy.entrySet()) {
            String parentHeader = entry.getKey();
            for (String childHeader : entry.getValue()) {
                childToParentMap.put(childHeader, parentHeader);
            }
        }

        // Process adjacent columns to fix parent/child header issues
        processAdjacentColumns(consolidatedHeaders, finalParentHeadersMap, childToParentMap);

        if (!consolidatedHeaders.isEmpty()) {
            // Add headers row with its parent headers
            allTableRows.add(new TableRow(0, new HashMap<>(consolidatedHeaders), true, new HashMap<>(finalParentHeadersMap)));
        }

        // Add each data row while preserving row order
        List<Integer> sortedRowIndices = new ArrayList<>(dataRowsByIndex.keySet());
        Collections.sort(sortedRowIndices);

        int rowIndex = 1;
        for (Integer originalRowIndex : sortedRowIndices) {
            Map<Integer, String> rowData = dataRowsByIndex.get(originalRowIndex);
            if (!rowData.isEmpty()) {  // Only add non-empty rows
                log.info("Final consolidated data: {}", rowData);

                // Create new row with parent headers
                TableRow dataRow = TableRow.builder()
                        .rowIndex(rowIndex++)
                        .columnData(rowData)
                        .isHeader(false)
                        .parentHeaders(new HashMap<>(finalParentHeadersMap))
                        .build();

                allTableRows.add(dataRow);
            }
        }

        return allTableRows;
    }

    /**
     * Process adjacent columns to handle cases where a parent header appears as a column header
     * instead of the expected child header
     */
    private void processAdjacentColumns(Map<Integer, String> headers, Map<Integer, String> parentHeaders,
                                        Map<String, String> childToParentMap) {
        // Process columns with the same parent header that are adjacent
        for (int i = 0; i < Collections.max(headers.keySet()); i++) {
            // If we have two adjacent columns
            if (headers.containsKey(i) && headers.containsKey(i + 1)) {
                // If they have the same parent header
                if (parentHeaders.containsKey(i) && parentHeaders.containsKey(i + 1) &&
                        parentHeaders.get(i).equals(parentHeaders.get(i + 1))) {

                    // If the first column has a child header and the second has the parent header
                    if (headers.get(i) != null && headers.get(i + 1) != null &&
                            headers.get(i + 1).equals(parentHeaders.get(i))) {

                        // Copy the child header to the adjacent column
                        headers.put(i + 1, headers.get(i));
                        log.info("Fixed: Changed column {} header from '{}' to '{}'",
                                i + 1, parentHeaders.get(i), headers.get(i));
                    }
                }
            }
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
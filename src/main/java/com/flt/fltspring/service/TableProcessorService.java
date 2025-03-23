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
            if (shouldSkipTable(table, tableStructures)) {
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

        // Build a map from child header to parent header for easier lookup
        Map<String, String> childToParentMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : headerHierarchy.entrySet()) {
            String parentHeader = entry.getKey();
            for (String childHeader : entry.getValue()) {
                childToParentMap.put(childHeader, parentHeader);
            }
        }

        // Create a comprehensive parent headers map for each column
        Map<Integer, String> finalParentHeadersMap = new HashMap<>();

        // Process every column and set its parent header
        for (Map.Entry<Integer, String> entry : consolidatedHeaders.entrySet()) {
            int colIdx = entry.getKey();
            String header = entry.getValue();

            // If this header is a child in the hierarchy, use its parent
            if (childToParentMap.containsKey(header)) {
                finalParentHeadersMap.put(colIdx, childToParentMap.get(header));
                log.info("Column {} with header '{}' has parent '{}'", colIdx, header, childToParentMap.get(header));
            }
            // Otherwise check if we have a direct parent mapping
            else if (columnToParentHeader.containsKey(colIdx)) {
                finalParentHeadersMap.put(colIdx, columnToParentHeader.get(colIdx));
                log.info("Column {} with header '{}' has direct parent '{}'", colIdx, header, columnToParentHeader.get(colIdx));
            }
        }

        // Handle cases where parent header is repeated instead of child header
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
        // First, we'll process all adjacent columns to find patterns
        List<Integer> columnIndices = new ArrayList<>(headers.keySet());
        Collections.sort(columnIndices);

        // Iterate through all adjacent column pairs
        for (int i = 0; i < columnIndices.size() - 1; i++) {
            int currentCol = columnIndices.get(i);
            int nextCol = columnIndices.get(i + 1);

            // Only process if they're actually adjacent
            if (nextCol == currentCol + 1) {
                String currentHeader = headers.get(currentCol);
                String nextHeader = headers.get(nextCol);
                String currentParent = parentHeaders.get(currentCol);
                String nextParent = parentHeaders.get(nextCol);

                // Case 1: Current header is a specific value, next header is the parent
                // This is our main issue - e.g., "SINGLE-ENGINE LAND" followed by "AIRCRAFT CATEGORY"
                if (currentParent != null && nextHeader != null && nextHeader.equals(currentParent)) {
                    headers.put(nextCol, currentHeader);
                    log.info("Fixed: Changed column {} header from '{}' to '{}' based on adjacent column pattern",
                            nextCol, nextHeader, currentHeader);
                }

                // Case 2: Current header is a parent, next header is a child of that parent
                if (currentHeader != null && nextHeader != null && currentHeader.equals(nextParent)) {
                    headers.put(currentCol, nextHeader);
                    log.info("Fixed: Changed column {} header from '{}' to '{}' based on parent-child relationship",
                            currentCol, currentHeader, nextHeader);
                }

                // Case 3: Headers are the same but parents are different - should be consistent
                if (currentHeader != null && nextHeader != null &&
                        currentHeader.equals(nextHeader) &&
                        currentParent != null && nextParent != null &&
                        !currentParent.equals(nextParent)) {
                    // Use the more specific parent for both
                    parentHeaders.put(nextCol, currentParent);
                    log.info("Fixed: Changed column {} parent from '{}' to '{}' to maintain consistency",
                            nextCol, nextParent, currentParent);
                }
            }
        }

        // Group columns by parent header
        Map<String, List<Integer>> columnsByParent = new HashMap<>();
        for (Map.Entry<Integer, String> entry : parentHeaders.entrySet()) {
            int colIdx = entry.getKey();
            String parentHeader = entry.getValue();
            columnsByParent.computeIfAbsent(parentHeader, k -> new ArrayList<>()).add(colIdx);
        }

        // For each parent header, ensure all columns with that parent use the same header
        // if they appear in sequence
        for (Map.Entry<String, List<Integer>> entry : columnsByParent.entrySet()) {
            String parentHeader = entry.getKey();
            List<Integer> columns = entry.getValue();
            Collections.sort(columns);

            // Process consecutive columns with the same parent
            for (int i = 0; i < columns.size() - 1; i++) {
                int currentCol = columns.get(i);
                int nextCol = columns.get(i + 1);

                // If columns are adjacent
                if (nextCol == currentCol + 1) {
                    String currentHeader = headers.get(currentCol);
                    String nextHeader = headers.get(nextCol);

                    // If one header is the parent itself and the other isn't
                    if (currentHeader.equals(parentHeader) && !nextHeader.equals(parentHeader)) {
                        headers.put(currentCol, nextHeader);
                        log.info("Fixed: Changed column {} header from '{}' to '{}' for consistency",
                                currentCol, currentHeader, nextHeader);
                    } else if (!currentHeader.equals(parentHeader) && nextHeader.equals(parentHeader)) {
                        headers.put(nextCol, currentHeader);
                        log.info("Fixed: Changed column {} header from '{}' to '{}' for consistency",
                                nextCol, nextHeader, currentHeader);
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
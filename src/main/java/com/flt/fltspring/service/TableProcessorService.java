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
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TableProcessorService {

    private static final List<String> UNWANTED_STRINGS = List.of(
            "I certify that",
            "TOTALS",
            "AMT. FORWARDED",
            "LOGBOOK ENTRIES TYPE OF PILOT EXPERIENCE OR"
    );

    // Table-level unwanted strings (excluding logbook‐entries phrase)
    private static final List<String> UNWANTED_TABLE_STRINGS = List.of(
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

        // Reorder tables: process the table with the "DATE" header first for correct header ordering
        List<TableStructure> anchorTables = new ArrayList<>();
        List<TableStructure> otherTables = new ArrayList<>();
        for (TableStructure t : tableStructures) {
            boolean hasDate = t.getCells().stream()
                .map(TableCell::getContent)
                .map(this::cleanContent)
                .anyMatch(c -> "DATE".equalsIgnoreCase(c));
            if (hasDate) anchorTables.add(t);
            else otherTables.add(t);
        }
        List<TableStructure> sortedTables = new ArrayList<>();
        sortedTables.addAll(anchorTables);
        sortedTables.addAll(otherTables);
        tableStructures = sortedTables;

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

            // Determine where data rows start (row containing date-like content)
            int firstDataRowIndex = rowGroups.keySet().stream()
                .filter(idx -> rowGroups.get(idx).stream()
                    .anyMatch(cell -> cell.getContent() != null
                        && cell.getContent().matches("\\d{1,2}/\\d{1,2}")))
                .findFirst()
                .orElse(2);
            log.info("Detected firstDataRowIndex for table on page {}: {}", table.getPageNumber(), firstDataRowIndex);

            // Anchor-based header detection: start at the row containing "DATE"
            List<Integer> possibleHeaderRows = rowIndices.stream()
                .filter(idx -> idx < firstDataRowIndex)
                .filter(idx -> rowGroups.getOrDefault(idx, Collections.emptyList()).stream()
                    .map(TableCell::getContent)
                    .map(this::cleanContent)
                    .noneMatch(c -> UNWANTED_STRINGS.stream()
                        .anyMatch(unwanted ->
                            c.equalsIgnoreCase(unwanted) ||
                            c.toLowerCase().contains(unwanted.toLowerCase())
                        )
                    )
                )
                .collect(Collectors.toList());

            Optional<Integer> anchorHeaderIdxOpt = possibleHeaderRows.stream()
                .filter(idx -> rowGroups.getOrDefault(idx, Collections.emptyList()).stream()
                    .map(TableCell::getContent)
                    .map(this::cleanContent)
                    .anyMatch(c -> "DATE".equalsIgnoreCase(c)))
                .findFirst();

            List<Integer> headerRowIndices;
            if (anchorHeaderIdxOpt.isPresent()) {
                int anchorIdx = anchorHeaderIdxOpt.get();
                headerRowIndices = possibleHeaderRows.stream()
                    .filter(idx -> idx <= anchorIdx)
                    .sorted()
                    .collect(Collectors.toList());
            } else {
                // Fallback to all rows above data if no anchor found
                headerRowIndices = possibleHeaderRows;
            }

            if (!headerRowIndices.isEmpty()) {
                int topHeaderRowIdx = headerRowIndices.get(0);
                for (int headerRowIdx : headerRowIndices) {
                    for (TableCell cell : rowGroups.getOrDefault(headerRowIdx, Collections.emptyList())) {
                        if (cell.getContent() != null && !cell.getContent().trim().isEmpty()) {
                            int startIdx = cell.getColumnIndex() + columnOffset;
                            String headerText = cleanContent(cell.getContent());

                            // Top header row defines parent headers
                            if (headerRowIdx == topHeaderRowIdx) {
                                headerHierarchy.putIfAbsent(headerText, new HashSet<>());
                                for (int i = 0; i < cell.getColumnSpan(); i++) {
                                    columnToParentHeader.put(startIdx + i, headerText);
                                }
                            } else {
                                // Subsequent header rows define child headers under existing parents
                                for (int i = 0; i < cell.getColumnSpan(); i++) {
                                    int colIdx = startIdx + i;
                                    String parent = columnToParentHeader.get(colIdx);
                                    if (parent != null) {
                                        headerHierarchy.get(parent).add(headerText);
                                    }
                                }
                            }

                            // Consolidate the header (deepest header wins)
                            for (int i = 0; i < cell.getColumnSpan(); i++) {
                                consolidatedHeaders.put(startIdx + i, headerText);
                            }
                        }
                    }
                }
            }

            // Process data rows to collect all used column indices (grouping by relative data row)
            for (int rowIndex : rowIndices) {
                if (rowIndex >= firstDataRowIndex) {
                    List<TableCell> cells = rowGroups.getOrDefault(rowIndex, Collections.emptyList()).stream()
                            .sorted(Comparator.comparingInt(TableCell::getColumnIndex))
                            .collect(Collectors.toList());

                    if (!shouldSkipRow(cells)) {
                        int groupIndex = rowIndex - firstDataRowIndex;
                        Map<Integer, String> rowData = dataRowsByIndex.computeIfAbsent(groupIndex, k -> new HashMap<>());

                        for (TableCell cell : cells) {
                            if (cell.getContent() != null && !cell.getContent().trim().isEmpty()) {
                                int globalIndex = cell.getColumnIndex() + columnOffset;
                                String content = cleanContent(cell.getContent());
                                rowData.put(globalIndex, content);
                                allDataColumns.add(globalIndex);
                                log.info("Data from row {} (group {}) at index {}: {}", rowIndex, groupIndex, globalIndex, content);
                            }
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
            // Order headers by column index to preserve natural table order
            Map<Integer, String> orderedHeaders = new LinkedHashMap<>();
            consolidatedHeaders.keySet().stream().sorted()
                .forEach(colIdx -> orderedHeaders.put(colIdx, consolidatedHeaders.get(colIdx)));

            // Order parent headers to match the ordered columns
            Map<Integer, String> orderedParentHeaders = new LinkedHashMap<>();
            orderedHeaders.keySet().forEach(colIdx -> {
                if (finalParentHeadersMap.containsKey(colIdx)) {
                    orderedParentHeaders.put(colIdx, finalParentHeadersMap.get(colIdx));
                }
            });

            log.info("Ordered header sequence: {}", orderedHeaders);
            allTableRows.add(new TableRow(0, orderedHeaders, true, orderedParentHeaders));
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
                        content != null && UNWANTED_TABLE_STRINGS.stream()
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
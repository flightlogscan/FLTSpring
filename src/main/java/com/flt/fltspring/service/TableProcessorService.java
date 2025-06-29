package com.flt.fltspring.service;

import com.flt.fltspring.model.bizlogic.TableCell;
import com.flt.fltspring.model.bizlogic.TableRow;
import com.flt.fltspring.model.bizlogic.TableStructure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Flattens and merges multiple TableStructure inputs into a cohesive list of TableRow objects.
 */
@Service
@Slf4j
public class TableProcessorService {

    /**
     * Converts a list of TableStructure objects into a flat list of TableRow.
     *
     * @param tables list of input TableStructure instances with raw cells
     * @return list containing a header TableRow (if present) followed by data TableRows
     */
    public List<TableRow> extractRowsFromTables(List<TableStructure> tables) {
        log.info("Processing {} tables", tables.size());

        final List<TableStructure> ordered = TableUtils.reorderTablesByDate(tables);
        final Map<Integer, String> headers = new LinkedHashMap<>();
        final Map<Integer, Map<Integer, String>> rowGroups = new TreeMap<>();
        int offset = 0;

        for (final TableStructure table : ordered) {
            // Skip tables deemed irrelevant based on their cell contents and total count
            if (TableUtils.shouldSkipTable(table.getCells(), ordered.size() <= 2)) {
                continue;
            }

            // Group cells by their source row index
            final Map<Integer, List<TableCell>> rowsByIndex =
                    table.getCells().stream()
                            .collect(Collectors.groupingBy(TableCell::getRowIndex));

            final List<Integer> sortedIndices = rowsByIndex.keySet().stream()
                    .sorted()
                    .toList();

            int firstDataRow = findFirstDataRow(sortedIndices, rowsByIndex);
            extractHeaders(firstDataRow, sortedIndices, rowsByIndex, offset, headers);
            extractDataRows(firstDataRow, sortedIndices, rowsByIndex, offset, rowGroups);

            // Increment cumulative column offset to merge columns from multiple tables
            offset += table.getColumnCount();
        }

        List<TableRow> result = new ArrayList<>();
        if (!headers.isEmpty()) {
            result.add(createHeaderRow(headers));
        }
        result.addAll(createDataRows(rowGroups));

        log.info("Returning {} TableRow(s)", result.size());
        return result;
    }

    /**
     * Identify the first row index that contains a date-like pattern (MM/DD or M/D).
     */
    private int findFirstDataRow(
            List<Integer> rowIndices,
            Map<Integer, List<TableCell>> rowsByIndex
    ) {
        return rowIndices.stream()
                // Detect first row containing a date pattern (MM/DD or M/D) to mark start of data rows
                .filter(idx -> rowsByIndex.get(idx).stream()
                        .map(TableCell::getContent)
                        .filter(Objects::nonNull)
                        .map(TableUtils::clean)
                        .anyMatch(txt -> txt.matches("\\d{1,2}/\\d{1,2}")))
                .findFirst()
                .orElse(2);
    }

    /**
     * Extracts header mappings from rows before the first data row,
     * ignoring any rows with unwanted or blank content.
     */
    private void extractHeaders(
            int firstDataRow,
            List<Integer> sortedIndices,
            Map<Integer, List<TableCell>> rowsByIndex,
            int offset,
            Map<Integer, String> headers
    ) {
        List<Integer> headerRows = sortedIndices.stream()
                .filter(idx -> idx < firstDataRow)
                .filter(idx -> rowsByIndex.get(idx).stream()
                        .map(TableCell::getContent)
                        .filter(Objects::nonNull)
                        .map(TableUtils::clean)
                        .noneMatch(TableUtils::isUnwanted))
                .collect(Collectors.toList());

        headers.putAll(
                TableHeaderExtractor.extractHeaders(headerRows, rowsByIndex, offset)
        );
    }

    /**
     * Extracts and groups data row values starting at the first data row,
     * applying a global column offset for multi-table stitching.
     */
    private void extractDataRows(
            int firstDataRow,
            List<Integer> sortedIndices,
            Map<Integer, List<TableCell>> rowsByIndex,
            int offset,
            Map<Integer, Map<Integer, String>> rowGroups
    ) {
        for (int idx : sortedIndices) {
            if (idx < firstDataRow) continue;

            List<TableCell> cells = rowsByIndex.get(idx).stream()
                    .sorted(Comparator.comparingInt(TableCell::getColumnIndex))
                    .toList();
            if (TableUtils.shouldSkipRow(cells)) continue;

            int groupKey = idx - firstDataRow;
            Map<Integer, String> rowData = rowGroups
                    .computeIfAbsent(groupKey, k -> new LinkedHashMap<>());

            for (TableCell cell : cells) {
                String content = cell.getContent();
                if (content == null || content.isBlank()) continue;
                int globalCol = cell.getColumnIndex() + offset;
                rowData.put(globalCol, TableUtils.clean(content));
            }
        }
    }

    /**
     * Builds the header TableRow with sorted column keys.
     */
    private TableRow createHeaderRow(Map<Integer, String> headers) {
        Map<Integer, String> sorted = headers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existingValue, newValue) -> existingValue,
                        LinkedHashMap::new
                ));
        return new TableRow(0, sorted, true, null);
    }

    /**
     * Builds data TableRows in ascending group order, skipping empty rows.
     */
    private List<TableRow> createDataRows(
            Map<Integer, Map<Integer, String>> rowGroups
    ) {
        List<TableRow> rows = new ArrayList<>();
        int rowNum = 1;
        for (int groupKey : rowGroups.keySet().stream().sorted().toList()) {
            Map<Integer, String> data = rowGroups.get(groupKey);
            if (data.isEmpty()) continue;
            rows.add(
                    TableRow.builder()
                            .rowIndex(rowNum++)
                            .columnData(data)
                            .isHeader(false)
                            .build()
            );
        }
        return rows;
    }
}

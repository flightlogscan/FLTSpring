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
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TableProcessorService {

    public List<TableRow> extractRows(final List<TableStructure> tables) {
        log.info("Processing {} tables", tables.size());

        final List<TableStructure> ordered = TableUtils.reorderByDateColumn(tables);
        final Map<Integer, String> headerMap = new LinkedHashMap<>();
        final Map<Integer, Map<Integer, String>> groupedRows = new TreeMap<>();
        int cumulativeOffset = 0;

        for (final TableStructure table : ordered) {
            if (TableUtils.shouldSkipTable(table.getCells(), ordered.size() <= 2)) {
                continue;
            }

            final Map<Integer,List<TableCell>> rowsByIndex = table.getCells().stream()
                    .collect(Collectors.groupingBy(TableCell::getRowIndex));
            final List<Integer> sortedRowIndices = rowsByIndex.keySet().stream()
                    .sorted()
                    .toList();

            final int firstDataRow =
                    TableUtils.findFirstDataRow(rowsByIndex, sortedRowIndices);

            final List<Integer> headerRowIndices = sortedRowIndices.stream()
                    .filter(rowIndex -> rowIndex < firstDataRow)
                    .toList();
            headerMap.putAll(
                    TableHeaderExtractor.extractHeaders(
                            headerRowIndices, rowsByIndex, cumulativeOffset
                    )
            );

            for (final int rowIndex : sortedRowIndices) {
                if (rowIndex < firstDataRow) {
                    continue;
                }
                final List<TableCell> cells = rowsByIndex.get(rowIndex).stream()
                        .sorted(Comparator.comparingInt(TableCell::getColumnIndex))
                        .toList();
                if (TableUtils.shouldSkipRow(cells)) {
                    continue;
                }

                final int groupId = rowIndex - firstDataRow;
                final Map<Integer, String> rowData = groupedRows
                        .computeIfAbsent(groupId, key -> new LinkedHashMap<>());
                for (final TableCell cell : cells) {
                    final String content = cell.getContent();
                    if (content == null || content.isBlank()) {
                        continue;
                    }
                    final int globalColumn = cell.getColumnIndex() + cumulativeOffset;
                    rowData.put(globalColumn, TableUtils.clean(content));
                }
            }

            cumulativeOffset += table.getColumnCount();
        }

        final List<TableRow> result = new ArrayList<>();
        if (!headerMap.isEmpty()) {
            result.add(buildHeaderRow(headerMap));
        }
        result.addAll(buildDataRows(groupedRows));
        log.info("Returning {} rows", result.size());
        return result;
    }

    private TableRow buildHeaderRow(final Map<Integer, String> headers) {
        final Map<Integer,String> sorted = headers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));
        return new TableRow(0, sorted, true, null);
    }

    private List<TableRow> buildDataRows(
            final Map<Integer, Map<Integer, String>> groupedRows
    ) {
        final List<TableRow> rows = new ArrayList<>();
        int rowNumber = 1;
        for (final int groupId : groupedRows.keySet().stream().sorted().toList()) {
            final Map<Integer,String> data = groupedRows.get(groupId);
            if (data.isEmpty()) {
                continue;
            }
            rows.add(TableRow.builder()
                    .rowIndex(rowNumber++)
                    .columnData(data)
                    .isHeader(false)
                    .build());
        }
        return rows;
    }
}
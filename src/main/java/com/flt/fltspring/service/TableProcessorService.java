package com.flt.fltspring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.model.bizlogic.TableCell;
import com.flt.fltspring.model.bizlogic.TableRow;
import com.flt.fltspring.model.bizlogic.TableStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TableProcessorService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public List<TableRow> extractRowsFromTables(final List<TableStructure> tables) {
        try {
            log.debug("Full input tables: {}", OBJECT_MAPPER.writeValueAsString(tables));
        } catch (Exception e) {
            log.warn("Failed to serialize input tables", e);
            throw new RuntimeException("Failed to serialize input tables", e);
        }

        log.info("Processing {} tables", tables.size());

        final List<TableStructure> orderedTables = TableUtils.reorderTablesByDate(tables);
        final Map<Integer, String> headers = new HashMap<>();
        final Map<Integer, Map<Integer, String>> rowGroups = new HashMap<>();

        int offset = 0;

        for (final TableStructure table : orderedTables) {
            if (TableUtils.shouldSkipTable(table.getCells(), orderedTables.size() <= 2)) continue;

            final Map<Integer, List<TableCell>> rowsByIndex = table.getCells().stream()
                    .collect(Collectors.groupingBy(TableCell::getRowIndex));

            final List<Integer> sortedRowIndices = rowsByIndex.keySet().stream()
                    .sorted()
                    .toList();

            final int firstDataRow = sortedRowIndices.stream()
                    .filter(rowIndex -> rowsByIndex.get(rowIndex).stream()
                            .map(TableCell::getContent)
                            .filter(Objects::nonNull)
                            .map(TableUtils::clean)
                            .anyMatch(txt -> txt.matches("\\d{1,2}/\\d{1,2}")))
                    .findFirst()
                    .orElse(2);

            final List<Integer> headerRowIndices = sortedRowIndices.stream()
                    .filter(rowIndex -> rowIndex < firstDataRow)
                    .filter(rowIndex -> rowsByIndex.get(rowIndex).stream()
                            .map(TableCell::getContent)
                            .filter(Objects::nonNull)
                            .map(TableUtils::clean)
                            .noneMatch(TableUtils::isUnwanted))
                    .collect(Collectors.toList());

            headers.putAll(TableHeaderExtractor.extractHeaders(headerRowIndices, rowsByIndex, offset));

            for (int rowIndex : sortedRowIndices) {
                if (rowIndex < firstDataRow) continue;

                final List<TableCell> cells = rowsByIndex.get(rowIndex).stream()
                        .sorted(Comparator.comparingInt(TableCell::getColumnIndex))
                        .collect(Collectors.toList());

                if (TableUtils.shouldSkipRow(cells)) continue;

                final int group = rowIndex - firstDataRow;
                final Map<Integer, String> rowMap = rowGroups.computeIfAbsent(group, k -> new HashMap<>());

                for (TableCell cell : cells) {
                    final String content = cell.getContent();
                    if (content == null || content.isBlank()) continue;
                    final int globalCol = cell.getColumnIndex() + offset;
                    rowMap.put(globalCol, TableUtils.clean(content));
                }
            }

            offset += table.getColumnCount();
        }

        final List<TableRow> result = new ArrayList<>();

        if (!headers.isEmpty()) {
            final Map<Integer, String> sortedHeaders = headers.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            result.add(new TableRow(0, sortedHeaders, true, new HashMap<>()));
        }

        int rowIndex = 1;
        for (Integer group : rowGroups.keySet().stream().sorted().toList()) {
            final Map<Integer, String> rowData = rowGroups.get(group);
            if (!rowData.isEmpty()) {
                result.add(TableRow.builder()
                        .rowIndex(rowIndex++)
                        .columnData(rowData)
                        .isHeader(false)
                        .parentHeaders(new HashMap<>())
                        .build());
            }
        }

        try {
            log.debug("Full output rows: {}", OBJECT_MAPPER.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("Failed to serialize output rows", e);
        }

        log.info("Returning {} TableRow(s)", result.size());
        return result;
    }
}
package com.flt.fltspring.service;

import com.flt.fltspring.model.bizlogic.TableCell;
import com.flt.fltspring.model.bizlogic.TableRow;
import com.flt.fltspring.model.bizlogic.TableStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TableProcessorService {

    private static final List<String> UNWANTED_STRINGS = List.of(
            "I certify that", "TOTALS", "AMT. FORWARDED"
    );

    public List<TableRow> extractRowsFromTables(List<TableStructure> tables) {
        log.info("Processing {} tables", tables.size());
        List<TableStructure> orderedTables = reorderTablesByDate(tables);

        Map<Integer, String> headers = new HashMap<>();
        Map<Integer, Map<Integer, String>> rowGroups = new HashMap<>();
        int offset = 0;

        for (TableStructure table : orderedTables) {
            if (shouldSkipTable(table, orderedTables)) continue;

            Map<Integer, List<TableCell>> rowsByIndex = table.getCells().stream()
                    .collect(Collectors.groupingBy(TableCell::getRowIndex));

            List<Integer> sortedRowIdx = new ArrayList<>(rowsByIndex.keySet());
            Collections.sort(sortedRowIdx);

            int firstDataRow = sortedRowIdx.stream()
                    .filter(idx -> rowsByIndex.get(idx).stream()
                            .map(TableCell::getContent)
                            .filter(Objects::nonNull)
                            .map(this::clean)
                            .anyMatch(txt -> txt.matches("\\d{1,2}/\\d{1,2}")))
                    .findFirst()
                    .orElse(2);

            List<Integer> headerRows = sortedRowIdx.stream()
                    .filter(idx -> idx < firstDataRow)
                    .filter(idx -> rowsByIndex.get(idx).stream()
                            .map(TableCell::getContent)
                            .filter(Objects::nonNull)
                            .map(this::clean)
                            .noneMatch(this::isUnwanted))
                    .collect(Collectors.toList());

            for (int i = 0; i < headerRows.size(); i++) {
                for (TableCell cell : rowsByIndex.get(headerRows.get(i))) {
                    if (cell.getContent() == null || cell.getContent().isBlank()) continue;
                    String text = clean(cell.getContent());
                    int startCol = cell.getColumnIndex() + offset;
                    for (int span = 0; span < cell.getColumnSpan(); span++) {
                        headers.put(startCol + span, text);
                    }
                }
            }

            for (int idx : sortedRowIdx) {
                if (idx < firstDataRow) continue;
                List<TableCell> cells = rowsByIndex.get(idx).stream()
                        .sorted(Comparator.comparingInt(TableCell::getColumnIndex))
                        .collect(Collectors.toList());
                if (shouldSkipRow(cells)) continue;

                int group = idx - firstDataRow;
                Map<Integer, String> rowMap = rowGroups.computeIfAbsent(group, k -> new HashMap<>());

                for (TableCell cell : cells) {
                    String content = cell.getContent();
                    if (content == null || content.isBlank()) continue;
                    int globalCol = cell.getColumnIndex() + offset;
                    rowMap.put(globalCol, clean(content));
                }
            }

            offset += table.getColumnCount();
        }

        List<TableRow> result = new ArrayList<>();
        if (!headers.isEmpty()) {
            Map<Integer, String> sortedHeaders = headers.entrySet().stream()
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
            Map<Integer, String> rowData = rowGroups.get(group);
            if (!rowData.isEmpty()) {
                result.add(TableRow.builder()
                        .rowIndex(rowIndex++)
                        .columnData(rowData)
                        .isHeader(false)
                        .parentHeaders(new HashMap<>())
                        .build());
            }
        }

        log.info("Returning {} TableRow(s)", result.size());
        return result;
    }

    private List<TableStructure> reorderTablesByDate(List<TableStructure> tables) {
        List<TableStructure> withDate = new ArrayList<>();
        List<TableStructure> withoutDate = new ArrayList<>();
        for (TableStructure t : tables) {
            boolean hasDate = t.getCells().stream()
                    .map(TableCell::getContent)
                    .map(this::clean)
                    .anyMatch(txt -> "DATE".equalsIgnoreCase(txt));
            (hasDate ? withDate : withoutDate).add(t);
        }
        List<TableStructure> ordered = new ArrayList<>(withDate);
        ordered.addAll(withoutDate);
        return ordered;
    }

    private boolean shouldSkipTable(TableStructure table, List<TableStructure> allTables) {
        if (allTables.size() <= 2) return false;
        return table.getCells().stream()
                .map(TableCell::getContent)
                .filter(Objects::nonNull)
                .anyMatch(txt -> UNWANTED_STRINGS.stream()
                        .anyMatch(un -> txt.toLowerCase().contains(un.toLowerCase())));
    }

    private boolean shouldSkipRow(List<TableCell> row) {
        return row.stream()
                .map(TableCell::getContent)
                .filter(Objects::nonNull)
                .anyMatch(txt -> UNWANTED_STRINGS.stream()
                        .anyMatch(un -> txt.toLowerCase().contains(un.toLowerCase())));
    }

    private boolean isUnwanted(String text) {
        return UNWANTED_STRINGS.stream()
                .anyMatch(un -> text.equalsIgnoreCase(un) || text.toLowerCase().contains(un.toLowerCase()));
    }

    private String clean(String content) {
        return content == null ? null :
                content.replaceAll("\\r?\\n", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
    }
}
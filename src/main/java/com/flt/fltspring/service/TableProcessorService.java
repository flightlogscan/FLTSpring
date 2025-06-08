package com.flt.fltspring.service;

import com.flt.fltspring.model.bizlogic.TableCell;
import com.flt.fltspring.model.bizlogic.TableRow;
import com.flt.fltspring.model.bizlogic.TableStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private static final List<String> UNWANTED_TABLE_STRINGS = List.of(
            "I certify that",
            "TOTALS",
            "AMT. FORWARDED"
    );

    private static final Set<String> META_HEADERS = Set.of(
            "LOGBOOK ENTRIES",
            "TYPE OF PILOT EXPERIENCE OR TRAINING"
    );

    public List<TableRow> extractRowsFromTables(List<TableStructure> tables) {
        log.info("Processing {} tables", tables.size());
        List<TableStructure> orderedTables = reorderTablesByDate(tables);

        Map<Integer, String> headers = new HashMap<>();
        Map<Integer, String> colToParent = new HashMap<>();
        Map<String, Set<String>> hierarchy = new HashMap<>();
        Map<Integer, Map<Integer, String>> rowGroups = new HashMap<>();

        int offset = 0;

        for (TableStructure table : orderedTables) {
            log.info("Table page {}", table.getPageNumber());
            if (shouldSkipTable(table, orderedTables)) {
                log.info("Skipping page {}", table.getPageNumber());
                continue;
            }

            Map<Integer, List<TableCell>> rowsByIndex = table.getCells().stream()
                    .collect(Collectors.groupingBy(TableCell::getRowIndex));

            List<Integer> sortedRowIdx = rowsByIndex.keySet().stream()
                    .sorted()
                    .collect(Collectors.toList());

            int firstDataRow = sortedRowIdx.stream()
                    .filter(idx -> rowsByIndex.get(idx).stream()
                            .map(TableCell::getContent)
                            .filter(Objects::nonNull)
                            .map(this::clean)
                            .anyMatch(txt -> txt.matches("\\d{1,2}/\\d{1,2}")))
                    .findFirst()
                    .orElse(2);

            log.info("First data row at index {}", firstDataRow);

            List<Integer> headerRows = sortedRowIdx.stream()
                    .filter(idx -> idx < firstDataRow)
                    .filter(idx -> rowsByIndex.get(idx).stream()
                            .map(TableCell::getContent)
                            .filter(Objects::nonNull)
                            .map(this::clean)
                            .noneMatch(this::isUnwanted))
                    .collect(Collectors.toList());

            if (!headerRows.isEmpty()) {
                extractHeaders(headerRows, rowsByIndex, offset, headers, colToParent, hierarchy);
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
                    log.info("Row {} col {} = {}", idx, globalCol, clean(content));
                }
            }

            offset += table.getColumnCount();
        }

        for (Integer col : colToParent.keySet()) {
            String parent = colToParent.get(col);
            if (!headers.containsKey(col) && !META_HEADERS.contains(parent)) {
                headers.put(col, parent);
            }
        }

        Map<Integer, String> parentMap = buildParentMap(headers, colToParent, hierarchy);

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

            Map<Integer, String> sortedParents = sortedHeaders.keySet().stream()
                    .filter(parentMap::containsKey)
                    .collect(Collectors.toMap(
                            col -> col,
                            parentMap::get,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            result.add(new TableRow(0, sortedHeaders, true, sortedParents));
        }

        List<Integer> groupKeys = rowGroups.keySet().stream().sorted().collect(Collectors.toList());
        int rowIndex = 1;
        for (int group : groupKeys) {
            Map<Integer, String> rowData = rowGroups.get(group);
            if (rowData.isEmpty()) continue;
            result.add(TableRow.builder()
                    .rowIndex(rowIndex++)
                    .columnData(rowData)
                    .isHeader(false)
                    .parentHeaders(new HashMap<>(parentMap))
                    .build());
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
            if (hasDate) withDate.add(t);
            else withoutDate.add(t);
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
                .anyMatch(txt -> UNWANTED_TABLE_STRINGS.stream()
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
        if (content == null) return null;
        return content.replaceAll("\\r?\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void extractHeaders(
            List<Integer> headerRows,
            Map<Integer, List<TableCell>> rowsByIndex,
            int offset,
            Map<Integer, String> headers,
            Map<Integer, String> colToParent,
            Map<String, Set<String>> hierarchy
    ) {
        int top = headerRows.get(0);
        int bottom = headerRows.get(headerRows.size() - 1);

        for (TableCell cell : rowsByIndex.getOrDefault(top, Collections.emptyList())) {
            String text = cell.getContent();
            if (text == null || text.isBlank()) continue;
            String parent = clean(text);
            if (META_HEADERS.contains(parent)) continue;

            int startCol = cell.getColumnIndex() + offset;
            hierarchy.putIfAbsent(parent, new HashSet<>());

            for (int span = 0; span < cell.getColumnSpan(); span++) {
                colToParent.put(startCol + span, parent);
            }
        }

        if (headerRows.size() > 1) {
            for (int i = 1; i < headerRows.size() - 1; i++) {
                int mid = headerRows.get(i);
                for (TableCell cell : rowsByIndex.getOrDefault(mid, Collections.emptyList())) {
                    String child = cell.getContent();
                    if (child == null || child.isBlank()) continue;
                    String childTxt = clean(child);
                    if (META_HEADERS.contains(childTxt)) continue;

                    int startCol = cell.getColumnIndex() + offset;
                    for (int span = 0; span < cell.getColumnSpan(); span++) {
                        int colIdx = startCol + span;
                        headers.put(colIdx, childTxt);
                        String parent = colToParent.get(colIdx);
                        if (parent != null) hierarchy.get(parent).add(childTxt);
                    }
                }
            }
        }

        for (TableCell cell : rowsByIndex.getOrDefault(bottom, Collections.emptyList())) {
            String leaf = cell.getContent();
            if (leaf == null || leaf.isBlank()) continue;
            String leafTxt = clean(leaf);
            if (META_HEADERS.contains(leafTxt)) continue;

            int startCol = cell.getColumnIndex() + offset;
            for (int span = 0; span < cell.getColumnSpan(); span++) {
                int colIdx = startCol + span;
                headers.put(colIdx, leafTxt);
                String parent = colToParent.get(colIdx);
                if (parent != null) hierarchy.get(parent).add(leafTxt);
            }
        }
    }

    private Map<Integer, String> buildParentMap(
            Map<Integer, String> headers,
            Map<Integer, String> colToParent,
            Map<String, Set<String>> hierarchy
    ) {
        Map<String, String> childToParent = new HashMap<>();
        for (var entry : hierarchy.entrySet()) {
            String parent = entry.getKey();
            for (String child : entry.getValue()) {
                childToParent.put(child, parent);
            }
        }

        Map<Integer, String> parentMap = new HashMap<>();
        for (var e : headers.entrySet()) {
            int col = e.getKey();
            String hdr = e.getValue();
            if (childToParent.containsKey(hdr)) {
                parentMap.put(col, childToParent.get(hdr));
            }
        }

        for (var e : headers.entrySet()) {
            int col = e.getKey();
            if (!parentMap.containsKey(col) && colToParent.containsKey(col)) {
                parentMap.put(col, colToParent.get(col));
            }
        }

        return parentMap;
    }
}
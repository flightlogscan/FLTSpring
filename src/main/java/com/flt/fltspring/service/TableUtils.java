package com.flt.fltspring.service;

import com.flt.fltspring.config.ColumnType;
import com.flt.fltspring.model.bizlogic.TableCell;
import com.flt.fltspring.model.bizlogic.TableStructure;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TableUtils {

    private static final List<String> UNWANTED = List.of(
            "I certify that", "TOTALS", "AMT. FORWARDED"
    );

    // Matches integers or decimals, e.g. "1", "12.34"
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");
    // Matches dates like "5/4", "12/31", "Jan 12", "February 20" (case-insensitive)
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?i)(?:\\d{1,2}/\\d{1,2}|(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|"
                    + "jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+\\d{1,2})"
    );

    public static String clean(final String s) {
        if (s == null) return null;
        return s.replaceAll("\\r?\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static boolean isUnwanted(final String s) {
        return UNWANTED.stream()
                .anyMatch(unwanted -> s.equalsIgnoreCase(unwanted)
                        || s.toLowerCase().contains(unwanted.toLowerCase()));
    }

    public static boolean shouldSkipRow(final List<TableCell> row) {
        return row.stream()
                .map(TableCell::getContent)
                .filter(Objects::nonNull)
                .map(TableUtils::clean)
                .anyMatch(TableUtils::isUnwanted);
    }

    public static boolean shouldSkipTable(final List<TableCell> cells, final boolean allowShort) {
        if (allowShort) return false;
        return cells.stream()
                .map(TableCell::getContent)
                .filter(Objects::nonNull)
                .map(TableUtils::clean)
                .anyMatch(TableUtils::isUnwanted);
    }

    public static List<TableStructure> reorderByDateColumn(final List<TableStructure> tables) {
        return tables.stream()
                .sorted((first, second) -> Boolean.compare(
                        hasDateColumn(second), hasDateColumn(first)
                ))
                .collect(Collectors.toList());
    }

    private static boolean hasDateColumn(final TableStructure table) {
        return table.getCells().stream()
                .map(TableCell::getContent)
                .filter(Objects::nonNull)
                .map(TableUtils::clean)
                .anyMatch(text -> ColumnType.DATE.name().equalsIgnoreCase(text));
    }

    public static int findFirstDataRow(
            final Map<Integer, List<TableCell>> rowsByIndex,
            final List<Integer> sortedRowIndices
    ) {
        return sortedRowIndices.stream()
                .filter(rowIndex -> rowsByIndex.get(rowIndex).stream()
                        .map(TableCell::getContent)
                        .filter(Objects::nonNull)
                        .map(TableUtils::clean)
                        .anyMatch(TableUtils::isDataCell))
                .findFirst()
                .orElse(sortedRowIndices.size() > 2
                        ? sortedRowIndices.get(2)
                        : sortedRowIndices.get(0));
    }

    private static boolean isDataCell(final String content) {
        return DATE_PATTERN.matcher(content).matches()
                || NUMERIC_PATTERN.matcher(content).matches();
    }
}
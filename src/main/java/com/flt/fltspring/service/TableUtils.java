package com.flt.fltspring.service;

import com.flt.fltspring.model.bizlogic.TableCell;

import java.util.List;

public class TableUtils {

    private static final List<String> UNWANTED_STRINGS = List.of(
            "I certify that", "TOTALS", "AMT. FORWARDED"
    );

    public static String clean(String content) {
        return content == null ? null :
                content.replaceAll("\\r?\\n", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
    }

    public static boolean isUnwanted(String text) {
        return UNWANTED_STRINGS.stream()
                .anyMatch(un -> text.equalsIgnoreCase(un) || text.toLowerCase().contains(un.toLowerCase()));
    }

    public static boolean shouldSkipRow(List<TableCell> row) {
        return row.stream()
                .map(TableCell::getContent)
                .filter(c -> c != null && !c.isBlank())
                .anyMatch(TableUtils::isUnwanted);
    }

    public static boolean shouldSkipTable(List<TableCell> cells, boolean allowShortTable) {
        if (allowShortTable) return false;
        return cells.stream()
                .map(TableCell::getContent)
                .filter(c -> c != null && !c.isBlank())
                .anyMatch(TableUtils::isUnwanted);
    }
}
package com.flt.fltspring.service;

import com.flt.fltspring.config.ColumnType;
import com.flt.fltspring.model.bizlogic.TableCell;
import com.flt.fltspring.model.bizlogic.TableStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
                .anyMatch(unwanted -> text.equalsIgnoreCase(unwanted) ||
                        text.toLowerCase().contains(unwanted.toLowerCase()));
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

    /**
     * Moves the single table containing a "DATE" column to the front.
     * Assumes only one such table will ever exist.
     */
    public static List<TableStructure> reorderTablesByDate(List<TableStructure> tables) {
        for (TableStructure table : tables) {
            final boolean hasDate = table.getCells().stream()
                    .map(TableCell::getContent)
                    .filter(Objects::nonNull)
                    .map(TableUtils::clean)
                    .anyMatch(txt -> ColumnType.DATE.name().equalsIgnoreCase(txt));

            if (hasDate) {
                final List<TableStructure> result = new ArrayList<>();
                result.add(table);
                for (TableStructure t : tables) {
                    if (t != table) result.add(t);
                }
                return result;
            }
        }
        return tables;
    }
}
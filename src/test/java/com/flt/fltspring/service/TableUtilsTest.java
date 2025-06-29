package com.flt.fltspring.service;

import com.flt.fltspring.model.bizlogic.TableCell;
import com.flt.fltspring.model.bizlogic.TableStructure;
import org.junit.jupiter.api.Test;
import support.UnitTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TableUtilsTest extends UnitTestBase {

    @Test
    void clean_removesWhitespaceAndNewlines() {
        String cleaned = TableUtils.clean("  A\nB  ");
        assertThat(cleaned).isEqualTo("A B");
    }

    @Test
    void isUnwanted_matchesPredefinedStrings() {
        assertThat(TableUtils.isUnwanted("TOTALS")).isTrue();
        assertThat(TableUtils.isUnwanted("Some I certify that text")).isTrue();
        assertThat(TableUtils.isUnwanted("keep me")).isFalse();
    }

    @Test
    void shouldSkipRow_detectsUnwantedContent() {
        List<TableCell> row = List.of(
                cell(0, 0, "ok"),
                cell(0, 1, "TOTALS")
        );
        assertThat(TableUtils.shouldSkipRow(row)).isTrue();
    }

    @Test
    void shouldSkipTable_respectsAllowShortTableFlag() {
        List<TableCell> cells = List.of(cell(0,0,"TOTALS"));
        assertThat(TableUtils.shouldSkipTable(cells, true)).isFalse();
        assertThat(TableUtils.shouldSkipTable(cells, false)).isTrue();
    }

    @Test
    void reorderTablesByDate_movesDateTableFirst() {
        TableStructure table1 = new TableStructure(1, List.of(cell(0,0,"NOPE")), 1);
        TableStructure table2 = new TableStructure(1, List.of(cell(0,0,"DATE")), 1);
        List<TableStructure> result = TableUtils.reorderTablesByDate(List.of(table1, table2));
        assertThat(result.getFirst()).isSameAs(table2);
    }
}

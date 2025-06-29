package com.flt.fltspring.service;

import org.junit.jupiter.api.Test;
import support.UnitTestBase;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class TableHeaderExtractorTest extends UnitTestBase {

    @Test
    void extractHeaders_collectsCellsWithOffsetAndSpan() {
        Map<Integer, List<com.flt.fltspring.model.bizlogic.TableCell>> rows = new HashMap<>();
        rows.put(0, List.of(
                cell(0, 0, "  Head1  "),
                cell(0, 1, null),
                cell(0, 2, "   "),
                cell(0, 3, "Head\nTwo", 2)
        ));

        Map<Integer, String> headers = TableHeaderExtractor.extractHeaders(
                List.of(0), rows, 1);

        assertThat(headers)
                .containsEntry(1, "Head1")
                .containsEntry(4, "Head Two")
                .containsEntry(5, "Head Two")
                .hasSize(3);
    }
}

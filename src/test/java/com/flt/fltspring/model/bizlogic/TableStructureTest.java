package com.flt.fltspring.model.bizlogic;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class TableStructureTest {
    @Test
    void builder_setsValues() {
        TableStructure ts = TableStructure.builder()
                .columnCount(2)
                .cells(List.of(new TableCell("a",0,0,1)))
                .pageNumber(1)
                .build();
        assertThat(ts.getColumnCount()).isEqualTo(2);
        assertThat(ts.getPageNumber()).isEqualTo(1);
        assertThat(ts.getCells()).hasSize(1);
    }
}

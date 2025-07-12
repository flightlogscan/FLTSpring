package com.flt.fltspring.model.bizlogic;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TableCellTest {
    @Test
    void constructors_setValues() {
        TableCell cell = new TableCell("a",1,2,3);
        assertThat(cell.getContent()).isEqualTo("a");
        assertThat(cell.getRowIndex()).isEqualTo(1);
        assertThat(cell.getColumnIndex()).isEqualTo(2);
        assertThat(cell.getColumnSpan()).isEqualTo(3);
    }
}

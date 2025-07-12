package com.flt.fltspring.model.bizlogic;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class TableRowTest {
    @Test
    void builder_defaultsParentHeaders() {
        TableRow row = TableRow.builder().rowIndex(1).columnData(Map.of()).isHeader(false).build();
        assertThat(row.getParentHeaders()).isEmpty();
    }
}

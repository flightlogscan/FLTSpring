package com.flt.fltspring.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ColumnConfigTest {
    @Test
    void getters_returnValues() {
        ColumnConfig cfg = new ColumnConfig("DATE", ColumnType.DATE);
        assertThat(cfg.getFieldName()).isEqualTo("DATE");
        assertThat(cfg.getType()).isEqualTo(ColumnType.DATE);
    }
}

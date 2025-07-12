package com.flt.fltspring.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ColumnTypeTest {
    @Test
    void enum_hasValues() {
        assertThat(ColumnType.valueOf("STRING")).isEqualTo(ColumnType.STRING);
    }
}

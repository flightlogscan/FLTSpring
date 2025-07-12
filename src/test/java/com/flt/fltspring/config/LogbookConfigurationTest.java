package com.flt.fltspring.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LogbookConfigurationTest {
    @Test
    void columnConfigs_containsExpected() {
        LogbookConfiguration cfg = new LogbookConfiguration();
        ColumnConfig[] columns = cfg.columnConfigs();
        assertThat(columns).isNotEmpty();
        assertThat(columns[0].getFieldName()).isEqualTo("DATE");
    }
}

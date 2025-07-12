package com.flt.fltspring.model.service;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class RowDTOTest {
    @Test
    void record_holdsData() {
        RowDTO dto = new RowDTO(1, Map.of(1,"a"), Map.of(), true);
        assertThat(dto.rowIndex()).isEqualTo(1);
        assertThat(dto.content().get(1)).isEqualTo("a");
        assertThat(dto.header()).isTrue();
    }
}

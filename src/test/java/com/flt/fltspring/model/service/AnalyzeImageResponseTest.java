package com.flt.fltspring.model.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class AnalyzeImageResponseTest {
    @Test
    void builder_createsObject() {
        AnalyzeImageResponse resp = AnalyzeImageResponse.builder()
                .tables(List.of())
                .status("OK")
                .errorMessage(null)
                .build();
        assertThat(resp.getStatus()).isEqualTo("OK");
        assertThat(resp.getTables()).isNotNull();
    }
}

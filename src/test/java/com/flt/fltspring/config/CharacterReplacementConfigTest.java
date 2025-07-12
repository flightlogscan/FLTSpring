package com.flt.fltspring.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterReplacementConfigTest {
    @Test
    void initialize_populatesMaps() {
        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.initialize();

        assertThat(config.getNumericReplacements()).isNotEmpty();
        assertThat(config.getAirportCodeReplacements()).isNotEmpty();
        assertThat(config.getStringReplacements()).isNotNull();
    }
}

package com.flt.fltspring;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class PingRestControllerTest {

    @Test
    void getPing_returnsPong() {
        PingRestController controller = new PingRestController();
        ResponseEntity<String> resp = controller.getPing();
        assertThat(resp.getBody()).isEqualTo("pong");
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }
}

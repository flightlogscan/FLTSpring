package com.flt.fltspring;

import com.azure.core.util.BinaryData;
import com.flt.fltspring.dao.DocumentIntelligenceDao;
import com.flt.fltspring.model.service.AnalyzeImageResponse;
import com.flt.fltspring.service.ResultConverterService;
import com.flt.fltspring.service.RowConversionService;
import com.flt.fltspring.service.TableDataTransformerService;
import com.flt.fltspring.service.TableProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageAnalyzerRestControllerTest {
    @Mock
    private DocumentIntelligenceDao dao;
    @Mock
    private ResultConverterService converter;
    @Mock
    private TableProcessorService processor;
    @Mock
    private TableDataTransformerService transformer;
    @Mock
    private RowConversionService rowConv;

    private ImageAnalyzerRestController controller;

    @BeforeEach
    void setup() {
        controller = new ImageAnalyzerRestController(
                new com.fasterxml.jackson.databind.ObjectMapper(),
                dao, converter, processor, transformer, rowConv);
    }

    @Test
    void submitAnalyzeImage_payloadTooLarge() throws Exception {
        byte[] bytes = new byte[10 * 1024 * 1024 + 1];
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(bytes);
        ResponseEntity<AnalyzeImageResponse> resp = controller.submitAnalyzeImage(request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void submitAnalyzeImage_daoThrowsTimeout() throws Exception {
        byte[] bytes = "data".getBytes();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(bytes);

        when(dao.analyzeDocumentSync(any(BinaryData.class))).thenThrow(new TimeoutException());
        ResponseEntity<AnalyzeImageResponse> resp = controller.submitAnalyzeImage(request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }
}

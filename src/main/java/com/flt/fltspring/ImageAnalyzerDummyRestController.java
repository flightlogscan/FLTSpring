package com.flt.fltspring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.claims.AdminAuthenticator;
import com.flt.fltspring.model.service.AnalyzeImageResponse;
import com.flt.fltspring.model.bizlogic.TableRow;
import com.flt.fltspring.model.bizlogic.TableStructure;
import com.flt.fltspring.model.dummy.DummyAnalyzeResult;
import com.flt.fltspring.service.RowConversionService;
import com.flt.fltspring.service.TableProcessorService;
import com.flt.fltspring.service.dummy.DummyResultConverterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
public class ImageAnalyzerDummyRestController {
    private final ObjectMapper objectMapper;
    private final DummyResultConverterService dummyResultConverterService;
    private final RowConversionService rowConversionService;
    private final TableProcessorService tableProcessorService;
    private final AdminAuthenticator adminAuthenticator;

    @PostMapping("/analyze/dummy")
    public ResponseEntity<AnalyzeImageResponse> submitAnalyzeImageDummy(final HttpServletRequest request) {
        log.info("Received dummy analysis request");

        final String firebaseEmail = (String) request.getAttribute("firebaseEmail");
        if (!adminAuthenticator.isAdmin(firebaseEmail)) {
            log.warn("Non-admin hitting dummy endpoint, returning not found");
            return ResponseEntity.notFound().build();
        }

        try {
            final File file = new File("dummyResponse.txt");
            final String rawData = FileUtils.readFileToString(file, "UTF-8");

            log.info("Successfully read dummy response file");
            
            if (log.isDebugEnabled()) {
                log.debug("Dummy response content: {}", rawData);
            }

            final DummyAnalyzeResult dummyResult = objectMapper.readValue(rawData, DummyAnalyzeResult.class);

            final List<TableStructure> tables = dummyResultConverterService.convertToTable(dummyResult);
            final List<TableRow> tableRows = tableProcessorService.processTables(tables);
            final AnalyzeImageResponse response = rowConversionService.toRowDTO(tableRows);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.warn("Error processing dummy response", e);
            return ResponseEntity.internalServerError()
                    .body(AnalyzeImageResponse.builder()
                            .status("ERROR")
                            .errorMessage("Failed to process dummy data")
                            .build());
        }
    }
}
package com.flt.fltspring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.claims.AdminAuthenticator;
import com.flt.fltspring.model.AnalyzeImageResponse;
import com.flt.fltspring.model.TableResponseDTO;
import com.flt.fltspring.model.TableRow;
import com.flt.fltspring.model.TableStructure;
import com.flt.fltspring.model.dummy.DummyAnalyzeResult;
import com.flt.fltspring.service.RowProcessorService;
import com.flt.fltspring.service.TableProcessorService;
import com.flt.fltspring.service.dummy.DummyResultConverterService;
import com.google.common.collect.Table;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@EnableCaching
@Slf4j
@RequiredArgsConstructor
public class ImageAnalyzerDummyRestController {
    private final ObjectMapper objectMapper;
    private final DummyResultConverterService dummyResultConverterService;
    private final RowProcessorService rowProcessorService;
    private final TableProcessorService tableProcessorService;

    @RequestMapping(method = RequestMethod.POST, path = "/api/analyze/dummy")
    public ResponseEntity<AnalyzeImageResponse> submitAnalyzeImageDummy(final HttpServletRequest request) {

        log.info("Received dummy analysis request");

        final String firebaseEmail = (String) request.getAttribute("firebaseEmail");
        if (!AdminAuthenticator.isAdmin(firebaseEmail)) {
            log.warn("Non-admin hitting dummy endpoint, returning not found");
            return ResponseEntity.notFound().build();
        }

        try {
            final File file = new File("dummyResponse.txt");
            final String rawData = FileUtils.readFileToString(file, "UTF-8");

            log.info("Successfully read dummy response file: {}", rawData);

            final DummyAnalyzeResult dummyResult = objectMapper.readValue(rawData, DummyAnalyzeResult.class);

            final List<TableStructure> tables = dummyResultConverterService.convertToTable(dummyResult);
            final List<TableRow> tableRows = tableProcessorService.processTables(tables);
            final TableResponseDTO tableResponse = rowProcessorService.processTableRows(tableRows);

            final AnalyzeImageResponse response = AnalyzeImageResponse.builder()
                    .status("SUCCESS")
                    .tables(tableResponse.getRows())
                    .build();

            log.info("Final response: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.warn("IOException parsing dummy response: ", e);
            return ResponseEntity.internalServerError()
                    .body(AnalyzeImageResponse.builder()
                            .status("ERROR")
                            .build());
        }
    }
}
package com.flt.fltspring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.claims.AdminAuthenticator;
import com.flt.fltspring.model.AnalyzeImageResponse;
import com.flt.fltspring.model.DocumentAnalysisService;
import com.flt.fltspring.model.LogbookType;
import com.flt.fltspring.model.TableResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final DocumentAnalysisService documentAnalysisService;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DummyAnalyzeResult {
        private List<DummyTable> tables;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DummyTable {
        private int rowCount;
        private int columnCount;
        private List<DummyCell> cells;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DummyCell {
        private String kind;
        private int rowIndex;
        private int columnIndex;
        private String content;
    }

    @RequestMapping(method = RequestMethod.POST, path = "/api/analyze/dummy")
    public ResponseEntity<AnalyzeImageResponse> submitAnalyzeImageDummy(
            final HttpServletRequest request,
            @RequestParam(defaultValue = "JEPPESEN") LogbookType logbookType) {

        final String firebaseEmail = (String) request.getAttribute("firebaseEmail");
        if (!AdminAuthenticator.isAdmin(firebaseEmail)) {
            return ResponseEntity.notFound().build();
        }

        try {
            log.info("Received dummy analysis request");

            File file = new File("dummyResponse.txt");
            String rawData = FileUtils.readFileToString(file, "UTF-8");

            log.info("Successfully read dummy response file");
            log.debug("Raw file content: {}", rawData);

            DummyAnalyzeResult dummyResult = objectMapper.readValue(rawData, DummyAnalyzeResult.class);

            // Use the consolidated service method to process the dummy result
            TableResponseDTO tableResponse = documentAnalysisService.analyzeDummyDocument(dummyResult, logbookType);

            final AnalyzeImageResponse response = AnalyzeImageResponse.builder()
                    .status("SUCCESS")
                    .tables(tableResponse.getRows())
                    .build();

            log.info("Returning response");
            log.debug("Final response: {}", objectMapper.writeValueAsString(response));

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
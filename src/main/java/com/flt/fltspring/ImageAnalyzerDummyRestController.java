package com.flt.fltspring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.claims.AdminAuthenticator;
import com.flt.fltspring.model.AnalyzeImageResponse;
import com.flt.fltspring.model.DocumentAnalysisService;
import com.flt.fltspring.model.LogbookType;
import com.flt.fltspring.model.TableResponseDTO;
import com.flt.fltspring.model.TableRow;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    static class DummyAnalyzeResult {
        private List<DummyTable> tables;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        static class DummyTable {
            private List<DummyCell> cells;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        static class DummyCell {
            private String kind;
            private int rowIndex;
            private int columnIndex;
            private String content;
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/api/analyze/dummy")
    public ResponseEntity<AnalyzeImageResponse> submitAnalyzeImageDummy(
            final HttpServletRequest request,
            @RequestParam(defaultValue = "JEPPESEN") LogbookType logbookType) {

        final String firebaseEmail = (String) request.getAttribute("firebaseEmail");
        if(!AdminAuthenticator.isAdmin(firebaseEmail)) {
            return ResponseEntity.notFound().build();
        }

        try {
            log.info("Received request: ");

            File file = new File("dummyResponse.txt");
            String rawData = FileUtils.readFileToString(file, "UTF-8");

            log.info("Successfully read file");

            DummyAnalyzeResult dummyResult = objectMapper.readValue(rawData, DummyAnalyzeResult.class);
            List<TableRow> tableRows = convertDummyToTableRows(dummyResult);
            TableResponseDTO tableResponse = documentAnalysisService.processTableRows(tableRows, logbookType);

            final AnalyzeImageResponse response = AnalyzeImageResponse.builder()
                    .status("SUCCESS")
                    .rawResults(rawData)
                    .tables(tableResponse.getRows())
                    .build();

            log.info("Returning response: ");
            log.info(objectMapper.writeValueAsString(response));

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.warn("IOException parsing dummy response: ", e);
            return ResponseEntity.internalServerError()
                    .body(AnalyzeImageResponse.builder()
                            .status("ERROR")
                            .rawResults(e.getMessage())
                            .build());
        }
    }

    private List<TableRow> convertDummyToTableRows(DummyAnalyzeResult dummyResult) {
        final List<TableRow> tableRows = new ArrayList<>();
        final Map<Integer, Map<Integer, String>> rowData = new HashMap<>();
        final Map<Integer, Boolean> headerRows = new HashMap<>();

        for (DummyAnalyzeResult.DummyTable table : dummyResult.getTables()) {
            for (DummyAnalyzeResult.DummyCell cell : table.getCells()) {
                int rowIndex = cell.getRowIndex();
                int columnIndex = cell.getColumnIndex();
                String content = cell.getContent();
                boolean isHeader = "columnHeader".equals(cell.getKind());

                rowData.putIfAbsent(rowIndex, new HashMap<>());
                rowData.get(rowIndex).put(columnIndex, content);

                if (isHeader) {
                    headerRows.put(rowIndex, true);
                }
            }

            rowData.forEach((rowIndex, columnData) -> {
                TableRow tableRow = new TableRow(
                        rowIndex,
                        columnData,
                        headerRows.getOrDefault(rowIndex, false)
                );
                tableRows.add(tableRow);
            });
        }
        return tableRows;
    }
}
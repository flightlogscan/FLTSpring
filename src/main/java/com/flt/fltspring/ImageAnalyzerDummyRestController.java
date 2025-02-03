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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class DummyTable {
        private int rowCount;
        private int columnCount;
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
            log.debug("Raw file content: {}", rawData);

            DummyAnalyzeResult dummyResult = objectMapper.readValue(rawData, DummyAnalyzeResult.class);
            List<TableRow> tableRows = convertDummyToTableRows(dummyResult);
            log.debug("Converted table rows: {}", tableRows);

            TableResponseDTO tableResponse = documentAnalysisService.processTableRows(tableRows, logbookType);

            final AnalyzeImageResponse response = AnalyzeImageResponse.builder()
                    .status("SUCCESS")
                    .rawResults(rawData)
                    .tables(tableResponse.getRows())
                    .build();

            log.info("Returning response: ");
            log.debug("Final response: {}", objectMapper.writeValueAsString(response));

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
        final List<TableRow> allTableRows = new ArrayList<>();
        int columnOffset = 0;

        log.debug("Converting dummy result with {} tables", dummyResult.getTables().size());

        for (DummyTable table : dummyResult.getTables()) {
            log.debug("Processing table with {} cells, {} rows, {} columns",
                    table.getCells().size(), table.getRowCount(), table.getColumnCount());

            // Process cells for current table
            for (DummyCell cell : table.getCells()) {
                int rowIndex = cell.getRowIndex();
                int columnIndex = columnOffset + cell.getColumnIndex();
                String content = cell.getContent();
                boolean isHeader = "columnHeader".equals(cell.getKind());

                log.debug("Processing cell - Row: {}, Col: {}, Content: '{}', IsHeader: {}",
                        rowIndex, columnIndex, content, isHeader);

                // Check if row already exists
                TableRow tableRow = allTableRows.stream()
                        .filter(row -> row.getRowIndex() == rowIndex)
                        .findFirst()
                        .orElseGet(() -> {
                            TableRow newRow = new TableRow(
                                    rowIndex,
                                    new HashMap<>(),
                                    isHeader
                            );
                            allTableRows.add(newRow);
                            log.debug("Created new row: {}", newRow);
                            return newRow;
                        });

                // Add content to row
                if (content != null && !content.trim().isEmpty()) {
                    tableRow.getColumnData().put(columnIndex, content.trim());
                    log.debug("Added content to row {}: Column {} -> '{}'",
                            rowIndex, columnIndex, content.trim());
                }
            }

            // Update column offset for next table
            columnOffset += table.getColumnCount();
            log.debug("Updated column offset to: {}", columnOffset);
        }

        // Sort rows by index for consistent ordering
        List<TableRow> sortedRows = allTableRows.stream()
                .sorted(Comparator.comparingInt(TableRow::getRowIndex))
                .collect(Collectors.toList());

        log.debug("Final sorted rows: {}", sortedRows);
        return sortedRows;
    }
}
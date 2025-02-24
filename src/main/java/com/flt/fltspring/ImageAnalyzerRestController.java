package com.flt.fltspring;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.dao.DocumentIntelligenceDao;
import com.flt.fltspring.model.AnalyzeImageResponse;
import com.flt.fltspring.model.TableResponseDTO;
import com.flt.fltspring.model.TableRow;
import com.flt.fltspring.model.TableStructure;
import com.flt.fltspring.service.ResultConverterService;
import com.flt.fltspring.service.RowProcessorService;
import com.flt.fltspring.service.TableProcessorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@EnableCaching
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
public class ImageAnalyzerRestController {

    //TODO: put secret in client singleton class once it exists.
    private final String flsDocumentAiSecret;

    private final ObjectMapper objectMapper;
    private final ResultConverterService resultConverterService;
    private final DocumentIntelligenceDao documentIntelligenceDao;
    private final RowProcessorService rowProcessorService;
    private final TableProcessorService tableProcessorService;


    @RequestMapping(method = RequestMethod.POST, path = "/analyze")
    public ResponseEntity<AnalyzeImageResponse> submitAnalyzeImage(final HttpServletRequest request) {

        try {
            log.info("Starting document analysis");

            final BinaryData documentData = BinaryData.fromBytes(request.getInputStream().readAllBytes());
            log.info("Successfully read binary data, length: {}", documentData.getLength());

            final AnalyzeResult analyzeResult = documentIntelligenceDao
                    .analyzeDocumentSync(documentData, flsDocumentAiSecret);

            final List<TableStructure> tables = resultConverterService.convertToTable(analyzeResult);
            final List<TableRow> tableRows = tableProcessorService.processTables(tables);
            final TableResponseDTO tableResponse = rowProcessorService.processTableRows(tableRows);

            final AnalyzeImageResponse response = AnalyzeImageResponse.builder()
                    .status("SUCCESS")
                    .tables(tableResponse.getRows())
                    .build();

            log.info("Final response: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to process document", e);
            return ResponseEntity.internalServerError()
                    .body(AnalyzeImageResponse.builder()
                            .status("ERROR")
                            .build());
        }
    }
}
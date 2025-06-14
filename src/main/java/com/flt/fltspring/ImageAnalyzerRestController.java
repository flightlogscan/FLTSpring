package com.flt.fltspring;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.core.exception.AzureException;
import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.dao.DocumentIntelligenceDao;
import com.flt.fltspring.model.bizlogic.TableRow;
import com.flt.fltspring.model.bizlogic.TableStructure;
import com.flt.fltspring.model.service.AnalyzeImageResponse;
import com.flt.fltspring.service.ResultConverterService;
import com.flt.fltspring.service.RowConversionService;
import com.flt.fltspring.service.TableDataTransformerService;
import com.flt.fltspring.service.TableProcessorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
public class ImageAnalyzerRestController {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final String ERROR_STATUS = "ERROR";
    
    private final ObjectMapper objectMapper;
    private final DocumentIntelligenceDao documentIntelligenceDao;
    private final ResultConverterService resultConverterService;
    private final TableProcessorService tableProcessorService;
    private final TableDataTransformerService transformer;
    private final RowConversionService rowConversionService;

    /**
     * Takes an image of a flight log and return the text as structured data.
     *
     * Local call:
     * curl --data-binary @"/your/image/path" \
     * -H "Authorization: Bearer {INSERT_BEARER_TOKEN_HERE}" \
     * -X POST localhost:8080/api/analyze > response.txt
     *
     * @param request HttpServlet request
     * @return
     */
    @PostMapping(path = "/analyze", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<AnalyzeImageResponse> submitAnalyzeImage(final HttpServletRequest request) {
        try {
            log.info("Starting document analysis");

            byte[] fileBytes = IOUtils.toByteArray(request.getInputStream());
            if (fileBytes.length > MAX_FILE_SIZE) {
                log.warn("File size exceeds limit: {} bytes", fileBytes.length);
                return buildErrorResponse("File size exceeds maximum limit of 10MB", HttpStatus.PAYLOAD_TOO_LARGE);
            }

            final BinaryData documentData = BinaryData.fromBytes(fileBytes);

            // Raw result from downstream
            final AnalyzeResult analyzeResult = documentIntelligenceDao.analyzeDocumentSync(documentData);

            // Convert to our internal table models with a lightweight transform
            final List<TableStructure> tables = resultConverterService.convertToTable(analyzeResult);

            if (CollectionUtils.isEmpty(tables)) {
                log.warn("No tables detected in the document");
                return buildErrorResponse("No tables detected in the document", HttpStatus.BAD_REQUEST);
            }

            // Extract rows from tables
            final List<TableRow> tableRows = tableProcessorService.extractRowsFromTables(tables);

            // Correct common header and cell errors (For example - '5' being read as 'S' in a number field)
            final List<TableRow> transformedRows = transformer.transformData(tableRows);

            // Convert to response object - light transform just to separate internal and external models
            final AnalyzeImageResponse response = rowConversionService.toRowDTO(transformedRows);

            log.info("Final response structure: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (AzureException e) {
            log.error("Azure service error: {}", e.getMessage());
            return buildErrorResponse("Error from document analysis service", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (TimeoutException e) {
            log.error("Document analysis timed out", e);
            return buildErrorResponse("Document analysis timed out", HttpStatus.GATEWAY_TIMEOUT);
        } catch (IOException e) {
            log.error("Error reading request data", e);
            return buildErrorResponse("Unable to read request data", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Unexpected error processing document", e);
            return buildErrorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private ResponseEntity<AnalyzeImageResponse> buildErrorResponse(String message, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(AnalyzeImageResponse.builder()
                        .status(ERROR_STATUS)
                        .errorMessage(message)
                        .build());
    }
}
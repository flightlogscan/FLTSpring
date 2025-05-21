package com.flt.fltspring;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.core.exception.AzureException;
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
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private static final String SUCCESS_STATUS = "SUCCESS";
    private static final String ERROR_STATUS = "ERROR";
    
    private final ObjectMapper objectMapper;
    private final ResultConverterService resultConverterService;
    private final DocumentIntelligenceDao documentIntelligenceDao;
    private final RowProcessorService rowProcessorService;
    private final TableProcessorService tableProcessorService;

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
            
            // Read input stream with size check
            byte[] fileBytes = IOUtils.toByteArray(request.getInputStream());
            if (fileBytes.length > MAX_FILE_SIZE) {
                log.warn("File size exceeds limit: {} bytes", fileBytes.length);
                return buildErrorResponse("File size exceeds maximum limit of 10MB", HttpStatus.PAYLOAD_TOO_LARGE);
            }
            
            final BinaryData documentData = BinaryData.fromBytes(fileBytes);
            log.info("Successfully read binary data, length: {}", documentData.getLength());

            // Process document through Azure
            final AnalyzeResult analyzeResult = documentIntelligenceDao.analyzeDocumentSync(documentData);
            
            // Process results
            final List<TableStructure> tables = resultConverterService.convertToTable(analyzeResult);
            if (tables.isEmpty()) {
                log.warn("No tables detected in the document");
                return buildErrorResponse("No tables detected in the document", HttpStatus.BAD_REQUEST);
            }
            
            final List<TableRow> tableRows = tableProcessorService.processTables(tables);
            final TableResponseDTO tableResponse = rowProcessorService.processTableRows(tableRows);

            final AnalyzeImageResponse response = AnalyzeImageResponse.builder()
                    .status(SUCCESS_STATUS)
                    .tables(tableResponse.getRows())
                    .build();

            if (log.isDebugEnabled()) {
                log.debug("Final response structure: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
            }

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
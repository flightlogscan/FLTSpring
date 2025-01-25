package com.flt.fltspring;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentRequest;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzeResultOperation;
import com.azure.ai.documentintelligence.models.DocumentAnalysisFeature;
import com.azure.ai.documentintelligence.models.StringIndexType;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.model.AnalyzeImageResponse;
import com.flt.fltspring.model.DocumentAnalysisService;
import com.flt.fltspring.model.LogbookType;
import com.flt.fltspring.model.TableResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;

@RestController
@EnableCaching
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
public class ImageAnalyzerRestController {
    private static final String ENDPOINT = "https://flight-log-scan.cognitiveservices.azure.com/";

    private final String flsDocumentAiSecret;
    private final DocumentAnalysisService documentAnalysisService;
    private final ObjectMapper objectMapper;

    @RequestMapping(method = RequestMethod.POST, path = "/analyze")
    public ResponseEntity<AnalyzeImageResponse> submitAnalyzeImage(
            final HttpServletRequest request,
            @RequestParam(defaultValue = "JEPPESEN") LogbookType logbookType) {

        try {
            // Get binary data from request
            BinaryData documentData = readBinaryData(request);

            // Get Azure analysis result
            AnalyzeResult analyzeResult = getDocumentAnalysis(documentData);

            // Process through service layer with validation and transformation
            TableResponseDTO tableResponse = documentAnalysisService.analyzeDocument(
                    analyzeResult,
                    logbookType
            );

            // Create final response
            final String resultString = serializeResult(analyzeResult);
            final AnalyzeImageResponse response = AnalyzeImageResponse.builder()
                    .status("SUCCESS")
                    //.rawResults(resultString)
                    .tables(tableResponse.getRows())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to process document", e);
            return ResponseEntity.internalServerError()
                    .body(AnalyzeImageResponse.builder()
                            .status("ERROR")
                            .rawResults(e.getMessage())
                            .build());
        }
    }

    private BinaryData readBinaryData(HttpServletRequest request) throws IOException {
        BinaryData data = BinaryData.fromBytes(request.getInputStream().readAllBytes());
        log.info("Successfully read binary data, length: {}", data.getLength());
        return data;
    }

    private AnalyzeResult getDocumentAnalysis(BinaryData documentData) {
        DocumentIntelligenceClient client = new DocumentIntelligenceClientBuilder()
                .credential(new AzureKeyCredential(flsDocumentAiSecret))
                .endpoint(ENDPOINT)
                .buildClient();

        AnalyzeDocumentRequest analyzeRequest = new AnalyzeDocumentRequest()
                .setBase64Source(documentData.toBytes());

        SyncPoller<AnalyzeResultOperation, AnalyzeResult> analyzePoller = client.beginAnalyzeDocument(
                "prebuilt-layout",
                "1",
                "en-US",
                StringIndexType.UTF16CODE_UNIT,
                Collections.singletonList(DocumentAnalysisFeature.KEY_VALUE_PAIRS),
                null,
                null,
                Collections.emptyList(),
                analyzeRequest
        );

        return analyzePoller.getFinalResult();
    }

    private String serializeResult(AnalyzeResult result) throws IOException {
        return objectMapper.writeValueAsString(result);
    }
}
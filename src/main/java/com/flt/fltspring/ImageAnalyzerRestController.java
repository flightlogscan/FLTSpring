package com.flt.fltspring;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
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

    @RequestMapping(method = RequestMethod.POST, path = "/analyze")
    public ResponseEntity<AnalyzeImageResponse> submitAnalyzeImage(
            final HttpServletRequest request,
            @RequestParam(defaultValue = "JEPPESEN") LogbookType logbookType) {

        try {
            log.info("Starting document analysis for logbook type: {}", logbookType);

            // Get binary data from request
            final BinaryData documentData = readBinaryData(request);

            // Get Azure analysis result
            final AnalyzeResult analyzeResult = getDocumentAnalysis(documentData);

            final TableResponseDTO tableResponse = documentAnalysisService.analyzeDocument(
                    analyzeResult,
                    logbookType
            );

            final AnalyzeImageResponse response = AnalyzeImageResponse.builder()
                    .status("SUCCESS")
                    .tables(tableResponse.getRows())
                    .build();

            log.info("Successfully processed document");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to process document", e);
            return ResponseEntity.internalServerError()
                    .body(AnalyzeImageResponse.builder()
                            .status("ERROR")
                            .build());
        }
    }

    private BinaryData readBinaryData(HttpServletRequest request) throws IOException {
        final BinaryData data = BinaryData.fromBytes(request.getInputStream().readAllBytes());
        log.info("Successfully read binary data, length: {}", data.getLength());
        return data;
    }

    private AnalyzeResult getDocumentAnalysis(BinaryData documentData) {
        log.info("Beginning Azure document analysis");
        final DocumentIntelligenceClient client = new DocumentIntelligenceClientBuilder()
                .credential(new AzureKeyCredential(flsDocumentAiSecret))
                .endpoint(ENDPOINT)
                .buildClient();

        final AnalyzeDocumentRequest analyzeRequest = new AnalyzeDocumentRequest()
                .setBase64Source(documentData.toBytes());

        final SyncPoller<AnalyzeResultOperation, AnalyzeResult> analyzePoller = client.beginAnalyzeDocument(
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

        final AnalyzeResult result = analyzePoller.getFinalResult();
        log.info("Completed Azure document analysis");
        return result;
    }
}
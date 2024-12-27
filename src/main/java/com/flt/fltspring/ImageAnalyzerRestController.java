package com.flt.fltspring;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentRequest;
import com.azure.ai.documentintelligence.models.AnalyzeOutputOption;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzeResultOperation;
import com.azure.ai.documentintelligence.models.DocumentAnalysisFeature;
//import com.azure.ai.documentintelligence.models.DocumentTable;
import com.azure.ai.documentintelligence.models.StringIndexType;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.secret.AzureSecretRetriever;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@EnableCaching
@Slf4j
public class ImageAnalyzerRestController {

    private static final String ENDPOINT = "https://flight-log-scan.cognitiveservices.azure.com/";

    @RequestMapping(method = RequestMethod.POST, path = "/api/analyze")
    public ResponseEntity<String> submitAnalyzeImage(final HttpServletRequest request) {
        BinaryData selectionMarkDocumentData;

        try {
            selectionMarkDocumentData = BinaryData.fromBytes(request.getInputStream().readAllBytes());
            log.info("Successfully read binary data from input stream");
            log.info(String.format("Length of binary data: %d", selectionMarkDocumentData.getLength()));
        } catch (IOException e) {
            log.error("Failed to read servlet request input stream.");
            throw new RuntimeException(e);
        }

        final DocumentIntelligenceClient client = new DocumentIntelligenceClientBuilder()
                .credential(new AzureKeyCredential(AzureSecretRetriever.getSecret()))
                .endpoint(ENDPOINT)
                .buildClient();

        final AnalyzeDocumentRequest analyzeRequest = new AnalyzeDocumentRequest()
                .setBase64Source(selectionMarkDocumentData.toBytes());

        // Set parameters
        String modelId = "prebuilt-layout"; // or the appropriate model ID
        String pages = "1"; // Specify the pages to analyze, e.g., "1" for the first page
        String locale = "en-US"; // Set the locale
        StringIndexType stringIndexType = StringIndexType.UTF16CODE_UNIT; // Choose the appropriate string index type
        List<AnalyzeOutputOption> outputOptions = Collections.emptyList(); // Set desired output options

        final SyncPoller<AnalyzeResultOperation, AnalyzeResult> analyzePoller = client.beginAnalyzeDocument(
                modelId,
                pages,
                locale,
                stringIndexType,
                Collections.singletonList(DocumentAnalysisFeature.KEY_VALUE_PAIRS), // Adjust features as needed
                null, // Optional query fields
                null, // Optional output content format
                outputOptions,
                analyzeRequest
        );

        final AnalyzeResult analyzeResult = analyzePoller.getFinalResult();

        final ObjectMapper objectMapper = new ObjectMapper();
        final String resultString;
        try {
            resultString = objectMapper.writeValueAsString(analyzeResult);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(resultString);
    }
}

package com.flt.fltspring;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentRequest;
import com.azure.ai.documentintelligence.models.AnalyzeOutputOption;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzeResultOperation;
import com.azure.ai.documentintelligence.models.DocumentAnalysisFeature;
import com.azure.ai.documentintelligence.models.DocumentTable;
import com.azure.ai.documentintelligence.models.StringIndexType;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.secret.AzureSecretRetriever;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@EnableCaching
public class ImageAnalyzerRestController {

    private static final String ENDPOINT = "https://flight-log-scan.cognitiveservices.azure.com/";

    @RequestMapping(method = RequestMethod.POST, path = "/api/analyze")
    public ResponseEntity<String> submitAnalyzeImage(final HttpServletRequest request) {
        BinaryData selectionMarkDocumentData;

        try {
            selectionMarkDocumentData = BinaryData.fromBytes(request.getInputStream().readAllBytes());
            System.out.println("Successfully read binary data from input stream");
            System.out.printf(String.format("Length of binary data: %d", selectionMarkDocumentData.getLength()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final DocumentIntelligenceClient client = new DocumentIntelligenceClientBuilder()
                .credential(new AzureKeyCredential(AzureSecretRetriever.getSecret()))
                .endpoint(ENDPOINT)
                .buildClient();

        // Create AnalyzeDocumentRequest
        AnalyzeDocumentRequest analyzeRequest = new AnalyzeDocumentRequest()
                .setBase64Source(selectionMarkDocumentData.toBytes()); // Set the Base64 content

        // Set parameters
        String modelId = "prebuilt-layout"; // or the appropriate model ID
        String pages = "1"; // Specify the pages to analyze, e.g., "1" for the first page
        String locale = "en-US"; // Set the locale
        StringIndexType stringIndexType = StringIndexType.UTF16CODE_UNIT; // Choose the appropriate string index type
        List<AnalyzeOutputOption> outputOptions = Collections.emptyList(); // Set desired output options

        // Begin document analysis
        SyncPoller<AnalyzeResultOperation, AnalyzeResult> analyzePoller = client.beginAnalyzeDocument(
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

        // Get the final result
        AnalyzeResult analyzeResult = analyzePoller.getFinalResult();

        // pages
        analyzeResult.getPages().forEach(documentPage -> {
            System.out.printf("Page has width: %.2f and height: %.2f, measured with unit: %s%n",
                    documentPage.getWidth(),
                    documentPage.getHeight(),
                    documentPage.getUnit());

            // words
            if (documentPage.getWords() != null) {
                documentPage.getWords().forEach(documentWord ->
                        System.out.printf("Word '%s' has a confidence score of %.2f%n.",
                                documentWord.getContent(),
                                documentWord.getConfidence()));
            }
        });

        // tables
        List<DocumentTable> tables = analyzeResult.getTables();
        if (tables != null) {
            for (int i = 0; i < tables.size(); i++) {
                DocumentTable documentTable = tables.get(i);
                System.out.printf("Table %d has %d rows and %d columns.%n", i, documentTable.getRowCount(),
                        documentTable.getColumnCount());
                documentTable.getCells().forEach(documentTableCell -> {
                    System.out.printf("Cell '%s', has row index %d and column index %d.%n", documentTableCell.getContent(),
                            documentTableCell.getRowIndex(), documentTableCell.getColumnIndex());
                });
                System.out.println();
            }
        }

        // styles
        if (analyzeResult.getStyles() != null) {
            analyzeResult.getStyles().forEach(documentStyle
                    -> System.out.printf("Document is handwritten %s%n.", documentStyle.isHandwritten()));
        }

        final ObjectMapper objectMapper = new ObjectMapper();
        String resultString;
        try {
            resultString = objectMapper.writeValueAsString(analyzeResult);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(resultString);
    }
}

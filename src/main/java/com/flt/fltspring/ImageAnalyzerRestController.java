package com.flt.fltspring;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.DocumentTable;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.rest.RequestOptions;
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

import java.util.List;

@RestController
@EnableCaching
public class ImageAnalyzerRestController {

    private static final String ENDPOINT = "https://lanceinstance.cognitiveservices.azure.com/";

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

        // Create RequestOptions and set base64 content
        RequestOptions requestOptions = new RequestOptions()
                .addHeader("Content-Type", "application/json")
                .setBody(selectionMarkDocumentData); // This is where the document data goes

        // Begin document analysis and poll for result
        SyncPoller<BinaryData, BinaryData> analyzeLayoutResultPoller =
                client.beginAnalyzeDocument("prebuilt-layout", requestOptions);

        System.out.println(client);

        // Get the final result as BinaryData
        BinaryData binaryResult = analyzeLayoutResultPoller.getFinalResult();

        // Deserialize BinaryData into AnalyzeResult using ObjectMapper
        AnalyzeResult analyzeLayoutResult;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            analyzeLayoutResult = objectMapper.readValue(binaryResult.toString(), AnalyzeResult.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize analyze result.", e);
        }

        // pages
        analyzeLayoutResult.getPages().forEach(documentPage -> {
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
        List<DocumentTable> tables = analyzeLayoutResult.getTables();
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
        if (analyzeLayoutResult.getStyles() != null) {
            analyzeLayoutResult.getStyles().forEach(documentStyle
                    -> System.out.printf("Document is handwritten %s%n.", documentStyle.isHandwritten()));
        }

        final ObjectMapper objectMapper = new ObjectMapper();
        String resultString = "";
        try {
            resultString = objectMapper.writeValueAsString(analyzeLayoutResult);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(resultString);
    }
}

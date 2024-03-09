package com.flt.fltspring;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentTable;
import com.azure.ai.formrecognizer.documentanalysis.models.OperationResult;
import com.azure.ai.formrecognizer.documentanalysis.models.Point;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.secret.AzureSecretRetriever;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
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

        final DocumentAnalysisClient client = new DocumentAnalysisClientBuilder()
                .credential(new AzureKeyCredential(AzureSecretRetriever.getSecret()))
                .endpoint(ENDPOINT)
                .buildClient();

        SyncPoller<OperationResult, AnalyzeResult> analyzeLayoutResultPoller =
                client.beginAnalyzeDocument("prebuilt-layout", selectionMarkDocumentData);

        AnalyzeResult analyzeLayoutResult = analyzeLayoutResultPoller.getFinalResult();

        // pages
        analyzeLayoutResult.getPages().forEach(documentPage -> {
            System.out.printf("Page has width: %.2f and height: %.2f, measured with unit: %s%n",
                    documentPage.getWidth(),
                    documentPage.getHeight(),
                    documentPage.getUnit());

            // lines
            if (documentPage.getLines() != null) {
                documentPage.getLines().forEach(documentLine ->
                        System.out.printf("Line '%s; is within a bounding polygon %s.%n",
                                documentLine.getContent(),
                                getBoundingCoordinates(documentLine.getBoundingPolygon())));
            }

            // words
            if (documentPage.getWords() != null) {
                documentPage.getWords().forEach(documentWord ->
                        System.out.printf("Word '%s' has a confidence score of %.2f%n.",
                                documentWord.getContent(),
                                documentWord.getConfidence()));
            }

            // selection marks
            if (documentPage.getSelectionMarks() != null) {
                documentPage.getSelectionMarks().forEach(documentSelectionMark ->
                        System.out.printf("Selection mark is '%s' and is within a bounding polygon %s with confidence %.2f.%n",
                                documentSelectionMark.getSelectionMarkState().toString(),
                                getBoundingCoordinates(documentSelectionMark.getBoundingPolygon()),
                                documentSelectionMark.getConfidence()));
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

    /**
     * Utility function to get the bounding polygon coordinates.
     */
    private static String getBoundingCoordinates(List<Point> boundingPolygon) {
        return boundingPolygon.stream().map(point -> String.format("[%.2f, %.2f]", point.getX(),
                point.getY())).collect(Collectors.joining(", "));
    }

//    // TODO: We may not need this API if we do everything synchronously in the other one
//    @RequestMapping(method = RequestMethod.GET, path = "/api/analyze/results/{resultId}")
//    public ResponseEntity<AnalyzeImageResponse> getAnalysisResults(@PathVariable final String resultId) {
//        // TODO: Call Azure and get real result status and data
//        final String status = "RUNNING";
//        final String rawResults = "{}";
//
//        final AnalyzeImageResponse response = AnalyzeImageResponse.builder()
//                                                                  .status(status)
//                                                                  .rawResults(rawResults)
//                                                                  .build();
//;
//        // This result ID is used to poll for results
//        return ResponseEntity.ok(response);
//    }

}

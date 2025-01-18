package com.flt.fltspring;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentRequest;
import com.azure.ai.documentintelligence.models.AnalyzeOutputOption;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzeResultOperation;
import com.azure.ai.documentintelligence.models.DocumentAnalysisFeature;
import com.azure.ai.documentintelligence.models.DocumentTable;
import com.azure.ai.documentintelligence.models.DocumentTableCell;
import com.azure.ai.documentintelligence.models.StringIndexType;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.model.AnalyzeImageResponse;
import com.flt.fltspring.model.TableRow;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@EnableCaching
@Slf4j
public class ImageAnalyzerRestController {

    @Autowired
    private String flsDocumentAiSecret;
    private static final String ENDPOINT = "https://flight-log-scan.cognitiveservices.azure.com/";

    @RequestMapping(method = RequestMethod.POST, path = "/api/analyze")
    public ResponseEntity<AnalyzeImageResponse> submitAnalyzeImage(final HttpServletRequest request) {
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
                .credential(new AzureKeyCredential(flsDocumentAiSecret))
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

        final List<TableRow> tableRows = new ArrayList<>();
        final Map<Integer, Map<Integer, String>> rowData = new HashMap<>();
        final Map<Integer, Boolean> headerRows = new HashMap<>();

        final AnalyzeResult analyzeResult = analyzePoller.getFinalResult();

        final List<DocumentTable> documentTables = analyzeResult.getTables();
        for (final DocumentTable documentTable : documentTables) {
            for (final DocumentTableCell documentTableCell : documentTable.getCells()) {
                int rowIndex = documentTableCell.getRowIndex();
                int columnIndex = documentTableCell.getColumnIndex();
                String content = documentTableCell.getContent();
                boolean isHeader = "columnHeader".equals(documentTableCell.getKind());

                // Initialize row data if needed
                rowData.putIfAbsent(rowIndex, new HashMap<>());
                rowData.get(rowIndex).put(columnIndex, content);

                // Mark row as header if any cell is a header
                if (isHeader) {
                    headerRows.put(rowIndex, true);
                }
            }

            // Create TableRow objects from collected data
            rowData.forEach((rowIndex, columnData) -> {
                TableRow tableRow = new TableRow(
                        rowIndex,
                        columnData,
                        headerRows.getOrDefault(rowIndex, false)
                );
                tableRows.add(tableRow);
            });
        }

        final ObjectMapper objectMapper = new ObjectMapper();
        final String resultString;
        try {
            resultString = objectMapper.writeValueAsString(analyzeResult);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final AnalyzeImageResponse analyzeImageResponse = AnalyzeImageResponse.builder()
                .rawResults(resultString)
                .tables(tableRows)
                .build();
        return ResponseEntity.ok(analyzeImageResponse);
    }
}

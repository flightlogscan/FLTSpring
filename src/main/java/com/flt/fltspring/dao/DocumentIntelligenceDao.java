package com.flt.fltspring.dao;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIntelligenceDao {
    private static final String ENDPOINT = "https://flight-log-scan.cognitiveservices.azure.com/";

    public AnalyzeResult getDocumentAnalysis(BinaryData documentData, String flsDocumentAiSecret) {
        log.info("Beginning Azure document analysis");

        //TODO: Make singleton. Don't create client on per request basis.
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

package com.flt.fltspring.dao;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentRequest;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzeResultOperation;
import com.azure.ai.documentintelligence.models.DocumentAnalysisFeature;
import com.azure.ai.documentintelligence.models.StringIndexType;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.exception.AzureException;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class DocumentIntelligenceDao {
    private final DocumentIntelligenceClient client;
    
    public DocumentIntelligenceDao(@Value("${azure.document-intelligence.endpoint:https://flight-log-scan.cognitiveservices.azure.com/}") String endpoint,
                                   String flsDocumentAiSecret) {
        this.client = new DocumentIntelligenceClientBuilder()
                .credential(new AzureKeyCredential(flsDocumentAiSecret))
                .endpoint(endpoint)
                .buildClient();
        log.info("Document Intelligence client initialized with endpoint: {}", endpoint);
    }

    public AnalyzeResult analyzeDocumentSync(BinaryData documentData) throws AzureException, TimeoutException {
        if (documentData == null || documentData.getLength() == 0) {
            throw new IllegalArgumentException("Document data cannot be null or empty");
        }
        
        log.info("Beginning Azure document analysis for document of size: {} bytes", documentData.getLength());

        try {
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
            log.info("Successfully completed Azure document analysis");
            return result;
        } catch (AzureException e) {
            log.error("Azure document analysis failed with error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during document analysis", e);
            throw e;
        }
    }
}

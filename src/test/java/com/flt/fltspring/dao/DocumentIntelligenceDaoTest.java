package com.flt.fltspring.dao;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentOptions;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzeOperationDetails;
import com.azure.ai.documentintelligence.models.DocumentAnalysisFeature;
import com.azure.ai.documentintelligence.models.StringIndexType;
import com.azure.core.exception.AzureException;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIntelligenceDaoTest {

    @Mock
    private DocumentIntelligenceClient mockClient;

    @Mock
    private SyncPoller<AnalyzeOperationDetails, AnalyzeResult> mockPoller;

    @Mock
    private AnalyzeResult mockAnalyzeResult;

    private DocumentIntelligenceDao documentIntelligenceDao;

    @Captor
    private ArgumentCaptor<AnalyzeDocumentOptions> optionsCaptor;

    private static final String TEST_ENDPOINT = "https://test-endpoint.cognitiveservices.azure.com/";
    private static final String TEST_SECRET = "test-secret";

    @BeforeEach
    void setUp() {
        documentIntelligenceDao = new DocumentIntelligenceDao(TEST_ENDPOINT, TEST_SECRET);

        // Use reflection to set the final 'client' field with our mock
        try {
            java.lang.reflect.Field clientField = DocumentIntelligenceDao.class.getDeclaredField("client");
            clientField.setAccessible(true);
            clientField.set(documentIntelligenceDao, mockClient);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject mock client", e);
        }
    }

    @Test
    void analyzeDocumentSync_success() throws AzureException, TimeoutException {
        BinaryData documentData = BinaryData.fromString("Test document content.");
        when(mockClient.beginAnalyzeDocument(eq("prebuilt-layout"), any(AnalyzeDocumentOptions.class)))
                .thenReturn(mockPoller);
        when(mockPoller.getFinalResult()).thenReturn(mockAnalyzeResult);

        AnalyzeResult result = documentIntelligenceDao.analyzeDocumentSync(documentData);

        assertNotNull(result);
        assertSame(mockAnalyzeResult, result);
        verify(mockClient).beginAnalyzeDocument(eq("prebuilt-layout"), optionsCaptor.capture());
        AnalyzeDocumentOptions capturedOptions = optionsCaptor.getValue();
        assertNotNull(capturedOptions);
        assertEquals("1", capturedOptions.getPages().get(0));
        assertEquals("en-US", capturedOptions.getLocale());
        assertNotNull(capturedOptions.getDocumentAnalysisFeatures());
        assertFalse(capturedOptions.getDocumentAnalysisFeatures().isEmpty());
        assertEquals(DocumentAnalysisFeature.KEY_VALUE_PAIRS.toString(), capturedOptions.getDocumentAnalysisFeatures().get(0).toString());
        assertEquals(StringIndexType.UTF16_CODE_UNIT, capturedOptions.getStringIndexType());
        // The BinaryData is passed to the constructor of AnalyzeDocumentOptions,
        // but there's no public getter for it on the options object itself.
        // Its presence is implicitly tested by the successful call and option configuration.
    }

    @Test
    void analyzeDocumentSync_nullDocumentData_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            documentIntelligenceDao.analyzeDocumentSync(null);
        });
        assertEquals("Document data cannot be null or empty", exception.getMessage());
        verifyNoInteractions(mockClient);
    }

    @Test
    void analyzeDocumentSync_emptyDocumentData_throwsIllegalArgumentException() {
        BinaryData emptyData = BinaryData.fromBytes(new byte[0]);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            documentIntelligenceDao.analyzeDocumentSync(emptyData);
        });
        assertEquals("Document data cannot be null or empty", exception.getMessage());
        verifyNoInteractions(mockClient);
    }

    @Test
    void analyzeDocumentSync_beginAnalyzeThrowsAzureException_rethrowsAzureException() throws TimeoutException {
        BinaryData documentData = BinaryData.fromString("Test document content.");
        AzureException expectedException = new AzureException("Azure analysis failed from beginAnalyzeDocument");

        when(mockClient.beginAnalyzeDocument(eq("prebuilt-layout"), any(AnalyzeDocumentOptions.class)))
                .thenThrow(expectedException);

        AzureException actualException = assertThrows(AzureException.class, () -> {
            documentIntelligenceDao.analyzeDocumentSync(documentData);
        });

        assertSame(expectedException, actualException);
        assertEquals("Azure analysis failed from beginAnalyzeDocument", actualException.getMessage());
        verify(mockClient).beginAnalyzeDocument(eq("prebuilt-layout"), any(AnalyzeDocumentOptions.class));
        verifyNoInteractions(mockPoller); // Poller is not obtained if beginAnalyzeDocument fails
    }

    @Test
    void analyzeDocumentSync_beginAnalyzeThrowsRuntimeException_rethrowsRuntimeException() throws TimeoutException {
        BinaryData documentData = BinaryData.fromString("Test document content.");
        RuntimeException expectedException = new RuntimeException("Unexpected error from beginAnalyzeDocument");

        when(mockClient.beginAnalyzeDocument(eq("prebuilt-layout"), any(AnalyzeDocumentOptions.class)))
                .thenThrow(expectedException);

        RuntimeException actualException = assertThrows(RuntimeException.class, () -> {
            documentIntelligenceDao.analyzeDocumentSync(documentData);
        });

        assertSame(expectedException, actualException);
        assertEquals("Unexpected error from beginAnalyzeDocument", actualException.getMessage());
        verify(mockClient).beginAnalyzeDocument(eq("prebuilt-layout"), any(AnalyzeDocumentOptions.class));
        verifyNoInteractions(mockPoller);
    }

    @Test
    void analyzeDocumentSync_pollerGetFinalResultThrowsAzureException_rethrowsAzureException() throws TimeoutException {
        BinaryData documentData = BinaryData.fromString("Test document content.");
        AzureException expectedException = new AzureException("Azure analysis failed from poller.getFinalResult");

        when(mockClient.beginAnalyzeDocument(eq("prebuilt-layout"), any(AnalyzeDocumentOptions.class)))
                .thenReturn(mockPoller);
        when(mockPoller.getFinalResult()).thenThrow(expectedException); // AzureException is a RuntimeException, so thenThrow is fine.

        AzureException actualException = assertThrows(AzureException.class, () -> {
            documentIntelligenceDao.analyzeDocumentSync(documentData);
        });

        assertSame(expectedException, actualException);
        assertEquals("Azure analysis failed from poller.getFinalResult", actualException.getMessage());
        verify(mockClient).beginAnalyzeDocument(eq("prebuilt-layout"), any(AnalyzeDocumentOptions.class));
        verify(mockPoller).getFinalResult();
    }

    @Test
    void analyzeDocumentSync_pollerGetFinalResultThrowsTimeoutException_rethrowsTimeoutException() throws AzureException {
        BinaryData documentData = BinaryData.fromString("Test document content.");
        TimeoutException expectedTimeoutException = new TimeoutException("Poller timed out");

        when(mockClient.beginAnalyzeDocument(eq("prebuilt-layout"), any(AnalyzeDocumentOptions.class)))
                .thenReturn(mockPoller);
        when(mockPoller.getFinalResult()).thenThrow(new RuntimeException(expectedTimeoutException));

        RuntimeException actualException = assertThrows(RuntimeException.class, () -> {
            documentIntelligenceDao.analyzeDocumentSync(documentData);
        });

        assertTrue(actualException.getCause() instanceof TimeoutException);
        assertEquals("Poller timed out", actualException.getCause().getMessage());
        verify(mockClient).beginAnalyzeDocument(eq("prebuilt-layout"), any(AnalyzeDocumentOptions.class));
        verify(mockPoller).getFinalResult();
    }

    @Test
    void analyzeDocumentSync_pollerGetFinalResultThrowsRuntimeException_rethrowsRuntimeException() throws TimeoutException {
        BinaryData documentData = BinaryData.fromString("Test document content.");
        RuntimeException underlyingSdkException = new RuntimeException("Simulated SDK internal error from poller");

        when(mockClient.beginAnalyzeDocument(eq("prebuilt-layout"), any(AnalyzeDocumentOptions.class)))
                .thenReturn(mockPoller);
        when(mockPoller.getFinalResult()).thenThrow(underlyingSdkException);

        RuntimeException actualException = assertThrows(RuntimeException.class, () -> {
            documentIntelligenceDao.analyzeDocumentSync(documentData);
        });

        assertSame(underlyingSdkException, actualException); // The original exception is rethrown.
        assertEquals("Simulated SDK internal error from poller", actualException.getMessage());
        verify(mockClient).beginAnalyzeDocument(eq("prebuilt-layout"), any(AnalyzeDocumentOptions.class));
        verify(mockPoller).getFinalResult();
    }

    @Test
    void constructor_initializesClientSuccessfully() {
        // Test that the constructor can be called without issue and that the (mocked) client
        // would be used in subsequent operations. This is indirectly verified by other tests
        // not throwing NPEs related to the client.
        // For direct verification of the real client initialization logic (which is out of scope for this pure unit test
        // of the DAO's methods, as we mock the client), an integration test would be needed.
        assertNotNull(documentIntelligenceDao);
        // The DAO logs upon initialization; log verification could be added if a test logger is set up.
    }
}

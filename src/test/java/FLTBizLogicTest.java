import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.config.CharacterReplacementConfig;
import com.flt.fltspring.config.LogbookConfiguration;
import com.flt.fltspring.model.bizlogic.TableRow;
import com.flt.fltspring.model.bizlogic.TableStructure;
import com.flt.fltspring.model.service.AnalyzeImageResponse;
import com.flt.fltspring.model.service.RowDTO;
import com.flt.fltspring.service.LogbookValidationService;
import com.flt.fltspring.service.ResultConverterService;
import com.flt.fltspring.service.RowConversionService;
import com.flt.fltspring.service.TableDataTransformerService;
import com.flt.fltspring.service.TableProcessorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import support.UnitTestBase;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This not-quite end-to-end test checks the FLTSpring business logic for ImageAnalyzerRestController.
 * Goal is to make sure the logic from after the Azure call until the API Response acts as expected.
 * It shouldn't mock anything or have downstream dependencies.
 * It's so we don't fuck up business logic when making changes.
 *
 * Input is AnalyzeResult
 * Output is AnalyzeImageResponse
 */
@ExtendWith(MockitoExtension.class)
class FLTBizLogicTest extends UnitTestBase {

    private ResultConverterService resultConverterService;
    private TableProcessorService tableProcessorService;
    private TableDataTransformerService tableDataTransformerService;
    private LogbookValidationService logbookValidationService;
    private RowConversionService rowConversionService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        CharacterReplacementConfig config = new CharacterReplacementConfig();
        resultConverterService = new ResultConverterService();
        tableProcessorService = new TableProcessorService();
        tableDataTransformerService = new TableDataTransformerService(
                    new LogbookConfiguration().columnConfigs(),
                    config
                );

        config.initialize();
        logbookValidationService = new LogbookValidationService();
        rowConversionService = new RowConversionService();
        objectMapper = new ObjectMapper();
    }

    @Test
    void FLTBizLogicTest_1row_success() throws Exception {
        runAnalyzeImageTest("AnalyzeResultOneRowExample.json", expectedAnalyzeImageResponse);
    }

    private void runAnalyzeImageTest(String inputFile, AnalyzeImageResponse expectedResponse) throws Exception {
        try (InputStream analyzeResultStream = getClass().getClassLoader().getResourceAsStream(inputFile)) {
            Assertions.assertNotNull(analyzeResultStream);
            String analyzeResultJson = new String(analyzeResultStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonReader jsonReader = JsonProviders.createReader(analyzeResultJson);
            AnalyzeResult analyzeResult = AnalyzeResult.fromJson(jsonReader);

            final List<TableStructure> tables = resultConverterService.convertToTable(analyzeResult);
            final List<TableRow> tableRows = tableProcessorService.processTables(tables);
            final List<TableRow> transformed = tableDataTransformerService.transformData(tableRows);
            final List<TableRow> validated = logbookValidationService.validateAndCorrect(transformed);
            final AnalyzeImageResponse actualResponse = rowConversionService.toRowDTO(validated);

            assertEquals(expectedResponse.getStatus(), actualResponse.getStatus(), "Status mismatch");
            assertEquals(expectedResponse.getErrorMessage(), actualResponse.getErrorMessage(), "Error message mismatch");
            assertRowsEqual(expectedResponse.getTables(), actualResponse.getTables());
        }
    }

    private void assertRowsEqual(List<RowDTO> expectedRows, List<RowDTO> actualRows) {
        assertEquals(expectedRows.size(), actualRows.size(), "Table size mismatch");

        for (int i = 0; i < expectedRows.size(); i++) {
            var expectedRow = expectedRows.get(i);
            var actualRow = actualRows.get(i);

            assertEquals(expectedRow.rowIndex(), actualRow.rowIndex(), "Row index mismatch at row " + i);
            assertEquals(expectedRow.header(), actualRow.header(), "Header flag mismatch at row " + i);

            for (java.util.Map.Entry<Integer, String> entry : expectedRow.content().entrySet()) {
                Integer key = entry.getKey();
                String expectedVal = entry.getValue();
                String actualVal = actualRow.content().get(key);
                assertEquals(expectedVal, actualVal, "Content mismatch at row " + i + ", key: " + key);
            }

            assertEquals(expectedRow.parentHeaders(), actualRow.parentHeaders(), "Parent headers mismatch at row " + i);
        }
    }
}

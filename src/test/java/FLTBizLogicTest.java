import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.config.CharacterReplacementConfig;
import com.flt.fltspring.config.LogbookConfiguration;
import com.flt.fltspring.model.bizlogic.TableRow;
import com.flt.fltspring.model.bizlogic.TableStructure;
import com.flt.fltspring.model.service.AnalyzeImageResponse;
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
    void FLTBizLogicTest_success() throws Exception {
        try (InputStream analyzeResultStream = getClass().getClassLoader().getResourceAsStream("AnalyzeResultExample.json")) {

            Assertions.assertNotNull(analyzeResultStream);
            String analyzeResultJson = new String(analyzeResultStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonReader jsonReader = JsonProviders.createReader(analyzeResultJson);
            AnalyzeResult analyzeResult = AnalyzeResult.fromJson(jsonReader);

            final List<TableStructure> tables = resultConverterService.convertToTable(analyzeResult);
            final List<TableRow> tableRows = tableProcessorService.processTables(tables);
            final List<TableRow> transformed = tableDataTransformerService.transformData(tableRows);
            final List<TableRow> validated = logbookValidationService.validateAndCorrect(transformed);
            final AnalyzeImageResponse actualResponse = rowConversionService.toRowDTO(validated);

            assertEquals(expectedAnalyzeImageResponse, actualResponse);
        }
    }
}
package support;

import com.flt.fltspring.model.bizlogic.TableRow;
import com.flt.fltspring.model.service.AnalyzeImageResponse;
import com.flt.fltspring.model.service.RowDTO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UnitTestBase {

    protected final RowDTO headerRow;
    protected final RowDTO dataRow;
    protected final AnalyzeImageResponse response;

    public UnitTestBase() {
        Map<Integer, String> headerContent = new LinkedHashMap<>();
        headerContent.put(0, "DATE");
        headerContent.put(1, "AIRCRAFT");
        headerContent.put(2, "REGISTRATION");
        headerContent.put(3, "FROM");
        headerContent.put(4, "TO");
        headerContent.put(5, "TIME");
        headerContent.put(6, "PIC");
        headerContent.put(7, "SIC");
        headerContent.put(8, "DUAL");
        headerContent.put(9, "GROUND");
        headerContent.put(10, "SIM");
        headerContent.put(11, "XC");
        headerContent.put(12, "DAY");
        headerContent.put(13, "NIGHT");
        headerContent.put(14, "IFR");
        headerContent.put(15, "ACT");
        headerContent.put(16, "SIM");
        headerContent.put(17, "LANDINGS");
        headerContent.put(18, "REMARKS");

        Map<Integer, String> headerParentHeaders = Map.ofEntries(
                Map.entry(3, "ROUTE"),
                Map.entry(4, "ROUTE"),
                Map.entry(12, "CONDITION"),
                Map.entry(13, "CONDITION"),
                Map.entry(14, "CONDITION"),
                Map.entry(15, "INSTRUMENT"),
                Map.entry(16, "INSTRUMENT")
        );

        headerRow = new RowDTO(
                0,
                headerContent,
                headerParentHeaders,
                true
        );

        Map<Integer, String> dataContent = new LinkedHashMap<>();
        dataContent.put(0, "2023-01-01");
        dataContent.put(1, "Cessna 172");
        dataContent.put(2, "N12345");
        dataContent.put(3, "KJFK");
        dataContent.put(4, "KLAX");
        dataContent.put(5, "5.0");
        dataContent.put(6, "5.0");
        dataContent.put(7, "0.0");
        dataContent.put(8, "0.0");
        dataContent.put(9, "0.0");
        dataContent.put(10, "0.0");
        dataContent.put(11, "5.0");
        dataContent.put(12, "5.0");
        dataContent.put(13, "0.0");
        dataContent.put(14, "0.0");
        dataContent.put(15, "0");
        dataContent.put(16, "0");
        dataContent.put(17, "1");
        dataContent.put(18, "No remarks");

        Map<Integer, String> dataParentHeaders = Map.ofEntries(
                Map.entry(3, "ROUTE"),
                Map.entry(4, "ROUTE"),
                Map.entry(12, "CONDITION"),
                Map.entry(13, "CONDITION"),
                Map.entry(14, "CONDITION"),
                Map.entry(15, "INSTRUMENT"),
                Map.entry(16, "INSTRUMENT")
        );

        dataRow = new RowDTO(
                1,
                dataContent,
                dataParentHeaders,
                false
        );

        response = AnalyzeImageResponse.builder()
                .tables(List.of(headerRow, dataRow))
                .status("SUCCESS")
                .errorMessage(null)
                .build();
    }

    protected List<TableRow> defaultTableRows() {
        return List.of(
            new TableRow(headerRow.rowIndex(), headerRow.content(), headerRow.isHeader(), headerRow.parentHeaders()),
            new TableRow(dataRow.rowIndex(), dataRow.content(), dataRow.isHeader(), dataRow.parentHeaders())
        );
    }
}

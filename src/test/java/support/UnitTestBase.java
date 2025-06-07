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
    protected final AnalyzeImageResponse expectedAnalyzeImageResponse;

    public UnitTestBase() {
        Map<Integer, String> headerContent = new LinkedHashMap<>();
        headerContent.put(1,  "DATE");
        headerContent.put(2,  "AIRCRAFT TYPE");
        headerContent.put(3,  "AIRCRAFT IDENT");
        headerContent.put(4,  "FROM");
        headerContent.put(5,  "TO");
        headerContent.put(6,  "NR INST APP");
        headerContent.put(7,  "REMARKS AND ENDORSEMENTS");
        headerContent.put(8,  "NR T/O");
        headerContent.put(9,  "NR LDG");
        headerContent.put(10, "SINGLE-ENGINE LAND");
        headerContent.put(11, "SINGLE-ENGINE LAND");
        headerContent.put(12, "MULTI ENGINE LAND");
        headerContent.put(13, "MULTI ENGINE LAND");
        headerContent.put(14, "INT APR");
        headerContent.put(15, "INT APR");
        headerContent.put(16, "INST APR");
        headerContent.put(17, "INST APR");
        headerContent.put(18, "NIGHT");
        headerContent.put(19, "NIGHT");
        headerContent.put(20, "ACTUAL INSTRUMENT");
        headerContent.put(21, "ACTUAL INSTRUMENT");
        headerContent.put(22, "SIMULATED INSTRUMENT (HOOD)");
        headerContent.put(23, "SIMULATED INSTRUMENT (HOOD)");
        headerContent.put(24, "FLIGHT SIMULATOR");
        headerContent.put(25, "FLIGHT SIMULATOR");
        headerContent.put(26, "CROSS COUNTRY");
        headerContent.put(27, "CROSS COUNTRY");
        headerContent.put(28, "AS FLIGHT INSTRUCTOR");
        headerContent.put(29, "AS FLIGHT INSTRUCTOR");
        headerContent.put(30, "DUAL RECEIVED");
        headerContent.put(31, "DUAL RECEIVED");
        headerContent.put(32, "PILOT IN COMMAND (INCL SOLO)");
        headerContent.put(33, "PILOT IN COMMAND (INCL SOLO)");
        headerContent.put(34, "TOTAL DURATION OF FLIGHT");
        headerContent.put(35, "TOTAL DURATION OF FLIGHT");

        Map<Integer, String> headerParentHeaders = Map.ofEntries(
            Map.entry(1,  "DATE"),
            Map.entry(2,  "AIRCRAFT TYPE"),
            Map.entry(3,  "AIRCRAFT IDENT"),
            Map.entry(4,  "ROUTE OF FLIGHT"),
            Map.entry(5,  "ROUTE OF FLIGHT"),
            Map.entry(6,  "NR INST. APP."),
            Map.entry(7,  "REMARKS AND ENDORSEMENTS"),
            Map.entry(8,  "NR T/O"),
            Map.entry(9,  "NR LDG"),
            Map.entry(10, "AIRCRAFT CATEGORY SINGLE-"),
            Map.entry(11, "AIRCRAFT CATEGORY SINGLE-"),
            Map.entry(12, "AIRCRAFT CATEGORY SINGLE-"),
            Map.entry(13, "AIRCRAFT CATEGORY SINGLE-"),
            Map.entry(14, "AND CLASS"),
            Map.entry(15, "AND CLASS"),
            Map.entry(16, "AND CLASS"),
            Map.entry(17, "AND CLASS"),
            Map.entry(18, "CONDITIONS OF FLIGHT"),
            Map.entry(19, "CONDITIONS OF FLIGHT"),
            Map.entry(20, "CONDITIONS OF FLIGHT"),
            Map.entry(21, "CONDITIONS OF FLIGHT"),
            Map.entry(22, "CONDITIONS OF FLIGHT"),
            Map.entry(23, "CONDITIONS OF FLIGHT"),
            Map.entry(24, "FLIGHT SIMULATOR"),
            Map.entry(25, "FLIGHT SIMULATOR"),
            Map.entry(26, "TYPE OF PILOTING TIME"),
            Map.entry(27, "TYPE OF PILOTING TIME"),
            Map.entry(28, "TYPE OF PILOTING TIME"),
            Map.entry(29, "TYPE OF PILOTING TIME"),
            Map.entry(30, "TYPE OF PILOTING TIME"),
            Map.entry(31, "TYPE OF PILOTING TIME"),
            Map.entry(32, "TYPE OF PILOTING TIME"),
            Map.entry(33, "TYPE OF PILOTING TIME"),
            Map.entry(34, "TOTAL DURATION OF FLIGHT"),
            Map.entry(35, "TOTAL DURATION OF FLIGHT")
        );

        headerRow = new RowDTO(
                0,
                headerContent,
                headerParentHeaders,
                true
        );

        Map<Integer, String> dataContent = new LinkedHashMap<>();
        dataContent.put(1,  "2/2");
        dataContent.put(2,  "C1725");
        dataContent.put(3,  "N678ND");
        dataContent.put(4,  "PHX");
        dataContent.put(5,  "SEA");
        dataContent.put(6,  "2");
        dataContent.put(7,  "avilliqué");
        dataContent.put(8,  "3");
        dataContent.put(9,  "3");
        dataContent.put(10, "1");
        dataContent.put(11, "5");
        dataContent.put(12, "0");
        dataContent.put(13, "0");
        dataContent.put(14, "RNAV");
        dataContent.put(15, "CGC");
        dataContent.put(16, "VOR RW-15");
        dataContent.put(17, "ClaZ");
        dataContent.put(18, "0");
        dataContent.put(19, "0");
        dataContent.put(20, "0");
        dataContent.put(21, "0");
        dataContent.put(22, "0");
        dataContent.put(23, "0");
        dataContent.put(24, "0");
        dataContent.put(25, "0");
        dataContent.put(26, "1");
        dataContent.put(27, "5");
        dataContent.put(28, "0");
        dataContent.put(29, "0");
        dataContent.put(30, "0");
        dataContent.put(31, "0");
        dataContent.put(32, "1");
        dataContent.put(33, "5");
        dataContent.put(34, "1");
        dataContent.put(35, "5");

        Map<Integer, String> dataParentHeaders = headerParentHeaders;

        dataRow = new RowDTO(
                1,
                dataContent,
                dataParentHeaders,
                false
        );

        expectedAnalyzeImageResponse = AnalyzeImageResponse.builder()
                .tables(List.of(headerRow, dataRow))
                .status("SUCCESS")
                .errorMessage(null)
                .build();
    }

    protected List<TableRow> defaultTableRows() {
        return List.of(
            new TableRow(headerRow.rowIndex(), headerRow.content(), headerRow.header(), headerRow.parentHeaders()),
            new TableRow(dataRow.rowIndex(), dataRow.content(), dataRow.header(), dataRow.parentHeaders())
        );
    }
}

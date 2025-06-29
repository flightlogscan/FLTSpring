package com.flt.fltspring.service;

import com.flt.fltspring.config.CharacterReplacementConfig;
import com.flt.fltspring.config.LogbookConfiguration;
import com.flt.fltspring.model.bizlogic.TableRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TableDataTransformerServiceTest {

    private TableDataTransformerService service;

    @BeforeEach
    void setUp() {
        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.initialize();
        service = new TableDataTransformerService(new LogbookConfiguration().columnConfigs(), config);
    }

    @Test
    void transformData_returnsEmptyForNull() {
        assertThat(service.transformData(null)).isEmpty();
    }

    @Test
    void transformData_normalizesHeaderAndValues() {
        LinkedHashMap<Integer, String> headerData = new LinkedHashMap<>();
        headerData.put(1, "MULTI- ENGINE LAND");
        TableRow headerRow = TableRow.builder()
                .rowIndex(0)
                .columnData(headerData)
                .isHeader(true)
                .build();

        LinkedHashMap<Integer, String> rowData = new LinkedHashMap<>();
        rowData.put(1, "O5");
        TableRow dataRow = TableRow.builder()
                .rowIndex(1)
                .columnData(rowData)
                .isHeader(false)
                .build();

        List<TableRow> result = service.transformData(List.of(headerRow, dataRow));

        assertThat(result).hasSize(2);
        TableRow normalizedHeader = result.get(0);
        assertThat(normalizedHeader.getColumnData().get(1)).isEqualTo("MULTI-ENGINE LAND");
        TableRow normalizedData = result.get(1);
        assertThat(normalizedData.getColumnData().get(1)).isEqualTo("05");
    }
}

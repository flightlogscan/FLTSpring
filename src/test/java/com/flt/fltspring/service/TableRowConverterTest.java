package com.flt.fltspring.service;

import com.flt.fltspring.model.AnalyzeImageResponse;
import com.flt.fltspring.model.RowDTO;
import com.flt.fltspring.model.TableRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import support.UnitTestBase;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TableRowConverterTest extends UnitTestBase {

    private LogbookValidationService validationService;
    private TableDataTransformerService transformerService;
    private RowConversionService rowConversionService;

    @BeforeEach
    void setUp() {
        validationService = mock(LogbookValidationService.class);
        transformerService = mock(TableDataTransformerService.class);
        rowConversionService = new RowConversionService(validationService, transformerService);
    }

    @Test
    void convertRowsToDTO_basicConversion() {
        List<TableRow> inputRows = defaultTableRows();

        when(validationService.validateAndCorrect(inputRows)).thenReturn(inputRows);
        when(transformerService.transformData(inputRows)).thenReturn(inputRows);

        AnalyzeImageResponse response = rowConversionService.toRowDTO(inputRows);

        assertThat(response).isNotNull();
        assertThat(response.getTables()).hasSize(2);

        RowDTO row0 = response.getTables().get(0);
        assertThat(row0.rowIndex()).isEqualTo(headerRow.rowIndex());
        assertThat(row0.isHeader()).isEqualTo(headerRow.isHeader());
        assertThat(row0.content()).isEqualTo(headerRow.content());
        assertThat(row0.parentHeaders()).isEqualTo(headerRow.parentHeaders());

        RowDTO row1 = response.getTables().get(1);
        assertThat(row1.rowIndex()).isEqualTo(dataRow.rowIndex());
        assertThat(row1.isHeader()).isEqualTo(dataRow.isHeader());
        assertThat(row1.content()).isEqualTo(dataRow.content());
        assertThat(row1.parentHeaders()).isEqualTo(dataRow.parentHeaders());
    }

    @Test
    void convertRowsToDTO_handlesEmptyInput() {
        when(validationService.validateAndCorrect(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(transformerService.transformData(Collections.emptyList())).thenReturn(Collections.emptyList());

        AnalyzeImageResponse response = rowConversionService.toRowDTO(Collections.emptyList());

        assertThat(response).isNotNull();
        assertThat(response.getTables()).isEmpty();
    }

    @Test
    void convertRowsToDTO_handlesNullInput() {
        AnalyzeImageResponse response = rowConversionService.toRowDTO(null);
        assertThat(response).isNotNull();
        assertThat(response.getTables()).isEmpty();
    }

    @Test
    void convertRowsToDTO_transfersParentHeaders() {
        TableRow rowWithParentHeaders = new TableRow(dataRow.rowIndex(), dataRow.content(), dataRow.isHeader(), dataRow.parentHeaders());
        List<TableRow> inputRows = List.of(rowWithParentHeaders);

        when(validationService.validateAndCorrect(inputRows)).thenReturn(inputRows);
        when(transformerService.transformData(inputRows)).thenReturn(inputRows);

        AnalyzeImageResponse response = rowConversionService.toRowDTO(inputRows);

        assertThat(response).isNotNull();
        assertThat(response.getTables()).hasSize(1);
        RowDTO dto = response.getTables().get(0);
        assertThat(dto.rowIndex()).isEqualTo(dataRow.rowIndex());
        assertThat(dto.parentHeaders()).isNotNull();
        assertThat(dto.parentHeaders()).containsAllEntriesOf(dataRow.parentHeaders());
    }
}
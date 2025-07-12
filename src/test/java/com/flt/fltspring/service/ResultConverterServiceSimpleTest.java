package com.flt.fltspring.service;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.DocumentTable;
import com.azure.ai.documentintelligence.models.DocumentTableCell;
import com.azure.ai.documentintelligence.models.BoundingRegion;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ResultConverterServiceSimpleTest {
    @Test
    void convertToTable_basic() {
        AnalyzeResult result = Mockito.mock(AnalyzeResult.class);
        DocumentTable table = Mockito.mock(DocumentTable.class);
        DocumentTableCell cell = Mockito.mock(DocumentTableCell.class);
        BoundingRegion region = Mockito.mock(BoundingRegion.class);

        when(result.getTables()).thenReturn(List.of(table));
        when(table.getColumnCount()).thenReturn(1);
        when(table.getCells()).thenReturn(List.of(cell));
        when(table.getBoundingRegions()).thenReturn(List.of(region));
        when(region.getPageNumber()).thenReturn(2);
        when(cell.getRowIndex()).thenReturn(0);
        when(cell.getColumnIndex()).thenReturn(0);
        when(cell.getContent()).thenReturn("c");
        when(cell.getColumnSpan()).thenReturn(1);

        ResultConverterService svc = new ResultConverterService();
        var tables = svc.convertToTable(result);
        assertThat(tables).hasSize(1);
        assertThat(tables.get(0).getColumnCount()).isEqualTo(1);
        assertThat(tables.get(0).getPageNumber()).isEqualTo(2);
        assertThat(tables.get(0).getCells()).hasSize(1);
    }
}

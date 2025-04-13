package com.flt.fltspring.service;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.DocumentPage;
import com.azure.ai.documentintelligence.models.DocumentTable;
import com.azure.ai.documentintelligence.models.DocumentTableCell;
import com.flt.fltspring.model.DocumentTableCellAdapter;
import com.flt.fltspring.model.TableStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.AbstractMap.SimpleEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultConverterService {
    public List<TableStructure> convertToTable(final AnalyzeResult analyzeResult) {
        if (analyzeResult.getTables() == null) {
            log.warn("No tables found in analyze result");
            return new ArrayList<>();
        }
        
        // Debug: Log basic table information from Azure
        log.info("Azure returned {} tables", analyzeResult.getTables().size());
        for (int i = 0; i < analyzeResult.getTables().size(); i++) {
            DocumentTable table = analyzeResult.getTables().get(i);
            
            // Print table dimensions
            log.info("Table {} info - Rows: {}, Columns: {}", 
                    i, table.getRowCount(), table.getColumnCount());
            
            // Print bounding regions
            if (table.getBoundingRegions() != null) {
                log.info("Table {} has {} bounding regions", i, table.getBoundingRegions().size());
                for (int j = 0; j < table.getBoundingRegions().size(); j++) {
                    log.info("  Bounding region {} - Page: {}, Coordinates: {}",
                            j, 
                            table.getBoundingRegions().get(j).getPageNumber(),
                            table.getBoundingRegions().get(j).getPolygon());
                }
            }
            
            // Sample headers from first two rows
            StringBuilder headerSample = new StringBuilder("Table " + i + " header sample: ");
            table.getCells().stream()
                    .filter(cell -> cell.getRowIndex() < 2)  // First two rows (headers)
                    .filter(cell -> cell.getContent() != null && !cell.getContent().trim().isEmpty())
                    .limit(5)  // Just show first 5 headers
                    .forEach(cell -> headerSample.append(cell.getContent()).append(", "));
            log.info(headerSample.toString());
            
            // Print position information if available from spans
            if (table.getSpans() != null && !table.getSpans().isEmpty()) {
                log.info("Table {} spans - Offset: {}, Length: {}", 
                        i, 
                        table.getSpans().get(0).getOffset(),
                        table.getSpans().get(0).getLength());
            }
        }
        
        // Create mapping of tables to page numbers using bounding regions
        Map<DocumentTable, Integer> tablePageMap = new HashMap<>();
        
        // Process each page to find tables
        if (analyzeResult.getPages() != null) {
            // Map of table spans to tables
            Map<String, DocumentTable> tableSpanMap = new HashMap<>();
            
            // Create a mapping of table spans to tables
            for (DocumentTable table : analyzeResult.getTables()) {
                if (table.getSpans() != null && !table.getSpans().isEmpty()) {
                    String spanKey = table.getSpans().get(0).getOffset() + "-" + table.getSpans().get(0).getLength();
                    tableSpanMap.put(spanKey, table);
                }
            }
            
            // Match tables to pages by examining spans and bounding regions
            for (DocumentPage page : analyzeResult.getPages()) {
                int pageNumber = page.getPageNumber();
                
                // Process tables that have bounding regions on this page
                for (DocumentTable table : analyzeResult.getTables()) {
                    if (table.getBoundingRegions() != null) {
                        boolean tableOnThisPage = table.getBoundingRegions().stream()
                                .anyMatch(region -> region.getPageNumber() == pageNumber);
                        
                        if (tableOnThisPage) {
                            tablePageMap.put(table, pageNumber);
                            log.info("Found table on page {} using bounding region", pageNumber);
                        }
                    }
                }
            }
        }
        
        // Debug: Log the natural Y-position (top-to-bottom) of each table
        List<SimpleEntry<DocumentTable, Double>> tablesWithYPosition = new ArrayList<>();
        for (DocumentTable table : analyzeResult.getTables()) {
            if (table.getBoundingRegions() != null && !table.getBoundingRegions().isEmpty()) {
                // Use the y-coordinate of the first point in the polygon as the top position
                Double topY = table.getBoundingRegions().get(0).getPolygon().get(1);
                tablesWithYPosition.add(new SimpleEntry<>(table, topY));
                log.info("Table top Y-coordinate: {}", topY);
            }
        }
        
        // Try sorting by Y-position (top to bottom) within each page
        tablesWithYPosition.sort(Comparator.comparing(SimpleEntry::getValue));
        log.info("Tables sorted by Y-position (top to bottom):");
        for (int i = 0; i < tablesWithYPosition.size(); i++) {
            DocumentTable table = tablesWithYPosition.get(i).getKey();
            Double yPos = tablesWithYPosition.get(i).getValue();
            
            // Sample a header to identify the table
            String sampleHeader = table.getCells().stream()
                    .filter(cell -> cell.getRowIndex() < 2)
                    .filter(cell -> cell.getContent() != null && !cell.getContent().trim().isEmpty())
                    .findFirst()
                    .map(DocumentTableCell::getContent)
                    .orElse("Unknown");
            
            log.info("Position {}: Table with header '{}' at Y={}", i, sampleHeader, yPos);
        }
        
        // Convert to TableStructure objects with page numbers
        List<TableStructure> tableStructures = new ArrayList<>();
        
        // First add tables in Y-position order (top to bottom)
        for (SimpleEntry<DocumentTable, Double> tableWithPos : tablesWithYPosition) {
            DocumentTable table = tableWithPos.getKey();
            tableStructures.add(new TableStructure(
                    table.getColumnCount(),
                    table.getCells().stream()
                            .map(DocumentTableCellAdapter::new)
                            .collect(Collectors.toList()),
                    tablePageMap.getOrDefault(table, 0) // Use 0 if page not found
            ));
        }
        
        // Then add any remaining tables that didn't have position info
        for (DocumentTable table : analyzeResult.getTables()) {
            if (tablesWithYPosition.stream().noneMatch(pair -> pair.getKey() == table)) {
                tableStructures.add(new TableStructure(
                        table.getColumnCount(),
                        table.getCells().stream()
                                .map(DocumentTableCellAdapter::new)
                                .collect(Collectors.toList()),
                        tablePageMap.getOrDefault(table, 0) // Use 0 if page not found
                ));
            }
        }
        
        // Log detailed info about final table order
        for (int i = 0; i < tableStructures.size(); i++) {
            TableStructure table = tableStructures.get(i);
            log.info("Table {} is from page {}", i, table.getPageNumber());
            
            // Print sample headers to verify correct ordering
            StringBuilder headerSample = new StringBuilder("Table " + i + " header sample: ");
            table.getCells().stream()
                    .filter(cell -> cell.getRowIndex() < 2)  // First two rows (headers)
                    .filter(cell -> cell.getContent() != null && !cell.getContent().trim().isEmpty())
                    .limit(5)  // Just show first 5 headers
                    .forEach(cell -> headerSample.append(cell.getContent()).append(", "));
            log.info(headerSample.toString());
        }
        
        log.info("Converted {} tables sorted by vertical position on page", tableStructures.size());
        return tableStructures;
    }
}
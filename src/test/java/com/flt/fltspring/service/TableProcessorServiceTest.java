package com.flt.fltspring.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flt.fltspring.model.bizlogic.TableRow;
import com.flt.fltspring.model.bizlogic.TableStructure;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableProcessorServiceTest {

    private static ObjectMapper mapper;
    private static TableProcessorService service;

    @BeforeAll
    static void setup() {
        mapper  = new ObjectMapper();
        service = new TableProcessorService();
    }

    @Test
    void testSampleInput1() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/sample1.json"));
        List<TableStructure> tables = mapper.readValue(json, new TypeReference<>(){});
        List<TableRow> rows = service.extractRowsFromTables(tables);

        // We expect exactly 1 header + 1 data row
        assertEquals(2, rows.size(), "Must return 2 rows");

        // --- header row checks ---
        TableRow header = rows.get(0);
        assertTrue(header.isHeader(), "First row must be header");
        assertEquals(0, header.getRowIndex());
        Map<Integer,String> h = header.getColumnData();
        assertEquals("DATE",                      h.get(0));
        assertEquals("AIRCRAFT TYPE",             h.get(1));
        assertEquals("AIRCRAFT IDENT",            h.get(2));
        assertEquals("FROM",                      h.get(3));
        assertEquals("TO",                        h.get(4));
        assertEquals("DURATION OF FLIGHT",        h.get(5));
        assertEquals("CROSS COUNTRY",             h.get(11));
        assertEquals("SINGLE- ENGINE LAND",       h.get(17));
        assertEquals("NOTES & ENDORSEMENTS",      h.get(20));

        // --- data row checks ---
        TableRow data = rows.get(1);
        assertFalse(data.isHeader(), "Second row must be data");
        assertEquals(1, data.getRowIndex());
        Map<Integer,String> d = data.getColumnData();
        assertEquals("5/4", d.get(0));
        assertEquals("A124", d.get(1));
        assertEquals("C-454",d.get(2));
        assertEquals("PHX",  d.get(3));
        assertEquals("SAN",  d.get(4));
        assertEquals("1.2",  d.get(5));
        assertEquals("1.2",  d.get(6));
        assertEquals("1.2",  d.get(11));
        assertEquals("1",    d.get(15));
    }

    @Test
    void testSampleInput2() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/sample2.json"));
        List<TableStructure> tables = mapper.readValue(json, new TypeReference<>(){});
        List<TableRow> rows = service.extractRowsFromTables(tables);

        // Again, 1 header + 1 data row
        assertEquals(2, rows.size());

        TableRow header = rows.get(0);
        assertTrue(header.isHeader());
        Map<Integer,String> h = header.getColumnData();
        // These keys come from your second sample's combined headers:
        assertEquals("DATE",                h.get(1));
        assertEquals("NR INST. APP.",       h.get(6));
        assertEquals("REMARKS AND ENDORSEMENTS", h.get(7));
        assertEquals("NR T/O",              h.get(8));
        assertEquals("ENGINE LAND",         h.get(10));
        assertEquals("MULTI- ENGINE LAND",  h.get(12));
        assertEquals("INT APR",             h.get(14));
        assertEquals("INST APR",            h.get(16));
        assertEquals("SIMULATED INSTRUMENT (HOOD)", h.get(22));
        assertEquals("CROSS COUNTRY",       h.get(26));
        assertEquals("PILOT IN COMMAND (INCL. SOLO)", h.get(32));
        assertEquals("TOTAL DURATION OF FLIGHT",     h.get(34));

        TableRow data = rows.get(1);
        assertFalse(data.isHeader());
        Map<Integer,String> d = data.getColumnData();
        assertEquals("2/2",   d.get(1));
        assertEquals("C1725", d.get(2));
        assertEquals("N678ND",d.get(3));
        assertEquals("PHX",   d.get(4));
        assertEquals("SEA",   d.get(5));
        assertEquals("2",     d.get(6));
        assertEquals("avilliqué", d.get(7));
        assertEquals("3",     d.get(8));
        assertEquals("3",     d.get(9));
        assertEquals("1",     d.get(10));
        assertEquals("5",     d.get(11));
        assertEquals("0",     d.get(12));
        assertEquals("0",     d.get(13));
    }
}
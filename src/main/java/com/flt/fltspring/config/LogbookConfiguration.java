package com.flt.fltspring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogbookConfiguration {

    @Bean
    public ColumnConfig[] columnConfigs() {
        return new ColumnConfig[] {
                new ColumnConfig("DATE", ColumnType.DATE),
                new ColumnConfig("AIRCRAFT TYPE", ColumnType.STRING),
                new ColumnConfig("AIRCRAFT IDENT", ColumnType.STRING),
                new ColumnConfig("FROM", ColumnType.AIRPORT_CODE),
                new ColumnConfig("TO", ColumnType.AIRPORT_CODE),
                new ColumnConfig("NR INST. APP.", ColumnType.INTEGER),
                new ColumnConfig("REMARKS AND ENDORSEMENTS", ColumnType.STRING),
                new ColumnConfig("NR T/O", ColumnType.INTEGER),
                new ColumnConfig("NR LDG", ColumnType.INTEGER),
                new ColumnConfig("SINGLE-ENGINE LAND", ColumnType.INTEGER),
                new ColumnConfig("SINGLE- ENGINE LAND", ColumnType.INTEGER),
                new ColumnConfig("MULTI-ENGINE LAND", ColumnType.INTEGER),
                new ColumnConfig("MULTI- ENGINE LAND", ColumnType.INTEGER),
                new ColumnConfig("INT APR", ColumnType.STRING),
                new ColumnConfig("INST APR", ColumnType.STRING),
                new ColumnConfig("NIGHT", ColumnType.INTEGER),
                new ColumnConfig("ACTUAL INSTRUMENT", ColumnType.INTEGER),
                new ColumnConfig("SIMULATED INSTRUMENT (HOOD)", ColumnType.INTEGER),
                new ColumnConfig("FLIGHT SIMULATOR", ColumnType.INTEGER),
                new ColumnConfig("CROSS COUNTRY", ColumnType.INTEGER),
                new ColumnConfig("AS FLIGHT INSTRUCTOR", ColumnType.INTEGER),
                new ColumnConfig("DUAL RECEIVED", ColumnType.INTEGER),
                new ColumnConfig("PILOT IN COMMAND (INCL. SOLO)", ColumnType.INTEGER),
                new ColumnConfig("TOTAL DURATION OF FLIGHT", ColumnType.INTEGER)
        };
    }
}
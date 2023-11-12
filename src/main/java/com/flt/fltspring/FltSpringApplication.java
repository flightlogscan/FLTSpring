package com.flt.fltspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class FltSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(FltSpringApplication.class, args);
    }

}

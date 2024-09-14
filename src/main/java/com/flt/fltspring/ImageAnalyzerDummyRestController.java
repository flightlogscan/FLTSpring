package com.flt.fltspring;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

@RestController
@EnableCaching
public class ImageAnalyzerDummyRestController {

    @RequestMapping(method = RequestMethod.POST, path = "/api/analyze/dummy")
    public ResponseEntity<String> submitAnalyzeImageDummy(final HttpServletRequest request) {

        File file = new File("dummyResponse.txt");
        String data = "";
        try {
            data = FileUtils.readFileToString(file, "UTF-8");
        } catch (final IOException e) {
            System.out.println("IOException: " + e);
        }

        return ResponseEntity.ok(data);
    }
}

package com.flt.fltspring;

import com.flt.fltspring.claims.AdminAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ImageAnalyzerDummyRestController {

    @RequestMapping(method = RequestMethod.POST, path = "/api/analyze/dummy")
    public ResponseEntity<String> submitAnalyzeImageDummy(final HttpServletRequest request) {

        // Only admins have access to dummy api
        final String firebaseEmail = (String) request.getAttribute("firebaseEmail");
        if(!AdminAuthenticator.isAdmin(firebaseEmail)) {
            // Throw a NOT FOUND instead of UNAUTHORIZED because we don't want to confirm to callers if this API exists
            return ResponseEntity.notFound().build();
        }

        File file = new File("dummyResponse.txt");
        String data = "";
        try {
            data = FileUtils.readFileToString(file, "UTF-8");
        } catch (final IOException e) {
            // Only warn in dummy response API
            log.warn("IOException parsing dummy response: " + e);
        }

        return ResponseEntity.ok(data);
    }
}

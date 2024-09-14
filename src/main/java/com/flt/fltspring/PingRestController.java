package com.flt.fltspring;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableCaching
public class PingRestController {

    @RequestMapping(method = RequestMethod.GET, path = "/api/ping")
    public ResponseEntity<String> getPing() {
        return ResponseEntity.ok("pong");
    }

}

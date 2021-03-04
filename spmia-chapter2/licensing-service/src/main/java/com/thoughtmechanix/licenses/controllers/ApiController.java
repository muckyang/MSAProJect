package com.thoughtmechanix.licenses.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/")
public class ApiController {
    @GetMapping( value = "test", produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Test Sample")
    public Object sample(@RequestParam String param) {
        return ResponseEntity.ok(param);
    }
}

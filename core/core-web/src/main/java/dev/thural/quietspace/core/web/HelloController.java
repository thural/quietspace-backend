package dev.thural.quietspace.shared.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hello")
public class HelloController {

    @GetMapping
    public String hello() {
        return "Hello Backend";
    }

    @GetMapping("/get-remote-host")
    public String getRemoteHost(HttpServletRequest request) {
        return "Remote Host: " + request.getRemoteAddr();
    }

}

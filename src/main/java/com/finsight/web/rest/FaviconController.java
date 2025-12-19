package com.finsight.web.rest;

import org.springframework.stereotype.Controller;

@Controller
public class FaviconController {
    @org.springframework.web.bind.annotation.RequestMapping("/favicon.ico")
    public void favicon(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_NO_CONTENT);
    }
}

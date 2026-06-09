package com.finsight.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA client-route fallback — serves index.html for React Router paths under /app.
 */
@Controller
public class SpaController {

    /**
     * Exact /app entry; deeper client routes are handled by {@link SpaWebConfig}
     * (static files when present, otherwise index.html for React Router).
     */
    @GetMapping({"/app", "/app/"})
    public String appRoot() {
        return "forward:/app/index.html";
    }
}

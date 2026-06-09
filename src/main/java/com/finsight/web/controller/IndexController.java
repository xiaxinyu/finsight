package com.finsight.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    @RequestMapping({"", "index.html"})
    public String index() {
        return "redirect:/app/dashboard";
    }

    @RequestMapping("north.html")
    public String north() {
        return "redirect:/app/dashboard";
    }

    @RequestMapping("menu.html")
    public String menu() {
        return "redirect:/app/dashboard";
    }

    @RequestMapping("navigation.html")
    public String navigation() {
        return "redirect:/app/dashboard";
    }

    @RequestMapping("login.html")
    public String login() {
        return "redirect:/app/login";
    }

    @RequestMapping("login-error.html")
    public String loginError() {
        return "redirect:/app/login";
    }
}

package com.finsight.web.contoller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {
    @RequestMapping({"", "index.html"})
    public String index(ModelMap model) {
        return "index";
    }

    @RequestMapping("north.html")
    public String north(ModelMap model) {
        return "system/navigation/north";
    }

    @RequestMapping("menu.html")
    public String menu(ModelMap model) {
        return "system/navigation/menu";
    }

    @RequestMapping("navigation.html")
    public String navigation(ModelMap model) {
        return "system/navigation/navigation";
    }

    @RequestMapping("login.html")
    public String login(ModelMap model) {
        return "login";
    }

    @RequestMapping("login-error.html")
    public String loginError(ModelMap model) {
        return "system/login-error";
    }
}

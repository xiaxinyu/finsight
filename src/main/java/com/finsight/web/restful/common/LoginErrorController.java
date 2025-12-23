package com.finsight.web.restful.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping
public class LoginErrorController {
    @ResponseBody
    @GetMapping("/login-error.json")
    public Map<String, Object> error(HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        Object code = session.getAttribute("LOGIN_ERROR_CODE");
        Object msg = session.getAttribute("LOGIN_ERROR_MSG");
        if (code != null && msg != null) {
            resp.put("code", String.valueOf(code));
            resp.put("msg", String.valueOf(msg));
            session.removeAttribute("LOGIN_ERROR_CODE");
            session.removeAttribute("LOGIN_ERROR_MSG");
        } else {
            resp.put("code", "");
            resp.put("msg", "");
        }
        return resp;
    }
}

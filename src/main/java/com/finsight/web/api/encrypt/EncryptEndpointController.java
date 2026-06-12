package com.finsight.web.api.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Dev-only helpers for password hashing. Jasypt-based property encryption has been removed;
 * use environment variables or your platform secret store for sensitive config.
 */
@Controller
@Profile("!prod")
@RequestMapping({"/encrypt"})
public class EncryptEndpointController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @ResponseBody
    @GetMapping("/bcrypt")
    public Map<String, String> bcrypt(@RequestParam(value = "raw", required = false) String raw,
                                        @RequestParam(value = "key", required = false) String key) {
        String input = (raw != null && !raw.trim().isEmpty()) ? raw : key;
        Map<String, String> map = new HashMap<>();
        if (input == null || input.trim().isEmpty()) {
            map.put("error", "Missing parameter: provide ?raw= or ?key=");
            return map;
        }
        String hash = passwordEncoder.encode(input);
        map.put("raw", input);
        map.put("bcrypt", hash);
        return map;
    }
}

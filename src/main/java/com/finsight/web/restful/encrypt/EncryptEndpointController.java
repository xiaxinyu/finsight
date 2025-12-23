package com.finsight.web.restful.encrypt;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * 加密Controller
 *
 * @author XIAXINYU3
 * @date 2020.6.4
 */
@Controller
@RequestMapping({"/encrypt"})
public class EncryptEndpointController {

    @Autowired
    StringEncryptor encryptor;
    @Autowired
    PasswordEncoder passwordEncoder;

    public EncryptEndpointController() {
    }

    @ResponseBody
    @GetMapping
    public Map encrypt(@RequestParam String key) {
        String encryptKey = this.encryptor.encrypt(key);
        Map<String, String> map = new HashMap();
        map.put("key", key);
        map.put("encryptKey", encryptKey);
        return map;
    }

    @ResponseBody
    @GetMapping("/bcrypt")
    public Map bcrypt(@RequestParam(value = "raw", required = false) String raw,
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

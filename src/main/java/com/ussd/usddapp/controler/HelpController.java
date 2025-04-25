package com.ussd.usddapp.controler;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HelpController {

    @GetMapping("/help")
    public String getHelp() {
        return "This is the help endpoint. You can use it to get help information.";
    }
}

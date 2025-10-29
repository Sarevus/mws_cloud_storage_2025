package com.MWS.handlers;

import org.springframework.web.bind.annotation.*;


@RestController
public class Home {
    @RequestMapping("/")
    public String home() {
        return "Home Page";
    }
}




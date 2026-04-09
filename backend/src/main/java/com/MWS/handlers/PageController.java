package com.MWS.handlers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/register")
    public String register() {
        return "forward:/registerIndex.html";
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/loginIndex.html";
    }

    @GetMapping("/files")
    public String files() {
        return "forward:/fileExchange.html";
    }

    @GetMapping("/myProfile")
    public String myProfile() {
        return "forward:/myProfile.html";
    }

    @GetMapping("/editProfile")
    public String editProfile() {
        return "forward:/editProfile.html";
    }

    @GetMapping("/verificationPage")
    public String verificationPage() {
        return "forward:/verificationPage.html";
    }
}
package com.MWS.handlers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // методы контроллера возвращают JSON
public class Home {

    // метод вызывается на гет-запрос
    @GetMapping("/")
    public String home() {
        return "Home Page";
    }
}




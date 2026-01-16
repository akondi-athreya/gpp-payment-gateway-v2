package com.example.gateway.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CheckoutController {

    @GetMapping({"/checkout", "/checkout/"})
    public String checkoutPage() {
        return "forward:/checkout/iframe.html";
    }
}

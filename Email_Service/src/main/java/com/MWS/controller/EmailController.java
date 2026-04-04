package com.MWS.controller;

import com.MWS.dto.EmailRequest;
import com.MWS.service.DefaultEmailService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final DefaultEmailService emailService;

    public EmailController(DefaultEmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.OK)
    public void sendEmail(@RequestBody EmailRequest request) {
        boolean hasTemplate = request.templateName() != null && !request.templateName().isBlank();
        if (hasTemplate) {
            Context context = new Context();
            if (request.variables() != null) {
                context.setVariables(request.variables());
            }

            try {
                emailService.sendHtmlEmail(
                        request.to(),
                        request.subject(),
                        request.templateName(),
                        context
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to send email: " + e.getMessage());
            }
        } else {
            String messageText = (request.variables() != null)
                    ? request.variables().getOrDefault("message", "").toString()
                    : "";

            emailService.sendTextEmail(request.to(), request.subject(), messageText);
        }
    }
}
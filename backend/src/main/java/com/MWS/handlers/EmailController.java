package com.MWS.handlers;

//import com.MWS.service.EmailService;

import com.MWS.dto.EmailRequest;
import com.MWS.service.VerificationCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/email")
public class EmailController {
    @Autowired
    private VerificationCodeService verificationCodeService;

    private static final Logger LOG = LoggerFactory.getLogger(EmailController.class);
    private final RestTemplate restTemplate;
    private final String EMAIL_SERVICE_URL = "http://localhost:6767/api/email/send";

    public EmailController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    @PostMapping(value = "/send-email/{user-email}")
    public ResponseEntity<String> sendEmail(@PathVariable("user-email") String email) {
        int code = (int) (Math.random() * 900000) + 100000;

        Map<String, Object> vars = new HashMap<>();
        vars.put("code", code);

        EmailRequest emailRequest = new EmailRequest(
                email,
                "Cloud Storage verification",
                "VerificationCodeTemplate",
                vars
        );

        try {
            restTemplate.postForEntity(EMAIL_SERVICE_URL, emailRequest, Void.class);

            verificationCodeService.saveCode(email, code);

            return new ResponseEntity<>("Verification code was sent", HttpStatus.OK);

        } catch (Exception e) {
            LOG.error("Error while sending out email via microservice", e);
            return new ResponseEntity<>("Unable to send email (Service unavailable)", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(
            @RequestParam("email") String email,
            @RequestParam("code") int code
    ) {
        boolean isValid = verificationCodeService.verifyCode(email, code);

        if (!isValid) {
            return new ResponseEntity<>("Invalid code", HttpStatus.BAD_REQUEST);
        }

        verificationCodeService.removeCode(email);
        return ResponseEntity.ok("Email verified");
    }
}
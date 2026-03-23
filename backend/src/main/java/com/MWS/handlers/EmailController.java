package com.MWS.handlers;

import com.MWS.service.EmailService;
import com.MWS.service.VerificationCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    private static final Logger LOG = LoggerFactory.getLogger(EmailController.class);

    @PostMapping(value = "/send-email/{user-email}")
    public ResponseEntity<String> sendEmail(@PathVariable("user-email") String email) {
        int code = (int) (Math.random() * 900000) + 100000;

        try {
            emailService.sendTextEmail(email, "Cloud Storage verification", "Your verification code:" + code);
            verificationCodeService.saveCode(email, code);
        } catch (MailException mailException) {
            LOG.error("Error while sending out email", mailException);
            return new ResponseEntity<>("Unable to send email", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("Verification code was sent", HttpStatus.OK);
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
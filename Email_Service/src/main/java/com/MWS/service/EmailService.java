package com.MWS.service;

import jakarta.mail.MessagingException;
import org.thymeleaf.context.Context;

import java.io.FileNotFoundException;

public interface EmailService {

    void sendTextEmail(String toAddress, String subject, String message);

    void sendEmailWithAttachment(
            String toAddress,
            String subject,
            String message,
            String attachment
    ) throws MessagingException, FileNotFoundException;

    void sendHtmlEmail(String to, String subject, String template, Context context)
            throws MessagingException, FileNotFoundException;
}
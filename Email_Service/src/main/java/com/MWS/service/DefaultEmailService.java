package com.MWS.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileNotFoundException;

@Service
public class DefaultEmailService implements EmailService {

    public JavaMailSender emailSender;
    private final TemplateEngine templateEngine;

    @Autowired
    public DefaultEmailService(JavaMailSender emailSender, TemplateEngine templateEngine) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendTextEmail(String toAddress, String subject, String message) {

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(toAddress.trim());
        simpleMailMessage.setFrom("CloudStorageMIPT@yandex.com");
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(message);
        emailSender.send(simpleMailMessage);
    }

    @Override
    public void sendEmailWithAttachment(String toAddress, String subject, String message, String attachment) throws MessagingException, FileNotFoundException {

        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
        messageHelper.setTo(toAddress);
        messageHelper.setSubject(subject);
        messageHelper.setText(message);
        FileSystemResource file = new FileSystemResource(ResourceUtils.getFile(attachment));
        messageHelper.addAttachment("file-name:", file);
        emailSender.send(mimeMessage);
    }

    public void sendHtmlEmail(String to, String subject, String template, Context context) throws MessagingException, FileNotFoundException {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper htmlMessageSender = new MimeMessageHelper(mimeMessage, true);

        String htmlContent = templateEngine.process(template, context);

        htmlMessageSender.setTo(to);
        htmlMessageSender.setFrom("CloudStorageMIPT@yandex.com");
        htmlMessageSender.setSubject(subject);
        htmlMessageSender.setText(htmlContent, true); // Set true for HTML content

        emailSender.send(mimeMessage);
    }
}

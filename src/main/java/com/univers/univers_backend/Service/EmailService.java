package com.univers.univers_backend.Service;


import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.transactional.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {


    private final MailjetClient client;

    @Value("${mailjet.sender.email}")
    private String senderEmail;

    @Value("${mailjet.template.id}")
    private Long templateId;

    public EmailService(@Value("${mailjet.api.key}") String apiKey,
                        @Value("${mailjet.api.secret}") String apiSecret) {
        this.client = new MailjetClient(ClientOptions.builder()
                .apiKey(apiKey)
                .apiSecretKey(apiSecret)
                .build());
    }

    public boolean sendVerificationEmail(String recipientEmail, String verificationCode) {
        try {
            TransactionalEmail email = TransactionalEmail
                    .builder()
                    .to(List.of(new SendContact(recipientEmail)))
                    .from(new SendContact(senderEmail))
                    .templateID(templateId)
                    .templateLanguage(true)
                    .variables(Map.of("verification_code", verificationCode))
                    .subject("Verify Your Email")
                    .build();

            SendEmailsRequest request = SendEmailsRequest.builder().message(email).build();
            request.sendWith(client);
            return true;
        } catch (MailjetException e) {
            e.printStackTrace();
            return false;
        }
    }
}

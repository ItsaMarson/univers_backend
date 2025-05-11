/* (C)2025 */
package com.univers.univers_backend.Service;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.transactional.*;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final MailjetClient client;

    @Value("${mailjet.sender.email}")
    private String senderEmail;

    @Value("${mailjet.template.id}")
    private Long templateId;

    private final UserRepository userRepository;

    public EmailService(
            UserRepository userRepository,
            @Value("${mailjet.api.key}") String apiKey,
            @Value("${mailjet.api.secret}") String apiSecret) {
        this.client =
                new MailjetClient(
                        ClientOptions.builder().apiKey(apiKey).apiSecretKey(apiSecret).build());
        this.userRepository = userRepository;
    }

    public boolean sendVerificationEmail(
            String recipientEmail,
            String verificationCode,
            String recipientName,
            Long templatedId,
            String subject) {
        try {
            TransactionalEmail email =
                    TransactionalEmail.builder()
                            .to(List.of(new SendContact(recipientEmail)))
                            .from(new SendContact(senderEmail))
                            .templateID(templatedId)
                            .templateLanguage(true)
                            .variables(
                                    Map.of(
                                            "verification_code",
                                            verificationCode,
                                            "first_name",
                                            recipientName))
                            .subject(subject)
                            .build();

            SendEmailsRequest request = SendEmailsRequest.builder().message(email).build();
            request.sendWith(client);
            return true;
        } catch (MailjetException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String resendVerificationCode(String email) {

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "User not found.";
        }

        if (user.getEmailVerified()) {
            return "Email is already verified.";
        }

        String newVerificationCode = String.format("%06d", new Random().nextInt(1000000));

        // Update the user with the new code and expiration time
        user.setVerificationCode(newVerificationCode);
        user.setVerificationCodeExpiration(Instant.now().plus(10, ChronoUnit.MINUTES));
        userRepository.save(user);

        String subject = "New Verification Code";
        sendVerificationEmail(
                user.getEmail(), newVerificationCode, user.getFirstname(), templateId, subject);

        return "A new verification code has been sent to your email.";
    }
}

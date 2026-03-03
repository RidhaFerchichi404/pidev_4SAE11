package org.example.offer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    /**
     * Envoie un email de notification au freelancer lorsqu'un client pose une question.
     * L'envoi est asynchrone pour ne pas bloquer la réponse HTTP.
     */
    @Async
    public void sendNewQuestionEmail(String toEmail, String freelancerName,
                                      String offerTitle, String questionText, Long offerId) {
        log.info("[EMAIL] Preparing to send notification to={} offer={} enabled={}", toEmail, offerId, mailEnabled);
        if (!mailEnabled) {
            log.warn("[EMAIL] Mail is DISABLED in config (app.mail.enabled=false) — skipping email to {}", toEmail);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("[EMAIL] No email address available for offer {} — skipping", offerId);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Nouvelle question sur votre offre : " + offerTitle);

            String preview = questionText.length() > 150
                    ? questionText.substring(0, 150) + "…"
                    : questionText;

            String html = """
                    <!DOCTYPE html>
                    <html lang="fr">
                    <head><meta charset="UTF-8"></head>
                    <body style="font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px;">
                      <div style="max-width: 600px; margin: auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
                        
                        <!-- Header -->
                        <div style="background: linear-gradient(135deg, #E37E33, #E23D59); padding: 30px 40px;">
                          <h1 style="color: #ffffff; margin: 0; font-size: 22px;">💬 Nouvelle question reçue</h1>
                          <p style="color: rgba(255,255,255,0.85); margin: 8px 0 0;">Un client s'intéresse à votre offre</p>
                        </div>
                        
                        <!-- Body -->
                        <div style="padding: 32px 40px;">
                          <p style="color: #374151; font-size: 16px;">Bonjour <strong>%s</strong>,</p>
                          <p style="color: #374151;">Un client vient de poser une question sur votre offre <strong>"%s"</strong> :</p>
                          
                          <!-- Question block -->
                          <div style="background: #f9fafb; border-left: 4px solid #E37E33; padding: 16px 20px; border-radius: 0 8px 8px 0; margin: 20px 0;">
                            <p style="margin: 0; color: #1f2937; font-style: italic;">"%s"</p>
                          </div>
                          
                          <p style="color: #6b7280; font-size: 14px;">Répondez rapidement pour augmenter vos chances d'obtenir cette mission !</p>
                          
                          <!-- CTA Button -->
                          <div style="text-align: center; margin: 28px 0;">
                            <a href="http://localhost:4200/dashboard/my-offers"
                               style="background: linear-gradient(135deg, #E37E33, #E23D59); color: #ffffff; text-decoration: none;
                                      padding: 14px 32px; border-radius: 8px; font-weight: bold; font-size: 15px; display: inline-block;">
                              Répondre à la question
                            </a>
                          </div>
                        </div>
                        
                        <!-- Footer -->
                        <div style="background: #f9fafb; padding: 20px 40px; text-align: center; border-top: 1px solid #e5e7eb;">
                          <p style="color: #9ca3af; font-size: 12px; margin: 0;">
                            © 2026 Freelancia — Vous recevez cet email car vous avez une offre active sur notre plateforme.
                          </p>
                        </div>
                      </div>
                    </body>
                    </html>
                    """.formatted(
                    freelancerName != null ? freelancerName : "Freelancer",
                    offerTitle,
                    preview
            );

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email 'new question' sent to {} for offer {}", toEmail, offerId);

        } catch (Exception e) {
            log.error("Failed to send email to {} for offer {}: {}", toEmail, offerId, e.getMessage());
        }
    }
}

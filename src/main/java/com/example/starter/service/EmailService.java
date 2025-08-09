package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;

public class EmailService {

  private final MailClient mailClient;
  private final String fromEmail;
  private final String fromName;

  public EmailService(MailClient mailClient, JsonObject config) {
    this.mailClient = mailClient;
    this.fromEmail = config.getString("fromEmail", "noreply@example.com");
    this.fromName = config.getString("fromName", "My App");
  }

  public Future<Void> sendWelcomeEmail(String toEmail, String username, String password) {
    MailMessage message = new MailMessage()
      .setFrom(fromName + " <" + fromEmail + ">")
      .setTo(toEmail)
      .setSubject("Your Account Has Been Created")
      .setHtml(buildWelcomeHtml(username, password))
      .setText(buildWelcomeText(username, password));

    return mailClient.sendMail(message).mapEmpty();
  }


  public Future<Void> sendVerificationEmail(String toEmail, String username, String verificationLink) {
    MailMessage message = new MailMessage()
      .setFrom(fromName + " <" + fromEmail + ">")
      .setTo(toEmail)
      .setSubject("Verify your email address")
      .setHtml(buildVerificationHtml(username, verificationLink))
      .setText("Please verify your email: " + verificationLink);

    return mailClient.sendMail(message)
      .mapEmpty();
  }

  public Future<Void> sendPasswordResetEmail(String toEmail, String resetLink) {
    MailMessage message = new MailMessage()
      .setFrom(fromName + " <" + fromEmail + ">")
      .setTo(toEmail)
      .setSubject("Reset your password")
      .setHtml(buildPasswordResetHtml(resetLink))
      .setText("Reset your password: " + resetLink);

    return mailClient.sendMail(message)
      .mapEmpty();
  }

  private String buildWelcomeHtml(String username, String password) {
    return """
    <html>
    <body style="font-family: Arial, sans-serif;">
        <h2>Hello, %s!</h2>
        <p>Your account has been created successfully.</p>
        <p><strong>Username:</strong> %s</p>
        <p><strong>Password:</strong> %s</p>
        <p>You can now login to your account.</p>
        <br>
        <p>Best regards,<br>The Team</p>
    </body>
    </html>
    """.formatted(username, username, password);
  }


  private String buildWelcomeText(String username, String password) {
    return String.format(
      "Hello, %s!\n\n" +
        "Your account has been created successfully.\n\n" +
        "Username: %s\n" +
        "Password: %s\n\n" +
        "You can now login to your account.\n\n" +
        "Best regards,\nThe Team",
      username, username, password
    );
  }

  private String buildVerificationHtml(String username, String link) {
    return """
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>Hello %s,</h2>
                <p>Please verify your email address by clicking the link below:</p>
                <p><a href="%s" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Verify Email</a></p>
                <p>Or copy this link: %s</p>
                <p>This link will expire in 24 hours.</p>
            </body>
            </html>
            """.formatted(username, link, link);
  }

  private String buildPasswordResetHtml(String link) {
    return """
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>Password Reset Request</h2>
                <p>You requested to reset your password. Click the link below:</p>
                <p><a href="%s" style="background-color: #008CBA; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Reset Password</a></p>
                <p>If you didn't request this, please ignore this email.</p>
                <p>This link will expire in 1 hour.</p>
            </body>
            </html>
            """.formatted(link);
  }

  // Add this method to your EmailService class

  public Future<Void> sendContactFormEmail(String adminEmail, String senderName, String senderEmail, String message) {
    MailMessage mailMessage = new MailMessage()
      .setFrom(fromName + " <" + fromEmail + ">")
      .setTo(adminEmail)
      .setSubject("Nouveau message de contact - " + senderName)
      .setHtml(buildContactFormHtml(senderName, senderEmail, message))
      .setText(buildContactFormText(senderName, senderEmail, message));
    // So admin can reply directly to the sender

    return mailClient.sendMail(mailMessage).mapEmpty();
  }

  private String buildContactFormHtml(String senderName, String senderEmail, String message) {
    return """
    <html>
    <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 10px;">
            <h2 style="color: #1890ff; border-bottom: 2px solid #1890ff; padding-bottom: 10px;">
                Nouveau Message de Contact
            </h2>

            <div style="background-color: white; padding: 20px; border-radius: 5px; margin-top: 20px;">
                <h3 style="color: #333;">Informations de l'expéditeur:</h3>
                <p style="margin: 10px 0;">
                    <strong>Nom:</strong> %s
                </p>
                <p style="margin: 10px 0;">
                    <strong>Email:</strong> <a href="mailto:%s">%s</a>
                </p>

                <h3 style="color: #333; margin-top: 30px;">Message:</h3>
                <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; white-space: pre-wrap;">%s</div>
            </div>

            <div style="margin-top: 20px; text-align: center; color: #666; font-size: 12px;">
                <p>Ce message a été envoyé depuis le formulaire de contact du site.</p>
                <p>Vous pouvez répondre directement à cet email pour contacter l'expéditeur.</p>
            </div>
        </div>
    </body>
    </html>
    """.formatted(senderName, senderEmail, senderEmail, message);
  }

  private String buildContactFormText(String senderName, String senderEmail, String message) {
    return String.format(
      "Nouveau Message de Contact\n" +
        "========================\n\n" +
        "De: %s\n" +
        "Email: %s\n\n" +
        "Message:\n" +
        "--------\n" +
        "%s\n\n" +
        "--------\n" +
        "Ce message a été envoyé depuis le formulaire de contact du site.\n" +
        "Vous pouvez répondre directement à cet email pour contacter l'expéditeur.",
      senderName, senderEmail, message
    );
  }

  



}

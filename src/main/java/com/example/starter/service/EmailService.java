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

  public Future<Void> sendWelcomeEmail(String toEmail, String username) {
    MailMessage message = new MailMessage()
      .setFrom(fromName + " <" + fromEmail + ">")
      .setTo(toEmail)
      .setSubject("Welcome to Our App!")
      .setHtml(buildWelcomeHtml(username))
      .setText(buildWelcomeText(username)); // Fallback for non-HTML clients

    return mailClient.sendMail(message)
      .mapEmpty(); // Convert to Future<Void>
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

  private String buildWelcomeHtml(String username) {
    return """
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>Welcome, %s!</h2>
                <p>Thank you for registering with our application.</p>
                <p>You can now login and start using our services.</p>
                <br>
                <p>Best regards,<br>The Team</p>
            </body>
            </html>
            """.formatted(username);
  }

  private String buildWelcomeText(String username) {
    return String.format(
      "Welcome, %s!\n\nThank you for registering with our application.\n" +
        "You can now login and start using our services.\n\nBest regards,\nThe Team",
      username
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



}

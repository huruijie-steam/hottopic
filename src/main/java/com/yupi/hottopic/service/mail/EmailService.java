package com.yupi.hottopic.service.mail;

import com.yupi.hottopic.entity.Hotspot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * 邮件通知(需求 NT-3:仅 high/urgent 级别热点发送 HTML 邮件)
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final String to;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.enabled:false}") boolean enabled,
                        @Value("${spring.mail.username:}") String from,
                        @Value("${app.mail.to:}") String to) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.to = to;
    }

    /**
     * 发送热点通知邮件。未启用邮件/收件人未配置时静默跳过。
     */
    public void sendHotspotEmail(Hotspot hotspot) {
        if (!enabled) {
            log.info("邮件通知未启用,跳过发送(热点: {})", hotspot.getTitle());
            return;
        }
        if (to == null || to.isBlank()) {
            log.warn("未配置收件人(app.mail.to),跳过邮件发送");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("🔥 新热点: " + truncate(hotspot.getTitle(), 60));
            helper.setText(buildHtml(hotspot), true);
            mailSender.send(message);
            log.info("邮件已发送至 {}: {}", to, hotspot.getTitle());
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage(), e);
        }
    }

    /** 简单文本邮件(备用) */
    public void sendText(String subject, String text) {
        if (!enabled || to == null || to.isBlank()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("文本邮件发送失败: {}", e.getMessage(), e);
        }
    }

    private String buildHtml(Hotspot h) {
        String importance = h.getImportance() == null ? "low" : h.getImportance();
        String importanceLabel = switch (importance) {
            case "urgent" -> "🔴 紧急";
            case "high" -> "🟠 高";
            case "medium" -> "🟡 中";
            default -> "⚪ 低";
        };
        String content = h.getContent() == null ? "" : h.getContent();
        return """
                <div style="font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto;
                            border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;">
                  <div style="background: linear-gradient(135deg, #6366f1, #ec4899); padding: 16px 24px; color: #fff;">
                    <h2 style="margin: 0;">🔥 热点监控提醒</h2>
                  </div>
                  <div style="padding: 24px;">
                    <h3 style="margin-top: 0;">%s</h3>
                    <p>重要程度:<b>%s</b> | 相关性:<b>%d/100</b> | 来源:<b>%s</b></p>
                    <p style="color: #4b5563;">%s</p>
                    <p style="color: #6b7280; font-size: 14px;">AI 摘要:%s</p>
                    <p><a href="%s" style="color: #6366f1;">查看原文 →</a></p>
                  </div>
                </div>
                """.formatted(escapeHtml(h.getTitle()), importanceLabel, h.getRelevance() == null ? 0 : h.getRelevance(),
                escapeHtml(h.getSource()), escapeHtml(truncate(content, 300)),
                escapeHtml(h.getSummary() == null ? "" : h.getSummary()), h.getUrl());
    }

    private String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}

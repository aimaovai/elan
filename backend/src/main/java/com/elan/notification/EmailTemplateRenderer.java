package com.elan.notification;

import com.elan.entity.NotificationOutbox;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateRenderer {
    private final ObjectMapper mapper;

    public EmailTemplateRenderer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public EmailMessage render(NotificationOutbox event, String actionUrl) {
        try {
            Map<String, String> data = mapper.readValue(event.templateData, new TypeReference<>() {});
            String headline = switch (event.notificationType) {
                case "RESERVATION_CONFIRMATION" -> "Your table awaits.";
                case "RESERVATION_MODIFICATION" -> "A detail, thoughtfully arranged.";
                case "RESERVATION_CANCELLATION" -> "We have noted your cancellation.";
                case "PRIVATE_DINING_ACKNOWLEDGMENT" -> "We look forward to learning more.";
                default -> "A new inquiry awaits review.";
            };
            String detail = switch (event.notificationType) {
                case "RESERVATION_CONFIRMATION", "RESERVATION_MODIFICATION" ->
                        "Reference " + data.get("confirmationCode") + " · " + data.get("date") + " at "
                                + data.get("time") + " · " + data.get("partySize") + " guests";
                case "RESERVATION_CANCELLATION" ->
                        "Reference " + data.get("confirmationCode") + " for " + data.get("date") + " at " + data.get("time");
                case "PRIVATE_DINING_ACKNOWLEDGMENT" ->
                        "Inquiry " + data.get("referenceNumber") + " for " + data.get("date") + " · "
                                + data.get("guestCount") + " guests";
                default -> "Inquiry " + data.get("referenceNumber") + " · " + data.get("guestName")
                        + " · " + data.get("date") + " · " + data.get("guestCount") + " guests";
            };
            String firstName = escape(data.getOrDefault("firstName", "ÉLAN team"));
            String safeDetail = escape(detail);
            String visitingInfo = event.aggregateType.equals("RESERVATION")
                    ? "<p><strong>Visiting ÉLAN</strong><br>" + escape(data.getOrDefault("restaurantCity", ""))
                            + "<br>" + escape(data.getOrDefault("restaurantHours", "")) + "</p>"
                            + "<p><strong>Reservation policy</strong><br>"
                            + escape(data.getOrDefault("cancellationPolicy", "")) + "</p>"
                    : "";
            String button = actionUrl == null ? "" : "<p style=\"margin:28px 0\"><a href=\""
                    + escape(actionUrl) + "\" style=\"background:#653a42;color:#fff;padding:14px 22px;"
                    + "text-decoration:none\">Manage reservation</a></p>";
            String inquiryNotice = "PRIVATE_DINING_ACKNOWLEDGMENT".equals(event.notificationType)
                    ? "<p>This acknowledges your inquiry only; it is not a confirmed event booking.</p>" : "";
            String html = "<!doctype html><html><body style=\"margin:0;background:#f5f1ea;color:#292925;"
                    + "font-family:Arial,sans-serif\"><table role=\"presentation\" width=\"100%\" cellspacing=\"0\" "
                    + "style=\"background:#f5f1ea;padding:28px 12px\"><tr><td align=\"center\"><table role=\"presentation\" "
                    + "width=\"100%\" cellspacing=\"0\" style=\"max-width:600px;background:#fffdf9;padding:40px 34px\">"
                    + "<tr><td><p style=\"margin:0;color:#653a42;font-family:Georgia,serif;font-size:28px;letter-spacing:5px\">ÉLAN</p>"
                    + "<p style=\"margin:32px 0 8px;color:#653a42;font-size:11px;letter-spacing:2px;text-transform:uppercase\">"
                    + "Thoughtful hospitality</p><h1 style=\"font-family:Georgia,serif;font-size:32px;font-weight:normal\">"
                    + headline + "</h1><p>Dear " + firstName + ",</p><p>" + safeDetail + "</p>" + button
                    + "<p>Thank you for choosing ÉLAN. We look forward to welcoming you for an evening of thoughtful "
                    + "hospitality and seasonal cuisine.</p>" + visitingInfo + inquiryNotice
                    + "<hr style=\"border:0;border-top:1px solid #ddd6cd\">"
                    + "<p style=\"color:#77736d;font-size:12px\">ÉLAN is a fictional demonstration restaurant. "
                    + "This message does not reserve or represent a real restaurant booking.</p></td></tr></table></td></tr></table></body></html>";
            String text = headline + "\n\nDear " + data.getOrDefault("firstName", "ÉLAN team") + ",\n\n"
                    + detail + ("PRIVATE_DINING_ACKNOWLEDGMENT".equals(event.notificationType)
                            ? "\n\nThis acknowledges your inquiry only; it is not a confirmed event booking." : "")
                    + (event.aggregateType.equals("RESERVATION")
                            ? "\n\nVisiting ÉLAN: " + data.getOrDefault("restaurantCity", "")
                                    + "\nHours: " + data.getOrDefault("restaurantHours", "")
                                    + "\nReservation policy: " + data.getOrDefault("cancellationPolicy", "") : "")
                    + (actionUrl == null ? "" : "\n\nManage reservation: " + actionUrl)
                    + "\n\nThank you for choosing ÉLAN. We look forward to welcoming you.\n\n"
                    + "ÉLAN is a fictional demonstration restaurant.";
            return new EmailMessage(event.recipientEmail, event.subject, html, text);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to render notification template.", exception);
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}

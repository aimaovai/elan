package com.elan.notification;

public record EmailMessage(String recipient, String subject, String html, String text) {}

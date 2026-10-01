package com.lipaharaka.api.collections;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "lipaharaka.reminders")
public record ReminderProperties(List<Integer> intervalsDaysBeforeDue, List<Integer> intervalsDaysAfterDue) {
}

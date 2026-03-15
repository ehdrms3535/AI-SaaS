package com.example.saas.dm;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DmReplyTemplateService {

    private static final DateTimeFormatter SHORT_DATE_TIME = DateTimeFormatter.ofPattern("M/d HH:mm");

    public String bookingConfirmed(String customerName, OffsetDateTime startAt) {
        return "%s님, 예약이 %s로 확정되었습니다.".formatted(displayName(customerName), SHORT_DATE_TIME.format(startAt));
    }

    public String bookingNeedsReview() {
        return "요청을 확인했습니다. 예약 가능 여부를 검토한 뒤 다시 안내드리겠습니다.";
    }

    public String bookingUpdated(String customerName, OffsetDateTime startAt) {
        return "%s님, 예약이 %s로 변경되었습니다.".formatted(displayName(customerName), SHORT_DATE_TIME.format(startAt));
    }

    public String bookingCanceled(String customerName) {
        return "%s님, 예약 취소가 완료되었습니다.".formatted(displayName(customerName));
    }

    public List<String> suggestionQuickReplies(List<OffsetDateTime> suggestedStartTimes) {
        return suggestedStartTimes.stream()
                .map(SHORT_DATE_TIME::format)
                .toList();
    }

    private String displayName(String customerName) {
        return (customerName == null || customerName.isBlank()) ? "고객" : customerName.trim();
    }
}

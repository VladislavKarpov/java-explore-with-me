package ru.practicum.ewm.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class RequestDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipationRequestDto {
        private Long id;
        private Long event;
        private Long requester;
        private String status;
        private String created;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventRequestStatusUpdateRequest {
        private List<Long> requestIds;
        private String status; // CONFIRMED, REJECTED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventRequestStatusUpdateResult {
        private List<ParticipationRequestDto> confirmedRequests;
        private List<ParticipationRequestDto> rejectedRequests;
    }
}

package Marshmello.MarshmelloWas.domain.checkin.model;

import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;

import java.util.List;

public record CheckInPage<T>(List<T> content, long totalElements, int totalPages) {
    public CheckInPage {
        content = List.copyOf(content);
    }
}

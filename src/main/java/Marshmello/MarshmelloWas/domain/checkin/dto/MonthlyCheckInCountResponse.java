package Marshmello.MarshmelloWas.domain.checkin.dto;

import java.time.YearMonth;

public record MonthlyCheckInCountResponse(
        YearMonth requestMonth,
        long count,
        long achievedCount
) {
    public MonthlyCheckInCountResponse(long count, long achievedCount) {
        this(null, count, achievedCount);
    }
}

package Marshmello.MarshmelloWas.domain.checkin.entity;

import java.util.Arrays;

public enum BodyRegion {
    CHEST((short) 1),
    ABDOMEN((short) 2),
    PELVIS((short) 3),
    BUTTOCKS((short) 4),
    LEFT_ARM((short) 5),
    RIGHT_ARM((short) 6),
    LEFT_LEG((short) 7),
    RIGHT_LEG((short) 8);

    private final short code;

    BodyRegion(short code) {
        this.code = code;
    }

    public short code() {
        return code;
    }

    public static BodyRegion fromCode(short code) {
        return Arrays.stream(values())
                .filter(region -> region.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Body region must be between 1 and 8"));
    }
}

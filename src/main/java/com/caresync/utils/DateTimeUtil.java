package com.caresync.utils;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class DateTimeUtil {
    private DateTimeUtil() {
    }

    public static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    public static LocalTime toLocalTime(Time time) {
        return time == null ? null : time.toLocalTime();
    }

    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}

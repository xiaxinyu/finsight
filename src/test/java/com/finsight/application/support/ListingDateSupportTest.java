package com.finsight.application.support;

import com.finsight.common.exception.AppServiceException;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListingDateSupportTest {

    @Test
    void parseMmDdYyyy_acceptsIsoFormat() throws Exception {
        Date date = ListingDateSupport.parseMmDdYyyy("2025-01-01");
        assertEquals("2025-01-01", format(date));
    }

    @Test
    void parseMmDdYyyy_acceptsLegacyDashFormat() throws Exception {
        Date date = ListingDateSupport.parseMmDdYyyy("01-01-2025");
        assertEquals("2025-01-01", format(date));
    }

    @Test
    void parseMmDdYyyy_acceptsSlashFormat() throws Exception {
        Date date = ListingDateSupport.parseMmDdYyyy("06/08/2026");
        assertEquals("2026-06-08", format(date));
    }

    @Test
    void parseMmDdYyyyOrDefaultOneYear_usesProvidedIsoRange() throws Exception {
        Date[] range = ListingDateSupport.parseMmDdYyyyOrDefaultOneYear("2025-03-01", "2025-03-31");
        assertNotNull(range[0]);
        assertNotNull(range[1]);
        assertEquals("2025-03-01", format(range[0]));
        assertEquals("2025-03-31", format(range[1]));
    }

    @Test
    void parseMmDdYyyyOrNull_returnsNullBoundsWhenBothBlank() throws Exception {
        Date[] range = ListingDateSupport.parseMmDdYyyyOrNull("", "");
        assertNull(range[0]);
        assertNull(range[1]);
    }

    @Test
    void parseMmDdYyyy_rejectsGarbage() {
        assertThrows(AppServiceException.class, () -> ListingDateSupport.parseMmDdYyyy("not-a-date"));
    }

    private static String format(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
    }
}

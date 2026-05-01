package com.officeseatbooking.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter[] FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        String dateTimeString = jsonParser.getText();

        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }

        // Try different formats
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTimeString, formatter);
            } catch (DateTimeParseException e) {
                // Continue to next format
            }
        }

        // If no format worked, try to append seconds if missing
        try {
            if (dateTimeString.length() == 16 && dateTimeString.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) {
                return LocalDateTime.parse(dateTimeString + ":00");
            }
        } catch (DateTimeParseException e) {
            // Ignore
        }

        throw new IOException("Unable to parse date: " + dateTimeString +
            ". Expected formats: yyyy-MM-ddTHH:mm:ss or yyyy-MM-ddTHH:mm");
    }
}

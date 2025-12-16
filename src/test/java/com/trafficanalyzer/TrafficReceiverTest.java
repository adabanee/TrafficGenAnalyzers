package com.trafficanalyzer;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class TrafficReceiverTest {

    @Test
    public void testAverageDelayCalculation() {
        // Given
        ArrayList<Long> delays = new ArrayList<>(Arrays.asList(10L, 20L, 30L));

        // When
        double avg = delays.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        // Then
        assertEquals(20.0, avg, 0.001);
    }

    @Test
    public void testPacketLossCalculation() {
        // Given
        long maxSeq = 10;
        long received = 8;

        // When
        long lost = maxSeq - received;
        double lossPercent = (maxSeq > 0) ? (lost * 100.0 / maxSeq) : 0.0;

        // Then
        assertEquals(2, lost);
        assertEquals(20.0, lossPercent, 0.001);
    }
}
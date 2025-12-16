package com.trafficanalyzer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Generates UDP network traffic with configurable parameters.
 * Each packet contains a timestamp and sequence number for delay and loss analysis.
 */
public class TrafficGenerator {

    private static final Logger logger = LogManager.getLogger();

    private final String targetHost;
    private final int port;
    private final int packetCount;
    private final int packetSizeBytes;
    private final int frequencyPerSecond;

    /**
     * Constructs a new TrafficGenerator.
     *
     * @param targetHost the destination host (e.g., "localhost")
     * @param port the destination port
     * @param packetCount number of packets to send
     * @param packetSizeBytes size of each packet in bytes (minimum 16)
     * @param frequencyPerSecond packets per second (0 = no delay)
     */
    public TrafficGenerator(String targetHost, int port, int packetCount, int packetSizeBytes, int frequencyPerSecond) {
        this.targetHost = targetHost;
        this.port = port;
        this.packetCount = packetCount;
        this.packetSizeBytes = packetSizeBytes;
        this.frequencyPerSecond = frequencyPerSecond;
    }

    /**
     * Starts sending UDP packets according to the configured parameters.
     * Logs each sent packet and handles errors gracefully.
     */
    public void start() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(targetHost);
            int payloadSize = Math.max(16, packetSizeBytes);
            byte[] buffer = new byte[payloadSize];

            for (long seq = 1; seq <= packetCount; seq++) {
                long sendTime = System.currentTimeMillis();
                ByteBuffer.wrap(buffer).putLong(sendTime).putLong(seq);

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
                socket.send(packet);

                logger.info("[Generator] Packet {} sent", seq);

                if (frequencyPerSecond > 0) {
                    long delayMs = 1000L / frequencyPerSecond;
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("[Generator] Error: {}", e.getMessage(), e);
        }
    }
}
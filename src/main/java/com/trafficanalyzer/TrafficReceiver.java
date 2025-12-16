package com.trafficanalyzer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Receives UDP traffic and calculates network performance metrics:
 * packet loss, delay, and throughput.
 */
public class TrafficReceiver {

    private static final Logger logger = LogManager.getLogger();

    private final int port;
    private final Map<Long, Long> packetDelays = new ConcurrentHashMap<>();
    private final List<Long> delaysList = Collections.synchronizedList(new ArrayList<>());
    private long firstReceiveTime = -1;
    private long totalBytes = 0;
    private volatile boolean running = true;

    /**
     * Constructs a new TrafficReceiver.
     *
     * @param port the port to listen on
     */
    public TrafficReceiver(int port) {
        this.port = port;
    }

    /**
     * Starts listening for incoming UDP packets.
     * Uses a 1-second socket timeout to allow graceful shutdown via {@link #stop()}.
     */
    public void start() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.setSoTimeout(1000);
            byte[] buffer = new byte[65507];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            logger.info("[Receiver] Waiting for packets on port {}...", port);
            firstReceiveTime = System.currentTimeMillis();

            while (running) {
                try {
                    socket.receive(packet);
                    long receiveTime = System.currentTimeMillis();
                    totalBytes += packet.getLength();

                    if (packet.getLength() < 16) {
                        continue;
                    }

                    ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, 16);
                    long sendTime = bb.getLong();
                    long seqNum = bb.getLong();

                    long delay = receiveTime - sendTime;
                    packetDelays.put(seqNum, delay);
                    delaysList.add(delay);

                    logger.info("[Receiver] Packet #{}, delay: {} ms", seqNum, delay);
                } catch (java.io.InterruptedIOException e) {
                    continue;
                }
            }
        } catch (IOException e) {
            if (running) {
                logger.error("[Receiver] Error: {}", e.getMessage(), e);
            }
        }
        logger.info("[Receiver] Stopped.");
    }

    /**
     * Signals the receiver to stop listening.
     * The {@link #start()} method will exit its loop on the next timeout.
     */
    public void stop() {
        running = false;
    }

    /**
     * Calculates and logs network performance statistics:
     * - Expected vs received packets
     * - Packet loss percentage
     * - Average delay
     * - Throughput in bps and Kbps
     */
    public void printStats() {
        if (packetDelays.isEmpty()) {
            logger.info("Stats: No packets received.");
            return;
        }

        long now = System.currentTimeMillis();
        long totalTimeMs = now - firstReceiveTime;
        long totalTimeSec = Math.max(1, totalTimeMs / 1000);

        long maxSeq = packetDelays.keySet().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        long received = packetDelays.size();
        long lost = maxSeq - received;
        double lossPercent = (maxSeq > 0) ? (lost * 100.0 / maxSeq) : 0.0;
        double avgDelayMs = delaysList.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double speedBps = (totalBytes * 8.0) / totalTimeSec;

        logger.info("\n========== NETWORK TRAFFIC STATISTICS ==========");
        logger.info("Expected packets: {}", maxSeq);
        logger.info("Received packets: {}", received);
        logger.info("Lost packets:     {} ({:.2f} %)", lost, lossPercent);
        logger.info("Avg delay:        {:.2f} ms", avgDelayMs);
        logger.info("Throughput:       {:.2f} bps ({:.2f} Kbps)", speedBps, speedBps / 1000);
        logger.info("Total time:       {} ms", totalTimeMs);
        logger.info("================================================");
    }
}
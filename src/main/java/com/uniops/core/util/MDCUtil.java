package com.uniops.core.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MDCUtil 类的简要描述
 *
 * @author liyang
 * @since 2026/1/30
 */
public class MDCUtil {
    public static final String TRACE_ID = "traceId";

    /**
     * 雪花算法生成器
     */
    private static class SnowflakeGenerator {
        // 开始时间截 (2026-01-01)，避免时间回拨问题
        private static final long START_TIME = 1767225600000L; // 2026-01-01 00:00:00

        // 数据中心ID所占位数
        private static final long DATA_CENTER_ID_BITS = 2L;

        // 机器ID所占位数
        private static final long MACHINE_ID_BITS = 8L;

        // 序列号所占位数
        private static final long SEQUENCE_BITS = 12L;

        // 数据中心ID最大值 (2^2 - 1 = 3)
        private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

        // 机器ID最大值 (2^8 - 1 = 255)
        private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);

        // 序列号最大值 (2^12 - 1 = 4095)
        private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

        // 机器ID偏移量
        private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;

        // 数据中心ID偏移量
        private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

        // 时间戳偏移量
        private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATA_CENTER_ID_BITS;

        // 数据中心ID
        private final long dataCenterId;

        // 机器ID
        private final long machineId;

        // 毫秒内序列(0~4095)
        private long sequence = 0L;

        // 上次生成ID的时间截
        private long lastTimestamp = -1L;

        // 原子操作确保线程安全
        private final AtomicLong atomicSequence = new AtomicLong(0);

        public SnowflakeGenerator(long dataCenterId, long machineId) {
            if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
                throw new IllegalArgumentException(
                    String.format("数据中心ID不能大于 %d 或小于 0", MAX_DATA_CENTER_ID));
            }
            if (machineId > MAX_MACHINE_ID || machineId < 0) {
                throw new IllegalArgumentException(
                    String.format("机器ID不能大于 %d 或小于 0", MAX_MACHINE_ID));
            }
            this.dataCenterId = dataCenterId;
            this.machineId = machineId;
        }

        /**
         * 获得下一个ID (该方法是线程安全的)
         * @return Snowflake ID
         */
        public synchronized long nextId() {
            long timestamp = System.currentTimeMillis();

            // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，抛出异常
            if (timestamp < lastTimestamp) {
                throw new RuntimeException(
                    String.format("时钟向后移动。拒绝生成ID直到 %d", lastTimestamp - timestamp));
            }

            // 如果是同一时间生成的，则进行毫秒内序列
            if (lastTimestamp == timestamp) {
                sequence = atomicSequence.incrementAndGet() & MAX_SEQUENCE;

                // 毫秒内序列溢出
                if (sequence == 0) {
                    // 阻塞到下一毫秒,获得新的时间戳
                    timestamp = waitNextMillis(lastTimestamp);
                }
            } else {
                // 时间戳改变，毫秒内序列重置为0
                atomicSequence.set(0);
                sequence = 0L;
            }

            // 上次生成ID的时间截
            lastTimestamp = timestamp;

            // 移位并通过或运算拼到一起组成64位的ID
            return ((timestamp - START_TIME) << TIMESTAMP_LEFT_SHIFT) |
                   (dataCenterId << DATA_CENTER_ID_SHIFT) |
                   (machineId << MACHINE_ID_SHIFT) |
                   sequence;
        }

        /**
         * 阻塞到下一毫秒，直到获得新的时间戳
         * @param lastTimestamp 上次生成ID的时间截
         * @return 当前时间戳
         */
        private long waitNextMillis(long lastTimestamp) {
            long timestamp = System.currentTimeMillis();
            while (timestamp <= lastTimestamp) {
                timestamp = System.currentTimeMillis();
            }
            return timestamp;
        }
    }

    // 单例实例
    private static volatile MDCUtil instance;
    private static final Object lock = new Object();
    private SnowflakeGenerator generator;

    private MDCUtil() {
        // 获取机器ID，使用MAC地址的一部分作为机器标识
        long machineId = generateMachineId();
        // 使用进程ID的一部分作为数据中心ID
        long dataCenterId = generateDataCenterId();
        this.generator = new SnowflakeGenerator(dataCenterId, machineId);
    }

    public static MDCUtil getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new MDCUtil();
                }
            }
        }
        return instance;
    }

    /**
     * 生成机器ID
     * @return 机器ID
     */
    private long generateMachineId() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
            if (networkInterface != null && networkInterface.getHardwareAddress() != null) {
                byte[] mac = networkInterface.getHardwareAddress();
                // 使用MAC地址的后几位生成机器ID，确保在范围内
                return ((mac[mac.length - 1] & 0xFF) << 8 | (mac[mac.length - 2] & 0xFF)) % 256;
            }
        } catch (Exception e) {
            // 如果获取失败，使用随机数或默认值
        }

        // 默认机器ID
        return System.currentTimeMillis() % 256;
    }

    /**
     * 生成数据中心ID
     * @return 数据中心ID
     */
    private long generateDataCenterId() {
        try {
            String processName = ManagementFactory.getRuntimeMXBean().getName();
            // 使用进程名的一部分作为数据中心ID
            int hash = processName.hashCode();
            return Math.abs(hash) % 4; // 限制在0-3之间
        } catch (Exception e) {
            // 如果获取失败，使用默认值
            return 1;
        }
    }

    /**
     * 生成唯一的链路ID
     * @return 唯一的链路ID字符串
     */
    public static String generateTraceId() {
        return String.valueOf(getInstance().generator.nextId());
    }
}

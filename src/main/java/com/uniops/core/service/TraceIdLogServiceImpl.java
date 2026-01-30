package com.uniops.core.service;

import com.uniops.core.condition.TraceIdLogCondition;
import com.uniops.core.entity.LogInfo;
import com.uniops.core.entity.HttpRequestLog;
import com.uniops.core.entity.ScheduledLog;
import com.uniops.core.mapper.HttpRequestLogMapper;
import com.uniops.core.mapper.ScheduledLogMapper;
import com.uniops.core.vo.LogInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * TraceIdLogServiceImpl 实现类
 * 用于根据traceId和时间范围检索日志文件（包括压缩文件）
 *
 * @author liyang
 * @since 2026/1/30
 */
@Service
public class TraceIdLogServiceImpl implements TraceIdLogService {

    @Autowired
    private HttpRequestLogMapper httpRequestLogMapper;

    @Autowired
    private ScheduledLogMapper scheduledLogMapper;

    // 日志目录路径
    private static final String LOG_DIR_PATH = "logs/";
    private static final String LOG_FILE_PATTERN = "application.log";

    // 定义日志条目的起始模式（时间戳开头）
    private static final Pattern LOG_START_PATTERN = Pattern.compile(
        "^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}");

    // 定义traceId的模式，仅匹配日志框架输出的traceId部分
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile(
            "\\[traceId=([\\w-]+)\\]|traceId=([\\w-]+)(?=\\s|$)|\"traceId\":\"([^\"]+)\"");


    @Override
    public LogInfoVO getLogLinesByTraceId(TraceIdLogCondition condition) {
        List<LogInfo> allLogs = getAllLogsByTraceId(condition);

        // 进行分页处理
        int page = condition.getPage() != 0 ? condition.getPage() : 1;
        int size = condition.getSize() != 0 ? condition.getSize() : 10;

        int startIndex = (page - 1) * size;
        if (startIndex >= allLogs.size()) {
            return new LogInfoVO(page, size, allLogs.size(), new ArrayList<>());
        }

        int endIndex = Math.min(startIndex + size, allLogs.size());
        List<LogInfo> pagedLogs = allLogs.subList(startIndex, endIndex);

        return new LogInfoVO(page, size, allLogs.size(), pagedLogs);
    }

    @Override
    public LogInfoVO getHttpLogsByTraceId(TraceIdLogCondition condition) {
        return getLogLinesByTraceId(condition);
    }

    @Override
    public LogInfoVO getScheduledLogsByTraceId(TraceIdLogCondition condition) {
        return getLogLinesByTraceId(condition);
    }

    @Override
    public LogInfoVO getFullTraceLog(String traceId, Date startTime, Date endTime, int page, int size) {
        TraceIdLogCondition condition = new TraceIdLogCondition();
        condition.setTraceId(traceId);
        condition.setStartTime(startTime);
        condition.setEndTime(endTime);
        condition.setPage(page);
        condition.setSize(size);

        return getLogLinesByTraceId(condition);
    }

    /**
     * 获取所有符合条件的日志（不进行分页）
     */
    private List<LogInfo> getAllLogsByTraceId(TraceIdLogCondition condition) {
        List<LogInfo> matchingLogs = new ArrayList<>();

        // 首先查找当前日志文件
        File currentLogFile = findCurrentLogFile();
        if (currentLogFile != null && currentLogFile.exists()) {
            matchingLogs.addAll(searchInFile(currentLogFile, condition));
        }

        // 然后查找压缩的日志文件
        List<File> archivedLogFiles = findArchivedLogFiles(condition);
        for (File archivedFile : archivedLogFiles) {
            matchingLogs.addAll(searchInArchivedFile(archivedFile, condition));
        }

        // 按时间排序
        matchingLogs.sort(Comparator.comparing(LogInfo::getLogTime));

        return matchingLogs;
    }

    /**
     * 查找当前活动的日志文件
     */
    private File findCurrentLogFile() {
        File logDir = new File(LOG_DIR_PATH);
        if (!logDir.exists() || !logDir.isDirectory()) {
            // 尝试其他常见日志路径
            String[] paths = {
                "target/logs/",
                "src/main/resources/logs/",
                System.getProperty("user.dir") + "/logs/"
            };

            for (String path : paths) {
                logDir = new File(path);
                if (logDir.exists() && logDir.isDirectory()) {
                    break;
                }
            }
        }

        if (!logDir.exists() || !logDir.isDirectory()) {
            return null;
        }

        // 查找最新的日志文件
        File[] files = logDir.listFiles((dir, name) ->
            name.startsWith(LOG_FILE_PATTERN) && !name.endsWith(".gz") && !name.endsWith(".zip"));

        if (files != null && files.length > 0) {
            // 返回最新的日志文件
            File latestFile = files[0];
            for (File file : files) {
                if (file.lastModified() > latestFile.lastModified()) {
                    latestFile = file;
                }
            }
            return latestFile;
        }

        return null;
    }

    /**
     * 根据时间条件查找归档的日志文件
     */
    private List<File> findArchivedLogFiles(TraceIdLogCondition condition) {
        List<File> archivedFiles = new ArrayList<>();

        File logDir = new File(LOG_DIR_PATH);
        if (!logDir.exists() || !logDir.isDirectory()) {
            String[] paths = {
                "target/logs/",
                "src/main/resources/logs/",
                System.getProperty("user.dir") + "/logs/"
            };

            for (String path : paths) {
                logDir = new File(path);
                if (logDir.exists() && logDir.isDirectory()) {
                    break;
                }
            }
        }

        if (!logDir.exists() || !logDir.isDirectory()) {
            return archivedFiles;
        }

        File[] allFiles = logDir.listFiles((dir, name) ->
            (name.endsWith(".gz") || name.endsWith(".zip")) &&
            (name.contains(LOG_FILE_PATTERN) || name.startsWith("application")));

        if (allFiles != null) {
            for (File file : allFiles) {
                // 检查归档文件的时间戳是否在查询范围内
                if (isArchiveFileInTimeRange(file, condition.getStartTime(), condition.getEndTime())) {
                    archivedFiles.add(file);
                }
            }
        }

        return archivedFiles;
    }

    /**
     * 检查归档文件是否在时间范围内
     */
    private boolean isArchiveFileInTimeRange(File archiveFile, Date startTime, Date endTime) {
        if (startTime == null && endTime == null) {
            return true;
        }

        // 从文件名中提取日期信息
        String fileName = archiveFile.getName();
        LocalDateTime fileTime = extractDateTimeFromFileName(fileName);

        if (fileTime == null) {
            // 如果无法从文件名中提取时间，使用文件修改时间
            fileTime = new Date(archiveFile.lastModified()).toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        }

        if (startTime != null) {
            LocalDateTime startLocal = startTime.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            if (fileTime.isBefore(startLocal)) {
                return false;
            }
        }

        if (endTime != null) {
            LocalDateTime endLocal = endTime.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            if (fileTime.isAfter(endLocal)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 从文件名中提取日期时间信息
     */
    private LocalDateTime extractDateTimeFromFileName(String fileName) {
        try {
            // 匹配常见的日期格式，如 application.log.2026-01-30 或 application.2026-01-30.0.log.gz
            Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})(?:\\.\\d+)?");
            Matcher matcher = datePattern.matcher(fileName);

            if (matcher.find()) {
                String dateStr = matcher.group(1);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return LocalDate.parse(dateStr, formatter).atStartOfDay();
            }
        } catch (Exception e) {
            // 忽略解析错误
        }

        return null;
    }

    /**
     * 在普通日志文件中搜索
     */
    private List<LogInfo> searchInFile(File logFile, TraceIdLogCondition condition) {
        List<LogInfo> matchingLogs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            // 读取整个日志文件并按日志条目解析
            List<String> logEntries = readLogEntries(reader);

            for (String logEntry : logEntries) {
                // 查找条目中是否包含指定的traceId（仅在日志框架输出部分匹配）
                String extractedTraceId = extractTraceId(logEntry);
                if (extractedTraceId != null && extractedTraceId.equals(condition.getTraceId())) {
                    // 解析日志条目并创建LogInfo对象
                    LogInfo logInfo = parseLogEntry(logEntry, condition.getTraceId());
                    if (logInfo != null && isLogInTimeRange(logInfo, condition.getStartTime(), condition.getEndTime())) {
                        matchingLogs.add(logInfo);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取日志文件失败: " + e.getMessage(), e);
        }

        return matchingLogs;
    }

    /**
     * 提取日志条目中的traceId（只匹配日志框架输出的traceId部分）
     */
    private String extractTraceId(String logEntry) {
        // 获取日志条目的第一行，通常包含日志级别和traceId信息
        String[] lines = logEntry.split("\n");
        String firstLine = lines[0];

        // 在第一行中查找traceId
        Matcher traceMatcher = TRACE_ID_PATTERN.matcher(firstLine);
        if (traceMatcher.find()) {
            // 返回匹配到的traceId值
            String traceId = traceMatcher.group(1);
            if (traceId == null) {
                traceId = traceMatcher.group(2);
            }
            if (traceId == null) {
                traceId = traceMatcher.group(3);
            }
            return traceId;
        }

        return null;
    }

    /**
     * 读取日志条目（处理多行日志条目）
     */
    private List<String> readLogEntries(BufferedReader reader) throws IOException {
        List<String> logEntries = new ArrayList<>();
        StringBuilder currentEntry = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            // 检查这一行是否是新日志条目的开始
            if (LOG_START_PATTERN.matcher(line).find()) {
                // 如果当前条目不为空，将其添加到列表中
                if (currentEntry.length() > 0) {
                    logEntries.add(currentEntry.toString().trim());
                    currentEntry.setLength(0); // 清空StringBuilder
                }
                // 开始新的日志条目
                currentEntry.append(line);
            } else {
                // 这是多行日志的一部分，追加到当前条目
                if (currentEntry.length() > 0) {
                    currentEntry.append("\n").append(line);
                } else {
                    // 如果这是文件的第一行但不是日志开头，跳过
                    continue;
                }
            }
        }

        // 添加最后一个日志条目
        if (currentEntry.length() > 0) {
            logEntries.add(currentEntry.toString().trim());
        }

        return logEntries;
    }

    /**
     * 在归档文件中搜索
     */
    private List<LogInfo> searchInArchivedFile(File archiveFile, TraceIdLogCondition condition) {
        List<LogInfo> matchingLogs = new ArrayList<>();

        try {
            if (archiveFile.getName().endsWith(".gz")) {
                matchingLogs.addAll(searchInGzipFile(archiveFile, condition));
            } else if (archiveFile.getName().endsWith(".zip")) {
                matchingLogs.addAll(searchInZipFile(archiveFile, condition));
            }
        } catch (Exception e) {
            throw new RuntimeException("读取归档日志文件失败: " + e.getMessage(), e);
        }

        return matchingLogs;
    }

    /**
     * 在GZIP文件中搜索
     */
    private List<LogInfo> searchInGzipFile(File gzipFile, TraceIdLogCondition condition) throws IOException {
        List<LogInfo> matchingLogs = new ArrayList<>();

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(gzipFile));
             GZIPInputStream gzis = new GZIPInputStream(bis);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzis))) {

            // 读取整个日志文件并按日志条目解析
            List<String> logEntries = readLogEntries(reader);

            for (String logEntry : logEntries) {
                // 查找条目中是否包含指定的traceId（仅在日志框架输出部分匹配）
                String extractedTraceId = extractTraceId(logEntry);
                if (extractedTraceId != null && extractedTraceId.equals(condition.getTraceId())) {
                    // 解析日志条目并创建LogInfo对象
                    LogInfo logInfo = parseLogEntry(logEntry, condition.getTraceId());
                    if (logInfo != null && isLogInTimeRange(logInfo, condition.getStartTime(), condition.getEndTime())) {
                        matchingLogs.add(logInfo);
                    }
                }
            }
        }

        return matchingLogs;
    }

    /**
     * 在ZIP文件中搜索
     */
    private List<LogInfo> searchInZipFile(File zipFile, TraceIdLogCondition condition) throws IOException {
        List<LogInfo> matchingLogs = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".log")) {
                    // 处理ZIP中的日志文件
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis));

                    // 读取整个日志文件并按日志条目解析
                    List<String> logEntries = readLogEntries(reader);

                    for (String logEntry : logEntries) {
                        // 查找条目中是否包含指定的traceId（仅在日志框架输出部分匹配）
                        String extractedTraceId = extractTraceId(logEntry);
                        if (extractedTraceId != null && extractedTraceId.equals(condition.getTraceId())) {
                            // 解析日志条目并创建LogInfo对象
                            LogInfo logInfo = parseLogEntry(logEntry, condition.getTraceId());
                            if (logInfo != null && isLogInTimeRange(logInfo, condition.getStartTime(), condition.getEndTime())) {
                                matchingLogs.add(logInfo);
                            }
                        }
                    }

                    zis.closeEntry();
                }
            }
        }

        return matchingLogs;
    }

    /**
     * 解析日志条目并创建LogInfo对象
     */
    private LogInfo parseLogEntry(String logEntry, String traceId) {
        // 解析日志等级
        String level = parseLogLevel(logEntry);

        // 解析时间
        LocalDateTime logTime = parseLogTimeFromEntry(logEntry);

        // 使用整个日志条目作为详情
        String detail = logEntry;

        return new LogInfo(level, logTime, detail, traceId);
    }

    /**
     * 从日志条目中解析日志等级
     */
    private String parseLogLevel(String logEntry) {
        Pattern levelPattern = Pattern.compile("\\.(INFO|DEBUG|WARN|ERROR|TRACE)\\s");
        Matcher levelMatcher = levelPattern.matcher(logEntry.split("\n")[0]); // 只在第一行查找日志级别

        if (levelMatcher.find()) {
            return levelMatcher.group(1);
        }

        // 尝试其他常见的日志等级模式
        Pattern altLevelPattern = Pattern.compile("(INFO|DEBUG|WARN|ERROR|TRACE)");
        Matcher altLevelMatcher = altLevelPattern.matcher(logEntry.split("\n")[0]);

        if (altLevelMatcher.find()) {
            return altLevelMatcher.group(1);
        }

        return "UNKNOWN";
    }

    /**
     * 从日志条目中解析时间
     */
    private LocalDateTime parseLogTimeFromEntry(String logEntry) {
        // 获取日志条目的第一行来提取时间
        String firstLine = logEntry.split("\n")[0];

        // 匹配常见的日志时间格式
        Pattern timePatterns[] = {
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})"),
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})"),
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})")
        };

        for (Pattern pattern : timePatterns) {
            Matcher timeMatcher = pattern.matcher(firstLine);
            if (timeMatcher.find()) {
                String timeStr = timeMatcher.group(1);
                return parseLogTime(timeStr);
            }
        }

        // 如果没有找到时间，返回当前时间
        return LocalDateTime.now();
    }

    /**
     * 检查日志是否在时间范围内
     */
    private boolean isLogInTimeRange(LogInfo logInfo, Date startTime, Date endTime) {
        if (startTime == null && endTime == null) {
            return true; // 没有时间限制
        }

        LocalDateTime logTime = logInfo.getLogTime();

        if (startTime != null) {
            LocalDateTime startLocal = startTime.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            if (logTime.isBefore(startLocal)) {
                return false;
            }
        }

        if (endTime != null) {
            LocalDateTime endLocal = endTime.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            if (logTime.isAfter(endLocal)) {
                return false;
            }
        }

        return true;
    }

    private LocalDateTime parseLogTime(String timeStr) {
        try {
            if (timeStr.contains("T")) {
                // ISO格式: 2026-01-30T10:30:45.123
                if (timeStr.contains(".")) {
                    DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
                    return LocalDateTime.parse(timeStr, isoFormatter);
                } else {
                    DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                    return LocalDateTime.parse(timeStr, isoFormatter);
                }
            } else {
                // 标准格式: 2026-01-30 10:30:45.123 或 2026-01-30 10:30:45
                if (timeStr.contains(".")) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                    return LocalDateTime.parse(timeStr, formatter);
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    return LocalDateTime.parse(timeStr, formatter);
                }
            }
        } catch (Exception e) {
            // 如果解析失败，返回当前时间
            return LocalDateTime.now();
        }
    }
}

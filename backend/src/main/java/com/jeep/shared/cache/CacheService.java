package com.jeep.shared.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Cache-aside access to Redis, used for exactly two things:
 * organization analytics summaries and project task-list pages.
 *
 * Fail-open by design: every operation catches Redis failures and falls
 * back to "no cache" (reads behave as misses, writes/evictions are
 * skipped), so the app keeps working on the database when Redis is down
 * or absent. When no {@link StringRedisTemplate} bean exists at all
 * (e.g. JPA slice tests, which do not load Redis auto-configuration),
 * the cache is a silent no-op.
 *
 * Values are stored as JSON strings so a cached entry is readable and
 * version-tolerant; keys are namespaced per cached view.
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    /** Analytics summaries change rarely; 10 minutes bounds staleness. */
    public static final Duration ANALYTICS_TTL = Duration.ofMinutes(10);

    /** Task lists are evicted on every task write; 5 minutes is a safety net. */
    public static final Duration TASK_LIST_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public CacheService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this(redisTemplateProvider.getIfAvailable(), defaultObjectMapper());
    }

    // Package-private for unit tests, which inject a mocked template.
    CacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    /** Key for an organization's cached analytics summary. */
    public static String analyticsKey(UUID organizationId) {
        return "analytics:org:" + organizationId;
    }

    /** Prefix covering every cached task-list page of one project. */
    public static String taskListPrefix(UUID projectId) {
        return "tasks:project:" + projectId + ":";
    }

    /** Key for one task-list page. The search text is sanitized so the key stays safe. */
    public static String taskListKey(UUID projectId, String search, int page, int size, String sort) {
        return taskListPrefix(projectId)
            + "s:" + sanitize(search)
            + ":p:" + page
            + ":z:" + size
            + ":sort:" + sanitize(sort);
    }

    private static String sanitize(String value) {
        String cleaned = value == null ? "" : value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        cleaned = cleaned.replaceAll("^-+|-+$", "");
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64).replaceAll("-+$", "");
        }
        return cleaned.isEmpty() ? "all" : cleaned;
    }

    /** Cache-aside read: empty on a miss, on a corrupt entry, or when Redis is unavailable. */
    public <T> Optional<T> get(String key, Class<T> type) {
        if (redisTemplate == null) {
            return Optional.empty();
        }
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, type));
        } catch (RuntimeException | JsonProcessingException e) {
            log.warn("Cache read failed for key {} — falling back to the database", key, e);
            return Optional.empty();
        }
    }

    /** Cache-aside write: stores the value as JSON with a TTL; silently skipped when Redis is unavailable. */
    public void put(String key, Object value, Duration ttl) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (RuntimeException | JsonProcessingException e) {
            log.warn("Cache write failed for key {} — continuing without caching", key, e);
        }
    }

    /** Evicts one key; silently skipped when Redis is unavailable. */
    public void evict(String key) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException e) {
            log.warn("Cache evict failed for key {} — entry will expire via TTL", key, e);
        }
    }

    /**
     * Evicts every key under a prefix (used for a project's task-list pages,
     * which vary by search text, page and size). Uses KEYS, which is fine
     * at this app's scale; do not use for high-cardinality prefixes.
     */
    public void evictByPrefix(String prefix) {
        if (redisTemplate == null) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException e) {
            log.warn("Cache evict-by-prefix failed for prefix {} — entries will expire via TTL", prefix, e);
        }
    }
}

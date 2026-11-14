package com.jeep.shared.cache;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jeep.analytics.dto.OrganizationAnalyticsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        cacheService = new CacheService(redisTemplate, mapper);
    }

    private void withValueOperations() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void get_shouldReturnDeserializedValueOnHit() {
        withValueOperations();
        UUID orgId = UUID.randomUUID();
        when(valueOperations.get("analytics:org:" + orgId)).thenReturn(
            "{\"organizationId\":\"" + orgId + "\",\"totalMembers\":3,\"totalTeams\":1,"
                + "\"totalProjects\":2,\"openTasks\":5,\"completedTasks\":4,\"inProgressTasks\":1}");

        Optional<OrganizationAnalyticsResponse> result =
            cacheService.get("analytics:org:" + orgId, OrganizationAnalyticsResponse.class);

        assertThat(result).isPresent();
        assertThat(result.get().organizationId()).isEqualTo(orgId);
        assertThat(result.get().totalMembers()).isEqualTo(3);
        assertThat(result.get().openTasks()).isEqualTo(5);
    }

    @Test
    void get_shouldReturnEmptyOnMiss() {
        withValueOperations();
        when(valueOperations.get("missing")).thenReturn(null);

        assertThat(cacheService.get("missing", String.class)).isEmpty();
    }

    @Test
    void get_shouldReturnEmptyWhenRedisThrows() {
        withValueOperations();
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis down"));

        assertThat(cacheService.get("any", String.class)).isEmpty();
    }

    @Test
    void put_shouldStoreJsonWithTtl() throws Exception {
        withValueOperations();
        UUID orgId = UUID.randomUUID();
        OrganizationAnalyticsResponse response =
            new OrganizationAnalyticsResponse(orgId, 1, 1, 1, 0, 0, 0);

        cacheService.put("analytics:org:" + orgId, response, CacheService.ANALYTICS_TTL);

        org.mockito.ArgumentCaptor<String> json = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("analytics:org:" + orgId), json.capture(), eq(Duration.ofMinutes(10)));
        assertThat(new ObjectMapper().readTree(json.getValue()).get("totalMembers").asInt()).isEqualTo(1);
    }

    @Test
    void put_shouldNotThrowWhenRedisThrows() {
        withValueOperations();
        org.mockito.Mockito.doThrow(new RuntimeException("Redis down"))
            .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        cacheService.put("k", "v", Duration.ofMinutes(5));
    }

    @Test
    void evict_shouldDeleteKey() {
        cacheService.evict("analytics:org:abc");

        verify(redisTemplate).delete("analytics:org:abc");
    }

    @Test
    void evictByPrefix_shouldDeleteAllMatchingKeys() {
        when(redisTemplate.keys("tasks:project:abc:*")).thenReturn(Set.of("tasks:project:abc:p:0", "tasks:project:abc:p:1"));

        cacheService.evictByPrefix("tasks:project:abc:");

        verify(redisTemplate).delete(Set.of("tasks:project:abc:p:0", "tasks:project:abc:p:1"));
    }

    @Test
    void evictByPrefix_shouldDoNothingWhenNoKeysMatch() {
        when(redisTemplate.keys("tasks:project:abc:*")).thenReturn(Set.of());

        cacheService.evictByPrefix("tasks:project:abc:");

        verify(redisTemplate, never()).delete(any(Set.class));
    }

    @Test
    void allOperations_shouldBeNoOpsWhenNoRedisTemplateExists() {
        ObjectMapper mapper = new ObjectMapper();
        CacheService noRedis = new CacheService(null, mapper);

        assertThat(noRedis.get("k", String.class)).isEmpty();
        noRedis.put("k", "v", Duration.ofMinutes(1));
        noRedis.evict("k");
        noRedis.evictByPrefix("prefix:");
        // No exception, no interaction: the app works without Redis at all.
    }

    @Test
    void taskListKey_shouldSanitizeSearchAndSort() {
        String key = CacheService.taskListKey(UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "  Fix Login BUG!  ", 2, 12, "createdAt: DESC");

        assertThat(key).isEqualTo("tasks:project:00000000-0000-0000-0000-000000000001:s:fix-login-bug:p:2:z:12:sort:createdat-desc");
    }
}

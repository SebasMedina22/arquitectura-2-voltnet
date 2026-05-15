package com.voltnet.gridload.infrastructure.persistence.redis;

import com.voltnet.gridload.domain.model.Kw;
import com.voltnet.gridload.domain.model.StationId;
import com.voltnet.gridload.domain.model.StationLoad;
import com.voltnet.gridload.domain.port.out.StationLoadPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Patron Adapter (GoF): traduce las operaciones del puerto del dominio
 * (StationLoadPort) a las primitivas de Spring Data Redis.
 *
 * Llave en Redis: station:{stationId}:load_kw -> String con valor double.
 */
@Component
public class RedisStationLoadAdapter implements StationLoadPort {

    private static final String KEY_PREFIX = "station:";
    private static final String KEY_SUFFIX = ":load_kw";

    private final StringRedisTemplate redis;

    public RedisStationLoadAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<StationLoad> findByStationId(StationId stationId) {
        String raw = redis.opsForValue().get(key(stationId));
        if (raw == null) {
            return Optional.empty();
        }
        Kw kw = Kw.of(Double.parseDouble(raw));
        return Optional.of(StationLoad.of(stationId, kw, Instant.now()));
    }

    @Override
    public StationLoad save(StationId stationId, Kw currentLoad) {
        redis.opsForValue().set(key(stationId), Double.toString(currentLoad.value()));
        return StationLoad.of(stationId, currentLoad, Instant.now());
    }

    private String key(StationId stationId) {
        return KEY_PREFIX + stationId.value() + KEY_SUFFIX;
    }
}

package com.voltnet.orchestrator.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente Feign declarativo hacia MS-GridLoad.
 * El @FeignClient con url toma la base de application.yml (gridload.base-url).
 */
@FeignClient(name = "gridload", url = "${gridload.base-url}")
public interface GridLoadFeignClient {

    @GetMapping("/grid/load")
    GridLoadResponse fetchLoad(@RequestParam("stationId") String stationId);
}

package com.cumt.volunteer.am.controller;

import com.cumt.volunteer.am.service.MapService;
import com.cumt.volunteer.common.Result;
import com.cumt.volunteer.geo.model.FeatureCollection;
import com.cumt.volunteer.geo.service.SpatialCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;
    private final SpatialCalculator spatialCalculator;

    /**
     * 获取已发布活动的 GeoJSON FeatureCollection（供地图展示）
     */
    @GetMapping("/activities")
    public Result<FeatureCollection> getActivityGeoJSON() {
        return Result.ok(mapService.getActivityGeoJSON());
    }

    /**
     * 获取可用图层清单（P2-AM-06，供 GeoServer 图层控制面板使用）
     */
    @GetMapping("/layers")
    public Result<List<Map<String, String>>> listLayers() {
        return Result.ok(List.of(
                Map.of("id", "campus:buildings", "name", "校园建筑", "type", "wms"),
                Map.of("id", "campus:roads", "name", "校园道路", "type", "wms"),
                Map.of("id", "campus:greenland", "name", "校园绿地", "type", "wms"),
                Map.of("id", "campus:water", "name", "校园水系", "type", "wms"),
                Map.of("id", "campus:poi", "name", "校园POI", "type", "wms")
        ));
    }

    /**
     * 校验坐标是否在矿大南湖校区范围内（P2-AM-15）
     */
    @GetMapping("/check-bounds")
    public Result<Map<String, Boolean>> checkBounds(@RequestParam double lng, @RequestParam double lat) {
        return Result.ok(Map.of("within", spatialCalculator.isWithinCampus(lng, lat)));
    }
}

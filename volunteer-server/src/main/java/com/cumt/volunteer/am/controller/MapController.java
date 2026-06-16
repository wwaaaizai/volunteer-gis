package com.cumt.volunteer.am.controller;

import com.cumt.volunteer.am.service.MapService;
import com.cumt.volunteer.common.Result;
import com.cumt.volunteer.geo.model.FeatureCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    /**
     * 获取已发布活动的 GeoJSON FeatureCollection（供地图展示）
     */
    @GetMapping("/activities")
    public Result<FeatureCollection> getActivityGeoJSON() {
        return Result.ok(mapService.getActivityGeoJSON());
    }
}

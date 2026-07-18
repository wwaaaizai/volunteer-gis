package com.cumt.volunteer.am.controller;

import com.cumt.volunteer.am.service.MapService;
import com.cumt.volunteer.common.Result;
import com.cumt.volunteer.geo.model.FeatureCollection;
import com.cumt.volunteer.geo.service.SpatialCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 地图控制器（P2-AM）。
 *
 * <p><b>坐标系统</b>：天地图底图使用 GCJ-02 火星坐标系，
 * 本控制器中涉及前端地图交互的接口均使用 GCJ-02 坐标。</p>
 */
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;
    private final SpatialCalculator spatialCalculator;

    /**
     * 获取已发布活动的 GeoJSON FeatureCollection（供地图展示）。
     * 返回坐标已转为 GCJ-02，与天地图底图对齐。
     */
    @GetMapping("/activities")
    public Result<FeatureCollection> getActivityGeoJSON() {
        return Result.ok(mapService.getActivityGeoJSON());
    }

    // ──── P2-AM-06：图层清单接口 ──────────────────────

    /**
     * 获取可用图层清单（P2-AM-06）。
     *
     * <p>返回 GeoServer 可发布的校园 WMS 图层元数据，
     * 供前端地图控制面板展示图层列表。</p>
     *
     * <p>GeoServer WMS URL 模板：{@code /geoserver/campus/wms}</p>
     */
    @GetMapping("/layers")
    public Result<List<Map<String, String>>> listLayers() {
        return Result.ok(List.of(
                layer("campus:buildings", "校园建筑", "polygon",
                        "建筑物轮廓面图层，含楼名、楼层、用途等属性"),
                layer("campus:roads",    "校园道路", "line",
                        "校园主干道和支路线图层，含道路名称和等级"),
                layer("campus:greenland","校园绿地", "polygon",
                        "绿化带、草坪、花坛等绿色空间面图层"),
                layer("campus:water",    "校园水系", "polygon",
                        "镜湖、河流、池塘等水体面图层"),
                layer("campus:poi",      "校园POI",  "point",
                        "校门、食堂、超市、ATM 等兴趣点点图层")
        ));
    }

    // ──── P2-AM-15：校区范围校验 ──────────────────────

    /**
     * 获取校区边界框信息（P2-AM-15）。
     *
     * <p>返回 GCJ-02 和 WGS-84 两套边界的完整信息，
     * 前端可用 GCJ-02 边界设置 MapLibre {@code maxBounds}，
     * 后端用 WGS-84 边界校验活动坐标。</p>
     */
    @GetMapping("/campus-bounds")
    public Result<Map<String, Object>> getCampusBounds() {
        Map<String, Object> gcj02 = new LinkedHashMap<>();
        gcj02.put("sw", Map.of("lng", SpatialCalculator.CAMPUS_GCJ02_LNG_MIN,
                                "lat", SpatialCalculator.CAMPUS_GCJ02_LAT_MIN));
        gcj02.put("ne", Map.of("lng", SpatialCalculator.CAMPUS_GCJ02_LNG_MAX,
                                "lat", SpatialCalculator.CAMPUS_GCJ02_LAT_MAX));
        gcj02.put("center", Map.of("lng", SpatialCalculator.CAMPUS_CENTER_LNG_GCJ,
                                    "lat", SpatialCalculator.CAMPUS_CENTER_LAT_GCJ));

        Map<String, Object> wgs84 = new LinkedHashMap<>();
        wgs84.put("sw", Map.of("lng", SpatialCalculator.CAMPUS_WGS84_LNG_MIN,
                                "lat", SpatialCalculator.CAMPUS_WGS84_LAT_MIN));
        wgs84.put("ne", Map.of("lng", SpatialCalculator.CAMPUS_WGS84_LNG_MAX,
                                "lat", SpatialCalculator.CAMPUS_WGS84_LAT_MAX));
        wgs84.put("center", Map.of("lng", SpatialCalculator.CAMPUS_CENTER_LNG_WGS,
                                    "lat", SpatialCalculator.CAMPUS_CENTER_LAT_WGS));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gcj02", gcj02);
        result.put("wgs84", wgs84);
        return Result.ok(result);
    }

    /**
     * 校验 GCJ-02 坐标是否在矿大南湖校区范围内（P2-AM-15）。
     *
     * <p>前端天地图上的坐标均为 GCJ-02，直接传给本接口校验，
     * 无需前端做坐标系转换。</p>
     *
     * @param lng GCJ-02 经度
     * @param lat GCJ-02 纬度
     */
    @GetMapping("/check-bounds")
    public Result<Map<String, Object>> checkBounds(@RequestParam double lng,
                                                    @RequestParam double lat) {
        boolean within = spatialCalculator.isWithinCampusGcj02(lng, lat);
        return Result.ok(Map.of(
                "within", within,
                "lng", lng,
                "lat", lat,
                "coordSystem", "GCJ-02"
        ));
    }

    // ──── 活动热力图接口（P2-AM 热力图分析）───────────

    /**
     * 获取签到热力图数据。
     *
     * <p>返回所有已完成签到的坐标点集合，供前端 heatmap layer 渲染。
     * 支持按分类和月份筛选。</p>
     *
     * @param category 活动分类筛选（可选）
     * @param months   最近N个月（可选，默认12）
     */
    @GetMapping("/heatmap")
    public Result<FeatureCollection> getHeatmap(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "12") int months) {
        return Result.ok(mapService.getHeatmapData(category, months));
    }

    // ──── 工具方法 ──────────────────────────────────────

    private Map<String, String> layer(String id, String name, String geometryType, String desc) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("type", "wms");
        m.put("geometryType", geometryType);
        m.put("description", desc);
        return m;
    }
}

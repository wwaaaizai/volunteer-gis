package com.cumt.volunteer.geo.service;

import com.cumt.volunteer.geo.model.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 空间距离计算工具（Haversine 公式）。
 *
 * <p>原先内联在 {@code CheckInServiceImpl} 中，现抽出为横切组件，
 * 供签到校验、地图距离过滤、未来"附近活动"等功能复用。</p>
 *
 * <p><b>坐标系统说明（P2-AM-15）</b>：
 * <ul>
 *   <li>数据库存储：WGS-84（GPS 原始坐标）</li>
 *   <li>天地图底图：GCJ-02（国测局坐标系，中国境内偏移 300-500m）</li>
 *   <li>{@code isWithinCampusWgs84()} 校验数据库传入的 WGS-84 坐标</li>
 *   <li>{@code isWithinCampusGcj02()} 校验天地图前端传来的 GCJ-02 坐标</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SpatialCalculator {

    // ──── 矿大南湖校区边界（WGS-84，GPS原始坐标）───
    // GCJ-02 中心 [117.140, 34.215] 反推 WGS-84 中心约 [117.134, 34.217]
    // 各方向外扩约 2km → lng ±0.022°, lat ±0.018°

    /** WGS-84 西边界经度 */
    public static final double CAMPUS_WGS84_LNG_MIN = 117.112;
    /** WGS-84 东边界经度 */
    public static final double CAMPUS_WGS84_LNG_MAX = 117.156;
    /** WGS-84 南边界纬度 */
    public static final double CAMPUS_WGS84_LAT_MIN = 34.199;
    /** WGS-84 北边界纬度 */
    public static final double CAMPUS_WGS84_LAT_MAX = 34.235;

    // ──── 矿大南湖校区边界（GCJ-02，天地图底图使用）───
    // 以 [117.140, 34.215] 为中心，各方向外扩约 2km

    /** GCJ-02 西边界经度 */
    public static final double CAMPUS_GCJ02_LNG_MIN = 117.118;
    /** GCJ-02 东边界经度 */
    public static final double CAMPUS_GCJ02_LNG_MAX = 117.162;
    /** GCJ-02 南边界纬度 */
    public static final double CAMPUS_GCJ02_LAT_MIN = 34.197;
    /** GCJ-02 北边界纬度 */
    public static final double CAMPUS_GCJ02_LAT_MAX = 34.233;

    /** 校区中心 WGS-84（由 GCJ-02 [117.140, 34.215] 反推） */
    public static final double CAMPUS_CENTER_LNG_WGS = 117.134;
    public static final double CAMPUS_CENTER_LAT_WGS = 34.217;

    /** 校区中心 GCJ-02（天地图显示用） */
    public static final double CAMPUS_CENTER_LNG_GCJ = 117.140;
    public static final double CAMPUS_CENTER_LAT_GCJ = 34.215;

    /** 地球平均半径（米） */
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final CoordConvertService coordConvertService;

    // ──── 距离计算 ──────────────────────────────────────

    /**
     * 用 Haversine 公式计算两点间的大圆距离（米）。
     */
    public double distanceMeters(GeoPoint a, GeoPoint b) {
        return haversine(a.latitude().doubleValue(), a.longitude().doubleValue(),
                b.latitude().doubleValue(), b.longitude().doubleValue());
    }

    /**
     * 判断点 b 是否在以 a 为圆心、radiusMeters 为半径的圆内。
     */
    public boolean isWithin(GeoPoint a, GeoPoint b, double radiusMeters) {
        return distanceMeters(a, b) <= radiusMeters;
    }

    // ──── 校区范围校验（P2-AM-15）──────────────────────

    /**
     * 校验 WGS-84 坐标是否在矿大南湖校区范围内。
     *
     * <p>适用场景：活动创建/签到时，校验后端数据库传入的坐标。</p>
     *
     * @param lng WGS-84 经度
     * @param lat WGS-84 纬度
     * @return true 表示在校区范围内
     */
    public boolean isWithinCampusWgs84(double lng, double lat) {
        return lng >= CAMPUS_WGS84_LNG_MIN && lng <= CAMPUS_WGS84_LNG_MAX
                && lat >= CAMPUS_WGS84_LAT_MIN && lat <= CAMPUS_WGS84_LAT_MAX;
    }

    /**
     * 校验 GCJ-02 坐标是否在矿大南湖校区范围内。
     *
     * <p>适用场景：前端天地图点击/拖拽后传来的坐标校验。
     * 天地图底图使用 GCJ-02 坐标系，地图上所有坐标均为 GCJ-02。</p>
     *
     * @param lng GCJ-02 经度
     * @param lat GCJ-02 纬度
     * @return true 表示在校区范围内
     */
    public boolean isWithinCampusGcj02(double lng, double lat) {
        return lng >= CAMPUS_GCJ02_LNG_MIN && lng <= CAMPUS_GCJ02_LNG_MAX
                && lat >= CAMPUS_GCJ02_LAT_MIN && lat <= CAMPUS_GCJ02_LAT_MAX;
    }

    /**
     * 自动判断坐标系：将 WGS-84 转为 GCJ-02 后校验。
     *
     * <p>通用方法：无论传入哪种坐标，都先转 GCJ-02 再与 GCJ-02 边界比对。</p>
     *
     * @param lng 经度（WGS-84）
     * @param lat 纬度（WGS-84）
     * @return true 表示在校区范围内
     */
    public boolean isWithinCampus(double lng, double lat) {
        // 先转为 GCJ-02，与天地图底图对齐
        double[] gcj02 = coordConvertService.wgs84ToGcj02(lng, lat);
        return isWithinCampusGcj02(gcj02[0], gcj02[1]);
    }

    // ──── 射线法多边形判定（签到围栏）─────────────────

    /**
     * 射线法判断点是否在多边形内（P2-AM 签到地理围栏）。
     *
     * <p>从待测点向右发射水平射线，统计与多边形边的交点数：
     * 奇数 → 内部，偶数 → 外部。</p>
     *
     * @param lng    待测点经度
     * @param lat    待测点纬度
     * @param polygon 多边形顶点数组 [[lng1,lat1], [lng2,lat2], ...]
     * @return true 表示点在多边形内部或边上
     */
    public boolean isPointInPolygon(double lng, double lat, double[][] polygon) {
        if (polygon == null || polygon.length < 3) return false;
        int n = polygon.length;
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon[i][0], yi = polygon[i][1];
            double xj = polygon[j][0], yj = polygon[j][1];
            // 射线与边相交判断
            if ((yi > lat) != (yj > lat)
                    && lng < (xj - xi) * (lat - yi) / (yj - yi) + xi) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * 判断点是否在多个多边形集合的任意一个内（支持多围栏/多会场签到）。
     */
    public boolean isPointInAnyPolygon(double lng, double lat, List<double[][]> polygons) {
        if (polygons == null || polygons.isEmpty()) return false;
        for (double[][] polygon : polygons) {
            if (isPointInPolygon(lng, lat, polygon)) return true;
        }
        return false;
    }

    /**
     * 从 GeoJSON MultiPolygon 或 Polygon 字符串解析所有多边形顶点。
     * 支持: Polygon → [[[lng,lat],...]] 或 MultiPolygon → [[[[lng,lat],...]],[[[...]]]]
     */
    public List<double[][]> parsePolygonFromGeoJson(String geoJson) {
        if (geoJson == null || geoJson.isBlank()) return null;
        try {
            // 判断是 MultiPolygon 还是 Polygon
            if (geoJson.contains("\"MultiPolygon\"")) {
                return parseMultiPolygon(geoJson);
            } else {
                // Polygon 格式 → 返回单元素列表
                double[][] poly = parseSinglePolygon(geoJson);
                if (poly == null) return null;
                List<double[][]> list = new ArrayList<>();
                list.add(poly);
                return list;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** 解析单个 Polygon */
    private double[][] parseSinglePolygon(String geoJson) {
        String content = geoJson.replaceAll(".*?\\[\\[\\[", "")
                                 .replaceAll("\\]\\]\\].*", "");
        String[] pairs = content.split("\\],\\[");
        double[][] polygon = new double[pairs.length][2];
        for (int i = 0; i < pairs.length; i++) {
            String[] xy = pairs[i].trim().split(",");
            polygon[i][0] = Double.parseDouble(xy[0].trim());
            polygon[i][1] = Double.parseDouble(xy[1].trim());
        }
        return polygon;
    }

    /** 解析 MultiPolygon → 多个 Polygon 数组的列表 */
    private List<double[][]> parseMultiPolygon(String geoJson) {
        List<double[][]> polygons = new ArrayList<>();
        // MultiPolygon: [[[[...]]],[[[...]]]]
        // Remove outer wrapper and split by "]],[["
        String inner = geoJson.replaceAll(".*?\\[\\[\\[\\[", "")
                               .replaceAll("\\]\\]\\]\\].*", "");
        String[] polyBlocks = inner.split("\\]\\],\\[\\[");
        for (String block : polyBlocks) {
            // Each block is like lng1,lat1],[lng2,lat2
            String[] pairs = block.split("\\],\\[");
            double[][] polygon = new double[pairs.length][2];
            for (int i = 0; i < pairs.length; i++) {
                String[] xy = pairs[i].trim().split(",");
                polygon[i][0] = Double.parseDouble(xy[0].trim());
                polygon[i][1] = Double.parseDouble(xy[1].trim());
            }
            if (polygon.length >= 3) polygons.add(polygon);
        }
        return polygons.isEmpty() ? null : polygons;
    }

    // ──── Haversine 核心实现 ──────────────────────────

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}

package com.cumt.volunteer.am.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cumt.volunteer.abm.mapper.SignupMapper;
import com.cumt.volunteer.aca.mapper.ActivityMapper;
import com.cumt.volunteer.am.service.MapService;
import com.cumt.volunteer.entity.Activity;
import com.cumt.volunteer.entity.Signup;
import com.cumt.volunteer.geo.model.Feature;
import com.cumt.volunteer.geo.model.FeatureCollection;
import com.cumt.volunteer.geo.model.GeoPoint;
import com.cumt.volunteer.geo.service.CoordConvertService;
import com.cumt.volunteer.geo.service.GeoJsonBuilder;
import com.cumt.volunteer.geo.service.SpatialCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {

    private final ActivityMapper activityMapper;
    private final SignupMapper signupMapper;
    private final GeoJsonBuilder geoJsonBuilder;
    private final CoordConvertService coordConvertService;
    private final SpatialCalculator spatialCalculator;

    @Override
    public FeatureCollection getActivityGeoJSON() {
        List<Activity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>().eq(Activity::getStatus, "published")
        );

        List<Feature> features = new ArrayList<>();
        for (Activity a : activities) {
            if (a.getLongitude() == null || a.getLatitude() == null) continue;

            Map<String, Object> properties = geoJsonBuilder.props();
            properties.put("id", a.getId());
            properties.put("title", a.getTitle());
            properties.put("locationName", a.getLocationName());
            properties.put("startTime", a.getStartTime() != null ? a.getStartTime().toString() : null);

            // WGS-84 → GCJ-02 坐标转换（对齐天地图底图）
            double[] gcj = coordConvertService.wgs84ToGcj02(
                    a.getLongitude().doubleValue(), a.getLatitude().doubleValue());
            features.add(geoJsonBuilder.pointFeature(
                    GeoPoint.of(gcj[0], gcj[1]),
                    properties
            ));
        }

        return geoJsonBuilder.collection(features);
    }

    @Override
    public FeatureCollection getHeatmapData(String category, int months) {
        // 1. 确定时间范围
        LocalDateTime since = LocalDateTime.now().minusMonths(months);

        // 2. 按分类筛选活动ID（可选）
        List<Long> activityIds = null;
        if (category != null && !category.isBlank()) {
            activityIds = activityMapper.selectList(
                    new LambdaQueryWrapper<Activity>()
                            .eq(Activity::getCategory, category)
            ).stream().map(Activity::getId).collect(Collectors.toList());
            if (activityIds.isEmpty()) return geoJsonBuilder.collection(List.of());
        }

        // 3. 查询签到记录（有签到坐标的）
        LambdaQueryWrapper<Signup> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Signup::getSignInLng)
               .isNotNull(Signup::getSignInLat)
               .ge(Signup::getSignInTime, since);
        if (activityIds != null) {
            wrapper.in(Signup::getActivityId, activityIds);
        }
        List<Signup> signups = signupMapper.selectList(wrapper);

        // 4. 构造 GeoJSON Point FeatureCollection（转为 GCJ-02）
        List<Feature> features = new ArrayList<>();
        for (Signup s : signups) {
            double[] gcj = coordConvertService.wgs84ToGcj02(
                    s.getSignInLng().doubleValue(), s.getSignInLat().doubleValue());
            Map<String, Object> props = geoJsonBuilder.props();
            props.put("activityId", s.getActivityId());
            props.put("userId", s.getUserId());
            props.put("weight", s.getVolunteerHours() != null
                    ? s.getVolunteerHours().doubleValue() : 0.5);
            features.add(geoJsonBuilder.pointFeature(
                    GeoPoint.of(gcj[0], gcj[1]), props));
        }

        return geoJsonBuilder.collection(features);
    }

    // ──── 缓冲区分析 ──────────────────────────────

    @Override
    public Map<String, Object> bufferAnalysis(double lng, double lat, double radius) {
        GeoPoint center = GeoPoint.of(lng, lat);
        List<Activity> published = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>().eq(Activity::getStatus, "published"));

        int covered = 0;
        List<Map<String, Object>> activities = new ArrayList<>();
        for (Activity a : published) {
            if (a.getLongitude() == null || a.getLatitude() == null) continue;
            double dist = spatialCalculator.distanceMeters(center,
                    GeoPoint.of(a.getLongitude(), a.getLatitude()));
            if (dist <= radius) {
                covered++;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", a.getId());
                item.put("title", a.getTitle());
                item.put("lng", a.getLongitude());
                item.put("lat", a.getLatitude());
                item.put("distance", Math.round(dist));
                item.put("signedCount", a.getSignedCount());
                activities.add(item);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("centerLng", lng);
        result.put("centerLat", lat);
        result.put("radius", radius);
        result.put("coveredCount", covered);
        result.put("totalSignups", activities.stream()
                .mapToInt(a -> (Integer) a.get("signedCount")).sum());
        result.put("activities", activities);
        return result;
    }

    // ──── 覆盖率分析 ──────────────────────────────

    @Override
    public Map<String, Object> coverageAnalysis(int gridSize) {
        double lngStep = (SpatialCalculator.CAMPUS_GCJ02_LNG_MAX - SpatialCalculator.CAMPUS_GCJ02_LNG_MIN) / gridSize;
        double latStep = (SpatialCalculator.CAMPUS_GCJ02_LAT_MAX - SpatialCalculator.CAMPUS_GCJ02_LAT_MIN) / gridSize;

        List<Activity> published = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>().eq(Activity::getStatus, "published"));

        List<Map<String, Object>> features = new ArrayList<>();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double swLng = SpatialCalculator.CAMPUS_GCJ02_LNG_MIN + i * lngStep;
                double swLat = SpatialCalculator.CAMPUS_GCJ02_LAT_MIN + j * latStep;
                double neLng = swLng + lngStep;
                double neLat = swLat + latStep;

                int count = 0;
                for (Activity a : published) {
                    if (a.getLongitude() == null || a.getLatitude() == null) continue;
                    double[] gcj = coordConvertService.wgs84ToGcj02(
                            a.getLongitude().doubleValue(), a.getLatitude().doubleValue());
                    if (gcj[0] >= swLng && gcj[0] <= neLng && gcj[1] >= swLat && gcj[1] <= neLat) count++;
                }

                Map<String, Object> f = new LinkedHashMap<>();
                f.put("type", "Feature");
                Map<String, Object> props = new LinkedHashMap<>();
                props.put("count", count);
                props.put("gridX", i);
                props.put("gridY", j);
                f.put("properties", props);

                Map<String, Object> geom = new LinkedHashMap<>();
                geom.put("type", "Polygon");
                List<List<List<Double>>> ring = new ArrayList<>();
                ring.add(Arrays.asList(
                        Arrays.asList(swLng, swLat), Arrays.asList(neLng, swLat),
                        Arrays.asList(neLng, neLat), Arrays.asList(swLng, neLat),
                        Arrays.asList(swLng, swLat)));
                geom.put("coordinates", ring);
                f.put("geometry", geom);
                features.add(f);
            }
        }

        Map<String, Object> fc = new LinkedHashMap<>();
        fc.put("type", "FeatureCollection");
        fc.put("features", features);
        return fc;
    }

    // ──── 时段空间分布 ───────────────────────────

    @Override
    public FeatureCollection timelineAnalysis(String yearMonth) {
        List<Activity> activities;
        if (yearMonth != null && !yearMonth.isBlank()) {
            // Filter by year-month, e.g., "2026-06"
            String prefix = yearMonth + "-";
            activities = activityMapper.selectList(
                    new LambdaQueryWrapper<Activity>()
                            .eq(Activity::getStatus, "published")
                            .and(w -> w.likeRight(Activity::getStartTime, prefix)
                                     .or().likeRight(Activity::getCreatedAt, prefix))
            );
        } else {
            activities = activityMapper.selectList(
                    new LambdaQueryWrapper<Activity>().eq(Activity::getStatus, "published"));
        }

        List<Feature> features = new ArrayList<>();
        for (Activity a : activities) {
            if (a.getLongitude() == null || a.getLatitude() == null) continue;
            double[] gcj = coordConvertService.wgs84ToGcj02(
                    a.getLongitude().doubleValue(), a.getLatitude().doubleValue());
            Map<String, Object> props = geoJsonBuilder.props();
            props.put("id", a.getId());
            props.put("title", a.getTitle());
            props.put("category", a.getCategory() != null ? a.getCategory() : "");
            props.put("startTime", a.getStartTime() != null ? a.getStartTime().toString() : null);
            features.add(geoJsonBuilder.pointFeature(GeoPoint.of(gcj[0], gcj[1]), props));
        }
        return geoJsonBuilder.collection(features);
    }

    // ──── 集合点推荐 ─────────────────────────────

    @Override
    public List<Map<String, Object>> clusterMeeting(Long activityId, int k) {
        // Get sign-in coordinates for this activity
        List<Signup> signups = signupMapper.selectList(
                new LambdaQueryWrapper<Signup>()
                        .eq(Signup::getActivityId, activityId)
                        .isNotNull(Signup::getSignInLng)
                        .isNotNull(Signup::getSignInLat));

        List<double[]> points = new ArrayList<>();
        for (Signup s : signups) {
            points.add(new double[]{
                    s.getSignInLng().doubleValue(),
                    s.getSignInLat().doubleValue()});
        }

        // Simple k-means clustering
        int actualK = Math.min(k, points.size());
        if (actualK == 0) return List.of();

        List<double[]> centroids = kMeans(points, actualK);

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < centroids.size(); i++) {
            double[] gcj = coordConvertService.wgs84ToGcj02(centroids.get(i)[0], centroids.get(i)[1]);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", i + 1);
            m.put("name", "推荐集合点 #" + (i + 1));
            m.put("lng", gcj[0]);
            m.put("lat", gcj[1]);
            result.add(m);
        }
        return result;
    }

    /** Simple k-means clustering */
    private List<double[]> kMeans(List<double[]> points, int k) {
        if (points.isEmpty()) return List.of();
        // Initialize centroids randomly from points
        List<double[]> centroids = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            centroids.add(points.get(i % points.size()).clone());
        }

        for (int iter = 0; iter < 20; iter++) {
            // Assign points to nearest centroid
            List<List<double[]>> clusters = new ArrayList<>();
            for (int i = 0; i < k; i++) clusters.add(new ArrayList<>());
            for (double[] p : points) {
                int best = 0;
                double bestDist = Double.MAX_VALUE;
                for (int i = 0; i < k; i++) {
                    double d = Math.pow(p[0] - centroids.get(i)[0], 2)
                             + Math.pow(p[1] - centroids.get(i)[1], 2);
                    if (d < bestDist) { bestDist = d; best = i; }
                }
                clusters.get(best).add(p);
            }
            // Recompute centroids
            boolean changed = false;
            for (int i = 0; i < k; i++) {
                if (clusters.get(i).isEmpty()) continue;
                double sumLng = 0, sumLat = 0;
                for (double[] p : clusters.get(i)) { sumLng += p[0]; sumLat += p[1]; }
                double newLng = sumLng / clusters.get(i).size();
                double newLat = sumLat / clusters.get(i).size();
                if (Math.abs(newLng - centroids.get(i)[0]) > 1e-6
                        || Math.abs(newLat - centroids.get(i)[1]) > 1e-6) changed = true;
                centroids.get(i)[0] = newLng;
                centroids.get(i)[1] = newLat;
            }
            if (!changed) break;
        }
        return centroids;
    }
}

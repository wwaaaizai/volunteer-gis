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
                new LambdaQueryWrapper<Activity>().eq(Activity::getStatus, "published"));
        List<Feature> features = new ArrayList<>();
        for (Activity a : activities) {
            if (a.getLongitude() == null || a.getLatitude() == null) continue;
            Map<String, Object> properties = geoJsonBuilder.props();
            properties.put("id", a.getId());
            properties.put("title", a.getTitle());
            properties.put("locationName", a.getLocationName());
            properties.put("startTime", a.getStartTime() != null ? a.getStartTime().toString() : null);
            double[] gcj = coordConvertService.wgs84ToGcj02(
                    a.getLongitude().doubleValue(), a.getLatitude().doubleValue());
            features.add(geoJsonBuilder.pointFeature(GeoPoint.of(gcj[0], gcj[1]), properties));
        }
        return geoJsonBuilder.collection(features);
    }

    @Override
    public FeatureCollection getHeatmapData(String category, int months) {
        LocalDateTime since = LocalDateTime.now().minusMonths(months);
        List<Long> activityIds = null;
        if (category != null && !category.isBlank()) {
            activityIds = activityMapper.selectList(
                    new LambdaQueryWrapper<Activity>().eq(Activity::getCategory, category))
                    .stream().map(Activity::getId).collect(Collectors.toList());
            if (activityIds.isEmpty()) return geoJsonBuilder.collection(List.of());
        }
        LambdaQueryWrapper<Signup> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Signup::getSignInLng).isNotNull(Signup::getSignInLat)
               .ge(Signup::getSignInTime, since);
        if (activityIds != null) wrapper.in(Signup::getActivityId, activityIds);
        List<Signup> signups = signupMapper.selectList(wrapper);
        List<Feature> features = new ArrayList<>();
        for (Signup s : signups) {
            double[] gcj = coordConvertService.wgs84ToGcj02(
                    s.getSignInLng().doubleValue(), s.getSignInLat().doubleValue());
            Map<String, Object> props = geoJsonBuilder.props();
            props.put("activityId", s.getActivityId());
            props.put("weight", s.getVolunteerHours() != null
                    ? s.getVolunteerHours().doubleValue() : 0.5);
            features.add(geoJsonBuilder.pointFeature(GeoPoint.of(gcj[0], gcj[1]), props));
        }
        return geoJsonBuilder.collection(features);
    }

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
                item.put("id", a.getId()); item.put("title", a.getTitle());
                item.put("lng", a.getLongitude()); item.put("lat", a.getLatitude());
                item.put("distance", Math.round(dist));
                item.put("signedCount", a.getSignedCount());
                activities.add(item);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("centerLng", lng); result.put("centerLat", lat);
        result.put("radius", radius); result.put("coveredCount", covered);
        result.put("totalSignups", activities.stream()
                .mapToInt(a -> (Integer) a.get("signedCount")).sum());
        result.put("activities", activities);
        return result;
    }

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
                double neLng = swLng + lngStep, neLat = swLat + latStep;
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
                props.put("count", count); props.put("gridX", i); props.put("gridY", j);
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
        fc.put("type", "FeatureCollection"); fc.put("features", features);
        return fc;
    }
    @Override
    public List<Map<String, Object>> clusterMeeting(Long activityId, int k) {
        List<Signup> signups = signupMapper.selectList(new LambdaQueryWrapper<Signup>()
                .eq(Signup::getActivityId, activityId)
                .isNotNull(Signup::getSignInLng).isNotNull(Signup::getSignInLat));
        List<double[]> points = new ArrayList<>();
        for (Signup s : signups) {
            points.add(new double[]{s.getSignInLng().doubleValue(), s.getSignInLat().doubleValue()});
        }
        int actualK = Math.min(k, points.size());
        if (actualK == 0 || points.isEmpty()) return List.of();

        // k-means++ (WGS-84坐标聚类)
        List<double[]> centroids = kMeansPlusPlus(points, actualK);

        // 统计每个簇的签到人数
        int[] sizes = new int[actualK];
        for (double[] p : points) {
            int best = 0; double bestD = Double.MAX_VALUE;
            for (int i = 0; i < actualK; i++) {
                double d = Math.pow(p[0] - centroids.get(i)[0], 2)
                         + Math.pow(p[1] - centroids.get(i)[1], 2);
                if (d < bestD) { bestD = d; best = i; }
            }
            sizes[best]++;
        }

        // 输出GCJ-02坐标（对齐天地图）
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < centroids.size(); i++) {
            double[] gcj = coordConvertService.wgs84ToGcj02(centroids.get(i)[0], centroids.get(i)[1]);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", i + 1);
            m.put("name", "集合点 #" + (i + 1));
            m.put("lng", gcj[0]);
            m.put("lat", gcj[1]);
            m.put("signupCount", sizes[i]);
            result.add(m);
        }
        return result;
    }

    /** k-means++ 初始化 + Lloyd迭代 */
    private List<double[]> kMeansPlusPlus(List<double[]> points, int k) {
        List<double[]> centroids = new ArrayList<>();
        Random rng = new Random();
        centroids.add(points.get(rng.nextInt(points.size())).clone());
        for (int c = 1; c < k; c++) {
            double[] distSq = new double[points.size()];
            double total = 0;
            for (int i = 0; i < points.size(); i++) {
                double minD = Double.MAX_VALUE;
                for (double[] cent : centroids) {
                    double d = Math.pow(points.get(i)[0] - cent[0], 2)
                             + Math.pow(points.get(i)[1] - cent[1], 2);
                    if (d < minD) minD = d;
                }
                distSq[i] = minD; total += minD;
            }
            double r = rng.nextDouble() * total;
            double cum = 0; int chosen = 0;
            for (int i = 0; i < points.size(); i++) {
                cum += distSq[i]; if (cum >= r) { chosen = i; break; }
            }
            centroids.add(points.get(chosen).clone());
        }
        for (int iter = 0; iter < 30; iter++) {
            List<List<double[]>> clusters = new ArrayList<>();
            for (int i = 0; i < k; i++) clusters.add(new ArrayList<>());
            for (double[] p : points) {
                int best = 0; double bestD = Double.MAX_VALUE;
                for (int i = 0; i < k; i++) {
                    double d = Math.pow(p[0] - centroids.get(i)[0], 2)
                             + Math.pow(p[1] - centroids.get(i)[1], 2);
                    if (d < bestD) { bestD = d; best = i; }
                }
                clusters.get(best).add(p);
            }
            boolean changed = false;
            for (int i = 0; i < k; i++) {
                if (clusters.get(i).isEmpty()) continue;
                double sl = 0, sa = 0;
                for (double[] p : clusters.get(i)) { sl += p[0]; sa += p[1]; }
                double nl = sl / clusters.get(i).size(), na = sa / clusters.get(i).size();
                if (Math.abs(nl - centroids.get(i)[0]) > 1e-7
                        || Math.abs(na - centroids.get(i)[1]) > 1e-7) changed = true;
                centroids.get(i)[0] = nl; centroids.get(i)[1] = na;
            }
            if (!changed) break;
        }
        return centroids;
    }
}

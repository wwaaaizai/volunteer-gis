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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {

    private final ActivityMapper activityMapper;
    private final SignupMapper signupMapper;
    private final GeoJsonBuilder geoJsonBuilder;
    private final CoordConvertService coordConvertService;

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

            double[] gcj = coordConvertService.wgs84ToGcj02(
                    a.getLongitude().doubleValue(), a.getLatitude().doubleValue());
            features.add(geoJsonBuilder.pointFeature(
                    GeoPoint.of(gcj[0], gcj[1]), properties));
        }

        return geoJsonBuilder.collection(features);
    }

    @Override
    public FeatureCollection getHeatmapData(String category, int months) {
        LocalDateTime since = LocalDateTime.now().minusMonths(months);

        List<Long> activityIds = null;
        if (category != null && !category.isBlank()) {
            activityIds = activityMapper.selectList(
                    new LambdaQueryWrapper<Activity>()
                            .eq(Activity::getCategory, category)
            ).stream().map(Activity::getId).collect(Collectors.toList());
            if (activityIds.isEmpty()) return geoJsonBuilder.collection(List.of());
        }

        LambdaQueryWrapper<Signup> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Signup::getSignInLng)
               .isNotNull(Signup::getSignInLat)
               .ge(Signup::getSignInTime, since);
        if (activityIds != null) {
            wrapper.in(Signup::getActivityId, activityIds);
        }
        List<Signup> signups = signupMapper.selectList(wrapper);

        List<Feature> features = new ArrayList<>();
        for (Signup s : signups) {
            double[] gcj = coordConvertService.wgs84ToGcj02(
                    s.getSignInLng().doubleValue(), s.getSignInLat().doubleValue());
            Map<String, Object> props = geoJsonBuilder.props();
            props.put("activityId", s.getActivityId());
            props.put("userId", s.getUserId());
            props.put("weight", s.getVolunteerHours() != null
                    ? s.getVolunteerHours().doubleValue() : 0.5);
            features.add(geoJsonBuilder.pointFeature(GeoPoint.of(gcj[0], gcj[1]), props));
        }

        return geoJsonBuilder.collection(features);
    }
}

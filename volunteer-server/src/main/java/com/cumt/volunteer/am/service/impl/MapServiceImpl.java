package com.cumt.volunteer.am.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cumt.volunteer.aca.mapper.ActivityMapper;
import com.cumt.volunteer.am.service.MapService;
import com.cumt.volunteer.entity.Activity;
import com.cumt.volunteer.geo.model.Feature;
import com.cumt.volunteer.geo.model.FeatureCollection;
import com.cumt.volunteer.geo.model.GeoPoint;
import com.cumt.volunteer.geo.service.CoordConvertService;
import com.cumt.volunteer.geo.service.GeoJsonBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {

    private final ActivityMapper activityMapper;
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
}

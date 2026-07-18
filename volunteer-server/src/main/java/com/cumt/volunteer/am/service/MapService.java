package com.cumt.volunteer.am.service;

import com.cumt.volunteer.geo.model.FeatureCollection;

public interface MapService {

    /**
     * 返回已发布活动点的 GeoJSON FeatureCollection
     */
    FeatureCollection getActivityGeoJSON();

    /**
     * 返回签到热力图数据（GeoJSON Point 集合）
     */
    FeatureCollection getHeatmapData(String category, int months);
}

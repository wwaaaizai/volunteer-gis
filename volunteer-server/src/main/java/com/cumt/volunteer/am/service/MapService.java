package com.cumt.volunteer.am.service;

import com.cumt.volunteer.geo.model.FeatureCollection;

public interface MapService {

    /**
     * 返回已发布活动点的 GeoJSON FeatureCollection
     */
    FeatureCollection getActivityGeoJSON();
}

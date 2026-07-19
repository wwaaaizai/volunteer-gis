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

    /** 缓冲区分析 */
    java.util.Map<String, Object> bufferAnalysis(double lng, double lat, double radius);

    /** 覆盖率分析 */
    java.util.Map<String, Object> coverageAnalysis(int gridSize);

    /** 集合点推荐 */
    java.util.List<java.util.Map<String, Object>> clusterMeeting(Long activityId, int k);

}

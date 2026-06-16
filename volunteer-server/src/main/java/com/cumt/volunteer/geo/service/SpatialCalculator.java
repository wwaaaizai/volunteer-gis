package com.cumt.volunteer.geo.service;

import com.cumt.volunteer.geo.model.GeoPoint;
import org.springframework.stereotype.Component;

/**
 * 空间距离计算工具（Haversine 公式）。
 * <p>原先内联在 {@code CheckInServiceImpl} 中，现抽出为横切组件，
 * 供签到校验、地图距离过滤、未来"附近活动"等功能复用。</p>
 */
@Component
public class SpatialCalculator {

    /** 地球平均半径（米） */
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    /**
     * 用 Haversine 公式计算两点间的大圆距离（米）。
     *
     * @param a 点 A
     * @param b 点 B
     * @return 距离（米）
     */
    public double distanceMeters(GeoPoint a, GeoPoint b) {
        return haversine(a.latitude().doubleValue(), a.longitude().doubleValue(),
                b.latitude().doubleValue(), b.longitude().doubleValue());
    }

    /**
     * 判断点 b 是否在以 a 为圆心、radiusMeters 为半径的圆内。
     *
     * @param a             圆心
     * @param b             待判定点
     * @param radiusMeters  半径（米）
     * @return true 表示在范围内
     */
    public boolean isWithin(GeoPoint a, GeoPoint b, double radiusMeters) {
        return distanceMeters(a, b) <= radiusMeters;
    }

    /**
     * Haversine 公式核心实现。
     *
     * @param lat1 纬度1
     * @param lng1 经度1
     * @param lat2 纬度2
     * @param lng2 经度2
     * @return 距离（米）
     */
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

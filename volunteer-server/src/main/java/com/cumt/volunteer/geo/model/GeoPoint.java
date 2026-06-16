package com.cumt.volunteer.geo.model;

import java.math.BigDecimal;

/**
 * 地理坐标值对象（WGS84），不可变。
 * <p>替代散落在实体中的裸 {@code BigDecimal longitude/latitude}，
 * 统一经纬度的传递与计算语义。</p>
 *
 * <p><b>注意</b>：GeoJSON 规范要求坐标顺序为 [经度, 纬度]（lng, lat），
 * {@link #toCoordinateArray()} 已遵循该顺序。</p>
 *
 * @param longitude 经度
 * @param latitude  纬度
 */
public record GeoPoint(BigDecimal longitude, BigDecimal latitude) {

    /**
     * 返回 GeoJSON 标准坐标数组 [经度, 纬度]。
     */
    public double[] toCoordinateArray() {
        return new double[]{longitude.doubleValue(), latitude.doubleValue()};
    }

    /**
     * 由两个 BigDecimal 直接构造。
     */
    public static GeoPoint of(BigDecimal longitude, BigDecimal latitude) {
        return new GeoPoint(longitude, latitude);
    }

    /**
     * 由两个 double 直接构造。
     */
    public static GeoPoint of(double longitude, double latitude) {
        return new GeoPoint(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude));
    }
}

package com.cumt.volunteer.geo.service;

import com.cumt.volunteer.geo.model.GeoPoint;
import org.springframework.stereotype.Component;

/**
 * WGS-84 → GCJ-02 坐标转换服务。
 *
 * <p>天地图底图使用 GCJ-02（国测局坐标系/火星坐标系），
 * 而数据库存储和浏览器 GPS 返回 WGS-84，两者在中国境内约有 300-500 米偏移。
 * 本服务将 WGS-84 坐标转换为 GCJ-02，使地图标注点与天地图底图对齐。</p>
 */
@Component
public class CoordConvertService {

    private static final double PI = Math.PI;
    private static final double A = 6378245.0; // 长半轴
    private static final double EE = 0.00669342162296594323; // 扁率

    private double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0;
        ret += ((20.0 * Math.sin(y * PI) + 40.0 * Math.sin((y / 3.0) * PI)) * 2.0) / 3.0;
        ret += ((160.0 * Math.sin((y / 12.0) * PI) + 320.0 * Math.sin((y * PI) / 30.0)) * 2.0) / 3.0;
        return ret;
    }

    private double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0;
        ret += ((20.0 * Math.sin(x * PI) + 40.0 * Math.sin((x / 3.0) * PI)) * 2.0) / 3.0;
        ret += ((150.0 * Math.sin((x / 12.0) * PI) + 300.0 * Math.sin((x / 30.0) * PI)) * 2.0) / 3.0;
        return ret;
    }

    private boolean outOfChina(double lng, double lat) {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    /**
     * 单个坐标 WGS-84 → GCJ-02 转换
     *
     * @param lng WGS-84 经度
     * @param lat WGS-84 纬度
     * @return GCJ-02 坐标 [经度, 纬度]
     */
    public double[] wgs84ToGcj02(double lng, double lat) {
        if (outOfChina(lng, lat)) {
            return new double[]{lng, lat};
        }
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLon = transformLon(lng - 105.0, lat - 35.0);
        double radLat = (lat / 180.0) * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / (((A * (1 - EE)) / (magic * sqrtMagic)) * PI);
        dLon = (dLon * 180.0) / ((A / sqrtMagic) * Math.cos(radLat) * PI);
        return new double[]{lng + dLon, lat + dLat};
    }

    /**
     * GeoPoint WGS-84 → GCJ-02 便捷方法
     */
    public GeoPoint wgs84ToGcj02(GeoPoint point) {
        double[] result = wgs84ToGcj02(point.longitude(), point.latitude());
        return GeoPoint.of(result[0], result[1]);
    }
}

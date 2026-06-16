package com.cumt.volunteer.geo.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * GeoJSON Geometry 的轻量 POJO 表示。
 * 当前仅支持 Point 类型（MVP 阶段活动均为点要素），
 * 未来如需 LineString/Polygon 可在此扩展 type 字段与对应坐标结构。
 */
public class Geometry {

    /** geometry.type 固定为 "Point" */
    private final String type = "Point";

    /** 坐标数组 [经度, 纬度]，与 GeoJSON 规范一致 */
    private double[] coordinates;

    public Geometry() {
    }

    public Geometry(double[] coordinates) {
        this.coordinates = coordinates;
    }

    /** 由 GeoPoint 构造 Point 几何 */
    public static Geometry point(GeoPoint point) {
        return new Geometry(point.toCoordinateArray());
    }

    public String getType() {
        return type;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }
}

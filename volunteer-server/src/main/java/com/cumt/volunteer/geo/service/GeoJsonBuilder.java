package com.cumt.volunteer.geo.service;

import com.cumt.volunteer.geo.model.Feature;
import com.cumt.volunteer.geo.model.FeatureCollection;
import com.cumt.volunteer.geo.model.GeoPoint;
import com.cumt.volunteer.geo.model.Geometry;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GeoJSON 构造器。
 * <p>原先在 {@code MapServiceImpl} 中用 {@code LinkedHashMap} 手工拼接，
 * 现统一收敛至此组件，返回强类型 {@link FeatureCollection}，
 * 由 Jackson 序列化为标准 GeoJSON。</p>
 */
@Component
public class GeoJsonBuilder {

    /**
     * 构造一个 Point Feature。
     *
     * @param point     点坐标
     * @param properties 要素属性（保持插入顺序）
     * @return Feature
     */
    public Feature pointFeature(GeoPoint point, Map<String, Object> properties) {
        return new Feature(Geometry.point(point), properties);
    }

    /**
     * 构造一个 Point Feature（便捷重载，直接传经纬度与属性 Map）。
     */
    public Feature pointFeature(double longitude, double latitude, Map<String, Object> properties) {
        return pointFeature(GeoPoint.of(longitude, latitude), properties);
    }

    /**
     * 由 Feature 列表构造 FeatureCollection。
     */
    public FeatureCollection collection(List<Feature> features) {
        return new FeatureCollection(features);
    }

    /**
     * 创建一个保持插入顺序的属性 Map（便于链式 put）。
     */
    public Map<String, Object> props() {
        return new LinkedHashMap<>();
    }
}

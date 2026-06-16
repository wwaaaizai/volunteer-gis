package com.cumt.volunteer.geo.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GeoJSON Feature（要素）。
 * <p>一个 Feature 由 geometry + properties 构成。</p>
 *
 * <p>properties 使用 {@link LinkedHashMap} 以保持字段插入顺序，
 * 与原 {@code MapServiceImpl} 的输出行为一致（前端依赖 title/locationName/startTime 等字段名）。</p>
 */
public class Feature {

    private final String type = "Feature";

    private Geometry geometry;

    private Map<String, Object> properties;

    public Feature() {
    }

    public Feature(Geometry geometry, Map<String, Object> properties) {
        this.geometry = geometry;
        this.properties = properties;
    }

    public String getType() {
        return type;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}

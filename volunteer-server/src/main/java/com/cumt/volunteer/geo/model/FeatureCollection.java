package com.cumt.volunteer.geo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * GeoJSON FeatureCollection（要素集合）。
 * <p>地图接口的统一返回类型，替代原先手工拼装的 {@code Map<String, Object>}。</p>
 *
 * <p>Jackson 序列化后即为标准 GeoJSON：</p>
 * <pre>{@code
 * { "type": "FeatureCollection", "features": [ ... ] }
 * }</pre>
 */
public class FeatureCollection {

    private final String type = "FeatureCollection";

    private List<Feature> features = new ArrayList<>();

    public FeatureCollection() {
    }

    public FeatureCollection(List<Feature> features) {
        this.features = features;
    }

    public String getType() {
        return type;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public void setFeatures(List<Feature> features) {
        this.features = features;
    }
}

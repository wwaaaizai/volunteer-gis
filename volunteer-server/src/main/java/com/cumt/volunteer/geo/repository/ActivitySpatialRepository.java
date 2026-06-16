package com.cumt.volunteer.geo.repository;

import com.cumt.volunteer.entity.Activity;
import com.cumt.volunteer.geo.model.GeoPoint;

import java.util.List;

/**
 * 活动空间查询抽象接口。
 *
 * <p><b>架构意义</b>：此接口是"空间数据访问"与"业务逻辑"之间的隔离层。
 * 当前实现（{@code ActivitySpatialRepositoryImpl}）基于 MySQL + 内存 Haversine 过滤；
 * <b>未来迁移 PostGIS 时，仅需提供新的实现类，业务层（service）零改动</b>。
 * 这正是将空间查询抽到独立 geo 模块的核心价值。</p>
 *
 * <p>当前 MVP 阶段地图接口仍直接复用 {@code aca/mapper/ActivityMapper}，
 * 此接口为"附近活动"等后续 WebGIS 功能预留，并提供迁移锚点。</p>
 */
public interface ActivitySpatialRepository {

    /**
     * 查询已发布且位于指定中心点 radiusMeters 范围内的活动。
     *
     * @param center        中心点
     * @param radiusMeters  搜索半径（米）
     * @return 命中活动列表
     */
    List<Activity> findPublishedWithin(GeoPoint center, double radiusMeters);
}

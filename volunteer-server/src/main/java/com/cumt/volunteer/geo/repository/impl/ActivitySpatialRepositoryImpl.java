package com.cumt.volunteer.geo.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cumt.volunteer.aca.mapper.ActivityMapper;
import com.cumt.volunteer.entity.Activity;
import com.cumt.volunteer.geo.model.GeoPoint;
import com.cumt.volunteer.geo.repository.ActivitySpatialRepository;
import com.cumt.volunteer.geo.service.SpatialCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * {@link ActivitySpatialRepository} 的 MySQL 实现。
 *
 * <p><b>当前策略</b>：MySQL 无空间索引，先按 status=published 拉全量，
 * 再用 {@link SpatialCalculator} 在内存中做 Haversine 范围过滤。
 * 数据量在 MVP 阶段（百级活动）完全可接受。</p>
 *
 * <p><b>迁移指引</b>：切换 PostGIS 后，此实现可替换为
 * {@code ST_DWithin(geom, ST_MakePoint(lng,lat)::geography, radius)}
 * 的 SQL 查询，走 GiST 索引。业务层无需改动。</p>
 */
@Repository
@RequiredArgsConstructor
public class ActivitySpatialRepositoryImpl implements ActivitySpatialRepository {

    private final ActivityMapper activityMapper;
    private final SpatialCalculator spatialCalculator;

    @Override
    public List<Activity> findPublishedWithin(GeoPoint center, double radiusMeters) {
        List<Activity> published = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>().eq(Activity::getStatus, "published")
        );
        return published.stream()
                .filter(a -> a.getLongitude() != null && a.getLatitude() != null)
                .filter(a -> spatialCalculator.isWithin(
                        center,
                        GeoPoint.of(a.getLongitude(), a.getLatitude()),
                        radiusMeters))
                .toList();
    }
}

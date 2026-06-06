package com.zhisheng.mvp.process.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhisheng.mvp.process.dto.RouteTemplateOptionResponse;
import com.zhisheng.mvp.process.dto.RouteTemplateRequest;
import com.zhisheng.mvp.process.dto.RouteTemplateResponse;
import com.zhisheng.mvp.process.entity.ProcessRouteTemplate;
import com.zhisheng.mvp.process.entity.ProcessStepTemplate;
import com.zhisheng.mvp.process.mapper.ProcessRouteTemplateMapper;
import com.zhisheng.mvp.process.mapper.ProcessStepTemplateMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RouteTemplateService {

    private static final long DEFAULT_TENANT_ID = 1L;

    private final ProcessRouteTemplateMapper routeMapper;
    private final ProcessStepTemplateMapper stepMapper;

    public RouteTemplateService(
            ProcessRouteTemplateMapper routeMapper,
            ProcessStepTemplateMapper stepMapper) {
        this.routeMapper = routeMapper;
        this.stepMapper = stepMapper;
    }

    @Transactional
    public RouteTemplateResponse create(RouteTemplateRequest request) {
        ProcessRouteTemplate route = new ProcessRouteTemplate();
        route.setTenantId(DEFAULT_TENANT_ID);
        apply(route, request);
        route.setEnabled(request.enabled() == null || request.enabled());
        route.setVersion(0);
        route.setCreatedAt(LocalDateTime.now());
        route.setUpdatedAt(LocalDateTime.now());
        route.setDeleted(false);
        route.setDeleteMarker(0L);
        routeMapper.insert(route);
        return RouteTemplateResponse.from(route);
    }

    @Transactional(readOnly = true)
    public List<RouteTemplateResponse> list() {
        return routeMapper.selectList(new LambdaQueryWrapper<ProcessRouteTemplate>()
                        .eq(ProcessRouteTemplate::getTenantId, DEFAULT_TENANT_ID)
                        .eq(ProcessRouteTemplate::getDeleted, false)
                        .orderByDesc(ProcessRouteTemplate::getId))
                .stream()
                .map(RouteTemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteTemplateResponse get(Long id) {
        return RouteTemplateResponse.from(requiredRoute(id));
    }

    @Transactional
    public RouteTemplateResponse update(Long id, RouteTemplateRequest request) {
        ProcessRouteTemplate route = requiredRoute(id);
        apply(route, request);
        route.setUpdatedAt(LocalDateTime.now());
        routeMapper.updateById(route);
        return RouteTemplateResponse.from(requiredRoute(id));
    }

    @Transactional
    public RouteTemplateResponse setEnabled(Long id, boolean enabled) {
        ProcessRouteTemplate route = requiredRoute(id);
        route.setEnabled(enabled);
        route.setUpdatedAt(LocalDateTime.now());
        routeMapper.updateById(route);
        return RouteTemplateResponse.from(requiredRoute(id));
    }

    @Transactional
    public void delete(Long id) {
        ProcessRouteTemplate route = requiredRoute(id);
        route.setDeleted(true);
        route.setEnabled(false);
        route.setDeleteMarker(route.getId());
        route.setUpdatedAt(LocalDateTime.now());
        routeMapper.updateById(route);
    }

    @Transactional(readOnly = true)
    public List<RouteTemplateOptionResponse> options(String productType) {
        LambdaQueryWrapper<ProcessRouteTemplate> query = new LambdaQueryWrapper<ProcessRouteTemplate>()
                .eq(ProcessRouteTemplate::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProcessRouteTemplate::getEnabled, true)
                .eq(ProcessRouteTemplate::getDeleted, false)
                .orderByAsc(ProcessRouteTemplate::getRouteName);

        if (StringUtils.hasText(productType)) {
            query.and(wrapper -> wrapper
                    .eq(ProcessRouteTemplate::getProductType, productType)
                    .or()
                    .eq(ProcessRouteTemplate::getProductType, "GENERAL")
                    .or()
                    .eq(ProcessRouteTemplate::getProductType, "")
                    .or()
                    .isNull(ProcessRouteTemplate::getProductType));
        }

        return routeMapper.selectList(query).stream()
                .map(route -> new RouteTemplateOptionResponse(
                        route.getId(),
                        route.getRouteName(),
                        route.getProductType(),
                        activeStepCount(route.getId())))
                .toList();
    }

    public ProcessRouteTemplate requiredRoute(Long id) {
        ProcessRouteTemplate route = routeMapper.selectById(id);
        if (route == null || Boolean.TRUE.equals(route.getDeleted())) {
            throw new IllegalArgumentException("Route template not found");
        }
        return route;
    }

    private Long activeStepCount(Long routeId) {
        return stepMapper.selectCount(new LambdaQueryWrapper<ProcessStepTemplate>()
                .eq(ProcessStepTemplate::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProcessStepTemplate::getRouteTemplateId, routeId)
                .eq(ProcessStepTemplate::getEnabled, true)
                .eq(ProcessStepTemplate::getDeleted, false));
    }

    private void apply(ProcessRouteTemplate route, RouteTemplateRequest request) {
        if (!StringUtils.hasText(request.routeCode())) {
            throw new IllegalArgumentException("routeCode is required");
        }
        if (!StringUtils.hasText(request.routeName())) {
            throw new IllegalArgumentException("routeName is required");
        }
        route.setRouteCode(request.routeCode());
        route.setRouteName(request.routeName());
        route.setProductType(request.productType());
        route.setDescription(request.description());
        if (request.enabled() != null) {
            route.setEnabled(request.enabled());
        }
    }
}

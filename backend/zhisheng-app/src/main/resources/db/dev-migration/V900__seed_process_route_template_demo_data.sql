INSERT INTO process_route_template (
    tenant_id,
    route_code,
    route_name,
    product_type,
    description,
    enabled,
    version,
    deleted,
    delete_marker
) VALUES
    (1, 'RT-SPIRIT-FORTRESS', '精神堡垒工艺路线', 'SPIRIT_FORTRESS', '适用于大型精神堡垒、入口形象标识等非标结构件生产。', 1, 0, 0, 0),
    (1, 'RT-FLOOR-SIGN', '楼层牌工艺路线', 'FLOOR_SIGN', '适用于楼层索引牌、门牌、室内小型标识制作。', 1, 0, 0, 0),
    (1, 'RT-ILLUMINATED-LETTER', '发光字工艺路线', 'ILLUMINATED_LETTER', '适用于发光字、灯箱字等含电气测试的标识产品。', 1, 0, 0, 0),
    (1, 'RT-WAYFINDING-SIGN', '普通导视牌工艺路线', 'WAYFINDING_SIGN', '适用于常规导视牌、停车场指示牌、方向标识制作。', 1, 0, 0, 0),
    (1, 'RT-GENERAL-SIGN', '通用导视工艺路线', 'GENERAL', '适用于未细分产品类型的通用标识导视生产配置。', 1, 0, 0, 0);

INSERT INTO process_step_template (
    tenant_id,
    route_template_id,
    step_code,
    step_name,
    step_order,
    assigned_role,
    photo_required,
    remark_required,
    mobile_enabled,
    estimated_hours,
    operation_instruction,
    enabled,
    deleted,
    delete_marker
)
SELECT 1, route.id, step.step_code, step.step_name, step.step_order, step.assigned_role,
       step.photo_required, step.remark_required, step.mobile_enabled, step.estimated_hours,
       step.operation_instruction, 1, 0, 0
FROM process_route_template route
JOIN (
    SELECT 'RT-SPIRIT-FORTRESS' AS route_code, 'SPIRIT-DESIGN' AS step_code, '设计深化' AS step_name, 1 AS step_order, 'DESIGNER' AS assigned_role, 0 AS photo_required, 1 AS remark_required, 0 AS mobile_enabled, 4.00 AS estimated_hours, '确认结构尺寸、材质、外观和制作图纸。' AS operation_instruction
    UNION ALL SELECT 'RT-SPIRIT-FORTRESS', 'SPIRIT-SPLIT', '拆单', 2, 'PRODUCTION_MANAGER', 0, 1, 0, 2.00, '拆分生产工艺、材料和加工任务。'
    UNION ALL SELECT 'RT-SPIRIT-FORTRESS', 'SPIRIT-CUT', '下料', 3, 'WORKER', 1, 0, 1, 6.00, '按图纸完成板材、型材下料。'
    UNION ALL SELECT 'RT-SPIRIT-FORTRESS', 'SPIRIT-WELD', '焊接', 4, 'WORKER', 1, 1, 1, 8.00, '完成主体结构焊接并检查焊点。'
    UNION ALL SELECT 'RT-SPIRIT-FORTRESS', 'SPIRIT-POLISH', '打磨', 5, 'WORKER', 1, 0, 1, 5.00, '打磨焊点和边角，处理表面平整度。'
    UNION ALL SELECT 'RT-SPIRIT-FORTRESS', 'SPIRIT-PAINT', '喷漆', 6, 'WORKER', 1, 1, 1, 6.00, '按色号进行底漆、面漆处理。'
    UNION ALL SELECT 'RT-SPIRIT-FORTRESS', 'SPIRIT-ASSEMBLY', '组装', 7, 'WORKER', 1, 1, 1, 6.00, '组装结构件、面板和配件。'
    UNION ALL SELECT 'RT-SPIRIT-FORTRESS', 'SPIRIT-QC', '质检', 8, 'QC', 1, 1, 1, 2.00, '检查尺寸、外观、结构稳定性和包装前状态。'
    UNION ALL SELECT 'RT-SPIRIT-FORTRESS', 'SPIRIT-PACK', '包装', 9, 'WAREHOUSE', 1, 0, 1, 2.00, '按运输要求完成防护包装。'
    UNION ALL SELECT 'RT-SPIRIT-FORTRESS', 'SPIRIT-INSTALL', '安装', 10, 'INSTALLER', 1, 1, 1, 6.00, '现场安装并提交安装结果。'

    UNION ALL SELECT 'RT-FLOOR-SIGN', 'FLOOR-DESIGN', '设计深化', 1, 'DESIGNER', 0, 1, 0, 2.00, '确认楼层牌版式、尺寸和材质。'
    UNION ALL SELECT 'RT-FLOOR-SIGN', 'FLOOR-LAYOUT', '排版', 2, 'DESIGNER', 0, 1, 0, 1.50, '按清单完成图文排版。'
    UNION ALL SELECT 'RT-FLOOR-SIGN', 'FLOOR-UV', 'UV打印', 3, 'WORKER', 1, 0, 1, 3.00, '完成面板 UV 打印。'
    UNION ALL SELECT 'RT-FLOOR-SIGN', 'FLOOR-LAMINATE', '覆膜', 4, 'WORKER', 1, 0, 1, 2.00, '完成表面覆膜保护。'
    UNION ALL SELECT 'RT-FLOOR-SIGN', 'FLOOR-CUT', '裁切', 5, 'WORKER', 1, 0, 1, 2.00, '按尺寸裁切成品。'
    UNION ALL SELECT 'RT-FLOOR-SIGN', 'FLOOR-QC', '质检', 6, 'QC', 1, 1, 1, 1.00, '检查文字、颜色、尺寸和表面质量。'
    UNION ALL SELECT 'RT-FLOOR-SIGN', 'FLOOR-PACK', '包装', 7, 'WAREHOUSE', 1, 0, 1, 1.00, '按点位清单分类包装。'

    UNION ALL SELECT 'RT-ILLUMINATED-LETTER', 'LETTER-DESIGN', '设计深化', 1, 'DESIGNER', 0, 1, 0, 3.00, '确认字形、尺寸、电气方案和安装方式。'
    UNION ALL SELECT 'RT-ILLUMINATED-LETTER', 'LETTER-LASER-CUT', '激光切割', 2, 'WORKER', 1, 0, 1, 4.00, '完成金属板材激光切割。'
    UNION ALL SELECT 'RT-ILLUMINATED-LETTER', 'LETTER-WELD', '焊接', 3, 'WORKER', 1, 1, 1, 5.00, '完成字壳焊接。'
    UNION ALL SELECT 'RT-ILLUMINATED-LETTER', 'LETTER-POLISH', '打磨', 4, 'WORKER', 1, 0, 1, 3.00, '处理焊点和边缘。'
    UNION ALL SELECT 'RT-ILLUMINATED-LETTER', 'LETTER-BAKE-PAINT', '烤漆', 5, 'WORKER', 1, 1, 1, 4.00, '完成烤漆并检查色差。'
    UNION ALL SELECT 'RT-ILLUMINATED-LETTER', 'LETTER-LIGHTING', '装灯', 6, 'WORKER', 1, 1, 1, 4.00, '安装 LED 模组和线路。'
    UNION ALL SELECT 'RT-ILLUMINATED-LETTER', 'LETTER-TEST', '测试', 7, 'QC', 1, 1, 1, 1.50, '进行通电测试和亮度检查。'
    UNION ALL SELECT 'RT-ILLUMINATED-LETTER', 'LETTER-PACK', '包装', 8, 'WAREHOUSE', 1, 0, 1, 1.50, '按字组和安装点位包装。'
    UNION ALL SELECT 'RT-ILLUMINATED-LETTER', 'LETTER-INSTALL', '安装', 9, 'INSTALLER', 1, 1, 1, 5.00, '现场安装并完成点亮确认。'

    UNION ALL SELECT 'RT-WAYFINDING-SIGN', 'WAY-DESIGN', '设计深化', 1, 'DESIGNER', 0, 1, 0, 2.00, '确认导视牌版式、材质和点位。'
    UNION ALL SELECT 'RT-WAYFINDING-SIGN', 'WAY-LAYOUT', '排版', 2, 'DESIGNER', 0, 1, 0, 1.50, '按导视信息完成图文排版。'
    UNION ALL SELECT 'RT-WAYFINDING-SIGN', 'WAY-CUT', '切割', 3, 'WORKER', 1, 0, 1, 3.00, '按尺寸完成板材切割。'
    UNION ALL SELECT 'RT-WAYFINDING-SIGN', 'WAY-PRINT', '打印', 4, 'WORKER', 1, 0, 1, 3.00, '完成图文打印或贴膜。'
    UNION ALL SELECT 'RT-WAYFINDING-SIGN', 'WAY-ASSEMBLY', '组装', 5, 'WORKER', 1, 1, 1, 3.00, '组装面板、支架和配件。'
    UNION ALL SELECT 'RT-WAYFINDING-SIGN', 'WAY-QC', '质检', 6, 'QC', 1, 1, 1, 1.00, '检查内容、方向、尺寸和表面质量。'
    UNION ALL SELECT 'RT-WAYFINDING-SIGN', 'WAY-PACK', '包装', 7, 'WAREHOUSE', 1, 0, 1, 1.00, '按安装区域分类包装。'

    UNION ALL SELECT 'RT-GENERAL-SIGN', 'GENERAL-CONFIRM', '需求确认', 1, 'PRODUCTION_MANAGER', 0, 1, 0, 1.00, '确认产品类型、数量、点位和交付要求。'
    UNION ALL SELECT 'RT-GENERAL-SIGN', 'GENERAL-DESIGN', '设计深化', 2, 'DESIGNER', 0, 1, 0, 2.00, '完成通用标识设计深化。'
    UNION ALL SELECT 'RT-GENERAL-SIGN', 'GENERAL-GRAPHIC', '图文制作', 3, 'WORKER', 1, 0, 1, 3.00, '制作图文内容或表面画面。'
    UNION ALL SELECT 'RT-GENERAL-SIGN', 'GENERAL-PROCESS', '生产加工', 4, 'WORKER', 1, 1, 1, 4.00, '完成主体加工。'
    UNION ALL SELECT 'RT-GENERAL-SIGN', 'GENERAL-ASSEMBLY', '组装', 5, 'WORKER', 1, 1, 1, 3.00, '完成零部件组装。'
    UNION ALL SELECT 'RT-GENERAL-SIGN', 'GENERAL-QC', '质检', 6, 'QC', 1, 1, 1, 1.00, '检查成品质量和交付状态。'
    UNION ALL SELECT 'RT-GENERAL-SIGN', 'GENERAL-PACK', '包装', 7, 'WAREHOUSE', 1, 0, 1, 1.00, '完成通用包装。'
) step ON step.route_code = route.route_code
WHERE route.tenant_id = 1
  AND route.deleted = 0
  AND route.delete_marker = 0;

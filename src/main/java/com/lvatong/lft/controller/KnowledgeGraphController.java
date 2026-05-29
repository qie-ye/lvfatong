package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.knowledgegraph.GraphQueryService;
import com.lvatong.lft.knowledgegraph.GraphSyncService;
import com.lvatong.lft.knowledgegraph.KnowledgeGraphService;
import com.lvatong.lft.model.graph.LegalEntity;
import com.lvatong.lft.model.graph.LegalRelation;
import com.lvatong.lft.model.graph.GraphQueryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge-graph")
@RequiredArgsConstructor
@Tag(name = "知识图谱", description = "法律实体关系图谱查询和管理")
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;
    private final GraphQueryService graphQueryService;
    private final GraphSyncService graphSyncService;

    @GetMapping("/entities")
    @Operation(summary = "实体列表")
    public ApiResult<List<LegalEntity>> getEntities(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) LegalEntity.EntityType type,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResult.success(knowledgeGraphService.findEntities(name, type, limit));
    }

    @GetMapping("/entities/{id}")
    @Operation(summary = "实体详情")
    public ApiResult<LegalEntity> getEntity(@PathVariable String id) {
        LegalEntity entity = graphQueryService.getEntityById(id);
        if (entity == null) {
            return ApiResult.error("实体不存在");
        }
        return ApiResult.success(entity);
    }

    @GetMapping("/entities/{id}/relations")
    @Operation(summary = "实体关系")
    public ApiResult<List<LegalRelation>> getEntityRelations(@PathVariable String id) {
        return ApiResult.success(graphQueryService.getEntityRelations(id));
    }

    @GetMapping("/search")
    @Operation(summary = "图谱搜索")
    public ApiResult<GraphQueryResult> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResult.success(knowledgeGraphService.search(query, limit));
    }

    @PostMapping("/query")
    @Operation(summary = "图谱问答")
    public ApiResult<GraphQueryResult> query(@RequestBody String question) {
        return ApiResult.success(graphQueryService.queryGraph(question));
    }

    @GetMapping("/visualize/{id}")
    @Operation(summary = "可视化数据")
    public ApiResult<Map<String, Object>> getVisualizationData(@PathVariable String id) {
        return ApiResult.success(graphQueryService.getVisualizationData(id));
    }

    @PostMapping("/sync")
    @Operation(summary = "同步知识文档到图谱")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<String> syncDocuments() {
        graphSyncService.syncAllDocuments();
        return ApiResult.success("知识文档同步已启动");
    }

    @PostMapping("/sync/document/{id}")
    @Operation(summary = "同步单个文档到图谱")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<String> syncDocument(@PathVariable Long id) {
        // 这里需要获取文档并同步
        return ApiResult.success("文档同步已启动");
    }

    @PostMapping("/rebuild")
    @Operation(summary = "重建知识图谱")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<String> rebuildGraph() {
        graphSyncService.rebuildGraph();
        return ApiResult.success("知识图谱重建已启动");
    }
}
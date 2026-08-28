package com.algoviz.controller;

import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.Changelog;
import com.algoviz.mapper.ChangelogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 更新日志
 * 管理后台：/api/changelog  CRUD + 分页 + 分类
 * 前台公开：/api/public/changelog?type=&orderBy=release_date|version&orderDir=DESC|ASC
 */
@RestController
@RequestMapping("/api")
@Tag(name = "更新日志", description = "Changelog Admin & Public API")
public class ChangelogController {

    @Autowired
    private ChangelogMapper changelogMapper;

    private static final List<String> VALID_TYPES = List.of("all", "new", "optimize", "fix", "urgent");
    private static final List<String> VALID_TYPE_VALUES = List.of("new", "optimize", "fix", "urgent");

    // ======================== 管理后台：分页 + 分类查询 ========================

    @GetMapping("/changelog")
    @Operation(summary = "分页查询更新日志", description = "按类型、状态、版本分页查询更新日志列表与总数")
    public ApiResponse<Map<String, Object>> getPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String version) {
        if (type != null && !VALID_TYPES.contains(type)) type = "all";
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 200) pageSize = 200;
        int offset = (Math.max(page, 1) - 1) * pageSize;
        List<Changelog> list = changelogMapper.findPage(offset, pageSize, type, status, version);
        long total = changelogMapper.countPage(type, status, version);
        return ApiResponse.success(Map.of("list", list, "total", total));
    }

    @GetMapping("/changelog/{id}")
    @Operation(summary = "查询更新日志详情", description = "根据ID查询更新日志详情，不存在时返回错误")
    public ApiResponse<Changelog> getDetail(@PathVariable Long id) {
        Changelog c = changelogMapper.findById(id);
        if (c == null) return ApiResponse.error(40404, "记录不存在");
        return ApiResponse.success(c);
    }

    @PostMapping("/changelog")
    @Operation(summary = "新增更新日志", description = "新增一条更新日志，包含基础校验与默认值填充")
    public ApiResponse<Changelog> create(@RequestBody Changelog dto) {
        // 基础校验与默认值
        if (dto.getVersion() == null || dto.getVersion().isBlank()) {
            return ApiResponse.error(40001, "版本号不能为空");
        }
        if (dto.getType() == null || !VALID_TYPE_VALUES.contains(dto.getType())) {
            dto.setType("new");
        }
        if (dto.getReleaseDate() == null) dto.setReleaseDate(LocalDate.now());
        if (dto.getSummary() == null) dto.setSummary("");
        if (dto.getDetails() == null || dto.getDetails().isBlank()) {
            return ApiResponse.error(40002, "更新说明（Markdown）不能为空");
        }
        if (dto.getModules() == null) dto.setModules("[]");
        if (dto.getKnownIssues() == null) dto.setKnownIssues("");
        if (dto.getIssuesTitle() == null || dto.getIssuesTitle().isBlank()) {
            dto.setIssuesTitle("已知问题");
        }
        if (dto.getStatus() == null) dto.setStatus(1);
        changelogMapper.insert(dto);
        return ApiResponse.success(dto);
    }

    @PutMapping("/changelog/{id}")
    @Operation(summary = "更新更新日志", description = "根据ID更新更新日志，仅覆盖传入的非空字段")
    public ApiResponse<Changelog> update(@PathVariable Long id, @RequestBody Changelog dto) {
        Changelog existed = changelogMapper.findById(id);
        if (existed == null) return ApiResponse.error(40404, "记录不存在");
        if (dto.getVersion() != null && !dto.getVersion().isBlank()) existed.setVersion(dto.getVersion());
        if (dto.getType() != null && VALID_TYPE_VALUES.contains(dto.getType())) existed.setType(dto.getType());
        if (dto.getSummary() != null) existed.setSummary(dto.getSummary());
        if (dto.getReleaseDate() != null) existed.setReleaseDate(dto.getReleaseDate());
        if (dto.getModules() != null) existed.setModules(dto.getModules());
        if (dto.getDetails() != null && !dto.getDetails().isBlank()) existed.setDetails(dto.getDetails());
        if (dto.getKnownIssues() != null) existed.setKnownIssues(dto.getKnownIssues());
        if (dto.getIssuesTitle() != null && !dto.getIssuesTitle().isBlank()) existed.setIssuesTitle(dto.getIssuesTitle());
        if (dto.getStatus() != null) existed.setStatus(dto.getStatus());
        changelogMapper.update(existed);
        return ApiResponse.success(existed);
    }

    @DeleteMapping("/changelog/{id}")
    @Operation(summary = "删除更新日志", description = "根据ID删除更新日志，不存在时返回错误")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Changelog existed = changelogMapper.findById(id);
        if (existed == null) return ApiResponse.error(40404, "记录不存在");
        changelogMapper.deleteById(id);
        return ApiResponse.success(null);
    }

    // ======================== 前台公开接口：只返回已发布记录 ========================

    @GetMapping("/public/changelog")
    @Operation(summary = "前台查询已发布更新日志", description = "前台公开接口，按类型、排序字段与方向查询已发布的更新日志")
    public ApiResponse<List<Changelog>> getPublic(
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "release_date") String orderBy,
            @RequestParam(defaultValue = "DESC") String orderDir) {
        if (type != null && !VALID_TYPES.contains(type)) type = "all";
        if (!("version".equals(orderBy) || "release_date".equals(orderBy))) orderBy = "release_date";
        if (!("ASC".equalsIgnoreCase(orderDir) || "DESC".equalsIgnoreCase(orderDir))) orderDir = "DESC";
        List<Changelog> list = changelogMapper.findPublicList(type, orderBy, orderDir.toUpperCase());
        return ApiResponse.success(list);
    }
}

package com.algoviz.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用 id -> cnt 查询结果映射（用于批量 count 查询）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdCount {
    /** 目标 ID（如 root_id、solution_id 等） */
    private Long id;
    /** 统计数量 */
    private Integer cnt;
}

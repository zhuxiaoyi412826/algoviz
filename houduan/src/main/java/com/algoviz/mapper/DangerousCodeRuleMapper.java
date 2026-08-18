package com.algoviz.mapper;

import com.algoviz.entity.DangerousCodeRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DangerousCodeRuleMapper {

    List<DangerousCodeRule> selectByPage(@Param("keyword") String keyword,
                                         @Param("language") String language,
                                         @Param("riskLevel") String riskLevel,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    int countByPage(@Param("keyword") String keyword,
                    @Param("language") String language,
                    @Param("riskLevel") String riskLevel);

    /** 全部启用规则（构建正则缓存用） */
    List<DangerousCodeRule> selectAllEnabled();

    DangerousCodeRule selectById(@Param("id") Long id);

    DangerousCodeRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    int insert(DangerousCodeRule r);

    int updateById(DangerousCodeRule r);

    int deleteById(@Param("id") Long id);
}

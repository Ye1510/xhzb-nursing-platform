package com.xhzb.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.FamilyMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 老人家属Mapper接口
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@Mapper
public interface FamilyMemberMapper extends BaseMapper<FamilyMember>
{
    /**
     * 根据openId查询家属信息
     *
     * @param openId 微信OpenID
     * @return 家属信息
     */
    FamilyMember selectFamilyMemberByOpenId(String openId);
}

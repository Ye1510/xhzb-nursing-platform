package com.xhzb.nursing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.FamilyMember;
import com.xhzb.nursing.domain.dto.WxLoginDto;
import com.xhzb.nursing.domain.vo.MemberElderVo;
import com.xhzb.nursing.domain.vo.WxLoginVo;

import java.util.List;

/**
 * 老人家属Service接口
 *
 * @author ruoyi
 * @date 2026-06-14
 */
public interface IFamilyMemberService extends IService<FamilyMember>
{
    /**
     * 微信小程序登录
     *
     * @param dto 登录请求参数
     * @return 登录结果（token和昵称）
     */
    WxLoginVo login(WxLoginDto dto);

    /**
     * 绑定老人（根据身份证号查找老人并建立关联）
     *
     * @param familyMemberId 家属ID
     * @param name           老人姓名
     * @param idCard         老人身份证号
     * @param remark         备注
     */
    void bindElder(Long familyMemberId, String name, String idCard, String remark);

    /**
     * 查询家属已绑定的老人列表
     *
     * @param familyMemberId 家属ID
     * @return 老人列表
     */
    List<MemberElderVo> listBoundElders(Long familyMemberId);

    /**
     * 解绑老人（删除关联记录）
     *
     * @param id   关联记录ID
     * @param familyMemberId 当前家属ID（安全校验）
     */
    void unbindElder(Long id, Long familyMemberId);
}

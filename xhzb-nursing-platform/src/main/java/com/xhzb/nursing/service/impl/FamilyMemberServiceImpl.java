package com.xhzb.nursing.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.constant.Constants;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.utils.uuid.IdUtils;
import com.xhzb.framework.web.service.TokenService;
import com.xhzb.nursing.domain.Elder;
import com.xhzb.nursing.domain.FamilyMember;
import com.xhzb.nursing.domain.FamilyMemberElder;
import com.xhzb.nursing.domain.dto.WxLoginDto;
import com.xhzb.nursing.domain.vo.MemberElderVo;
import com.xhzb.nursing.domain.vo.WxLoginVo;
import com.xhzb.nursing.mapper.ElderMapper;
import com.xhzb.nursing.mapper.FamilyMemberElderMapper;
import com.xhzb.nursing.mapper.FamilyMemberMapper;
import com.xhzb.nursing.service.IFamilyMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 老人家属Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class FamilyMemberServiceImpl extends ServiceImpl<FamilyMemberMapper, FamilyMember> implements IFamilyMemberService
{
    /** 微信小程序AppID（来自配置文件） */
    @org.springframework.beans.factory.annotation.Value("${wx.miniapp.appid}")
    private String wxAppId;

    /** 微信小程序Secret（来自配置文件） */
    @org.springframework.beans.factory.annotation.Value("${wx.miniapp.secret}")
    private String wxSecret;

    /** 微信code2Session接口地址 */
    private static final String WX_CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    /** 微信获取access_token接口地址 */
    private static final String WX_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";

    /** 微信获取手机号接口地址 */
    private static final String WX_PHONE_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

    /** 随机昵称候选字 */
    private static final String[] NICKNAME_CHARS = {
            "快", "乐", "幸", "福", "美", "好", "健", "康", "安", "宁",
            "花", "开", "富", "贵", "吉", "祥", "如", "意", "喜", "乐",
            "阳", "光", "明", "月", "星", "辰", "春", "暖", "秋", "实",
            "心", "想", "事", "成", "万", "事", "顺", "遂", "平", "安"
    };

    @Autowired
    private FamilyMemberMapper familyMemberMapper;

    @Autowired
    private FamilyMemberElderMapper familyMemberElderMapper;

    @Autowired
    private ElderMapper elderMapper;

    @Autowired
    private TokenService tokenService;

    /**
     * 微信小程序登录
     *
     * @param dto 登录请求参数
     * @return 登录结果（token和昵称）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WxLoginVo login(WxLoginDto dto)
    {
        // 1. 根据前端传入的code结合appid和secret，调用微信code2Session接口，返回openid
        String openId = getOpenIdFromWeChat(dto.getCode());

        // 2. 根据openid查询家属信息
        FamilyMember familyMember = familyMemberMapper.selectFamilyMemberByOpenId(openId);

        // 3. 使用Hutool的HttpUtil调用微信getPhoneNumber接口，返回手机号
        String phone = getPhoneNumberFromWeChat(dto.getPhoneCode());

        // 4. 如果家属为空，直接新增家属，赋值openid、手机号、微信昵称（要求随机起四个字的名字+手机号后四位）
        String finalNickName;
        if (familyMember == null)
        {
            familyMember = new FamilyMember();
            familyMember.setOpenId(openId);
            familyMember.setPhone(phone);
            // 优先级：前端传入nickName > 随机四个字+手机号后四位
            if (dto.getNickName() != null && !dto.getNickName().isEmpty())
            {
                finalNickName = dto.getNickName();
            }
            else
            {
                finalNickName = generateNickName(phone);
            }
            familyMember.setName(finalNickName);
            save(familyMember);
        }
        else
        {
            // 5. 如果家属不为空，判断手机号是否和之前一致，如果不一致更新手机号
            if (phone != null && !phone.equals(familyMember.getPhone()))
            {
                familyMember.setPhone(phone);
                updateById(familyMember);
            }
            // 使用已有的昵称
            finalNickName = familyMember.getName();
        }

        // 6. 根据用户id和昵称，使用TokenService生成token字符串
        String token = createToken(familyMember.getId(), finalNickName);

        return new WxLoginVo(token, finalNickName);
    }

    /**
     * 调用微信code2Session接口获取openid
     *
     * @param code 临时登录凭证code
     * @return openid
     */
    private String getOpenIdFromWeChat(String code)
    {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("appid", wxAppId);
        paramMap.put("secret", wxSecret);
        paramMap.put("js_code", code);
        paramMap.put("grant_type", "authorization_code");

        String result = HttpUtil.get(WX_CODE2SESSION_URL, paramMap);
        JSONObject jsonResult = JSONUtil.parseObj(result);

        // 检查微信返回的错误码
        if (jsonResult.containsKey("errcode") && jsonResult.getInt("errcode") != 0)
        {
            throw new ServiceException("获取微信openid失败: " + jsonResult.getStr("errmsg"));
        }

        String openId = jsonResult.getStr("openid");
        if (openId == null || openId.isEmpty())
        {
            throw new ServiceException("获取微信openid失败，返回结果为空");
        }

        return openId;
    }

    /**
     * 调用微信getPhoneNumber接口获取手机号
     *
     * @param phoneCode 获取手机号的临时code
     * @return 手机号
     */
    private String getPhoneNumberFromWeChat(String phoneCode)
    {
        // 2.1 获取微信access_token
        String accessToken = getAccessToken();

        // 2.2 调用getPhoneNumber接口
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("code", phoneCode);
        String body = JSONUtil.toJsonStr(bodyMap);

        String result = HttpUtil.post(WX_PHONE_URL + "?access_token=" + accessToken, body);
        JSONObject jsonResult = JSONUtil.parseObj(result);

        // 检查微信返回的错误码
        if (jsonResult.getInt("errcode") != 0)
        {
            throw new ServiceException("获取微信手机号失败: " + jsonResult.getStr("errmsg"));
        }

        JSONObject phoneInfo = jsonResult.getJSONObject("phone_info");
        if (phoneInfo == null)
        {
            throw new ServiceException("获取微信手机号失败，phone_info为空");
        }

        String phoneNumber = phoneInfo.getStr("phoneNumber");
        if (phoneNumber == null || phoneNumber.isEmpty())
        {
            throw new ServiceException("获取微信手机号失败，phoneNumber为空");
        }

        return phoneNumber;
    }

    /**
     * 获取微信access_token
     *
     * @return access_token
     */
    private String getAccessToken()
    {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("grant_type", "client_credential");
        paramMap.put("appid", wxAppId);
        paramMap.put("secret", wxSecret);

        String result = HttpUtil.get(WX_ACCESS_TOKEN_URL, paramMap);
        JSONObject jsonResult = JSONUtil.parseObj(result);

        // 检查微信返回的错误码
        if (jsonResult.containsKey("errcode") && jsonResult.getInt("errcode") != 0)
        {
            throw new ServiceException("获取微信access_token失败: " + jsonResult.getStr("errmsg"));
        }

        String accessToken = jsonResult.getStr("access_token");
        if (accessToken == null || accessToken.isEmpty())
        {
            throw new ServiceException("获取微信access_token失败，返回结果为空");
        }

        return accessToken;
    }

    /**
     * 生成随机昵称：随机四个字的名字 + 手机号后四位
     *
     * @param phone 手机号
     * @return 昵称
     */
    private String generateNickName(String phone)
    {
        StringBuilder sb = new StringBuilder();
        // 随机选取四个字
        for (int i = 0; i < 4; i++)
        {
            sb.append(RandomUtil.randomEle(NICKNAME_CHARS));
        }
        // 拼接手机号后四位
        if (phone != null && phone.length() >= 4)
        {
            sb.append(phone.substring(phone.length() - 4));
        }
        return sb.toString();
    }

    /**
     * 根据用户ID和昵称生成JWT Token
     *
     * @param userId   用户ID
     * @param nickName 昵称
     * @return token字符串
     */
    private String createToken(Long userId, String nickName)
    {
        String uuid = IdUtils.fastUUID();
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, uuid);
        claims.put(Constants.JWT_USERID, userId);
        claims.put(Constants.JWT_USERNAME, nickName);
        return tokenService.createToken(claims);
    }

    /**
     * 绑定老人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindElder(Long familyMemberId, String name, String idCard, String remark)
    {
        // 根据身份证号查找老人
        LambdaQueryWrapper<Elder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Elder::getIdCardNo, idCard);
        Elder elder = elderMapper.selectOne(wrapper);
        if (elder == null)
        {
            throw new ServiceException("未找到该身份证号对应的老人，请确认后重试");
        }
        // 校验姓名是否一致
        if (name != null && !name.isEmpty() && !name.equals(elder.getName()))
        {
            throw new ServiceException("老人姓名与身份证号不匹配");
        }
        // 检查是否已绑定
        LambdaQueryWrapper<FamilyMemberElder> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(FamilyMemberElder::getFamilyMemberId, familyMemberId)
                .eq(FamilyMemberElder::getElderId, elder.getId());
        Long count = familyMemberElderMapper.selectCount(checkWrapper);
        if (count > 0)
        {
            throw new ServiceException("您已绑定该老人，请勿重复绑定");
        }
        // 建立关联
        FamilyMemberElder fme = new FamilyMemberElder();
        fme.setFamilyMemberId(familyMemberId);
        fme.setElderId(elder.getId());
        fme.setRemark(remark);
        familyMemberElderMapper.insert(fme);
    }

    /**
     * 查询家属已绑定的老人列表
     */
    @Override
    public List<MemberElderVo> listBoundElders(Long familyMemberId)
    {
        return familyMemberElderMapper.selectMemberElderList(familyMemberId);
    }

    /**
     * 解绑老人
     */
    @Override
    public void unbindElder(Long id, Long familyMemberId)
    {
        FamilyMemberElder fme = familyMemberElderMapper.selectById(id);
        if (fme == null)
        {
            throw new ServiceException("绑定记录不存在");
        }
        // 安全校验：只能解绑自己的关联
        if (!fme.getFamilyMemberId().equals(familyMemberId))
        {
            throw new ServiceException("无权解绑他人的老人");
        }
        familyMemberElderMapper.deleteById(id);
    }
}

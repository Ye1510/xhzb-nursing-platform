package com.xhzb.nursing.controller;

import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.domain.vo.MessageVO;
import com.xhzb.nursing.service.ChatHistoryService;
import com.xhzb.nursing.service.impl.RedisChatMemoryService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @GetMapping
    public AjaxResult getChatIds(){

        //获取当前登录人的id
        Long userId = SecurityUtils.getUserId();
        List<String> ids = chatHistoryService.getChatIds(userId);
        return AjaxResult.success(ids);
    }

    @Autowired
    private RedisChatMemoryService redisChatMemoryService;

    @GetMapping("/{chatId}")
    public AjaxResult getChatHistory(@PathVariable String chatId){

        List<Message> messages = redisChatMemoryService.get(chatId);
        if(null != messages && !messages.isEmpty()){
            List<MessageVO> list = messages.stream().map(MessageVO::new).toList();
            return AjaxResult.success(list);
        }
        return AjaxResult.success();
    }
    @DeleteMapping("/{chatId}")
    public AjaxResult delHistory(@PathVariable String chatId){

        chatHistoryService.delChatHistory(chatId);
        redisChatMemoryService.clear(chatId);
        return AjaxResult.success();
    }
}

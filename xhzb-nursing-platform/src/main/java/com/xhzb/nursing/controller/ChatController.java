package com.xhzb.nursing.controller;

import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.service.ChatHistoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class ChatController {

    @Autowired
    private ChatClient openAiChatClient;
    @Autowired
    private ChatHistoryService chatHistoryService;

    @RequestMapping(value = "/chat",produces = "text/html;charset=utf-8")
    public Flux<String> chat(String prompt,String chatId){

        //存储聊天历史
        chatHistoryService.save(SecurityUtils.getUserId()+"",chatId);

        return openAiChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,chatId))
                .stream()
                .content();

    }
}

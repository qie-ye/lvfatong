package com.lvatong.lft.multimodal;

import com.lvatong.lft.ai.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultimodalChatService {

    private final ChatService chatService;
    private final OcrService ocrService;
    private final AsrService asrService;
    private final TtsService ttsService;

    /**
     * 多模态聊天
     */
    public Map<String, Object> multimodalChat(String text, MultipartFile image, MultipartFile audio) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 处理图片输入
            if (image != null && !image.isEmpty()) {
                Map<String, Object> ocrResult = ocrService.recognizeText(image);
                String imageText = (String) ocrResult.get("text");
                result.put("imageText", imageText);
                
                // 将图片识别结果添加到聊天上下文
                if (text == null || text.isEmpty()) {
                    text = "请分析这张图片的内容：" + imageText;
                } else {
                    text = text + "\n图片内容：" + imageText;
                }
            }

            // 处理音频输入
            if (audio != null && !audio.isEmpty()) {
                Map<String, Object> asrResult = asrService.recognizeSpeech(audio);
                String audioText = (String) asrResult.get("text");
                result.put("audioText", audioText);
                
                // 将语音识别结果添加到聊天上下文
                if (text == null || text.isEmpty()) {
                    text = audioText;
                } else {
                    text = text + "\n语音内容：" + audioText;
                }
            }

            // 调用聊天服务
            if (text != null && !text.isEmpty()) {
                String response = chatService.simpleChat(text, "glm-4-flash", 0.7, 1024);
                result.put("response", response);
                
                // 生成语音回复
                Map<String, Object> ttsResult = ttsService.textToSpeech(response, null);
                result.put("audioResponse", ttsResult.get("audioData"));
            }

            result.put("success", true);
        } catch (Exception e) {
            log.error("多模态聊天失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 图片问答
     */
    public Map<String, Object> imageQuestion(MultipartFile image, String question) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 识别图片内容
            Map<String, Object> ocrResult = ocrService.recognizeText(image);
            String imageText = (String) ocrResult.get("text");
            
            // 构建问题
            String fullQuestion = "图片内容：" + imageText + "\n问题：" + question;
            
            // 调用聊天服务
            String response = chatService.simpleChat(fullQuestion, "glm-4-flash", 0.7, 1024);
            
            result.put("imageText", imageText);
            result.put("question", question);
            result.put("response", response);
            result.put("success", true);
        } catch (Exception e) {
            log.error("图片问答失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 语音问答
     */
    public Map<String, Object> voiceQuestion(MultipartFile audio) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 识别语音内容
            Map<String, Object> asrResult = asrService.recognizeSpeech(audio);
            String question = (String) asrResult.get("text");
            
            // 调用聊天服务
            String response = chatService.simpleChat(question, "glm-4-flash", 0.7, 1024);
            
            // 生成语音回复
            Map<String, Object> ttsResult = ttsService.textToSpeech(response, null);
            
            result.put("question", question);
            result.put("response", response);
            result.put("audioResponse", ttsResult.get("audioData"));
            result.put("success", true);
        } catch (Exception e) {
            log.error("语音问答失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }
}
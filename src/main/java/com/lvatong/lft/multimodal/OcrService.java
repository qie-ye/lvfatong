package com.lvatong.lft.multimodal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class OcrService {

    @Value("${lvatong.multimodal.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Value("${lvatong.multimodal.ocr.provider:baidu}")
    private String ocrProvider;

    @Value("${lvatong.multimodal.ocr.api-key:}")
    private String apiKey;

    /**
     * 识别图片中的文字
     */
    public Map<String, Object> recognizeText(MultipartFile file) throws IOException {
        if (!ocrEnabled) {
            throw new RuntimeException("OCR功能未启用");
        }

        log.info("开始OCR识别: fileName={}, size={}", file.getOriginalFilename(), file.getSize());

        // 读取图片
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new RuntimeException("无法读取图片文件");
        }

        // 根据提供商调用不同的OCR服务
        Map<String, Object> result;
        switch (ocrProvider.toLowerCase()) {
            case "baidu":
                result = callBaiduOcr(file);
                break;
            case "tesseract":
                result = callTesseractOcr(image);
                break;
            case "paddleocr":
                result = callPaddleOcr(image);
                break;
            default:
                throw new RuntimeException("不支持的OCR提供商: " + ocrProvider);
        }

        log.info("OCR识别完成: 识别到{}个字符", result.get("textLength"));
        return result;
    }

    /**
     * 识别合同扫描件
     */
    public Map<String, Object> recognizeContract(MultipartFile file) throws IOException {
        Map<String, Object> result = recognizeText(file);
        
        // 合同特定处理
        String text = (String) result.get("text");
        result.put("isContract", detectContract(text));
        result.put("contractType", detectContractType(text));
        result.put("keyTerms", extractKeyTerms(text));
        
        return result;
    }

    /**
     * 调用百度OCR API
     */
    private Map<String, Object> callBaiduOcr(MultipartFile file) throws IOException {
        // 这里应该调用百度OCR API
        // 简化实现，返回模拟结果
        Map<String, Object> result = new HashMap<>();
        result.put("text", "模拟OCR识别结果");
        result.put("textLength", 8);
        result.put("confidence", 0.95);
        result.put("provider", "baidu");
        return result;
    }

    /**
     * 调用Tesseract OCR
     */
    private Map<String, Object> callTesseractOcr(BufferedImage image) {
        // 这里应该调用Tesseract OCR
        // 简化实现，返回模拟结果
        Map<String, Object> result = new HashMap<>();
        result.put("text", "模拟OCR识别结果");
        result.put("textLength", 8);
        result.put("confidence", 0.90);
        result.put("provider", "tesseract");
        return result;
    }

    /**
     * 调用PaddleOCR
     */
    private Map<String, Object> callPaddleOcr(BufferedImage image) {
        // 这里应该调用PaddleOCR
        // 简化实现，返回模拟结果
        Map<String, Object> result = new HashMap<>();
        result.put("text", "模拟OCR识别结果");
        result.put("textLength", 8);
        result.put("confidence", 0.92);
        result.put("provider", "paddleocr");
        return result;
    }

    /**
     * 检测是否为合同
     */
    private boolean detectContract(String text) {
        if (text == null) return false;
        String[] contractKeywords = {"合同", "协议", "甲方", "乙方", "条款", "签署", "生效"};
        for (String keyword : contractKeywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测合同类型
     */
    private String detectContractType(String text) {
        if (text == null) return "未知";
        
        if (text.contains("劳动") || text.contains("雇佣")) {
            return "劳动合同";
        } else if (text.contains("买卖") || text.contains("购销")) {
            return "买卖合同";
        } else if (text.contains("租赁") || text.contains("出租")) {
            return "租赁合同";
        } else if (text.contains("借款") || text.contains("贷款")) {
            return "借款合同";
        } else if (text.contains("担保") || text.contains("保证")) {
            return "担保合同";
        } else {
            return "其他合同";
        }
    }

    /**
     * 提取关键条款
     */
    private Map<String, String> extractKeyTerms(String text) {
        Map<String, String> keyTerms = new HashMap<>();
        
        if (text == null) return keyTerms;
        
        // 提取合同期限
        if (text.contains("期限")) {
            keyTerms.put("期限", "见合同原文");
        }
        
        // 提取金额
        if (text.contains("元") || text.contains("金额")) {
            keyTerms.put("金额", "见合同原文");
        }
        
        // 提取违约责任
        if (text.contains("违约")) {
            keyTerms.put("违约责任", "见合同原文");
        }
        
        return keyTerms;
    }
}
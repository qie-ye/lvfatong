package com.lvatong.lft.contract;

import com.lvatong.lft.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DocumentParser {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final int MAX_PAGES_PER_BATCH = 50;
    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "doc", "docx");

    /**
     * 验证上传文件
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过20MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("无法识别文件名");
        }
        String extension = getExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持PDF和Word文档（.pdf, .doc, .docx）");
        }
    }

    /**
     * 解析文档为纯文本
     */
    public String parse(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) throw new BusinessException("无法识别文件名");
        String extension = getExtension(filename).toLowerCase();

        try {
            return switch (extension) {
                case "pdf" -> parsePdf(file.getInputStream());
                case "docx" -> parseDocx(file.getInputStream());
                case "doc" -> parseDocx(file.getInputStream());
                default -> throw new BusinessException("不支持的文件格式: " + extension);
            };
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Document parsing failed for {}: {}", filename, e.getMessage());
            throw new BusinessException("文档解析失败: " + e.getMessage());
        }
    }

    /**
     * PDF解析（使用临时文件避免OOM，分批读取）
     */
    private String parsePdf(InputStream inputStream) throws Exception {
        StringBuilder text = new StringBuilder();
        // Write to temp file to avoid loading entire PDF into heap
        Path tempFile = Files.createTempFile("contract-", ".pdf");
        try {
            Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            try (RandomAccessReadBufferedFile rar = new RandomAccessReadBufferedFile(tempFile.toFile());
                 PDDocument document = Loader.loadPDF(rar)) {
                int totalPages = document.getNumberOfPages();
                log.info("Parsing PDF with {} pages", totalPages);

                PDFTextStripper stripper = new PDFTextStripper();
                for (int startPage = 1; startPage <= totalPages; startPage += MAX_PAGES_PER_BATCH) {
                    int endPage = Math.min(startPage + MAX_PAGES_PER_BATCH - 1, totalPages);
                    stripper.setStartPage(startPage);
                    stripper.setEndPage(endPage);
                    text.append(stripper.getText(document));
                }
            }
            String result = text.toString().trim();
            if (result.isEmpty()) {
                throw new BusinessException("PDF文档内容为空，可能是扫描件（不支持OCR）");
            }
            return result;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Word文档解析（DOCX格式）
     */
    private String parseDocx(InputStream inputStream) throws Exception {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String paraText = paragraph.getText();
                if (paraText != null && !paraText.isBlank()) {
                    text.append(paraText).append("\n");
                }
            }
        }
        String result = text.toString().trim();
        if (result.isEmpty()) {
            throw new BusinessException("Word文档内容为空");
        }
        return result;
    }

    /**
     * 获取文件扩展名
     */
    public String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex + 1) : "";
    }

    /**
     * 检测文件MIME类型
     */
    public String getFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && ALLOWED_TYPES.contains(contentType)) {
            return contentType;
        }
        String ext = getExtension(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        return switch (ext.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            default -> "application/octet-stream";
        };
    }
}

package com.lvatong.lft.model.dto;

import lombok.Data;

@Data
public class ContractUploadResponse {
    private Long id;
    private String filename;
    private String fileType;
    private Long fileSize;
    private String status;

    public static ContractUploadResponse from(Long id, String filename, String fileType, Long fileSize, String status) {
        ContractUploadResponse resp = new ContractUploadResponse();
        resp.setId(id);
        resp.setFilename(filename);
        resp.setFileType(fileType);
        resp.setFileSize(fileSize);
        resp.setStatus(status);
        return resp;
    }
}

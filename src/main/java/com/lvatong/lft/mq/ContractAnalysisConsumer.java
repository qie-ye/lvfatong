package com.lvatong.lft.mq;

import com.lvatong.lft.contract.ContractService;
import com.lvatong.lft.mq.dto.AnalysisTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractAnalysisConsumer {

    private final ContractService contractService;
    private final ContractAnalysisProducer producer;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CONTRACT_ANALYSIS)
    public void handleAnalysisTask(AnalysisTaskMessage message) {
        log.info("Received contract analysis task: documentId={}, taskType={}", 
                message.getDocumentId(), message.getTaskType());

        try {
            switch (message.getTaskType()) {
                case CONTRACT_ANALYSIS -> {
                    String result = contractService.analyzeContractAsync(message.getDocumentId());
                    producer.sendAnalysisResult(message.getDocumentId(), result);
                    log.info("Contract analysis completed: documentId={}", message.getDocumentId());
                }
                case DOCUMENT_PARSING -> {
                    contractService.parseDocument(message.getDocumentId());
                    log.info("Document parsing completed: documentId={}", message.getDocumentId());
                }
                case RISK_ASSESSMENT -> {
                    contractService.assessRisk(message.getDocumentId());
                    log.info("Risk assessment completed: documentId={}", message.getDocumentId());
                }
                default -> log.warn("Unknown task type: {}", message.getTaskType());
            }
        } catch (Exception e) {
            log.error("Failed to process analysis task: documentId={}, error={}", 
                    message.getDocumentId(), e.getMessage(), e);
            throw e;
        }
    }
}

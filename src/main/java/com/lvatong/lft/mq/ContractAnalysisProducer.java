package com.lvatong.lft.mq;

import com.lvatong.lft.mq.dto.AnalysisTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractAnalysisProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendAnalysisTask(AnalysisTaskMessage message) {
        log.info("Sending contract analysis task: documentId={}, userId={}", 
                message.getDocumentId(), message.getUserId());
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_CONTRACT_ANALYSIS,
                message
        );
        
        log.info("Contract analysis task sent successfully: documentId={}", message.getDocumentId());
    }

    public void sendAnalysisResult(Long documentId, String result) {
        log.info("Sending contract analysis result: documentId={}", documentId);
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_CONTRACT_RESULT,
                new AnalysisResultMessage(documentId, result)
        );
        
        log.info("Contract analysis result sent successfully: documentId={}", documentId);
    }

    public static class AnalysisResultMessage implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private Long documentId;
        private String result;

        public AnalysisResultMessage() {}

        public AnalysisResultMessage(Long documentId, String result) {
            this.documentId = documentId;
            this.result = result;
        }

        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }
}

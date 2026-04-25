package com.lvatong.lft.async;

import com.lvatong.lft.contract.ContractService;
import com.lvatong.lft.service.LegalOpinionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskRouter {

    private final ContractService contractService;
    private final LegalOpinionService legalOpinionService;

    public void route(AsyncTaskMessage message) {
        log.info("[AsyncTaskRouter] routing type={} entityId={}", message.type(), message.entityId());
        switch (message.type()) {
            case CONTRACT -> contractService.executeAnalysis(message.entityId());
            case OPINION  -> legalOpinionService.executeGeneration(message.entityId());
            case DOCUMENT -> log.warn("[AsyncTaskRouter] DOCUMENT task type reserved, entityId={}", message.entityId());
        }
    }
}

package com.bank.bian.limitandexposuremanagement.api;

import com.bank.bian.limitandexposuremanagement.model.ControlRecord;
import com.bank.bian.limitandexposuremanagement.service.ControlRecordStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * BIAN semantic API for the "Limit And Exposure Management" service domain.
 *
 * Endpoints follow the BIAN action-term style:
 *   GET  /v1/service-domain                          → who am I (SD metadata)
 *   POST /v1/exposure-limit-management-plan/initiate                    → Initiate a control record
 *   GET  /v1/exposure-limit-management-plan                             → Retrieve (list)
 *   GET  /v1/exposure-limit-management-plan/{crId}/retrieve             → Retrieve (single)
 *   PUT  /v1/exposure-limit-management-plan/{crId}/update               → Update
 *   PUT  /v1/exposure-limit-management-plan/{crId}/control              → Control (suspend|resume|terminate)
 */
@RestController
@RequestMapping("/v1")
public class ServiceDomainController {

    private final ControlRecordStore store;

    public ServiceDomainController(ControlRecordStore store) {
        this.store = store;
    }

    @GetMapping("/service-domain")
    public Map<String, String> serviceDomain() {
        return Map.of(
                "serviceDomain", "Limit And Exposure Management",
                "businessArea", "Risk and Compliance",
                "businessDomain", "Credit Risk",
                "functionalPattern", "Manage",
                "assetType", "Exposure Limit",
                "controlRecord", "Exposure Limit Management Plan",
                "version", "0.1.0",
                "phase", "1-shallow"
        );
    }

    @PostMapping("/exposure-limit-management-plan/initiate")
    @CircuitBreaker(name = "serviceDomain")
    public ResponseEntity<ControlRecord> initiate(@RequestBody(required = false) Map<String, Object> properties) {
        return ResponseEntity.status(HttpStatus.CREATED).body(store.initiate(properties));
    }

    @GetMapping("/exposure-limit-management-plan")
    public Collection<ControlRecord> list() {
        return store.list();
    }

    @GetMapping("/exposure-limit-management-plan/{crId}/retrieve")
    public ResponseEntity<ControlRecord> retrieve(@PathVariable String crId) {
        return store.retrieve(crId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/exposure-limit-management-plan/{crId}/update")
    public ResponseEntity<ControlRecord> update(@PathVariable String crId,
                                                @RequestBody Map<String, Object> properties) {
        return store.update(crId, properties)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/exposure-limit-management-plan/{crId}/control")
    public ResponseEntity<?> control(@PathVariable String crId,
                                     @RequestBody Map<String, String> body) {
        try {
            return store.control(crId, body.get("action"))
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

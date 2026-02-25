package org.example.contract.controller;

import org.example.contract.dto.SignatureRequest;
import org.example.contract.entity.Contract;
import org.example.contract.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    @Autowired
    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    public ResponseEntity<Contract> createContract(@RequestBody Contract contract) {
        return ResponseEntity.ok(contractService.createContract(contract));
    }

    @GetMapping
    public ResponseEntity<List<Contract>> getAllContracts() {
        return ResponseEntity.ok(contractService.getAllContracts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable Long id) {
        return contractService.getContractById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contract> updateContract(@PathVariable Long id, @RequestBody Contract contract) {
        try {
            return ResponseEntity.ok(contractService.updateContract(id, contract));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Contract>> getContractsByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(contractService.getContractsByClientId(clientId));
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<Contract>> getContractsByFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(contractService.getContractsByFreelancerId(freelancerId));
    }

    /**
     * Upload a signature image for the contract.
     * Accepts a JSON body: { "role": "CLIENT"|"FREELANCER", "signatureData": "data:image/png;base64,..." }
     * Stores the Base64 image data in the DB (MEDIUMTEXT, up to 16 MB).
     * Auto-activates the contract when both parties have signed.
     */
    @PatchMapping("/{id}/sign")
    public ResponseEntity<Contract> signContract(
            @PathVariable Long id,
            @RequestBody SignatureRequest request) {
        try {
            return ResponseEntity.ok(contractService.signContract(id, request.getRole(), request.getSignatureData()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Standalone status update (submit for signature, mark conflict, complete, cancel). */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Contract> updateStatus(
            @PathVariable Long id,
            @RequestParam org.example.contract.entity.ContractStatus status) {
        try {
            return ResponseEntity.ok(contractService.updateStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

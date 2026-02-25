package org.example.contract.controller;

import org.example.contract.entity.Conflict;
import org.example.contract.entity.ConflictStatus;
import org.example.contract.service.ConflictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conflicts")
public class ConflictController {

    private final ConflictService conflictService;

    @Autowired
    public ConflictController(ConflictService conflictService) {
        this.conflictService = conflictService;
    }

    @PostMapping("/contract/{contractId}")
    public ResponseEntity<Conflict> createConflict(@PathVariable Long contractId, @RequestBody Conflict conflict) {
        try {
            return ResponseEntity.ok(conflictService.createConflict(contractId, conflict));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Conflict>> getAllConflicts() {
        return ResponseEntity.ok(conflictService.getAllConflicts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Conflict> getConflictById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(conflictService.getConflictById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Conflict> updateConflictStatus(@PathVariable Long id, @RequestParam ConflictStatus status) {
        try {
            return ResponseEntity.ok(conflictService.updateConflictStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<List<Conflict>> getConflictsByContractId(@PathVariable Long contractId) {
        return ResponseEntity.ok(conflictService.getConflictsByContractId(contractId));
    }
}

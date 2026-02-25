package org.example.contract.service;

import org.example.contract.entity.Conflict;
import org.example.contract.entity.ConflictStatus;
import org.example.contract.entity.Contract;
import org.example.contract.repository.ConflictRepository;
import org.example.contract.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConflictService {

    private final ConflictRepository conflictRepository;
    private final ContractRepository contractRepository;

    @Autowired
    public ConflictService(ConflictRepository conflictRepository, ContractRepository contractRepository) {
        this.conflictRepository = conflictRepository;
        this.contractRepository = contractRepository;
    }

    public Conflict createConflict(Long contractId, Conflict conflict) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id " + contractId));
        conflict.setContract(contract);
        conflict.setStatus(ConflictStatus.OPEN);
        return conflictRepository.save(conflict);
    }

    public List<Conflict> getAllConflicts() {
        return conflictRepository.findAll();
    }

    public Conflict getConflictById(Long id) {
        return conflictRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conflict not found with id " + id));
    }

    public Conflict updateConflictStatus(Long id, ConflictStatus status) {
        Conflict conflict = getConflictById(id);
        conflict.setStatus(status);
        if (status == ConflictStatus.RESOLVED) {
            conflict.setResolvedAt(LocalDateTime.now());
        }
        return conflictRepository.save(conflict);
    }

    public List<Conflict> getConflictsByContractId(Long contractId) {
        return conflictRepository.findByContractId(contractId);
    }
}

package org.example.contract.service;

import org.example.contract.entity.Contract;
import org.example.contract.entity.ContractStatus;
import org.example.contract.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ContractService {

    private final ContractRepository contractRepository;

    @Autowired
    public ContractService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public Contract createContract(Contract contract) {
        contract.setStatus(ContractStatus.DRAFT);
        return contractRepository.save(contract);
    }

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    public Optional<Contract> getContractById(Long id) {
        return contractRepository.findById(id);
    }

    public Contract updateContract(Long id, Contract contractDetails) {
        return contractRepository.findById(id).map(contract -> {
            contract.setTitle(contractDetails.getTitle());
            contract.setTerms(contractDetails.getTerms());
            contract.setAmount(contractDetails.getAmount());
            contract.setStartDate(contractDetails.getStartDate());
            contract.setEndDate(contractDetails.getEndDate());
            if (contractDetails.getStatus() != null) {
                contract.setStatus(contractDetails.getStatus());
            }
            if (contractDetails.getStatus() == ContractStatus.ACTIVE && contract.getSignedAt() == null) {
                contract.setSignedAt(LocalDateTime.now());
            }
            return contractRepository.save(contract);
        }).orElseThrow(() -> new RuntimeException("Contract not found with id " + id));
    }

    public void deleteContract(Long id) {
        contractRepository.deleteById(id);
    }

    public List<Contract> getContractsByClientId(Long clientId) {
        return contractRepository.findByClientId(clientId);
    }

    public List<Contract> getContractsByFreelancerId(Long freelancerId) {
        return contractRepository.findByFreelancerId(freelancerId);
    }

    public Contract signContract(Long id, String role, String signatureUrl) {
        return contractRepository.findById(id).map(contract -> {
            if ("CLIENT".equalsIgnoreCase(role)) {
                contract.setClientSignatureUrl(signatureUrl);
            } else if ("FREELANCER".equalsIgnoreCase(role)) {
                contract.setFreelancerSignatureUrl(signatureUrl);
            }
            if (contract.getClientSignatureUrl() != null && !contract.getClientSignatureUrl().isBlank()
                    && contract.getFreelancerSignatureUrl() != null && !contract.getFreelancerSignatureUrl().isBlank()) {
                contract.setStatus(ContractStatus.ACTIVE);
                contract.setSignedAt(LocalDateTime.now());
            }
            return contractRepository.save(contract);
        }).orElseThrow(() -> new RuntimeException("Contract not found with id " + id));
    }

    public Contract updateStatus(Long id, ContractStatus status) {
        return contractRepository.findById(id).map(contract -> {
            contract.setStatus(status);
            return contractRepository.save(contract);
        }).orElseThrow(() -> new RuntimeException("Contract not found with id " + id));
    }
}

package org.example.contract.repository;

import org.example.contract.entity.Conflict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConflictRepository extends JpaRepository<Conflict, Long> {
    List<Conflict> findByContractId(Long contractId);
    List<Conflict> findByRaisedById(Long raisedById);
}

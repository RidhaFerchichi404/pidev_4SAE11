package org.example.contract.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.contract.dto.ContractStatsDto;
import org.example.contract.dto.PdfExportRequest;
import org.example.contract.entity.Contract;
import org.example.contract.entity.ContractStatus;
import org.example.contract.service.ContractPdfService;
import org.example.contract.service.ContractService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ContractController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@TestPropertySource(properties = "welcome.message=HelloContract")
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContractService contractService;

    @MockBean
    private ContractPdfService contractPdfService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void welcomeReturnsConfiguredMessage() throws Exception {
        mockMvc.perform(get("/api/contracts/welcome"))
                .andExpect(status().isOk())
                .andExpect(content().string("HelloContract"));
    }

    @Test
    void getAllContractsReturnsJsonArray() throws Exception {
        when(contractService.getAllContracts()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/contracts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getContractByIdReturnsBodyWhenFound() throws Exception {
        Contract c = new Contract();
        c.setId(1L);
        c.setTitle("T");
        when(contractService.getContractById(1L)).thenReturn(Optional.of(c));
        mockMvc.perform(get("/api/contracts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getContractByIdReturns404WhenMissing() throws Exception {
        when(contractService.getContractById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/contracts/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createContractDelegatesToService() throws Exception {
        Contract in = new Contract();
        in.setTitle("New");
        Contract out = new Contract();
        out.setId(2L);
        out.setTitle("New");
        when(contractService.createContract(any(Contract.class))).thenReturn(out);
        mockMvc.perform(post("/api/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void getByClientReturnsList() throws Exception {
        when(contractService.getContractsByClientId(3L)).thenReturn(List.of());
        mockMvc.perform(get("/api/contracts/client/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deleteContractReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/contracts/5"))
                .andExpect(status().isOk());
    }

    @Test
    void getStatsReturnsDto() throws Exception {
        ContractStatsDto dto = new ContractStatsDto();
        dto.setTotal(7);
        when(contractService.getContractStats()).thenReturn(dto);
        mockMvc.perform(get("/api/contracts/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(7));
    }

    @Test
    void updateContractReturns404OnRuntimeException() throws Exception {
        Contract body = new Contract();
        body.setTitle("x");
        when(contractService.updateContract(anyLong(), any(Contract.class))).thenThrow(new RuntimeException("missing"));
        mockMvc.perform(put("/api/contracts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void signContractReturnsUpdatedContract() throws Exception {
        Contract signed = new Contract();
        signed.setId(3L);
        when(contractService.signContract(eq(3L), any(String.class), any(String.class))).thenReturn(signed);
        String json = "{\"role\":\"CLIENT\",\"signatureData\":\"data:image/png;base64,AAA\"}";
        mockMvc.perform(patch("/api/contracts/3/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void exportPdfReturnsBytesWhenContractExists() throws Exception {
        Contract c = new Contract();
        c.setId(9L);
        when(contractService.getContractById(9L)).thenReturn(Optional.of(c));
        when(contractPdfService.generateContractPdf(any(Contract.class), anyBoolean(), any(), any()))
                .thenReturn(new byte[]{1, 2, 3});
        PdfExportRequest req = new PdfExportRequest();
        req.setIncludeAttachments(true);
        mockMvc.perform(post("/api/contracts/9/export-pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void patchStatusDelegatesToService() throws Exception {
        Contract updated = new Contract();
        updated.setId(2L);
        when(contractService.updateStatus(2L, ContractStatus.ACTIVE)).thenReturn(updated);
        mockMvc.perform(patch("/api/contracts/2/status").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void getContractsByFreelancer() throws Exception {
        when(contractService.getContractsByFreelancerId(7L)).thenReturn(List.of());
        mockMvc.perform(get("/api/contracts/freelancer/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

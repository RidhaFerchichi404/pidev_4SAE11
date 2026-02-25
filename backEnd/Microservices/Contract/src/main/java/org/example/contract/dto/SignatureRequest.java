package org.example.contract.dto;

/**
 * Request body for PATCH /api/contracts/{id}/sign.
 * signatureData holds the full Base64 data-URL of the uploaded signature image
 * (e.g. "data:image/png;base64,iVBORw0KGgo...").
 */
public class SignatureRequest {

    /** "CLIENT" or "FREELANCER" */
    private String role;

    /** Full Base64 data-URL of the uploaded signature image */
    private String signatureData;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getSignatureData() { return signatureData; }
    public void setSignatureData(String signatureData) { this.signatureData = signatureData; }
}

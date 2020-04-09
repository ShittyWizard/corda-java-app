package com.template.services;

import java.io.IOException;

import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;

import com.template.states.KYCState;

import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;

public interface KYCFlowService {
    Vault.Page<KYCState> getAvailableAttachments(CordaRPCOps proxy);
    String uploadAttachmentByMultipartFile(MultipartFile file, String uploader, CordaRPCOps proxy)
            throws IOException;
    String uploadAttachmentByInpuStream(InputStreamResource inputStreamResource, String filename, String uploader, CordaRPCOps proxy)
            throws IOException;
    InputStreamResource downloadAttachmentByHash(String hash, CordaRPCOps proxy);
}

package com.template.services.impl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.template.services.KYCFlowService;
import com.template.states.KYCState;

import net.corda.core.crypto.SecureHash;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;

@Service
public class DefaultKYCFlowService implements KYCFlowService {
    private final static Logger LOG = LoggerFactory.getLogger(DefaultKYCFlowService.class);

    @Override
    public Vault.Page<KYCState> getAvailableAttachments(CordaRPCOps proxy) {
        return proxy.vaultQuery(KYCState.class);
    }

    @Override
    public String uploadAttachmentByMultipartFile(MultipartFile file, String uploader, CordaRPCOps proxy)
            throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name must be set");
        }
        String contentType = file.getContentType();
        SecureHash hash;
        if (contentType != null && !(contentType.equals("zip") || contentType.equals("jar"))) {
            hash = uploadZip(IOUtils.toByteArray(file.getInputStream()), uploader, filename, proxy);
        } else {
            hash = proxy.uploadAttachmentWithMetadata(file.getInputStream(), uploader, filename);
        }
        LOG.info("File {} was successfully uploaded", filename);
        return hash.toString();
    }

    @Override
    public String uploadAttachmentByInputStream(byte[] content, String filename, String uploader, CordaRPCOps proxy)
            throws IOException {
        if (filename == null) {
            throw new IllegalArgumentException("File name must be set");
        }
        SecureHash hash = uploadZip(content, uploader, filename, proxy);
        LOG.info("File {} was successfully uploaded", filename);
        return hash.toString();
    }

    @Override
    public InputStreamResource downloadAttachmentByHash(String hash, CordaRPCOps proxy) {
        SecureHash.SHA256 secureHash = SecureHash.parse(hash);
        if (!proxy.attachmentExists(secureHash)) {
            throw new IllegalArgumentException(String.format("Attachment with hash %s was not found", secureHash));
        }
        return new InputStreamResource(proxy.openAttachment(secureHash));
    }

    private SecureHash uploadZip(byte[] content, String uploader, String filename, CordaRPCOps proxy)
            throws IOException {
        String zipName = filename + "-" + UUID.randomUUID().toString() + ".zip";
        FileOutputStream outputStream = new FileOutputStream(zipName);
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        ZipEntry zipEntry = new ZipEntry(filename);
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(content);
        zipOutputStream.closeEntry();
        zipOutputStream.close();

        FileInputStream fileInputStream = new FileInputStream(zipName);
        SecureHash hash = proxy.uploadAttachmentWithMetadata(fileInputStream, uploader, filename);
        Files.deleteIfExists(Paths.get(zipName));
        return hash;
    }
}

package com.template.webserver;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.template.flows.KYCFlow;
import com.template.services.KYCFlowService;
import com.template.services.impl.DefaultKYCFlowService;
import com.template.states.KYCState;
import com.template.web.KYCStateResourceAssembler;
import com.template.web.NodeInfoResourceAssembler;
import com.template.web.dto.KYCStateResource;
import com.template.web.dto.NodeInfoResource;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/")
public class Controller {
    private NodeInfoResourceAssembler nodeInfoResourceAssembler = new NodeInfoResourceAssembler();
    private KYCStateResourceAssembler kycStateResourceAssembler = new KYCStateResourceAssembler();
    private KYCFlowService kycFlowService = new DefaultKYCFlowService();
    private RestTemplate restTemplate = new RestTemplate();

    private final CordaRPCOps proxy;

    private static final String CROSS_NODE_ADDRESS = "O=CrossNode,L=Moscow,C=RU";
    private static final String ETHEREUM_CHANGE_OWNER_URL = "http://localhost:8081/crosschain/ethereum/filestore/receiver/changeOwner";
    private static final String ETHEREUM_CHECK_GRANTS_URL = "http://178.154.248.132:8081/crosschain/ethereum/filestore/receiver/checkGrantForFile";
//    private static final String ETHEREUM_CHECK_GRANTS_URL = "http://localhost:8081/crosschain/ethereum/filestore/receiver/checkGrantForFile";

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @GetMapping(value = "/networkSnapshot", produces = APPLICATION_JSON_VALUE)
    public List<NodeInfoResource> getNetworkSnapshot() {
        return proxy.networkMapSnapshot().stream()
                    .map(nodeInfoResourceAssembler::toResource)
                    .collect(Collectors.toList());
    }

    @GetMapping(value = "/flows", produces = APPLICATION_JSON_VALUE)
    public List<String> getFlows() {
        return proxy.registeredFlows();
    }

    @GetMapping(value = "/attachments")
    public List<KYCStateResource> getKYCStates() {
        return kycFlowService.getAvailableAttachments(proxy).getStates().stream()
                             .map(StateAndRef<KYCState>::getState)
                             .map(TransactionState<KYCState>::getData)
                             .map(kycStateResourceAssembler::toResource)
                             .collect(Collectors.toList());
    }

    @PostMapping(value = "/attachments/upload")
    public ResponseEntity<String> uploadAttachment(
            @RequestBody
                    MultipartFile file,
            @RequestParam
                    String uploader
    )
            throws IOException {
        String hash = kycFlowService.uploadAttachmentByMultipartFile(file, uploader, proxy);
        return ResponseEntity.created(URI.create(String.format("attachments/%s", hash)))
                             .body(String.format("Attachment uploaded with hash - %s", hash));
    }

    @GetMapping(value = "/attachments/{hash}")
    public ResponseEntity<Resource> downloadAttachmentByHash(
            @PathVariable
                    String hash
    ) {

        InputStreamResource resource = kycFlowService.downloadAttachmentByHash(hash, proxy);
        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s.zip\"", hash))
                             .body(resource);
    }

    @PostMapping(value = "/attachments/startKYCFlow")
    public void startKYCFlow(
            @RequestBody
                    MultipartFile file,
            @RequestParam
                    String targetBankName,
            @RequestParam
                    String uploader
    )
            throws IOException {
        Party targetBank = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(targetBankName));
        System.out.println("TEST UPLOADING!");
        if (targetBank != null) {
            String hash = kycFlowService.uploadAttachmentByMultipartFile(file, uploader, proxy);
            System.out.println("Target bank " + targetBank.getName().getOrganisation());
            System.out.println("Hash " + hash);
            proxy.startFlowDynamic(KYCFlow.class, hash, targetBank);
        }
    }

    @PostMapping(value = "/attachments/startKYCFlow/crosschain/initFile")
    public String startCrosschainKYCFlowWithInitFile(
            @RequestBody
                    MultipartFile file,
            @RequestParam
                    String uploader,
            @RequestParam
                    String sendToAddress
    )
            throws IOException {
        Party crossNode = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(CROSS_NODE_ADDRESS));
        System.out.println("Crosschain flow for new file started...");
        if (crossNode != null) {
            String hashOfFile = kycFlowService.uploadAttachmentByMultipartFile(file, uploader, proxy);
            InputStreamResource inputStreamResource = new InputStreamResource(file.getInputStream());

            return composeAndSendRequestForCrosschainTransaction(inputStreamResource, sendToAddress, crossNode, hashOfFile);
        } else {
            System.out.println("Problem with getting cross node");
            throw new IllegalArgumentException("Wrong address of cross node");
        }
    }

    @PostMapping(value = "/attachments/startKYCFlow/crosschain/existingFile")
    public String startCrosschainKYCFlowWithExistingFile(
            @RequestParam
                    String hashOfFile,
            @RequestParam
                    String sendToAddress
    ) {
        Party crossNode = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(CROSS_NODE_ADDRESS));
        System.out.println("Crosschain flow for new file started...");
        if (crossNode != null) {
            InputStreamResource inputStreamResource = kycFlowService.downloadAttachmentByHash(hashOfFile, proxy);

            return composeAndSendRequestForCrosschainTransaction(inputStreamResource, sendToAddress, crossNode, hashOfFile);
        } else {
            System.out.println("Problem with getting cross node");
            throw new IllegalArgumentException("Wrong address of cross node");
        }
    }

//    FOR REQUESTING FROM ETHEREUM APP
    @PostMapping(value = "/attachments/startKYCFlow/crosschain/receiver")
    public void startKYCFlowByReceiver(
            @RequestBody
                    InputStreamResource inputStreamResource,
            @RequestParam
                    String ipfsHashFile,
            @RequestParam
                    String senderPublicKey,
            @RequestParam
                    String organisation,
            @RequestParam
                    String locality,
            @RequestParam
                    String country,
            @RequestParam
                    String filename,
            @RequestParam
                    String uploader
    )
            throws IOException {
        boolean isGrantCorrect = composeAndSendRequestForCheckingGrantsToFile(ipfsHashFile, senderPublicKey);
        if (!isGrantCorrect) {
            throw new IllegalStateException(String.format("IPFS hash file (%s) belong to %s. Probably, %s is frod.", ipfsHashFile, senderPublicKey, senderPublicKey));
        } else {
            System.out.printf("IPFS hash file (%s) does not belong to %s. Successful!%n", ipfsHashFile, senderPublicKey);
        }
        String sendToAddress = buildAddressByParameters(organisation, locality, country);
        Party targetBank = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(sendToAddress));
        if (targetBank != null) {
            String hash = kycFlowService.uploadAttachmentByInpuStream(inputStreamResource, filename, uploader, proxy);
            System.out.println("Target bank " + targetBank.getName().getOrganisation());
            System.out.println("Hash " + hash);
            proxy.startFlowDynamic(KYCFlow.class, hash, targetBank);
        } else {
            throw new IllegalArgumentException("Address of bank is incorrect");
        }
    }

    private String buildAddressByParameters(String organisation, String locality, String country) {
        return String.format("O=%s,L=%s,C=%s", organisation, locality, country);
    }

    private boolean composeAndSendRequestForCheckingGrantsToFile(
            String ipfsHashFile,
            String senderPublicKey
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ETHEREUM_CHECK_GRANTS_URL)
                .queryParam("senderPublicKey", senderPublicKey)
                .queryParam("ipfsHashOfFile", ipfsHashFile);

        Boolean result;
        try {
            result = restTemplate.getForObject(builder.toUriString(), Boolean.class);
            if (result != null) {
                return result;
            } else {
                throw new IllegalStateException("Something goes wrong with rest-request for checking grant for file..");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Something goes wrong with rest-request for checking grant for file..");
        }
    }

    private String composeAndSendRequestForCrosschainTransaction(
            InputStreamResource inputStreamResource,
            String sendToAddress,
            Party crossNode,
            String hashOfFile
    ) {
        proxy.startFlowDynamic(KYCFlow.class, hashOfFile, crossNode);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<InputStreamResource> entity = new HttpEntity<>(inputStreamResource, headers);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ETHEREUM_CHANGE_OWNER_URL)
                                                           .queryParam("sendToAddress", sendToAddress);

        return restTemplate.exchange(builder.toUriString(), HttpMethod.POST, entity, String.class).getBody();
    }
}
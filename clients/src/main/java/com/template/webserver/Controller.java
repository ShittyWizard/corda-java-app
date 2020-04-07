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
    private static final String ETHEREUM_INIT_URL = "http://localhost:8081/ethereum/filestore/crosschain/init";

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
        String hash = kycFlowService.uploadAttachment(file, uploader, proxy);
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
            String hash = kycFlowService.uploadAttachment(file, uploader, proxy);
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
                    String publicAddress
    )
            throws IOException {
        Party crossNode = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(CROSS_NODE_ADDRESS));
        System.out.println("Crosschain flow started...");
        if (crossNode != null) {
            String hash = kycFlowService.uploadAttachment(file, uploader, proxy);
            proxy.startFlowDynamic(KYCFlow.class, hash, crossNode);

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<InputStreamResource> entity = new HttpEntity<>(new InputStreamResource(file.getInputStream()), headers);

            return restTemplate.exchange("http://localhost:8081/ethereum/filestore/crosschain/init", HttpMethod.POST, entity, String.class).getBody();
        } else {
            System.out.println("Problem with getting cross node");
            throw new IllegalArgumentException("Wrong address of cross node");
        }
    }

    @PostMapping(value = "/attachments/startKYCFlow/crosschain/existingFile")
    public void startCrosschainKYCFlowWtihExistingFile(
            @RequestParam
                    String hashOfFile,
            @RequestParam
                    String publicAddress
    ) {

    }
}
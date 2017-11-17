
package care.solve.fabric.controller;

import care.solve.fabric.entity.ChaincodeMeta;
import care.solve.fabric.service.ChaincodeService;
import care.solve.fabric.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/chaincode")
public class ChaincodeController {

    private ChaincodeService chaincodeService;
    private HFClient peerAdminClient;
    private Channel healthChannel;
    private UserService userService;
    private Orderer orderer;
    private ObjectMapper mapper;

    @Autowired
    public ChaincodeController(ChaincodeService chaincodeService,
                               UserService userService,
                               @Qualifier("peerAdminHFClient") HFClient peerAdminClient,
                               Channel healthChannel, Orderer orderer, ObjectMapper mapper) {

        this.chaincodeService = chaincodeService;
        this.userService = userService;
        this.peerAdminClient = peerAdminClient;
        this.healthChannel = healthChannel;
        this.orderer = orderer;
        this.mapper = mapper;
    }

    @PostMapping(value = "upload")
    public void handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("meta") String chaincodeMetaString) throws Exception {
        ChaincodeMeta chaincodeMeta = mapper.readValue(chaincodeMetaString, ChaincodeMeta.class);

        List<Query.ChaincodeInfo> coll = healthChannel.queryInstantiatedChaincodes(healthChannel.getPeers().iterator().next());
        if (!CollectionUtils.isEmpty(coll)) {
            throw new RuntimeException("Already installed");
        }

        File tarGzFile = new File("/tmp/" + file.getOriginalFilename());
        file.transferTo(tarGzFile);
        chaincodeService.installChaincode(peerAdminClient, chaincodeMeta, healthChannel.getPeers(), tarGzFile);
        chaincodeService.instantiateChaincode(peerAdminClient, chaincodeMeta, healthChannel, orderer);

    }

    @PostMapping(value = "upgrade")
    public void handleFileUpgrade(@RequestParam("file") MultipartFile file, @RequestParam("meta") String chaincodeMetaString) throws Exception {
        ChaincodeMeta chaincodeMeta = mapper.readValue(chaincodeMetaString, ChaincodeMeta.class);

        List<Query.ChaincodeInfo> coll = healthChannel.queryInstantiatedChaincodes(healthChannel.getPeers().iterator().next());
        if (CollectionUtils.isEmpty(coll)) {
            throw new RuntimeException("Install first");
        }

        File tarGzFile = new File("/tmp/" + file.getOriginalFilename());
        file.transferTo(tarGzFile);
        chaincodeService.installChaincode(peerAdminClient, chaincodeMeta, healthChannel.getPeers(), tarGzFile);
        chaincodeService.upgradeChaincode(peerAdminClient, chaincodeMeta,  healthChannel, orderer);
    }

    @PostMapping("registerUsers")
    public void registerUsers() throws Exception {
        userService.registerUser("tim");
        userService.registerUser("tim.Doctor");
    }
}
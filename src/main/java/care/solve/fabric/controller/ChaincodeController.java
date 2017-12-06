
package care.solve.fabric.controller;

import care.solve.fabric.entity.ChaincodeMeta;
import care.solve.fabric.service.ChaincodeService;
import care.solve.fabric.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chaincode")
public class ChaincodeController {

    private ChaincodeService chaincodeService;
    private HFClient peerAdminClient;
    private UserService userService;
    private ObjectMapper mapper;
    private Map<String, Channel> channelMap;

    public ChaincodeController(ChaincodeService chaincodeService,
                               UserService userService,
                               HFClient peerAdminClient,
                               ObjectMapper mapper,
                               Map<String, Channel> channelMap) {

        this.chaincodeService = chaincodeService;
        this.userService = userService;
        this.peerAdminClient = peerAdminClient;
        this.mapper = mapper;
        this.channelMap = channelMap;
    }

    @PostMapping(value = "upload")
    public void handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("meta") String chaincodeMetaString) throws Exception {
        ChaincodeMeta chaincodeMeta = mapper.readValue(chaincodeMetaString, ChaincodeMeta.class);

        Channel channel = channelMap.get(chaincodeMeta.getChannelName());

        List<Query.ChaincodeInfo> coll = channel.queryInstantiatedChaincodes(channel.getPeers().iterator().next());
        if (!CollectionUtils.isEmpty(coll)) {
            throw new RuntimeException("Already installed");
        }

        File tarGzFile = new File("/tmp/" + file.getOriginalFilename());
        file.transferTo(tarGzFile);
        chaincodeService.installChaincode(peerAdminClient, chaincodeMeta, channel.getPeers(), tarGzFile);
        chaincodeService.instantiateChaincode(peerAdminClient, chaincodeMeta, channel);

    }

    @PostMapping(value = "upgrade")
    public void handleFileUpgrade(@RequestParam("file") MultipartFile file, @RequestParam("meta") String chaincodeMetaString) throws Exception {
        ChaincodeMeta chaincodeMeta = mapper.readValue(chaincodeMetaString, ChaincodeMeta.class);

        Channel channel = channelMap.get(chaincodeMeta.getChannelName());

        List<Query.ChaincodeInfo> coll = channel.queryInstantiatedChaincodes(channel.getPeers().iterator().next());
        if (CollectionUtils.isEmpty(coll)) {
            throw new RuntimeException("Install first");
        }

        File tarGzFile = new File("/tmp/" + file.getOriginalFilename());
        file.transferTo(tarGzFile);
        chaincodeService.installChaincode(peerAdminClient, chaincodeMeta, channel.getPeers(), tarGzFile);
        chaincodeService.upgradeChaincode(peerAdminClient, chaincodeMeta,  channel);
    }

    @PostMapping("registerUsers")
    public void registerUsers() throws Exception {
        userService.registerUser("tim");
        userService.registerUser("tim.Doctor");
    }
}
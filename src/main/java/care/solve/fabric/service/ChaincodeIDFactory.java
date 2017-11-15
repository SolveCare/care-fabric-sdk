package care.solve.fabric.service;

import care.solve.fabric.config.ChaincodeUpdateEvent;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;


//todo something wrong here
@Component
public class ChaincodeIDFactory implements ApplicationListener<ChaincodeUpdateEvent> {

    private static final String CHAIN_CODE_NAME = "scheduleChaincode_go";
    private static final String CHAIN_CODE_PATH = "solvecare-chaincode";

    private ChaincodeID currentChaincodeID;

    @Autowired
    private Channel healthChannel;

    @PostConstruct
    private void init() {
        String currentVersion = getCurrentVersion();
        if (currentVersion != null) this.currentChaincodeID = buildChaincodeID(currentVersion);
    }

    @Override
    public void onApplicationEvent(ChaincodeUpdateEvent chaincodeUpdateEvent) {
        this.currentChaincodeID = buildChaincodeID(getCurrentVersion());
    }

    public ChaincodeID getCurrentChaincodeID() {
        return currentChaincodeID;
    }

    public ChaincodeID getNextChaincode() {
        Integer version = currentChaincodeID == null ? 0 : Integer.valueOf(getCurrentVersion());
        version++;
        return buildChaincodeID(version.toString());
    }

    private String getCurrentVersion() {
        try {
            List<Query.ChaincodeInfo> chaincodeInfos = healthChannel.queryInstantiatedChaincodes(healthChannel.getPeers().iterator().next());

            if (CollectionUtils.isEmpty(chaincodeInfos)) return null;

            return chaincodeInfos.get(chaincodeInfos.size() - 1).getVersion();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ChaincodeID buildChaincodeID(String version) {
        return ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
                .setVersion(version)
                .setPath(CHAIN_CODE_PATH).build();
    }
}
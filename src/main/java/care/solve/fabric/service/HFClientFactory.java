package care.solve.fabric.service;

import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class HFClientFactory {

    private static final Map<String, HFClient> HF_CLIENT_MAP = new HashMap<>();

    private User individualPeerAdmin;

    @Autowired
    public HFClientFactory(@Qualifier("individualPeerAdmin") User individualPeerAdmin) {
        this.individualPeerAdmin = individualPeerAdmin;
    }


//    @Autowired
//    private SampleStore defaultStore;

//    public HFClient getClient() {
//        HFClient client;
//        try {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//            if (authentication == null) throw new SessionAuthenticationException("User must be logged in!");
//
//            String userName = authentication.getName();
//
//            if (HF_CLIENT_MAP.containsKey(userName)) {
//                return HF_CLIENT_MAP.get(userName);
//            }
//
//            SampleUser user = defaultStore.getMember(userName, UserService.org);
//            client = HFClient.createNewInstance();
//            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
//            client.setUserContext(user);
//            HF_CLIENT_MAP.put(userName, client);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        return client;
//    }

    public HFClient getClient() {
        HFClient client;
        try {
            client = HFClient.createNewInstance();
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            client.setUserContext(individualPeerAdmin);
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
        return client;
    }
}

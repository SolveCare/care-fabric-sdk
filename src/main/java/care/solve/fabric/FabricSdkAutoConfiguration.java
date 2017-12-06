package care.solve.fabric;

import care.solve.fabric.config.ChaincodeProperties;
import care.solve.fabric.config.HFConfig;
import care.solve.fabric.config.HFProperties;
import care.solve.fabric.config.StoreConfig;
import care.solve.fabric.controller.ChaincodeController;
import care.solve.fabric.entity.SampleStore;
import care.solve.fabric.entity.SampleUser;
import care.solve.fabric.service.ChaincodeService;
import care.solve.fabric.service.ChannelService;
import care.solve.fabric.service.HFClientFactory;
import care.solve.fabric.service.TransactionService;
import care.solve.fabric.service.TransactionServiceImpl;
import care.solve.fabric.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Map;

@Configuration
@EnableConfigurationProperties({HFProperties.class, ChaincodeProperties.class})
@Import(value = {HFConfig.class, StoreConfig.class})
public class FabricSdkAutoConfiguration {

    @Bean
    public ChaincodeService chaincodeService(ChaincodeProperties chaincodeProperties) {
        return new ChaincodeService(chaincodeProperties);
    }

    @Bean
    public ChannelService channelService(HFProperties hfProperties) {
        return new ChannelService();
    }

    @Bean
    public HFClientFactory hfClientFactory(@Qualifier("individualPeerAdmin") User user) {
        return new HFClientFactory(user);
    }

    @Bean
    public TransactionService transactionService(HFClientFactory hfClientFactory, Map<String,Channel> channelsMap, ChaincodeService chaincodeService) {
        return new TransactionServiceImpl(hfClientFactory, channelsMap, chaincodeService);
    }

    @Bean
    public UserService userService(
            @Qualifier("individualHfcaAdminClient") HFCAClient client,
            SampleStore defaultStore,
            @Qualifier("individualPeerAdmin") SampleUser adminUser,
            HFProperties hfProperties) {
        return new UserService(client, defaultStore, adminUser, hfProperties);
    }


    @Bean
    public ChaincodeController chaincodeController(ChaincodeService chaincodeService,
                                                   UserService userService,
                                                   @Qualifier("individualPeerAdminHFClient") HFClient peerAdminClient,
                                                   Map<String, Channel> channelMap) {
        ObjectMapper mapper = new ObjectMapper();
        return new ChaincodeController(chaincodeService, userService, peerAdminClient, mapper, channelMap);
    }
}

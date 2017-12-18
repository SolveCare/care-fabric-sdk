package care.solve.fabric.config;


import care.solve.fabric.entity.SampleStore;
import care.solve.fabric.entity.SampleUser;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

@Configuration
public class UserConfig {

    private HFCAClient hfcaAdminClient;
    private HFProperties hfProperties;

    @Autowired
    public UserConfig(HFCAClient hfcaAdminClient, HFProperties hfProperties) {
        this.hfcaAdminClient = hfcaAdminClient;
        this.hfProperties = hfProperties;
    }

    @Bean(name = "peerAdminUser")
    @Autowired
    public SampleUser createpeerAdminUser(
            @Qualifier("defaultStore") SampleStore sampleStore) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException, EnrollmentException, InvalidArgumentException, URISyntaxException {

        HFProperties.Organization clinicOrg = hfProperties.getOrgs().get("clinic");
        String peerAdminKeystoreFile = clinicOrg.getAdmin().getKeystoreFile();
        String peerAdminCertFile = clinicOrg.getAdmin().getCertFile();

        InputStream keystoreFile = new FileInputStream(peerAdminKeystoreFile);
        InputStream certFile = new FileInputStream(peerAdminCertFile);

        SampleUser peerAdmin = sampleStore.getMember(
                "peerAdmin",
                clinicOrg.getName(),
                clinicOrg.getMspId(),
                keystoreFile,
                certFile);

        return peerAdmin;
    }

    @Bean(name = "sampleUser")
    @Autowired
    public SampleUser createPeerSampleUser(
            @Qualifier("defaultStore") SampleStore sampleStore) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException, EnrollmentException, InvalidArgumentException, URISyntaxException {

        HFProperties.Organization clinicOrg = hfProperties.getOrgs().get("clinic");
        String userKeystoreFile = clinicOrg.getUsers().get(0).getKeystoreFile();
        String userCertFile = clinicOrg.getUsers().get(0).getCertFile();

        InputStream keystoreFile = new FileInputStream(userKeystoreFile);
        InputStream certFile = new FileInputStream(userCertFile);

        SampleUser user = sampleStore.getMember(
                "user",
                clinicOrg.getName(),
                clinicOrg.getMspId(),
                keystoreFile,
                certFile);

        return user;
    }

    @Bean(name = "adminUser")
    @Autowired
    public SampleUser createAdminUser(
            @Qualifier("defaultStore") SampleStore sampleStore) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException, EnrollmentException, InvalidArgumentException, URISyntaxException {

        HFProperties.Organization clinicOrg = hfProperties.getOrgs().get("clinic");
        Enrollment adminEnrollment = hfcaAdminClient.enroll("admin", "adminpw");
        SampleUser admin = sampleStore.getMember(
                "admin",
                clinicOrg.getName(),
                clinicOrg.getMspId(),
                adminEnrollment.getKey(),
                adminEnrollment.getCert());

        return admin;
    }

}

package care.solve.fabric.service;

import care.solve.fabric.config.HFProperties;
import care.solve.fabric.entity.SampleStore;
import care.solve.fabric.entity.SampleUser;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private HFCAClient hfcaAdminClient;
    private SampleStore defaultStore;
    private SampleUser adminUser;

    private HFProperties.Organization clinicOrg;

    public static final String org = "org1.department1";

    @Autowired
    public UserService(HFCAClient client, SampleStore defaultStore, SampleUser adminUser, HFProperties hfProperties) {
        this.hfcaAdminClient = client;
        this.defaultStore = defaultStore;
        this.adminUser = adminUser;

        this.clinicOrg = hfProperties.getOrgs().get("clinic");
    }

    public void registerUser(String userName) throws Exception {
        RegistrationRequest registrationRequest = new RegistrationRequest(userName, org);
        String enrollmentSecret = hfcaAdminClient.register(registrationRequest, adminUser);
        Enrollment enrollment = hfcaAdminClient.enroll(userName, enrollmentSecret);
        defaultStore.getMember(userName, org, clinicOrg.getMspId(), enrollment.getKey(), enrollment.getCert());
    }

}

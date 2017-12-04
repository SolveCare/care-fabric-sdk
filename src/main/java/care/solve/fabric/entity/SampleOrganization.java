package care.solve.fabric.entity;

import lombok.Builder;
import lombok.Data;
import org.hyperledger.fabric.sdk.Peer;

import java.util.List;

@Data
@Builder
public class SampleOrganization {
    private String name;
    private List<Peer> peers;
    private SampleUser adminUser;
    private SampleUser user;
}

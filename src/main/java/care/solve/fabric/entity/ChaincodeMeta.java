package care.solve.fabric.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChaincodeMeta {

    private String name;
    private String path;
    private String version;
    private String channelName;

}

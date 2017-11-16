package care.solve.fabric.config;

import org.springframework.context.ApplicationEvent;

public class ChaincodeUpdateEvent extends ApplicationEvent {
    public ChaincodeUpdateEvent(Object source) {
        super(source);
    }
}

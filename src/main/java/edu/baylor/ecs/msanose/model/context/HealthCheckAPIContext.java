package edu.baylor.ecs.msanose.model.context;

import edu.baylor.ecs.msanose.model.MicroserviceHealthcheckDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

@AllArgsConstructor
@Data public class HealthCheckAPIContext {
    List<MicroserviceHealthcheckDTO> healthCheckAPIs;
    int count;

    public HealthCheckAPIContext(){
        this.healthCheckAPIs = new ArrayList<>();
        this.count = 0;
    }

    public HealthCheckAPIContext(List<MicroserviceHealthcheckDTO> healthCheckAPIs){
        this.healthCheckAPIs = healthCheckAPIs;
        this.count = healthCheckAPIs.size();
    }

    public void addAPIContext(MicroserviceHealthcheckDTO object){
        this.healthCheckAPIs.add(object);
        this.count++;
    }
}

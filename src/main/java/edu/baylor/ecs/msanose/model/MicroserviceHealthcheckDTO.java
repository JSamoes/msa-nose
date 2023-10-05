package edu.baylor.ecs.msanose.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class MicroserviceHealthcheckDTO {

    String microserviceControllerName;
    boolean hasHealthcheckEndpoint;
}

package com.axibase.collector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {
    @JsonProperty(required = true)
    private BrokerConfiguration broker;
    private SimulationConfiguration simulation;
    @JsonProperty(required = true)
    private String topic;
    @JsonProperty(required = true)
    private int clientsCount;
}

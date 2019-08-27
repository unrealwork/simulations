package com.axibase.collector;

import lombok.Data;

@Data
public class SimulationConfiguration {
    private int elements = 10;
    private int randomLimit = 1000;
    private int publishPeriodMs = 10;
}

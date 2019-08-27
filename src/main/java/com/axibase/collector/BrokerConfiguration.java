package com.axibase.collector;

import lombok.Data;

@Data
public class BrokerConfiguration {
    private String url;
    private String username;
    private String password;
}

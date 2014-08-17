package com.softwarelab7.fishbowl.models;

import java.util.Date;

/**
 */
public class Sale {
    public Long id;
    public long session;
    @Deprecated
    public String location;
    public double lat;
    public double lon;
    public Date dateCreated;
}

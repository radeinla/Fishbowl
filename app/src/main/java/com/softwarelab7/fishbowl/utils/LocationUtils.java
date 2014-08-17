package com.softwarelab7.fishbowl.utils;

import android.location.Location;

/**
 */
public class LocationUtils {
    public static String toCoordinateString(double lat, double lon) {
        return "("+lat+","+lon+")";
    }

    public static String toCoordinateString(Location location) {
        if (location == null) {
            return "No new location!";
        } else {
            return toCoordinateString(location.getLatitude(), location.getLongitude());
        }
    }

    public static Location createLocation(double lat, double lon) {
        Location location = new Location(LocationUtils.class.getSimpleName());
        location.setLatitude(lat);
        location.setLongitude(lon);
        return location;
    }
}

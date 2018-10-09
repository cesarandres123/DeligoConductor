package com.vecolsoft.deligo_conductor.Common;

import android.location.Location;

import com.vecolsoft.deligo_conductor.Remote.FCMClient;
import com.vecolsoft.deligo_conductor.Remote.IFCMService;
import com.vecolsoft.deligo_conductor.Remote.IGoogleAPI;
import com.vecolsoft.deligo_conductor.Remote.RetrofitClient;

public class Common {


    public static final String driver_tbl = "Drivers";
    public static final String user_driver_tbl = "DriversInformation";
    public static final String user_rider_tbl  = "RidersInformation";
    public static final String pickup_request_tbl = "PickupResquest";
    public static final String token_tbl = "Tokens";

    public static Location MyLocation =  null;
    public static Boolean OnSeguimiento = null;

    public static final String baseURL = "https://maps.googleapis.com";
    public static final String fcmURL = "https://fcm.googleapis.com/";


    public static IGoogleAPI getGoogleAPI()
    {
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }

    public static IFCMService getFCMService()
    {
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }
}

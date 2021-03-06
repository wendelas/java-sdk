package com.konduto.sdk.models;

import com.google.gson.JsonObject;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 */
public class KondutoBusTravelLegTest {
    private static final String ORIGIN_CITY = "São Paulo";
    private static final String DESTINATION_CITY = "Rio de Janeiro";
    private static final String TRAVEL_DATE = "2015-05-07T12:00Z";

    private static final JsonObject BUS_TRAVEL_LEG_AS_JSON = new JsonObject();
    static {
        BUS_TRAVEL_LEG_AS_JSON.addProperty("origin_city", ORIGIN_CITY);
        BUS_TRAVEL_LEG_AS_JSON.addProperty("destination_city", DESTINATION_CITY);
        BUS_TRAVEL_LEG_AS_JSON.addProperty("date", TRAVEL_DATE);
        BUS_TRAVEL_LEG_AS_JSON.addProperty("number_of_connections", 0);
    }

    private static final KondutoBusTravelLeg BUS_TRAVEL_LEG = new KondutoBusTravelLeg();
    static {
        BUS_TRAVEL_LEG.setOriginCity(ORIGIN_CITY);
        BUS_TRAVEL_LEG.setDestinationCity(DESTINATION_CITY);
        BUS_TRAVEL_LEG.setNumberOfConnections(0);
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            BUS_TRAVEL_LEG.setDate(format.parse(TRAVEL_DATE));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void isValidTest() {
        KondutoBusTravelLeg travelLeg = new KondutoBusTravelLeg();
        // is invalid without origin and destination cities
        assertFalse(travelLeg.isValid());
        travelLeg.setOriginCity(ORIGIN_CITY);
        assertFalse(travelLeg.isValid());
        travelLeg.setDestinationCity(DESTINATION_CITY);
        assertTrue(travelLeg.isValid());
    }

    @Test
    public void serializeTest() throws Exception {
        assertEquals(BUS_TRAVEL_LEG_AS_JSON, BUS_TRAVEL_LEG.toJSON());
    }

    @Test
    public void deserializeTest() {
        assertEquals(KondutoModel.fromJSON(BUS_TRAVEL_LEG_AS_JSON, KondutoBusTravelLeg.class),
                BUS_TRAVEL_LEG);
    }

}

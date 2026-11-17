package com.wayfarer.travel.agent;

import com.wayfarer.travel.api.TravelModels.*;
import java.util.List;

public interface Maps {
    City city(String input);
    List<Place> search(City city, String query);
    Route route(Place from, Place to);
}

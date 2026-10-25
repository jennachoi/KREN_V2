package com.kren.letter.dto;

import com.kren.letter.CountryLocations;
import com.kren.letter.Letter;

/** Where a letter flies from and to, for the tracking map. Coordinates are null if unknown. */
public record Route(Place from, Place to, Integer distanceKm) {

	public record Place(String country, Double lat, Double lng) {

		public static Place of(String country, CountryLocations locations) {
			return locations.find(country)
				.map(l -> new Place(country, l.lat(), l.lng()))
				.orElse(new Place(country, null, null));
		}

	}

	public static Route of(Letter letter, CountryLocations locations) {
		return new Route(Place.of(letter.getOriginCountry(), locations), Place.of(letter.getDestinationCountry(), locations),
				letter.getDistanceKm());
	}

}

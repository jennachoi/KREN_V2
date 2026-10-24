package com.kren.letter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Where each country's letters take off and land (its capital), from geo/country-locations.csv. */
@Component
public class CountryLocations {

	public record Location(double lat, double lng) {
	}

	private static final double EARTH_RADIUS_KM = 6371.0;

	private final Map<String, Location> locations = new HashMap<>();

	public CountryLocations() throws IOException {
		var resource = new ClassPathResource("geo/country-locations.csv");
		try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			reader.lines()
				.filter(line -> !line.isBlank() && !line.startsWith("#"))
				.map(line -> line.split(","))
				.forEach(p -> locations.put(p[0], new Location(Double.parseDouble(p[1]), Double.parseDouble(p[2]))));
		}
	}

	public Optional<Location> find(String country) {
		return Optional.ofNullable(locations.get(country));
	}

	/** Great-circle distance; same country is 0. Empty if either country isn't in the table. */
	public Optional<Integer> distanceKm(String from, String to) {
		if (from.equals(to)) {
			return Optional.of(0);
		}
		return find(from).flatMap(a -> find(to).map(b -> (int) Math.round(haversineKm(a, b))));
	}

	static double haversineKm(Location a, Location b) {
		double dLat = Math.toRadians(b.lat() - a.lat());
		double dLng = Math.toRadians(b.lng() - a.lng());
		double h = Math.pow(Math.sin(dLat / 2), 2)
				+ Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat())) * Math.pow(Math.sin(dLng / 2), 2);
		return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
	}

}

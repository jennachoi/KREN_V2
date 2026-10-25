import { feature, mesh } from 'topojson-client'
import type { FeatureCollection } from 'geojson'
import type { GeometryCollection, Topology } from 'topojson-specification'
import countries110m from 'world-atlas/countries-110m.json'

const topology = countries110m as unknown as Topology<{ countries: GeometryCollection }>

/** Country shapes and internal borders, shared by every map. */
export const LAND = feature(topology, topology.objects.countries) as FeatureCollection
export const BORDERS = mesh(topology, topology.objects.countries, (a, b) => a !== b)

export type LngLat = [number, number]

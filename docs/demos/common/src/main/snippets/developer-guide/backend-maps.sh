// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::backend-maps-bash-001[]
pmtiles extract https://build.protomaps.com/20261120.pmtiles monaco.pmtiles \
    --bbox=7.40,43.72,7.44,43.76 --maxzoom=15
// end::backend-maps-bash-001[]

// tag::backend-maps-bash-002[]
osmium extract --bbox=7.40,43.72,7.44,43.76 europe-latest.osm.pbf -o monaco.osm.pbf
osmium cat some-lz4-extract.osm.pbf -o zlib.osm.pbf
// end::backend-maps-bash-002[]

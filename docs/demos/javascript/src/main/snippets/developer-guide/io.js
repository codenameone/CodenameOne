// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::io-javascript-001[]
[
  {
    "url": "http://www.anapioficeandfire.com/api/characters/13",
    "name": "Chayle",
    "culture": "",
    "born": "",
    "died": "In 299 AC, at Winterfell",
    "titles": [
      "Septon"
    ],
    "aliases": [],
    "father": "",
    "mother": "",
    "spouse": "",
    "allegiances": [],
    "books": [
      "http://www.anapioficeandfire.com/api/books/1",
      "http://www.anapioficeandfire.com/api/books/2",
      "http://www.anapioficeandfire.com/api/books/3"
    ],
    "povBooks": [],
    "tvSeries": [],
    "playedBy": []
  },
  {
    "url": "http://www.anapioficeandfire.com/api/characters/14",
    "name": "Gillam",
    "culture": "",
    "born": "",
    "died": "",
    "titles": [
      "Brother"
    ],
    "aliases": [],
    "father": "",
    "mother": "",
    "spouse": "",
    "allegiances": [],
    "books": [
      "http://www.anapioficeandfire.com/api/books/5"
    ],
    "povBooks": [],
    "tvSeries": [],
    "playedBy": []
  },
  {
    "url": "http://www.anapioficeandfire.com/api/characters/15",
    "name": "High Septon",
    "culture": "",
    "born": "",
    "died": "",
    "titles": [
      "High Septon",
      "His High Holiness",
      "Father of the Faithful",
      "Voice of the Seven on Earth"
    ],
    "aliases": [
      "The High Sparrow"
    ],
    "father": "",
    "mother": "",
    "spouse": "",
    "allegiances": [],
    "books": [
      "http://www.anapioficeandfire.com/api/books/5",
      "http://www.anapioficeandfire.com/api/books/8"
    ],
    "povBooks": [],
    "tvSeries": [
      "Season 5"
    ],
    "playedBy": [
      "Jonathan Pryce"
    ]
  }
]
// end::io-javascript-001[]

// tag::io-javascript-002[]
{
  "status": "OK",
  "results": [
    {
      "place_id": "ChIJJ5T9-iFawokRTPGaOginEO4",
      "formatted_address": "280 Broadway, New York, NY 10007, USA",
      "address_components": [
        {
          "short_name": "280",
          "types": ["street_number"],
          "long_name": "280"
        },
        {
          "short_name": "Broadway",
          "types": ["route"],
          "long_name": "Broadway"
        },
        {
          "short_name": "Lower Manhattan",
          "types": [
            "neighborhood",
            "political"
          ],
          "long_name": "Lower Manhattan"
        },
        {
          "short_name": "Manhattan",
          "types": [
            "sublocality_level_1",
            "sublocality",
            "political"
          ],
          "long_name": "Manhattan"
        },
        {
          "short_name": "New York",
          "types": [
            "locality",
            "political"
          ],
          "long_name": "New York"
        },
        {
          "short_name": "New York County",
          "types": [
            "administrative_area_level_2",
            "political"
          ],
          "long_name": "New York County"
        },
        {
          "short_name": "NY",
          "types": [
            "administrative_area_level_1",
            "political"
          ],
          "long_name": "New York"
        },
        {
          "short_name": "US",
          "types": [
            "country",
            "political"
          ],
          "long_name": "United States"
        },
        {
          "short_name": "10007",
          "types": ["postal_code"],
          "long_name": "10007"
        },
        {
          "short_name": "1868",
          "types": ["postal_code_suffix"],
          "long_name": "1868"
        }
      ],
      "types": ["street_address"],
      "geometry": {
        "viewport": {
          "northeast": {
            "lng": -74.0044642197085,
            "lat": 40.7156470802915
          },
          "southwest": {
            "lng": -74.0071621802915,
            "lat": 40.7129491197085
          }
        },
        "location_type": "ROOFTOP",
        "location": {
          "lng": -74.00581319999999,
          "lat": 40.7142981
        }
      }
    }
    /* SNIPED the rest */
  ]
}
// end::io-javascript-002[]

// tag::io-javascript-003[]
{
    "sid": "[sid value]",
    "date_created": "Sat, 09 Sep 2017 19:47:30 +0000",
    "date_updated": "Sat, 09 Sep 2017 19:47:30 +0000",
    "date_sent": null,
    "account_sid": "[sid value]",
    "to": "[to phone number]",
    "from": "[from phone number]",
    "messaging_service_sid": null,
    "body": "Sent from your Twilio trial account - Hello World",
    "status": "queued",
    "num_segments": "1",
    "num_media": "0",
    "direction": "outbound-api",
    "api_version": "2010-04-01",
    "price": null,
    "price_unit": "USD",
    "error_code": null,
    "error_message": null,
    "uri": "/2010-04-01/Accounts/[sid value]/Messages/SMe802d86b9f2246989c7c66e74b2d84ef.json",
    "subresource_uris": {
        "media": "/2010-04-01/Accounts/[sid value]/Messages/[message value]/Media.json"
    }
}
// end::io-javascript-003[]

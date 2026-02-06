/// XPath based expression language designed to assist in JSON/XML parsing/generating
///
/// The `com.codename1.processing.Result` class provides a subset of
/// [XPath](http://www.w3schools.com/xsl/xpath_intro.asp), but it is not limited to just XML
/// documents, it can also work with JSON documents, and even with raw `java.util.Map` objects.
///
/// As an example, we'll demonstrate how to process a response from the
/// [Google Reverse Geocoder API](https://developers.google.com/maps/documentation/geocoding/).
/// Lets start with this XML snippet:
///
/// ```java
///
///   OK
///
///
///           London
///           London
///           locality
///           political
///
///
///
///           Ontario
///           ON
///           administrative_area_level_1
///           political
///
///
///          Canada
///          CA
///          country
///          political
///
///
///
/// ```
///
/// We want to extract some of the data above into simpler string results. We can do this using:
///
/// ```java
/// Result result = Result.fromContent(input, Result.XML);
/// String country = result.getAsString("/result/address_component[type='country']/long_name");
/// String region = result.getAsString("/result/address_component[type='administrative_area_level_1']/long_name");
/// String city = result.getAsString("/result/address_component[type='locality']/long_name");
/// ```
///
/// If you are at all familiar with processing responses from webservices, you will notice that what would
/// normally require several lines of code of selecting and testing nodes in regular java can now be
/// done in a single line using the new path expressions.
///
/// In the code above, input can be any of:
///
///
/// - `java.lang.InputStream` directly from `com.codename1.io.ConnectionRequest#readResponse(java.io.InputStream)`
///
///
/// - XML or JSON document in the form of a `String`
///
/// - XML DOM `com.codename1.xml.Element` returned from `com.codename1.xml.XMLParser`
///
/// - JSON DOM `java.util.Map` returned from `com.codename1.io.JSONParser`
///
/// To use the expression processor when calling a webservice, you could use something like the following to
/// parse JSON (notice this is interchangeable between JSON and XML):
///
/// ```java
/// Form hi = new Form("Location", new BoxLayout(BoxLayout.Y_AXIS));
/// hi.add("Pinpointing Location");
/// Display.getInstance().callSerially(() -> {
///     Location l = Display.getInstance().getLocationManager().getCurrentLocationSync();
///     ConnectionRequest request = new ConnectionRequest("http://maps.googleapis.com/maps/api/geocode/json", false) {
///         private String country;
///         private String region;
///         private String city;
///         private String json;
///
/// @Override
///         protected void readResponse(InputStream input) throws IOException {
///                 Result result = Result.fromContent(input, Result.JSON);
///                 country = result.getAsString("/results/address_components[types='country']/long_name");
///                 region = result.getAsString("/results/address_components[types='administrative_area_level_1']/long_name");
///                 city = result.getAsString("/results/address_components[types='locality']/long_name");
///                 json = result.toString();
///         }
/// @Override
///         protected void postResponse() {
///             hi.removeAll();
///             hi.add(country);
///             hi.add(region);
///             hi.add(city);
///             hi.add(new SpanLabel(json));
///             hi.revalidate();
///         }
///     };
///     request.setContentType("application/json");
///     request.addRequestHeader("Accept", "application/json");
///     request.addArgument("sensor", "true");
///     request.addArgument("latlng", l.getLatitude() + "," + l.getLongitude());
///
///     NetworkManager.getInstance().addToQueue(request);
/// });
/// hi.show();
/// ```
///
/// The returned JSON looks something like this (notice it's snipped because the data is too long):
///
/// ```java
/// {
///   "status": "OK",
///   "results": [
///     {
///       "place_id": "ChIJJ5T9-iFawokRTPGaOginEO4",
///       "formatted_address": "280 Broadway, New York, NY 10007, USA",
///       "address_components": [
///         {
///           "short_name": "280",
///           "types": ["street_number"],
///           "long_name": "280"
///         },
///         {
///           "short_name": "Broadway",
///           "types": ["route"],
///           "long_name": "Broadway"
///         },
///         {
///           "short_name": "Lower Manhattan",
///           "types": [
///             "neighborhood",
///             "political"
///           ],
///           "long_name": "Lower Manhattan"
///         },
///         {
///           "short_name": "Manhattan",
///           "types": [
///             "sublocality_level_1",
///             "sublocality",
///             "political"
///           ],
///           "long_name": "Manhattan"
///         },
///         {
///           "short_name": "New York",
///           "types": [
///             "locality",
///             "political"
///           ],
///           "long_name": "New York"
///         },
///         {
///           "short_name": "New York County",
///           "types": [
///             "administrative_area_level_2",
///             "political"
///           ],
///           "long_name": "New York County"
///         },
///         {
///           "short_name": "NY",
///           "types": [
///             "administrative_area_level_1",
///             "political"
///           ],
///           "long_name": "New York"
///         },
///         {
///           "short_name": "US",
///           "types": [
///             "country",
///             "political"
///           ],
///           "long_name": "United States"
///         },
///         {
///           "short_name": "10007",
///           "types": ["postal_code"],
///           "long_name": "10007"
///         },
///         {
///           "short_name": "1868",
///           "types": ["postal_code_suffix"],
///           "long_name": "1868"
///         }
///       ],
///       "types": ["street_address"],
///       "geometry": {
///         "viewport": {
///           "northeast": {
///             "lng": -74.0044642197085,
///             "lat": 40.7156470802915
///           },
///           "southwest": {
///             "lng": -74.0071621802915,
///             "lat": 40.7129491197085
///           }
///         },
///         "location_type": "ROOFTOP",
///         "location": {
///           "lng": -74.00581319999999,
///           "lat": 40.7142981
///         }
///       }
///     }
///     /* SNIPED the rest */
///   ]
/// }
/// ```
///
/// The XML processor currently handles global selections by using a double slash anywhere within the
/// expression, for example:
///
/// ```java
/// // get all address_component names anywhere in the document with a type "political"
/// String array[] = result.getAsStringArray("//address_component[type='political']/long_name");
///
/// // get all types anywhere under the second result (dimension is 0-based)
/// String array[] = result.getAsStringArray("/result[1]//type");
/// ```
///
/// **NOTE:** Notice that Google's JSON webservice uses plural form for each of
/// the node names in that API (ie. results, address_components, and types) where they don't in the XML services
/// (ie result, address_component etc.).
///
/// Example 2
///
/// It also possible to do some more complex expressions. We'll use the following XML fragment for the next batch of
/// examples:
///
/// ```java
///
///
///         Bernard
///         Tomic
///         SOUTHPORT
///         QLD
///         1992-10-21
///
///
///         Mathew
///         Ebden
///         CHURCHLANDS
///         WA
///         1987-11-26
///
///
///        Lleyton
///        Hewitt
///        EXETER
///        SA
///        1981-02-24
///
///
///
/// ```
///
/// Above, if you want to select the IDs of all players that are ranked in the top 2, you can use an
/// expression like:
///
/// ```java
/// int top2[] = result.getAsIntegerArray("//player[@rank < 3]/@id");
/// ```
///
/// (Notice above that the expression is using an attribute for selecting both rank and id. In JSON
/// documents, if you attempt to select an attribute, it will look for a child node under the attribute name you ask
/// for).
///
/// If a document is ordered, you might want to select nodes by their position, for example:
///
/// ```java
/// String first2[] = result.getAsStringArray("//player[position() < 3]/firstname");
///
/// String secondLast = result.getAsString("//player[last() - 1]/firstName");
/// ```
///
/// It is also possible to select parent nodes, by using the '..' expression. For example:
///
/// ```java
/// int id = result.getAsInteger("//lastname[text()='Hewitt']/../@id");
/// ```
///
/// Above, we globally find a lastname element with a value of 'Hewitt', then grab the parent node of
/// lastname which happens to be the player node, then grab the id attribute from the player node.
/// Alternatively, you could get the same result from the following simpler statement:
///
/// ```java
/// int id = result.getAsInteger("//player[lastname='Hewitt']/@id");
/// ```
///
/// It is also possible to nest expressions, for example:
///
/// ```java
/// String id=result.getAsInteger("//player[//address[country/isocode='CA']]/@id");
/// ```
///
/// In the above example, if the player node had an address object, we'd be selecting all players from Canada.
/// This is a simple example of a nested expression, but they can get much more complex, which will be
/// required as the documents themselves get more complex.
///  Moving on, to select a node based
/// on the existence of an attribute:
///
/// ```java
/// int id[] = result.getAsIntegerArray("//player[@rank]/@id");
/// ```
///
/// Above, we selected the IDs of all ranked players. Conversely, we can select the non-ranked players like this:
///
/// ```java
/// int id[] = result.getAsIntegerArray("//player[@rank=null]/@id");
/// ```
///
/// (Logical not (!) operators currently are not implemented).
/// You can also select by the existence
/// of a child node.
///
/// ```java
/// int id[] = result.getAsIntegerArray("//player[middlename]/@id");
/// ```
///
///  Above, we selected all players that have a middle name.
///
/// Keep in mind that the Codename One path expression language is not a full implementation of
/// XPath 1.0, but does already handle many of the most useful features of the specification.
package com.codename1.processing;

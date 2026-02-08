---
title: "Expression Language for JSON and XML"
date: 2015-03-03
slug: "expression-language-for-json-and-xml"
---

# Expression Language for JSON and XML

Copied with permission from blog post by Eric Coolman, all rights reserved.

1. Home
2. Developers
3. Expression Language for JSON and XML

The com.codename1.processing.Result class provides a subset of XPath, but it is not limited to just XML documents, it can also work with JSON documents, and even with raw hashtables.

Example 1 As an example, we'll demonstrate how to process a response from the Google Reverse Geocoder API. The XML fragment below can be seen in full here, (or in JSON format here).

```
<?xml version="1.0" encoding="UTF-8"?>
  <GeocodeResponse>
    <status>OK</status> 
    <result> <!-- (irrelevant content removed) -->   
        <address_component>   
            <long_name>London</long_name>   
            <short_name>London</short_name>   
            <type>locality</type>   
            <type>political</type>  
        </address_component>
        <!-- (irrelevant content removed) -->  
       <address_component>   
            <long_name>Ontario</long_name>   
            <short_name>ON</short_name>   
            <type>administrative_area_level_1</type>   
            <type>political</type>  
        </address_component> 
        <address_component>   
           <long_name>Canada</long_name>   
           <short_name>CA</short_name>   
           <type>country</type>   
           <type>political</type>  
        </address_component>
```

Above, the data highlighted in red is the values I want to retrieve, and the data highlighted in blue are the unique qualifiers that I will use to select that data.

```
Result result = Result.fromContent(input, Result.XML);
String country = result.getAsString("/result/address_component[type='country']/long_name");
String region = result.getAsString("/result/address_component[type='administrative_area_level_1']/long_name");
String city = result.getAsString("/result/address_component[type='locality']/long_name");
```

If you are at all familiar with processing responses from webservices, you will notice that what would normally require several lines of code of selecting and testing nodes in regular java can now be done in a single line using the new path expressions.

In the code above, input can be any of:

- an InputStream directly from ConnectionRequest.readResponse()
- an XML or JSON document in the form of a string
- an XML DOM (Element) returned from XMLParser
- a JSON DOM (Hashtable) returned from JSONParser

To use the expression processor when calling a webservice, you could use something like the following:

```
public class GoogleReverseGeocoderService extends ConnectionRequest {
    private final static String BASEURL = "http://maps.googleapis.com/maps/api/geocode/json?sensor=true";
    private final static String PARAM_LATLNG="latlng";
    public GoogleReverseGeocoderService(double latitude, double longitude) {
           setUrl(BASEURL + '&' + PARAM_LATLNG + '=' + latitude + ',' + longitude);
           setContentType("application/json");
           addRequestHeader("Accept", "application/json");
           setPost(false);
     }
}
Location l = Display.getInstance().getLocationManager().getLocation();
ConnectionRequest request = new GoogleReverseGeocoderService(l.getLatitude(), l.getLongitude()) {
      protected void readResponse(InputStream input) throws IOException {
           Result result = Result.fromContent(input, Result.JSON); // ... expressions here
       }
};
NetworkManager.getInstance().addToQueue(request); 
```

It can also be instantiated with the result of one of the document parsers:

```
JSONParser parser = new JSONParser();
Result result = Result.fromContent(parser.parse(new InputStreamReader(input)));
```

The path processor is currently slightly more advanced for handling XML documents, but soon handling both JSON and XML will be exactly the same. For example, the XML processor currently handles global selections by using a double slash anywhere within the expression, for example:

```
// get all address_component names anywhere in the document with a type of political
String array[] = result.getAsStringArray("//address_component[type='political']/long_name");

// get all types anywhere under the second result (dimension is 0-based)
String array[] = result.getAsStringArray("/result[1]//type");
```

> **NOTE:** If you decide to try out the above code against Google’s JSON webservice, note that Google pluralizes each of the node names in that API (ie. results, address\_components, and types) where they don’t in the XML services.

## Example 2

It also possible to do some more complex expressions. We’ll use the following XML fragment for the next batch of examples:

```
<rankings type="aus" gender="male" date="2011-12-31">
    <player id="1036" coretennisid="6752" rank="1" delta="0" singlespoints="485000" doublespoints="675" deductedpoints="0" totalpoints="485675">
        <firstname>Bernard</firstname>
        <lastname>Tomic</lastname>
        <town>SOUTHPORT</town>
        <state>QLD</state>
        <dob>1992-10-21</dob>
    </player>
    <player id="2585" coretennisid="1500" rank="2" delta="0" singlespoints="313500" doublespoints="12630" deductedpoints="0" totalpoints="326130">
        <firstname>Mathew</firstname>    
        <lastname>Ebden</lastname>    
        <town>CHURCHLANDS</town>    
        <state>WA</state>    
        <dob>1987-11-26</dob>
    </player>
    <player id="6457" coretennisid="287" rank="3" delta="0" singlespoints="132500" doublespoints="1500" deductedpoints="0" totalpoints="134000">    
       <firstname>Lleyton</firstname>    
       <lastname>Hewitt</lastname>    
       <town>EXETER</town>    
       <state>SA</state>    
       <dob>1981-02-24</dob>
    </player>
    <!-- ... etc ... -->
</rankings>
```

Above, if you want to select the IDs of all players that are ranked in the top 2, you can use an expression like:

```
int top2[] = result.getAsIntegerArray("//player[@rank < 3]/@id");
```

(Notice above that the expression is using an attribute for selecting both rank and id. In JSON documents, if you attempt to select an attribute, it will look for a child node under the attribute name you ask for).

If a document is ordered, you might want to select nodes by their position, for example:

```
String first2[] = result.getAsStringArray("//player[position() < 3]/firstname");

String secondLast = result.getAsString(//player[last() - 1]/firstName);
```

It is also possible to select parent nodes, by using the ‘..’ expression. For example:

```
int id = result.getAsInteger("//lastname[text()='Hewitt']/../@id");
```

Above, we globally find a lastname element with a value of ‘Hewitt’, then grab the parent node of lastname which happens to be the player node, then grab the id attribute from the player node. Alternatively, you could get the same result from the following simpler statement:

```
int id = result.getAsInteger("//player[lastname='Hewitt']/@id");
```

It is also possible to nest expressions, for example:

```
String id=result.getAsInteger("//player[//address[country/isocode='CA']]/@id");
```

In the above example, if the player node had an address object, we’d be selecting all players from Canada. This is a simple example of a nested expression, but they can get much more complex, which will be required as the documents themselves get more complex. Moving on, to select a node based on the existence of an attribute:

```
int id[] = result.getAsIntegerArray("//player[@rank]/@id");
```

Above, we selected the IDs of all ranked players. Conversely, we can select the non-ranked players like this:

```
int id[] = result.getAsIntegerArray("//player[@rank=null]/@id");
```

(Logical not (!) operators currently are not implemented). You can also select by the existence of a child node.

```
int id[] = result.getAsIntegerArray("//player[middlename]/@id");
```

Above, we selected all players that have a middle name. Keep in mind that the Codename One path expression language is not a full implementation of XPath 1.0, but does already handle many of the most useful features of the specification. I hope you find it useful!

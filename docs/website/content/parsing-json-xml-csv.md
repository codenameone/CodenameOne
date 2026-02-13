---
title: "Parsing: JSON, XML & CSV"
date: 2015-03-03
slug: "parsing-json-xml-csv"
---

# Parsing: JSON, XML & CSV

Quick tutorial covering the builtin parsers in Codename One

1. [Home](/)
2. Developers
3. Parsing: JSON, XML & CSV

![](/uploads/parsing-json-xml--csv.png)

Codename One has several builtin parsers for JSON, XML & CSV formats which you can use to parse data from the internet or data that is shipping with your product. E.g. use the CSV data to setup default values for your application.

The parsers are all geared towards simplicity and small size, they don't validate and will fail in odd ways when faced with broken data.

CSV is probably the easiest to use, the "Comma Separated Values" format is just a list of values separated by commas (or some other character) with newlines to indicate another row in the table. These usually map well to an Excel spreadsheet or database table.

To parse a CSV just use the CSVParser class as such:

```
CSVParser parser = new CSVParser();
String[][] data = parser.read(stream);
```

The data array will contain a two dimensional array of the CSV data. You can change the delimiter character by using the CSVParser constructor that accepts a character.

The JSON "Java Script Object Notation" format is popular on the web for passing values to/from webservices since it works so well with JavaScript. Parsing JSON is just as easy but has two different variations. You can use the JSONParser class to build a tree of the JSON data as such:

```
JSONParser parser = new JSONParser();
Hashtable response = parser.parse(reader);
```

The response is a Hashtable containing a nested hierarchy of Vectors, Strings and numbers to represent the content of the submitted JSON. To extract the data from a specific path just iterate the Hashtable keys and recurs into it. Notice that there is a webservices demo as part of the kitchen sink showing the returned data as a Tree structure.

An alternative approach is to use the static data parse() method of the JSONParser class and implement a callback parser e.g.:

```
JSONParser.parse(reader, callback);
```

Notice that a static version of the method is used! The callback object is an instance of the JSONParseCallback interface which includes multiple methods. These will be invoked by the parser to indicate internal parser states in a similar way to a traditional XML SAX event based parser.

Advanced readers might want to dig deeper into the processing language contributed by Eric Coolman which allows for xpath like expressions when parsing JSON & XML. Read about it in Eric's blog.

Last but not least is the XML parser, to use it just create an instance of the XMLParser class and invoke parse:

```
XMLParser parser = new XMLParser();
Element elem = parser.parse(reader);
```

The element contains children and attributes and represents a tag element within the XML document or even the document itself. You can iterate over the XML tree to extract the data from within the XML file.

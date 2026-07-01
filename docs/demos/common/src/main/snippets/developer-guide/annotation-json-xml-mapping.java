// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::annotation-json-xml-mapping-java-001[]
package com.example;

import com.codename1.annotations.*;
import com.codename1.properties.*;

@Mapped
@XmlRoot("user")
public class User {

    @JsonProperty("first_name")
    @XmlElement("first")
    public String firstName;

    public int age;

    // Renders as <user role="admin"/> in XML; omitted from JSON.
    @XmlAttribute
    @JsonIgnore
    public String role;

    public User() { }                                  // <1>
}
// end::annotation-json-xml-mapping-java-001[]

// tag::annotation-json-xml-mapping-java-002[]
@Mapped
public class Item implements PropertyBusinessObject {
    public final Property<String, Item>  name = new Property<>("name");
    public final Property<Integer, Item> qty  = new Property<>("qty");

    private final PropertyIndex idx =
            new PropertyIndex(this, "Item", name, qty);

    public PropertyIndex getPropertyIndex() { return idx; }

    public Item() { }
}
// end::annotation-json-xml-mapping-java-002[]

// tag::annotation-json-xml-mapping-java-003[]
import com.codename1.mapping.Mappers;

User u = new User();
u.firstName = "Alice";
u.age = 30;

String json = Mappers.toJson(u);
//  -> {"first_name":"Alice","age":30}

User restored = Mappers.fromJson(json, User.class);

String xml = Mappers.toXml(u);
//  -> <user><first>Alice</first><age>30</age></user>

User fromXml = Mappers.fromXml(xml, User.class);
// end::annotation-json-xml-mapping-java-003[]

// tag::annotation-json-xml-mapping-java-004[]
Mappers.register(new Mapper<UUID>() {
    public Class<UUID> type() { return UUID.class; }
    public Map<String, Object> toMap(UUID u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("uuid", u.toString());
        return m;
    }
    public UUID fromMap(Map<String, Object> m) {
        return UUID.fromString((String) m.get("uuid"));
    }
    public String xmlRootName() { return "uuid"; }
    public void writeXml(UUID u, Element root) {
        root.addChild(new Element(u.toString(), true));
    }
    public UUID readXml(Element root) {
        return UUID.fromString(textOf(root));
    }
});
// end::annotation-json-xml-mapping-java-004[]

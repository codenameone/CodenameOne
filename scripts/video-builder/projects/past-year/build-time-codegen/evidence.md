# Evidence map

Source: `docs/website/content/blog/build-time-codegen.md`
Canonical: https://www.codenameone.com/blog/build-time-codegen/

## Thesis

One build-time code-generation pipeline for OpenAPI, ORM, mapping, SVG, and routing

## Supported beats

- **OpenAPI client generation:** A new cn1:generate-openapi-client Mojo reads an OpenAPI 3.x JSON spec (a URL or a local file) and writes typed Codename One client code that compiles into your app.
- **SQLite ORM:** The generated DAO does the typed work underneath. No reflection in insert; the generated code calls setString(1, e.title) and setLong(2, e.id) directly against the SQLite PreparedStatement.
- **JSON / XML mapping:** @Mapped marks a class as a transferable POJO. @JsonProperty and @XmlElement (plus @XmlRoot, @XmlAttribute, @JsonIgnore, @XmlTransient) shape the wire format. The runtime entry points are Mappers.toJson(...), Mappers.fromJson(...), Mappers.toXml(...), Mappers.fromXml(...).
- **Component binding with validation:** The fourth annotation processor on the same pipeline is the component binder. @Bindable marks a model class; @Bind(name = "userField") ties a field to a component on a form by the component's name. Field-level validation annotations compose with @Bind on the same field.
- **SVG at build time:** After the next build, every SVG is a regular Codename One Image. An SVG handled by the transcoder is a vector image, but it is still an Image.
- **Sizing in millimeters:** The SVG transcoder's most useful feature is also the one most easily missed: size every SVG in millimeters from CSS. SVGs in the wild routinely declare odd width / height attributes (a 1024×1024 export of a 24×24 icon, no dimensions at all, design-pixel values from one specific framework).

## Referenced evidence

- https://petstore3.swagger.io/api/v3/openapi.json</specUrl
- https://www.codenameone.com/developer-guide/#_appendix_goal_generate_openapi_client
- https://api.example.com/users/42
- https://api.example.com/users
- https://github.com/codenameone/CodenameOne/pull/5056
- https://github.com/codenameone/CodenameOne/issues
- https://www.codenameone.com/developer-guide/#_svg_transcoder
- https://www.codenameone.com/developer-guide/#_routing_and_deep_links

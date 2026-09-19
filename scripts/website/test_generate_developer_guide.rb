#!/usr/bin/env ruby
require 'tmpdir'
require_relative 'generate_developer_guide'

# Exercise the source parser and Markdown emitter together; no fixtures copied
# from current generated output, so include/ID/substitution regressions surface.
def check(condition, message)
  raise message unless condition
end

Dir.mktmpdir('guide-pages') do |dir|
  File.write(File.join(dir, 'book.adoc'), <<~ADOC)
    = Test book
    :doctype: book
    :icons: font

    = Topics

    include::First.adoc[]

    include::Second.adoc[]
  ADOC
  File.write(File.join(dir, 'First.adoc'), <<~ADOC)
    == First

    See <<destination>> and <<inline-anchor>>.

    [#source-section]
    === Code and images

    [source,java]
    ----
    String html = "href=\\"#not-an-anchor\\"";
    ----

    image::img/example.png[Example]

    NOTE: Keep this advice.

    TIP: A useful tip.

    CAUTION: Take care.

    IMPORTANT: Read this first.

    [#warning-anchor]
    .Before continuing
    [WARNING]
    ====
    Review <<destination>>.

    * Keep this list.

    TIP: Nested advice.
    ====

    Inline XML: `<profile id="example-not-anchor">` and `<a href="#example-link">`.


    A note.footnote:[Only the first chapter needs this definition.]
  ADOC
  File.write(File.join(dir, 'Second.adoc'), <<~ADOC)
    == Second

    [#destination]
    === A `code` heading

    [[inline-anchor]]Text with an anchor.

    |===
    |Name |Value
    |One |Two
    |===

    [source,java]
    ----
    call(); // <1>
    ----
    <1> A callout.
  ADOC
  guide = GuidePages.new(File.join(dir, 'book.adoc'), legacy: { 'anchors' => { 'old-destination' => 'destination' } })
  guide.write(dir)
  first = File.read(File.join(dir, 'content/developer-guide/chapters/first.md'))
  second = File.read(File.join(dir, 'content/developer-guide/chapters/second.md'))
  check(guide.chapters.map { |c| c['slug'] } == %w[first second], 'Book order lost')
  check(first.include?('/developer-guide/second/#destination'), 'Cross-chapter section link lost')
  check(first.include?('/developer-guide/second/#inline-anchor'), 'Inline anchor lost')
  check(guide.anchors['old-destination'] == '/developer-guide/second/#destination', 'Legacy fragment lost')
  check(!guide.anchors.key?('not-an-anchor'), 'Code was treated as an anchor')
  check(!guide.anchors.key?('example-not-anchor'), 'Inline XML was treated as an anchor')
  check(first.include?('&lt;a href="#example-link"&gt;'), 'Inline code link was rewritten')
  check(first.include?('"href=\\"#not-an-anchor\\""'), 'Code was rewritten as a link')
  check(first.include?('```java'), 'Plain code is not Markdown')
  check(first.include?('src="/developer-guide/img/example.png"'), 'Image path is chapter-relative')
  check(!first.include?('images/icons/'), 'Admonition tried to load an icon image')
  %w[Note Tip Caution Important Warning].each do |label|
    check(first.include?(%(role="img" aria-label="#{label}")), "#{label} icon has no accessible label")
  end
  check(first.scan('<svg ').length == 6, 'Top-level or nested admonition icon missing')
  check(!first.include?('<table'), 'Admonition still uses a layout table')
  check(first.include?('Before continuing') && first.include?('Keep this list.'), 'Complex admonition content lost')
  check(guide.anchors['warning-anchor'] == '/developer-guide/first/#warning-anchor', 'Admonition anchor lost')
  check(first.include?('Only the first chapter needs this definition.'), 'Footnote definition missing')
  check(!second.include?('Only the first chapter needs this definition.'), 'Footnotes leaked between chapters')
  check(second.include?('## A code heading {#destination}'), 'Heading ID changed')
  check(second.include?('<table'), 'Table lost')
  check(second.include?('A callout.'), 'Callout lost')
end
puts 'Guide generation tests passed.'

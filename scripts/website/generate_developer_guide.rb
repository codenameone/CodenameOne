#!/usr/bin/env ruby
# Asciidoctor owns includes, attributes, IDs and cross references. Hugo owns
# headings, page URLs and navigation. Rich AsciiDoc blocks retain their HTML.
require 'asciidoctor'
require 'cgi'
require 'json'
require 'fileutils'
require_relative 'guide_html_converter'

ROOT = File.expand_path('../..', __dir__)
SITE = File.join(ROOT, 'docs/website')

class GuidePages
  attr_reader :chapters, :anchors, :groups

  def initialize(source, legacy: JSON.parse(File.read(File.join(__dir__, 'guide-legacy-links.json'))))
    @legacy = legacy
    logger = Asciidoctor::MemoryLogger.new
    @doc = Asciidoctor.load_file(source, safe: :unsafe, sourcemap: true, logger: logger, converter: GuideHtmlConverter,
      attributes: { 'source-highlighter' => nil, 'sectnums!' => '', 'icons' => nil, 'tip-caption' => 'Tip' })
    @chapters, @anchors, @groups = [], {}, []
    @doc.sections.each do |part|
      group = { 'title' => plain(part.title), 'id' => part.id, 'chapters' => [] }
      (part.level == 0 ? part.sections : [part]).each do |chapter|
        slug = File.basename(chapter.source_location.file).sub(/\.(adoc|asciidoc)$/, '').downcase.gsub(/[^a-z0-9]+/, '-')
        raise "Duplicate chapter slug: #{slug}" if @chapters.any? { |c| c['slug'] == slug }
        entry = { 'title' => plain(chapter.title), 'slug' => slug,
          'url' => "/developer-guide/#{slug}/", 'id' => chapter.id,
          'group' => group['title'], 'node' => chapter }
        @chapters << entry
        group['chapters'] << entry.reject { |k, _| k == 'node' }
      end
      @groups << group
    end
    # Convert once, in book order, so numbered figures and footnotes keep their
    # original IDs. Collect rendered IDs as well as section IDs (inline anchors
    # and table/callout anchors do not all appear as AST blocks).
    @chapters.each do |entry|
      entry['body'] = render_blocks(entry['node'].blocks)
      @anchors[entry['id']] = entry['url'] + '#' + entry['id']
      ids = entry['body'].scan(/^\#{1,6} .*? \{#([^}]+)\}$/).flatten
      entry['body'].scan(/\{\{< guide-block >\}\}\n(.*?)\n\{\{< \/guide-block >\}\}/m).each do |html|
        ids.concat(html.first.scan(/<[a-zA-Z][^>]*?\sid="([^"]+)"/).flatten)
      end
      ids.each { |id| @anchors[id] ||= entry['url'] + '#' + id }
    end
    raise "AsciiDoc conversion errors: #{logger.messages.inspect}" if (logger.max_severity || 0) >= ::Logger::ERROR
    @groups.each { |g| @anchors[g['id']] ||= '/developer-guide/#' + g['id'] }
    legacy['anchors'].each do |old, current|
      raise "Unknown legacy anchor target: #{current}" unless @anchors[current]
      @anchors[old] = @anchors[current]
    end
  end

  def plain(text)
    CGI.unescapeHTML(text.gsub(/<[^>]+>/, ''))
  end

  def raw(html)
    # Raw HTML is scoped to this shortcode; no global Goldmark unsafe switch.
    "{{< guide-block >}}\n#{html}\n{{< /guide-block >}}\n\n"
  end

  def plain_source?(node)
    node.context == :listing && node.style == 'source' && !node.id && !node.title &&
      !node.source.match?(/\{\{[<%]/) && (node.subs - [:specialcharacters]).empty?
  end

  def render_blocks(blocks)
    blocks.map do |node|
      if node.context == :section
        "#{'#' * node.level} #{CGI.escapeHTML(plain(node.title))} {##{node.id}}\n\n" + render_blocks(node.blocks)
      elsif plain_source?(node)
        fence = '`' * [3, (node.source.scan(/`+/).map(&:length).max || 0) + 1].max
        "#{fence}#{node.attr('language')}\n#{node.source}\n#{fence}\n\n"
      else
        raw(node.convert)
      end
    end.join
  end

  def rewrite(text)
    text.gsub(/<[a-zA-Z][^>]*>/) do |tag|
      tag.gsub(/(href|src)="([^"]*)"/) do
        attr, url = $1, CGI.unescapeHTML($2)
        if url.start_with?('#') && @anchors[url[1..]]
          url = @anchors[url[1..]]
        elsif url.start_with?('img/')
          url = '/developer-guide/' + url
        elsif url =~ %r{\A(?:https?://(?:www\.)?codenameone\.com)?/(?:manual|developer-guide)(?:/|\.html)?#(.+)\z} && @anchors[$1]
          url = @anchors[$1]
        end
        "#{attr}=\"#{CGI.escapeHTML(url)}\""
      end
    end
  end

  def write(site)
    content = File.join(site, 'content/developer-guide/chapters')
    FileUtils.rm_rf(content)
    FileUtils.mkdir_p(content)
    FileUtils.mkdir_p(File.join(site, 'data'))
    FileUtils.mkdir_p(File.join(site, 'static/developer-guide'))
    @chapters.each_with_index do |entry, index|
      node = entry['node']
      meta = { 'title' => entry['title'], 'url' => entry['url'], 'layout' => 'guide-chapter',
        'weight' => index + 1, 'guide_id' => entry['id'], 'guide_group' => entry['group'],
        'guide_source' => node.source_location.file.delete_prefix(ROOT + '/'),
        'description' => "#{entry['title']} in the Codename One Developer Guide." }
      body = entry['body']
      # Footnote numbers are global in the source book; include only definitions
      # referenced by this chapter, with the original IDs and return links.
      refs = body.scan(/href="#_footnotedef_(\d+)"/).flatten.uniq
      notes = @doc.footnotes.select { |f| refs.include?(f.index.to_s) }.map do |f|
        "<div class=\"footnote\" id=\"_footnotedef_#{f.index}\"><a href=\"#_footnoteref_#{f.index}\">#{f.index}</a>. #{f.text}</div>"
      end
      body += raw('<div class="footnotes">' + notes.join("\n") + '</div>') unless notes.empty?
      File.write(File.join(content, entry['slug'] + '.md'), JSON.pretty_generate(meta) + "\n\n" + body.gsub(/(?<=\{\{< guide-block >\}\}\n).*?(?=\n\{\{< \/guide-block >\}\})/m) { |html| rewrite(html) })
    end
    File.write(File.join(site, 'data/developer-guide.json'), JSON.pretty_generate(@groups) + "\n")
    File.write(File.join(site, 'static/developer-guide/anchors.json'), JSON.generate(@anchors) + "\n")
    paths = %w[/manual /manual/ /developer-guide /developer-guide/ /developer-guide.html /developer-guide.html/].to_h { |path| [path, '/developer-guide/'] }
    @legacy.fetch('chapters', {}).each do |old, current|
      ['', '/', '.html', '.html/'].each { |suffix| paths["/manual/#{old}#{suffix}"] = "/developer-guide/#{current}/" }
    end
    File.write(File.join(site, 'data/guide-links.json'), JSON.generate({ 'anchors' => @anchors, 'paths' => paths }) + "\n")

    puts "Generated #{@chapters.length} guide chapters and #{@anchors.length} anchor routes."
  end
end

if $PROGRAM_NAME == __FILE__
  GuidePages.new(ARGV[0] || File.join(ROOT, 'docs/developer-guide/developer-guide.asciidoc')).write(ARGV[1] || SITE)
end

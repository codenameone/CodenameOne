#!/usr/bin/env ruby
# frozen_string_literal: true
#
# Fails when an AsciiDoc paragraph in the developer guide starts with a
# lowercase word. Catches the class of mistake from PR #5000 (a paragraph
# rendering as "many ways to animate…" with no leading subject).
#
# Vale cannot enforce this reliably because its asciidoc tokenizer fragments
# paragraphs at inline markup (`#kbd#`, code spans, links), making anchor
# regexes match on every inline-separated text run. We use the asciidoctor
# parser instead so paragraph boundaries match the rendered document.

require 'asciidoctor'
require 'json'
require 'optparse'

ALLOWED_FIRST_TOKENS = %w[
  iOS iPhone iPad iPod iCloud iMac iTunes
  macOS tvOS watchOS visionOS
  iframe
  eBay
].freeze

options = { output: nil }
OptionParser.new do |opts|
  opts.banner = 'Usage: check_paragraph_capitalization.rb [options] PATH [PATH ...]'
  opts.on('--output FILE', 'Write JSON report of all findings to FILE') do |f|
    options[:output] = f
  end
end.parse!

paths = ARGV
abort 'no input paths' if paths.empty?

# Each path is either a master document (with include:: directives) or a
# standalone file. We process the file as given; asciidoctor's source map
# preserves the originating file for each paragraph, so passing only the
# top-level developer-guide.asciidoc avoids double-counting paragraphs that
# appear via include::.
files = paths.flat_map do |path|
  if File.directory?(path)
    Dir.glob(File.join(path, '*.{adoc,asciidoc}')).sort
  else
    [path]
  end
end

def strip_html(html)
  text = html.gsub(/<[^>]+>/, '')
  text.gsub('&amp;', '&')
      .gsub('&lt;', '<')
      .gsub('&gt;', '>')
      .gsub('&quot;', '"')
      .gsub('&#39;', "'")
      .gsub('&nbsp;', ' ')
end

errors = []

files.each do |path|
  doc = Asciidoctor.load_file(
    path,
    sourcemap: true,
    safe: :unsafe,
    standalone: false,
    parse: true
  )

  doc.find_by(context: :paragraph).each do |para|
    next unless para.lineno

    rendered = para.content.to_s.strip
    next if rendered.empty?

    # Skip paragraphs that begin with a code identifier, keyboard shortcut,
    # link, or inline image. These are typically "`Name` — description"
    # pseudo-list entries where the leading element is an API symbol whose
    # case is determined by the language, not by prose conventions. We also
    # accept formatting wrappers (strong/em/b/i/mark/u/sub/sup) around the
    # identifier because asciidoctor sometimes preserves them — for example
    # `**\`a\` / \`b\`**` renders as `<strong><code>a</code> / <code>b</code></strong>`
    # while `**\`a\`**` collapses to `<code>a</code>`.
    next if rendered =~ %r{\A(?:<(?:strong|em|b|i|mark|u|sub|sup)>\s*)*<(code|kbd|samp|var|a\b|img\b|span class="image)}

    plain = strip_html(rendered).strip
    next if plain.empty?

    # One-word paragraphs are conventional transitional connectors between
    # adjacent code blocks ("becomes", "and", "to"). They aren't sentences,
    # so capitalization rules don't apply.
    next if plain.scan(/\S+/).length <= 1

    first_word = plain[/[A-Za-z][A-Za-z0-9]*/, 0]
    next if first_word.nil?
    next unless first_word[0].match?(/[a-z]/)
    next if ALLOWED_FIRST_TOKENS.any? { |w| w.casecmp(first_word).zero? }

    # Skip if the first "word" looks like a code identifier that asciidoctor
    # rendered as plain text (e.g. a fully-qualified package name without
    # backticks). Detection: the first token contains an internal dot or
    # camelCase boundary.
    extended_token = plain[/[A-Za-z][A-Za-z0-9._]*/, 0] || first_word
    next if extended_token.include?('.') || extended_token.match?(/[a-z][A-Z]/)

    source_file = para.file || path
    # Normalize to a repo-relative path when possible so reports are stable
    # across machines.
    if source_file && source_file.start_with?(Dir.pwd + '/')
      source_file = source_file.sub(Dir.pwd + '/', '')
    end

    errors << {
      file: source_file,
      line: para.lineno,
      word: first_word,
      excerpt: plain[0, 120]
    }
  end
end

if options[:output]
  payload = { total: errors.length, findings: errors }
  File.write(options[:output], JSON.pretty_generate(payload))
end

if errors.any?
  warn "Paragraph capitalization check failed: #{errors.length} paragraph(s) start with a lowercase word."
  errors.each do |e|
    warn "  #{e[:file]}:#{e[:line]}: '#{e[:word]}' — #{e[:excerpt]}"
  end
  warn ''
  warn 'Each flagged paragraph must be rewritten so its first prose word begins with a capital letter.'
  warn 'Example: "many ways to animate..." → "There are many ways to animate..."'
  exit 1
end

puts "Paragraph capitalization check passed: #{files.length} file(s), 0 issue(s)."
exit 0

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

options = { output: nil, baseline: nil, update_baseline: false }
OptionParser.new do |opts|
  opts.banner = 'Usage: check_paragraph_capitalization.rb [options] PATH [PATH ...]'
  opts.on('--output FILE', 'Write JSON report of all findings to FILE') do |f|
    options[:output] = f
  end
  opts.on('--baseline FILE', 'Ignore findings already listed in FILE; only new findings fail the build') do |f|
    options[:baseline] = f
  end
  opts.on('--update-baseline', 'Rewrite the baseline file to match current findings and exit 0') do
    options[:update_baseline] = true
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
    # case is determined by the language, not by prose conventions.
    next if rendered =~ /\A<(code|kbd|samp|var|a\b|img\b|span class="image)/

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

# Baseline support. Each baseline entry is a (file, word, excerpt) triple.
# We deliberately omit the line number because any edit above an unrelated
# paragraph would otherwise reclassify a pre-existing finding as "new".
def baseline_key(entry)
  [entry[:file] || entry['file'], entry[:word] || entry['word'], entry[:excerpt] || entry['excerpt']]
end

if options[:update_baseline]
  abort '--update-baseline requires --baseline FILE' unless options[:baseline]
  payload = {
    'generated_by' => 'scripts/developer-guide/check_paragraph_capitalization.rb',
    'note' => 'Pre-existing paragraph-capitalization findings. Regenerate with --update-baseline after fixing entries in the prose. New findings (not in this list) will fail CI.',
    'entries' => errors
  }
  File.write(options[:baseline], JSON.pretty_generate(payload) + "\n")
  puts "Baseline updated: #{options[:baseline]} (#{errors.length} entries)."
  exit 0
end

baseline_keys = []
if options[:baseline] && File.exist?(options[:baseline])
  data = JSON.parse(File.read(options[:baseline]))
  entries = data.is_a?(Hash) ? (data['entries'] || []) : data
  baseline_keys = entries.map { |e| baseline_key(e) }
end

new_errors = errors.reject { |e| baseline_keys.include?(baseline_key(e)) }

# Write a structured report so the CI summarizer can distinguish total
# findings from new findings.
if options[:output]
  payload = {
    total: errors.length,
    new: new_errors.length,
    baseline: baseline_keys.length,
    new_findings: new_errors,
    all_findings: errors
  }
  File.write(options[:output], JSON.pretty_generate(payload))
end

if new_errors.any?
  warn "Paragraph capitalization check failed: #{new_errors.length} new paragraph(s) start with a lowercase word."
  warn "(#{errors.length - new_errors.length} pre-existing finding(s) from the baseline are ignored.)" if errors.length != new_errors.length
  new_errors.each do |e|
    warn "  #{e[:file]}:#{e[:line]}: '#{e[:word]}' — #{e[:excerpt]}"
  end
  warn ''
  warn 'To fix: rewrite the flagged paragraph so its first prose word begins with a capital letter.'
  warn 'Example: "many ways to animate..." → "There are many ways to animate..."'
  warn 'If the finding is a legitimate exception, add a baseline entry by running:'
  warn "  ruby scripts/developer-guide/check_paragraph_capitalization.rb --baseline #{options[:baseline] || 'BASELINE.json'} --update-baseline docs/developer-guide/developer-guide.asciidoc"
  exit 1
end

if errors.any?
  puts "Paragraph capitalization check passed: #{errors.length} pre-existing finding(s) acknowledged by the baseline, 0 new finding(s)."
else
  puts "Paragraph capitalization check passed: #{files.length} file(s), 0 issue(s)."
end
exit 0

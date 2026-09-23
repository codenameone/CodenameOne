require 'asciidoctor/converter/html5'
require 'cgi'

# Use the same converter for nested blocks, so callouts inside lists and other
# containers also get icons without a layout table or an external icon font.
class GuideHtmlConverter < Asciidoctor::Converter::Html5Converter
  ICONS = {
    'note' => '<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7v.01"/>',
    'tip' => '<path d="M9 18h6M10 21h4M9 15c0-2-4-3-4-7a7 7 0 0 1 14 0c0 4-4 5-4 7v1H9z"/>',
    'warning' => '<path d="m12 3 10 18H2L12 3zM12 9v5M12 17v.01"/>',
    'caution' => '<path d="m8 3-5 5v8l5 5h8l5-5V8l-5-5H8zM12 7v6M12 17v.01"/>',
    'important' => '<path d="m12 2 10 10-10 10L2 12 12 2zM12 7v6M12 17v.01"/>'
  }.freeze

  def convert_admonition(node)
    name = node.attr('name')
    id = node.id ? %( id="#{CGI.escapeHTML(node.id)}") : ''
    classes = ['admonitionblock', name, node.role].compact.join(' ')
    label = CGI.escapeHTML(node.attr('textlabel'))
    title = node.title? ? %(<div class="title">#{node.title}</div>\n) : ''
    %(<div#{id} class="#{CGI.escapeHTML(classes)}">
<span class="cn1-guide-admonition-icon" role="img" aria-label="#{label}" title="#{label}">
<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">#{ICONS.fetch(name)}</svg>
</span>
<div class="content">#{title}#{node.content}</div>
</div>)
  end
end

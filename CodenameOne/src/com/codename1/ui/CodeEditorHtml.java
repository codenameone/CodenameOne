/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

/// Holds the self contained HTML/JS implementation of the `CodeEditor` built-in editing surface. Kept
/// in its own class so the large literal does not clutter the editor component. The page exposes the
/// `window.cn1editor` command/query bridge used by `AbstractEditorComponent` and supports syntax
/// highlighting, a line-number gutter, code completion, diagnostics (squiggly underlines + gutter
/// markers + tooltips), bracket auto-close and an active-line highlight.
final class CodeEditorHtml {
    private CodeEditorHtml() {
    }

    // Assigned in a static initializer (not at the declaration) so this large string is a runtime
    // constant rather than a compile-time constant - that keeps the compiler from inlining the whole
    // ~13KB literal into every class that references CodeEditorHtml.PAGE.
    static final String PAGE;

    static {
        PAGE =
            "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no\">"
            + "<meta name=\"color-scheme\" content=\"light\">"
            + "<style>"
            + "html,body{margin:0;padding:0;height:100%;background:#ffffff;}"
            + "*{box-sizing:border-box;-webkit-tap-highlight-color:rgba(0,0,0,0);}"
            + "#host{position:absolute;left:0;top:0;right:0;bottom:0;display:flex;overflow:auto;"
            + "font:14px/1.5 Menlo,Consolas,'Courier New',monospace;background:#ffffff;color:#24292e;}"
            + "#gutter{position:sticky;left:0;flex:0 0 auto;min-width:46px;text-align:right;padding:8px 8px 8px 6px;"
            + "color:#b0b6bd;background:#f6f8fa;user-select:none;white-space:pre;z-index:2;}"
            + "#gutter .g{display:block;}"
            + "#gutter .ge{color:#d73a49;font-weight:bold;}#gutter .gw{color:#e36209;font-weight:bold;}"
            + "#ed{flex:1 1 auto;margin:0;padding:8px;outline:0;white-space:pre;min-height:100%;"
            + "tab-size:4;-moz-tab-size:4;caret-color:#24292e;}"
            + "#ed .kw{color:#d73a49;}#ed .st{color:#032f62;}#ed .cm{color:#6a737d;font-style:italic;}#ed .nu{color:#005cc5;}"
            + "#ed .dg-error{text-decoration:underline wavy #e51400;text-decoration-skip-ink:none;}"
            + "#ed .dg-warning{text-decoration:underline wavy #e36209;text-decoration-skip-ink:none;}"
            + "#ed .dg-info{text-decoration:underline dotted #1a73e8;}"
            + "body[data-theme=dark] #host{background:#1e1e1e;color:#d4d4d4;}"
            + "body[data-theme=dark] #gutter{background:#252526;color:#6e7681;}"
            + "body[data-theme=dark] #ed{caret-color:#d4d4d4;}"
            + "body[data-theme=dark] #ed .kw{color:#569cd6;}body[data-theme=dark] #ed .st{color:#ce9178;}"
            + "body[data-theme=dark] #ed .cm{color:#6a9955;}body[data-theme=dark] #ed .nu{color:#b5cea8;}"
            + "body[data-theme=dark] #gutter .ge{color:#f48771;}body[data-theme=dark] #gutter .gw{color:#cca700;}"
            + "#pop{position:fixed;z-index:99;display:none;max-height:190px;overflow-y:auto;min-width:150px;"
            + "background:#fff;border:1px solid #c8cdd2;border-radius:6px;box-shadow:0 4px 14px rgba(0,0,0,.18);"
            + "font:13px/1.4 -apple-system,Roboto,sans-serif;}"
            + "body[data-theme=dark] #pop{background:#252526;border-color:#3c3c3c;color:#d4d4d4;}"
            + ".pi{padding:4px 10px;cursor:pointer;white-space:nowrap;display:flex;gap:8px;align-items:baseline;}"
            + ".pi .pt{color:#8a929a;font-size:11px;margin-left:auto;}"
            + ".pi.sel{background:#0a66c2;color:#fff;}.pi.sel .pt{color:#dbe7f5;}"
            + "</style></head><body>"
            + "<div id=\"host\"><div id=\"gutter\"><span class=\"g\">1</span></div><code id=\"ed\" contenteditable=\"true\" spellcheck=\"false\" autocapitalize=\"off\" autocorrect=\"off\"></code></div>"
            + "<div id=\"pop\"></div>"
            + "<script>"
            + "var host=document.getElementById('host'),ed=document.getElementById('ed'),gutter=document.getElementById('gutter'),pop=document.getElementById('pop');"
            + "var lang='text',tabSize=4,completionEnabled=false,composing=false,reqSeq=0,lastReq=0,popItems=[],popSel=0,debTimer=null;"
            + "var diags=[],diagByLine={};"
            + "var KW={"
            + "java:'abstract assert boolean break byte case catch char class const continue default do double else enum extends final finally float for goto if implements import instanceof int interface long native new package private protected public return short static strictfp super switch synchronized this throw throws transient try void volatile while true false null var record sealed',"
            + "kotlin:'as break by class continue do else false for fun if in interface is null object package return super this throw true try typealias val var when while abstract final open override private protected public internal companion data sealed suspend',"
            + "javascript:'await async break case catch class const continue debugger default delete do else export extends false finally for function if import in instanceof let new null return super switch this throw true try typeof var void while with yield of static get set',"
            + "python:'and as assert async await break class continue def del elif else except False finally for from global if import in is lambda None nonlocal not or pass raise return True try while with yield self',"
            + "css:'important inherit initial unset auto none flex grid block inline absolute relative fixed static',"
            + "json:'true false null',"
            + "xml:'',"
            + "c:'auto break case char const continue default do double else enum extern float for goto if int long register return short signed sizeof static struct switch typedef union unsigned void volatile while'"
            + "};"
            + "var CLOSE={'(' :')','[':']','{':'}'};var QUOTE={'\"':1,\"'\":1,'`':1};"
            + "function kwmap(l){var s=KW[l]||'';var m={};var a=s.split(' ');for(var i=0;i<a.length;i++){if(a[i])m[a[i]]=1;}return m;}"
            + "function cn1post(m){try{if(window.cn1PostMessage){window.cn1PostMessage(m);}else if(window.parent&&window.parent!==window){window.parent.postMessage(m,'*');}}catch(e){}}"
            + "function fire(t,v){cn1post('cn1ed:'+t+(v==null?'':(':'+v)));}"
            + "function esc(s){return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}"
            + "function span(c,t){return '<span class=\"'+c+'\">'+esc(t)+'</span>';}"
            + "function spanML(c,t){var p=t.split('\\n');for(var i=0;i<p.length;i++){p[i]=p[i].length?span(c,p[i]):'';}return p.join('\\n');}"
            + "function isIdStart(c){return (c>='a'&&c<='z')||(c>='A'&&c<='Z')||c=='_'||c=='$';}"
            + "function isId(c){return isIdStart(c)||(c>='0'&&c<='9');}"
            + "function highlight(code){"
            + "var k=kwmap(lang);var out='';var i=0;var n=code.length;"
            + "while(i<n){"
            + "var c=code.charAt(i);var two=code.substr(i,2);"
            + "if(two=='/*'){var j=code.indexOf('*/',i+2);if(j<0){j=n;}else{j+=2;}out+=spanML('cm',code.substring(i,j));i=j;continue;}"
            + "if(two=='//'&&lang!='text'&&lang!='python'){var j=code.indexOf('\\n',i);if(j<0){j=n;}out+=span('cm',code.substring(i,j));i=j;continue;}"
            + "if(c=='#'&&(lang=='python'||lang=='shell'||lang=='ruby')){var j=code.indexOf('\\n',i);if(j<0){j=n;}out+=span('cm',code.substring(i,j));i=j;continue;}"
            + "if(c=='\"'||c=='\\''||c=='`'){var j=i+1;while(j<n){var d=code.charAt(j);if(d=='\\\\'){j+=2;continue;}if(d==c){j++;break;}if(d=='\\n'&&c!='`'){break;}j++;}out+=spanML('st',code.substring(i,j));i=j;continue;}"
            + "if(c>='0'&&c<='9'){var j=i+1;while(j<n&&(isId(code.charAt(j))||code.charAt(j)=='.')){j++;}out+=span('nu',code.substring(i,j));i=j;continue;}"
            + "if(isIdStart(c)){var j=i+1;while(j<n&&isId(code.charAt(j))){j++;}var w=code.substring(i,j);out+=(k[w]?span('kw',w):esc(w));i=j;continue;}"
            + "out+=esc(c);i++;"
            + "}"
            + "return out;"
            + "}"
            + "function applyDiagLines(html){"
            + "if(!diags.length){return html;}"
            + "var lines=html.split('\\n');"
            + "for(var ln in diagByLine){var idx=parseInt(ln)-1;if(idx>=0&&idx<lines.length){var d=diagByLine[ln];lines[idx]='<span class=\"dg-'+d.s+'\" title=\"'+esc(d.m)+'\">'+lines[idx]+'</span>';}}"
            + "return lines.join('\\n');"
            + "}"
            + "function caretOffset(){var sel=window.getSelection();if(!sel||sel.rangeCount==0){return 0;}var r=sel.getRangeAt(0);var pre=r.cloneRange();pre.selectNodeContents(ed);pre.setEnd(r.endContainer,r.endOffset);return pre.toString().length;}"
            + "function setCaret(pos){var w=document.createTreeWalker(ed,NodeFilter.SHOW_TEXT,null,false);var n,rem=pos,found=null,off=0;while((n=w.nextNode())){var len=n.nodeValue.length;if(rem<=len){found=n;off=rem;break;}rem-=len;}var r=document.createRange();if(found){r.setStart(found,off);}else{r.selectNodeContents(ed);r.collapse(false);}r.collapse(true);var sel=window.getSelection();sel.removeAllRanges();sel.addRange(r);}"
            + "function syncGutter(){var lines=ed.textContent.split('\\n').length;var s='';for(var i=1;i<=lines;i++){var d=diagByLine[i];var cls=d?(d.s=='error'?' ge':(d.s=='warning'?' gw':'')):'';var tt=d?(' title=\"'+esc(d.m)+'\"'):'';s+='<span class=\"g'+cls+'\"'+tt+'>'+i+'</span>';}gutter.innerHTML=s;}"
            + "function rehighlight(){var pos=caretOffset();ed.innerHTML=applyDiagLines(highlight(ed.textContent));setCaret(pos);syncGutter();}"
            + "function onInput(){if(composing){fire('change',null);return;}rehighlight();fire('change',null);scheduleCompletion();}"
            + "ed.addEventListener('compositionstart',function(){composing=true;});"
            + "ed.addEventListener('compositionend',function(){composing=false;rehighlight();fire('change',null);});"
            + "ed.addEventListener('input',onInput);"
            + "ed.addEventListener('paste',function(e){e.preventDefault();var t=(e.clipboardData||window.clipboardData).getData('text');document.execCommand('insertText',false,t);});"
            + "function nextChar(){var pos=caretOffset();var t=ed.textContent;return pos<t.length?t.charAt(pos):'';}"
            + "ed.addEventListener('keydown',function(e){"
            + "if(pop.style.display=='block'){"
            + "if(e.key=='ArrowDown'){e.preventDefault();movePop(1);return;}"
            + "if(e.key=='ArrowUp'){e.preventDefault();movePop(-1);return;}"
            + "if(e.key=='Enter'||e.key=='Tab'){e.preventDefault();acceptPop();return;}"
            + "if(e.key=='Escape'){e.preventDefault();hidePop();return;}"
            + "}"
            + "if(CLOSE[e.key]){e.preventDefault();var cl=CLOSE[e.key];document.execCommand('insertText',false,e.key+cl);var p=caretOffset();setCaret(p-1);return;}"
            + "if(QUOTE[e.key]){if(nextChar()==e.key){e.preventDefault();var p2=caretOffset();setCaret(p2+1);return;}e.preventDefault();document.execCommand('insertText',false,e.key+e.key);var p3=caretOffset();setCaret(p3-1);return;}"
            + "if((e.key==')'||e.key==']'||e.key=='}')&&nextChar()==e.key){e.preventDefault();var p4=caretOffset();setCaret(p4+1);return;}"
            + "if(e.key=='Tab'){e.preventDefault();var sp='';for(var i=0;i<tabSize;i++){sp+=' ';}document.execCommand('insertText',false,sp);return;}"
            + "if(e.key=='Enter'){e.preventDefault();var ind=curIndent();document.execCommand('insertText',false,'\\n'+ind);return;}"
            + "if((e.ctrlKey||e.metaKey)&&(e.key==' '||e.code=='Space')){e.preventDefault();requestCompletion();return;}"
            + "});"
            + "function curIndent(){var pos=caretOffset();var t=ed.textContent;var ls=t.lastIndexOf('\\n',pos-1)+1;var ind='';for(var i=ls;i<t.length;i++){var c=t.charAt(i);if(c==' '||c=='\\t'){ind+=c;}else{break;}}return ind;}"
            + "function scheduleCompletion(){if(!completionEnabled){return;}if(debTimer){clearTimeout(debTimer);}debTimer=setTimeout(requestCompletion,140);}"
            + "function requestCompletion(){if(!completionEnabled){return;}reqSeq++;lastReq=reqSeq;fire('complete',lastReq+':'+caretOffset());}"
            + "function curPrefix(){var pos=caretOffset();var t=ed.textContent;var s=pos;while(s>0&&isId(t.charAt(s-1))){s--;}return t.substring(s,pos);}"
            + "function showPop(rid,items){if(rid<lastReq){return;}var pre=curPrefix();var filt=[];for(var i=0;i<items.length;i++){var d=items[i].d||'';if(!pre||d.toLowerCase().indexOf(pre.toLowerCase())>=0){filt.push(items[i]);}}popItems=filt;if(filt.length==0){hidePop();return;}popSel=0;renderPop();positionPop();}"
            + "function renderPop(){var h='';for(var i=0;i<popItems.length;i++){var it=popItems[i];h+='<div class=\"pi'+(i==popSel?' sel':'')+'\" data-i=\"'+i+'\">'+'<span>'+esc(it.d||'')+'</span>'+(it.x?'<span class=\"pt\">'+esc(it.x)+'</span>':(it.t?'<span class=\"pt\">'+esc(it.t)+'</span>':''))+'</div>';}pop.innerHTML=h;pop.style.display='block';var items=pop.querySelectorAll('.pi');for(var j=0;j<items.length;j++){items[j].addEventListener('mousedown',function(ev){ev.preventDefault();popSel=parseInt(this.getAttribute('data-i'));acceptPop();});}}"
            + "function positionPop(){var sel=window.getSelection();if(!sel||sel.rangeCount==0){return;}var rects=sel.getRangeAt(0).getClientRects();var r=rects.length?rects[rects.length-1]:ed.getBoundingClientRect();var x=r.left,y=r.bottom;if(y+pop.offsetHeight>window.innerHeight){y=r.top-pop.offsetHeight;}if(x+pop.offsetWidth>window.innerWidth){x=window.innerWidth-pop.offsetWidth-4;}pop.style.left=Math.max(0,x)+'px';pop.style.top=Math.max(0,y)+'px';}"
            + "function movePop(d){popSel=(popSel+d+popItems.length)%popItems.length;var items=pop.querySelectorAll('.pi');for(var i=0;i<items.length;i++){if(i==popSel){items[i].className='pi sel';items[i].scrollIntoView({block:'nearest'});}else{items[i].className='pi';}}}"
            + "function hidePop(){pop.style.display='none';popItems=[];}"
            + "function acceptPop(){if(popSel<0||popSel>=popItems.length){hidePop();return;}var ins=popItems[popSel].i||popItems[popSel].d||'';var pos=caretOffset();var t=ed.textContent;var s=pos;while(s>0&&isId(t.charAt(s-1))){s--;}var nt=t.substring(0,s)+ins+t.substring(pos);ed.textContent=nt;rehighlight();setCaret(s+ins.length);hidePop();fire('change',null);}"
            + "function setDiagnostics(arr){diags=arr||[];diagByLine={};for(var i=0;i<diags.length;i++){var d=diags[i];var s=d.s||'error';for(var l=d.l;l<=(d.el||d.l);l++){var ex=diagByLine[l];if(!ex||sev(s)>sev(ex.s)){diagByLine[l]={s:s,m:d.m||''};}else if(ex&&d.m){ex.m=ex.m?(ex.m+'\\n'+d.m):d.m;}}}rehighlight();}"
            + "function sev(s){return s=='error'?3:(s=='warning'?2:1);}"
            + "window.cn1editor={"
            + "cmd:function(name,arg){"
            + "switch(name){"
            + "case 'setText':ed.textContent=arg||'';rehighlight();break;"
            + "case 'insertText':document.execCommand('insertText',false,arg||'');break;"
            + "case 'setLanguage':lang=arg||'text';rehighlight();break;"
            + "case 'setTheme':document.body.setAttribute('data-theme',arg||'light');break;"
            + "case 'setLineNumbers':gutter.style.display=(arg=='1')?'block':'none';break;"
            + "case 'setTabSize':tabSize=parseInt(arg)||4;ed.style.tabSize=tabSize;ed.style.MozTabSize=tabSize;break;"
            + "case 'setEditable':ed.setAttribute('contenteditable',arg=='1'?'true':'false');break;"
            + "case 'setCompletionEnabled':completionEnabled=(arg=='1');if(!completionEnabled){hidePop();}break;"
            + "case 'setDiagnostics':try{setDiagnostics(JSON.parse(arg||'[]'));}catch(e){}break;"
            + "case 'showCompletions':var ci=arg.indexOf(':');var rid=parseInt(arg.substring(0,ci));var items;try{items=JSON.parse(arg.substring(ci+1));}catch(e){items=[];}showPop(rid,items);break;"
            + "case 'focus':ed.focus();break;"
            + "case 'blur':ed.blur();hidePop();break;"
            + "default:break;"
            + "}},"
            + "query:function(name,arg){"
            + "switch(name){"
            + "case 'getText':return ed.textContent;"
            + "case 'getCursor':return ''+caretOffset();"
            + "default:return '';"
            + "}}};"
            + "syncGutter();"
            + "fire('ready',null);"
            + "</script></body></html>";
    }
}

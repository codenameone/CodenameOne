
// This flag affects drawString.  If it is true, then drawString will use the alphabetic text baseline
// Otherwise it will use the top text baseline.  We used to use the top text baseline,
// but there are inconsistencies between Firefox and Chrome.  Chrome seems to add an offset.
// If we use the alphabetic baseline and simply add the ascent and leading, then we 
// get more consistent results.
// I am leaving this as a javacript flag to make it easier to toggle and experiment
// at runtime.
window.cn1_use_baseline_text_rendering =true;
/*window.cn1_debug_flags = window.cn1_debug_flags || {};
window.cn1_debug_flags.debugLog = true;
setTimeout(function() {
	window.cn1_debug_flags.debugLog = true;
}, 1000);*/
(function () {

  if ( typeof window.CustomEvent === "function" ) return false;

  function CustomEvent ( event, params ) {
    params = params || { bubbles: false, cancelable: false, detail: undefined };
    var evt = document.createEvent( 'CustomEvent' );
    evt.initCustomEvent( event, params.bubbles, params.cancelable, params.detail );
    return evt;
   }

  CustomEvent.prototype = window.Event.prototype;

  window.CustomEvent = CustomEvent;
})();

window.cn1_escape_single_quotes = function(str) {
    return String(str).replace(/\\/g, "\\\\").replace(/'/g, "\\'");
};
window.cn1NativeBacksideHooks = [];
window.cn1RunOnMainThread = function(callback) {
    window.cn1NativeBacksideHooks.push(callback);
};

(function() {
    // This section defines a cn1RunPrivileged() method which is a hack that allows us to run a function with MEI 
    // approval in iOS and Safari.
    // This may also be useful on Android but haven't tested yet.
    var isSafari = _isSafari();
    function _isSafari() {
        var ua = navigator.userAgent.toLowerCase();
        if (ua.indexOf('safari') != -1) {
            if (ua.indexOf('chrome') > -1) {
                //return false;
            } else {
                return true;
            }
        }
        return false;
    }

    function isIOS() {

    	return (/iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream) ||  (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
    }
    // Default runPrivileged just runs the function directly.
    window.cn1RunPrivileged = function(callback) {
    	callback();
    };
    
    // If this isn't iOS and Safari, we don't need any of the rest.
    if (!isSafari && !isIOS()) return;
    
    // 
    var unlockedSilentAudio;
    var unlockedSilentAudioPlayQueue = [];
    
    // Attempt to unlock an audio clip
    function unlockAudioClip(audio) {
        try {
            audio.setAttribute('data-cn1-unlocked', 'true');
            var testPlay = audio.play();
            if (testPlay && typeof Promise !== 'undefined' && (testPlay instanceof Promise || typeof testPlay.then === 'function')) {
                testPlay.then(function() {
                }, function(err) {
                    if (err.name=='NotAllowedError' || err.name == 'AbortError') {
                        unlockedSilentAudio = null;
                    }
                });
            }
        } catch (err) {

        }

    }
    function unlockAudio() {
    	try {
            // Set flag to indicate that we're unlocking clips
            // Used in HTMLMediaElement.play() method override to know not to do anything
            window.cn1UnlockingClips = true;
            
            // Remove all event handlers that were set up to unlock the audio.
            document.removeEventListener('touchstart', unlockAudio, true);
            document.removeEventListener('touchend', unlockAudio, true);
            document.removeEventListener('click', unlockAudio, true);
            window.removeEventListener('installbacksidehooks', unlockAudio, true);

            if (!unlockedSilentAudio) {
                initSilentAudio();
                unlockAudioClip(unlockedSilentAudio);
            }
    	} finally {
            window.cn1UnlockingClips = false;
    	}
        
    };
    
    // Add listeners to unlock the silent audio clip.  
    // We need the clip to be unlocked in an event to get around MEI restrictions
    document.addEventListener('touchstart', unlockAudio, true);
    document.addEventListener('touchend', unlockAudio, true);
    document.addEventListener('click', unlockAudio, true);
    
    // the installbacksidehooks event is a synthetic event that is intercepted
    // by CN1 to install backside hooks during valid user event.
    window.addEventListener('installbacksidehooks', unlockAudio, true);
	
    function initSilentAudio() {
        if (!unlockedSilentAudio) {
            unlockedSilentAudio = new Audio();
            unlockedSilentAudio.src = cn1CreateSilentAudio(0.1);
            unlockedSilentAudio.addEventListener('playing', function() {
               ///console.log("Unlocked silent audio is playing");
            }, true);

            unlockedSilentAudio.addEventListener('ended', function() {
                //console.log("Silend audio ended");
                while (unlockedSilentAudioPlayQueue.length > 0) {
                    var f = unlockedSilentAudioPlayQueue.pop();
                    setTimeout(f,0);
                }
            }, true);
        }
    }
	

    // Assign runPrivileged to global cn1RunPrivileged method
    window.cn1RunPrivileged = runPrivileged;
    
    // Function ro run callback in the 'ended' listener of the silent audio
    // clip.  Since the silent audio clip is unlocked, the ended event
    // will run with loosened MEI restrictions.
    // THis is a bit of a hack, but it seems to work.
    function runPrivileged(callback) {
        if (!unlockedSilentAudio) {
                callback();
                return;
        }
        var complete = false;
        var timeout = setTimeout(function() {
            if (complete) return;
            complete = true;
            console.log('WARNING: runPrivileged() reached timeout', callback);
            callback();
        }, 1000);
        unlockedSilentAudioPlayQueue.push(function() {
            if (complete) return;
            complete = true;
            clearTimeout(timeout);
            callback();
        });
        unlockedSilentAudio.play();
        
    }

	// We want to override the regular HTMLMediaElement.play() method
        // so that it runs privileged.  But we only want to do this for CN1 media clips.
	var origPlay = HTMLMediaElement.prototype.play;
	var alreadyMadeFirstPlay = false;

	var newPlay = function() {
            if (window.cn1UnlockingClips || !unlockedSilentAudio || (unlockedSilentAudio && this === unlockedSilentAudio)) {
                //console.log("HTMLMediaElement.play| using origPlay to play media because unlocking in progress, or there is no unlockedSilentAudio, or this is the unlockedSilentAudio", this);
                return origPlay.apply(this);
            }
            if (this.getAttribute('data-cn1-unlocked')) {
                console.log("HTMLMedia.play|already-unlocked", this);
                return origPlay.apply(this).then(function() {
                    console.log("HTMLMedia.play|already-unlocked >> successful play");
                });
            }
           

            var mediaId = this.getAttribute('cn1-audio-id');
            var video = this.tagName.toLowerCase() === 'video';
            console.log("HTMLMediaElement.play", this);
            if (video && !alreadyMadeFirstPlay) {
                console.log("HTMLMediaElement.play| using origPlay to play video it could be the 'test' play.", this);
                alreadyMadeFirstPlay = true;
                return origPlay.apply(this);
            }

            if (!video && !mediaId) {
                console.log("HTMLMediaElement.play| using origPlay to play audio because element has no mediaId attribute ", this);
                return origPlay.apply(this);
            }



            var self = this;
            alreadyMadeFirstPlay = true;

            //console.log("Requesting to play clip", this);
            return new Promise(function(resolve, reject) {
                runPrivileged(function() {
                    origPlay.apply(self).then(resolve, reject);
                });

            });
		

	}
	
	HTMLMediaElement.prototype.play = newPlay;
	
})();

/*
 * Copyright 2016 Small Batch, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/* Web Font Loader v1.6.26 - (c) Adobe Systems, Google. License: Apache 2.0 */(function(){function aa(a,b,c){return a.call.apply(a.bind,arguments)}function ba(a,b,c){if(!a)throw Error();if(2<arguments.length){var d=Array.prototype.slice.call(arguments,2);return function(){var c=Array.prototype.slice.call(arguments);Array.prototype.unshift.apply(c,d);return a.apply(b,c)}}return function(){return a.apply(b,arguments)}}function p(a,b,c){p=Function.prototype.bind&&-1!=Function.prototype.bind.toString().indexOf("native code")?aa:ba;return p.apply(null,arguments)}var q=Date.now||function(){return+new Date};function ca(a,b){this.a=a;this.m=b||a;this.c=this.m.document}var da=!!window.FontFace;function t(a,b,c,d){b=a.c.createElement(b);if(c)for(var e in c)c.hasOwnProperty(e)&&("style"==e?b.style.cssText=c[e]:b.setAttribute(e,c[e]));d&&b.appendChild(a.c.createTextNode(d));return b}function u(a,b,c){a=a.c.getElementsByTagName(b)[0];a||(a=document.documentElement);a.insertBefore(c,a.lastChild)}function v(a){a.parentNode&&a.parentNode.removeChild(a)}
function w(a,b,c){b=b||[];c=c||[];for(var d=a.className.split(/\s+/),e=0;e<b.length;e+=1){for(var f=!1,g=0;g<d.length;g+=1)if(b[e]===d[g]){f=!0;break}f||d.push(b[e])}b=[];for(e=0;e<d.length;e+=1){f=!1;for(g=0;g<c.length;g+=1)if(d[e]===c[g]){f=!0;break}f||b.push(d[e])}a.className=b.join(" ").replace(/\s+/g," ").replace(/^\s+|\s+$/,"")}function y(a,b){for(var c=a.className.split(/\s+/),d=0,e=c.length;d<e;d++)if(c[d]==b)return!0;return!1}
function z(a){if("string"===typeof a.f)return a.f;var b=a.m.location.protocol;"about:"==b&&(b=a.a.location.protocol);return"https:"==b?"https:":"http:"}function ea(a){return a.m.location.hostname||a.a.location.hostname}
function A(a,b,c){function d(){k&&e&&f&&(k(g),k=null)}b=t(a,"link",{rel:"stylesheet",href:b,media:"all"});var e=!1,f=!0,g=null,k=c||null;da?(b.onload=function(){e=!0;d()},b.onerror=function(){e=!0;g=Error("Stylesheet failed to load");d()}):setTimeout(function(){e=!0;d()},0);u(a,"head",b)}
function B(a,b,c,d){var e=a.c.getElementsByTagName("head")[0];if(e){var f=t(a,"script",{src:b}),g=!1;f.onload=f.onreadystatechange=function(){g||this.readyState&&"loaded"!=this.readyState&&"complete"!=this.readyState||(g=!0,c&&c(null),f.onload=f.onreadystatechange=null,"HEAD"==f.parentNode.tagName&&e.removeChild(f))};e.appendChild(f);setTimeout(function(){g||(g=!0,c&&c(Error("Script load timeout")))},d||5E3);return f}return null};function C(){this.a=0;this.c=null}function D(a){a.a++;return function(){a.a--;E(a)}}function F(a,b){a.c=b;E(a)}function E(a){0==a.a&&a.c&&(a.c(),a.c=null)};function G(a){this.a=a||"-"}G.prototype.c=function(a){for(var b=[],c=0;c<arguments.length;c++)b.push(arguments[c].replace(/[\W_]+/g,"").toLowerCase());return b.join(this.a)};function H(a,b){this.c=a;this.f=4;this.a="n";var c=(b||"n4").match(/^([nio])([1-9])$/i);c&&(this.a=c[1],this.f=parseInt(c[2],10))}function fa(a){return I(a)+" "+(a.f+"00")+" 300px "+J(a.c)}function J(a){var b=[];a=a.split(/,\s*/);for(var c=0;c<a.length;c++){var d=a[c].replace(/['"]/g,"");-1!=d.indexOf(" ")||/^\d/.test(d)?b.push("'"+d+"'"):b.push(d)}return b.join(",")}function K(a){return a.a+a.f}function I(a){var b="normal";"o"===a.a?b="oblique":"i"===a.a&&(b="italic");return b}
function ga(a){var b=4,c="n",d=null;a&&((d=a.match(/(normal|oblique|italic)/i))&&d[1]&&(c=d[1].substr(0,1).toLowerCase()),(d=a.match(/([1-9]00|normal|bold)/i))&&d[1]&&(/bold/i.test(d[1])?b=7:/[1-9]00/.test(d[1])&&(b=parseInt(d[1].substr(0,1),10))));return c+b};function ha(a,b){this.c=a;this.f=a.m.document.documentElement;this.h=b;this.a=new G("-");this.j=!1!==b.events;this.g=!1!==b.classes}function ia(a){a.g&&w(a.f,[a.a.c("wf","loading")]);L(a,"loading")}function M(a){if(a.g){var b=y(a.f,a.a.c("wf","active")),c=[],d=[a.a.c("wf","loading")];b||c.push(a.a.c("wf","inactive"));w(a.f,c,d)}L(a,"inactive")}function L(a,b,c){if(a.j&&a.h[b])if(c)a.h[b](c.c,K(c));else a.h[b]()};function ja(){this.c={}}function ka(a,b,c){var d=[],e;for(e in b)if(b.hasOwnProperty(e)){var f=a.c[e];f&&d.push(f(b[e],c))}return d};function N(a,b){this.c=a;this.f=b;this.a=t(this.c,"span",{"aria-hidden":"true"},this.f)}function O(a){u(a.c,"body",a.a)}function P(a){return"display:block;position:absolute;top:-9999px;left:-9999px;font-size:300px;width:auto;height:auto;line-height:normal;margin:0;padding:0;font-variant:normal;white-space:nowrap;font-family:"+J(a.c)+";"+("font-style:"+I(a)+";font-weight:"+(a.f+"00")+";")};function Q(a,b,c,d,e,f){this.g=a;this.j=b;this.a=d;this.c=c;this.f=e||3E3;this.h=f||void 0}Q.prototype.start=function(){var a=this.c.m.document,b=this,c=q(),d=new Promise(function(d,e){function k(){q()-c>=b.f?e():a.fonts.load(fa(b.a),b.h).then(function(a){1<=a.length?d():setTimeout(k,25)},function(){e()})}k()}),e=new Promise(function(a,d){setTimeout(d,b.f)});Promise.race([e,d]).then(function(){b.g(b.a)},function(){b.j(b.a)})};function R(a,b,c,d,e,f,g){this.v=a;this.B=b;this.c=c;this.a=d;this.s=g||"BESbswy";this.f={};this.w=e||3E3;this.u=f||null;this.o=this.j=this.h=this.g=null;this.g=new N(this.c,this.s);this.h=new N(this.c,this.s);this.j=new N(this.c,this.s);this.o=new N(this.c,this.s);a=new H(this.a.c+",serif",K(this.a));a=P(a);this.g.a.style.cssText=a;a=new H(this.a.c+",sans-serif",K(this.a));a=P(a);this.h.a.style.cssText=a;a=new H("serif",K(this.a));a=P(a);this.j.a.style.cssText=a;a=new H("sans-serif",K(this.a));a=
P(a);this.o.a.style.cssText=a;O(this.g);O(this.h);O(this.j);O(this.o)}var S={D:"serif",C:"sans-serif"},T=null;function U(){if(null===T){var a=/AppleWebKit\/([0-9]+)(?:\.([0-9]+))/.exec(window.navigator.userAgent);T=!!a&&(536>parseInt(a[1],10)||536===parseInt(a[1],10)&&11>=parseInt(a[2],10))}return T}R.prototype.start=function(){this.f.serif=this.j.a.offsetWidth;this.f["sans-serif"]=this.o.a.offsetWidth;this.A=q();la(this)};
function ma(a,b,c){for(var d in S)if(S.hasOwnProperty(d)&&b===a.f[S[d]]&&c===a.f[S[d]])return!0;return!1}function la(a){var b=a.g.a.offsetWidth,c=a.h.a.offsetWidth,d;(d=b===a.f.serif&&c===a.f["sans-serif"])||(d=U()&&ma(a,b,c));d?q()-a.A>=a.w?U()&&ma(a,b,c)&&(null===a.u||a.u.hasOwnProperty(a.a.c))?V(a,a.v):V(a,a.B):na(a):V(a,a.v)}function na(a){setTimeout(p(function(){la(this)},a),50)}function V(a,b){setTimeout(p(function(){v(this.g.a);v(this.h.a);v(this.j.a);v(this.o.a);b(this.a)},a),0)};function W(a,b,c){this.c=a;this.a=b;this.f=0;this.o=this.j=!1;this.s=c}var X=null;W.prototype.g=function(a){var b=this.a;b.g&&w(b.f,[b.a.c("wf",a.c,K(a).toString(),"active")],[b.a.c("wf",a.c,K(a).toString(),"loading"),b.a.c("wf",a.c,K(a).toString(),"inactive")]);L(b,"fontactive",a);this.o=!0;oa(this)};
W.prototype.h=function(a){var b=this.a;if(b.g){var c=y(b.f,b.a.c("wf",a.c,K(a).toString(),"active")),d=[],e=[b.a.c("wf",a.c,K(a).toString(),"loading")];c||d.push(b.a.c("wf",a.c,K(a).toString(),"inactive"));w(b.f,d,e)}L(b,"fontinactive",a);oa(this)};function oa(a){0==--a.f&&a.j&&(a.o?(a=a.a,a.g&&w(a.f,[a.a.c("wf","active")],[a.a.c("wf","loading"),a.a.c("wf","inactive")]),L(a,"active")):M(a.a))};function pa(a){this.j=a;this.a=new ja;this.h=0;this.f=this.g=!0}pa.prototype.load=function(a){this.c=new ca(this.j,a.context||this.j);this.g=!1!==a.events;this.f=!1!==a.classes;qa(this,new ha(this.c,a),a)};
function ra(a,b,c,d,e){var f=0==--a.h;(a.f||a.g)&&setTimeout(function(){var a=e||null,k=d||null||{};if(0===c.length&&f)M(b.a);else{b.f+=c.length;f&&(b.j=f);var h,m=[];for(h=0;h<c.length;h++){var l=c[h],n=k[l.c],r=b.a,x=l;r.g&&w(r.f,[r.a.c("wf",x.c,K(x).toString(),"loading")]);L(r,"fontloading",x);r=null;null===X&&(X=window.FontFace?(x=/Gecko.*Firefox\/(\d+)/.exec(window.navigator.userAgent))?42<parseInt(x[1],10):!0:!1);X?r=new Q(p(b.g,b),p(b.h,b),b.c,l,b.s,n):r=new R(p(b.g,b),p(b.h,b),b.c,l,b.s,a,
n);m.push(r)}for(h=0;h<m.length;h++)m[h].start()}},0)}function qa(a,b,c){var d=[],e=c.timeout;ia(b);var d=ka(a.a,c,a.c),f=new W(a.c,b,e);a.h=d.length;b=0;for(c=d.length;b<c;b++)d[b].load(function(b,d,c){ra(a,f,b,d,c)})};function sa(a,b){this.c=a;this.a=b}function ta(a,b,c){var d=z(a.c);a=(a.a.api||"fast.fonts.net/jsapi").replace(/^.*http(s?):(\/\/)?/,"");return d+"//"+a+"/"+b+".js"+(c?"?v="+c:"")}
sa.prototype.load=function(a){function b(){if(f["__mti_fntLst"+d]){var c=f["__mti_fntLst"+d](),e=[],h;if(c)for(var m=0;m<c.length;m++){var l=c[m].fontfamily;void 0!=c[m].fontStyle&&void 0!=c[m].fontWeight?(h=c[m].fontStyle+c[m].fontWeight,e.push(new H(l,h))):e.push(new H(l))}a(e)}else setTimeout(function(){b()},50)}var c=this,d=c.a.projectId,e=c.a.version;if(d){var f=c.c.m;B(this.c,ta(c,d,e),function(e){e?a([]):(f["__MonotypeConfiguration__"+d]=function(){return c.a},b())}).id="__MonotypeAPIScript__"+
d}else a([])};function ua(a,b){this.c=a;this.a=b}ua.prototype.load=function(a){var b,c,d=this.a.urls||[],e=this.a.families||[],f=this.a.testStrings||{},g=new C;b=0;for(c=d.length;b<c;b++)A(this.c,d[b],D(g));var k=[];b=0;for(c=e.length;b<c;b++)if(d=e[b].split(":"),d[1])for(var h=d[1].split(","),m=0;m<h.length;m+=1)k.push(new H(d[0],h[m]));else k.push(new H(d[0]));F(g,function(){a(k,f)})};function va(a,b,c){a?this.c=a:this.c=b+wa;this.a=[];this.f=[];this.g=c||""}var wa="//fonts.googleapis.com/css";function xa(a,b){for(var c=b.length,d=0;d<c;d++){var e=b[d].split(":");3==e.length&&a.f.push(e.pop());var f="";2==e.length&&""!=e[1]&&(f=":");a.a.push(e.join(f))}}
function ya(a){if(0==a.a.length)throw Error("No fonts to load!");if(-1!=a.c.indexOf("kit="))return a.c;for(var b=a.a.length,c=[],d=0;d<b;d++)c.push(a.a[d].replace(/ /g,"+"));b=a.c+"?family="+c.join("%7C");0<a.f.length&&(b+="&subset="+a.f.join(","));0<a.g.length&&(b+="&text="+encodeURIComponent(a.g));return b};function za(a){this.f=a;this.a=[];this.c={}}
var Aa={latin:"BESbswy","latin-ext":"\u00e7\u00f6\u00fc\u011f\u015f",cyrillic:"\u0439\u044f\u0416",greek:"\u03b1\u03b2\u03a3",khmer:"\u1780\u1781\u1782",Hanuman:"\u1780\u1781\u1782"},Ba={thin:"1",extralight:"2","extra-light":"2",ultralight:"2","ultra-light":"2",light:"3",regular:"4",book:"4",medium:"5","semi-bold":"6",semibold:"6","demi-bold":"6",demibold:"6",bold:"7","extra-bold":"8",extrabold:"8","ultra-bold":"8",ultrabold:"8",black:"9",heavy:"9",l:"3",r:"4",b:"7"},Ca={i:"i",italic:"i",n:"n",normal:"n"},
Da=/^(thin|(?:(?:extra|ultra)-?)?light|regular|book|medium|(?:(?:semi|demi|extra|ultra)-?)?bold|black|heavy|l|r|b|[1-9]00)?(n|i|normal|italic)?$/;
function Ea(a){for(var b=a.f.length,c=0;c<b;c++){var d=a.f[c].split(":"),e=d[0].replace(/\+/g," "),f=["n4"];if(2<=d.length){var g;var k=d[1];g=[];if(k)for(var k=k.split(","),h=k.length,m=0;m<h;m++){var l;l=k[m];if(l.match(/^[\w-]+$/)){var n=Da.exec(l.toLowerCase());if(null==n)l="";else{l=n[2];l=null==l||""==l?"n":Ca[l];n=n[1];if(null==n||""==n)n="4";else var r=Ba[n],n=r?r:isNaN(n)?"4":n.substr(0,1);l=[l,n].join("")}}else l="";l&&g.push(l)}0<g.length&&(f=g);3==d.length&&(d=d[2],g=[],d=d?d.split(","):
g,0<d.length&&(d=Aa[d[0]])&&(a.c[e]=d))}a.c[e]||(d=Aa[e])&&(a.c[e]=d);for(d=0;d<f.length;d+=1)a.a.push(new H(e,f[d]))}};function Fa(a,b){this.c=a;this.a=b}var Ga={Arimo:!0,Cousine:!0,Tinos:!0};Fa.prototype.load=function(a){var b=new C,c=this.c,d=new va(this.a.api,z(c),this.a.text),e=this.a.families;xa(d,e);var f=new za(e);Ea(f);A(c,ya(d),D(b));F(b,function(){a(f.a,f.c,Ga)})};function Ha(a,b){this.c=a;this.a=b}Ha.prototype.load=function(a){var b=this.a.id,c=this.c.m;b?B(this.c,(this.a.api||"https://use.typekit.net")+"/"+b+".js",function(b){if(b)a([]);else if(c.Typekit&&c.Typekit.config&&c.Typekit.config.fn){b=c.Typekit.config.fn;for(var e=[],f=0;f<b.length;f+=2)for(var g=b[f],k=b[f+1],h=0;h<k.length;h++)e.push(new H(g,k[h]));try{c.Typekit.load({events:!1,classes:!1,async:!0})}catch(m){}a(e)}},2E3):a([])};function Ia(a,b){this.c=a;this.f=b;this.a=[]}Ia.prototype.load=function(a){var b=this.f.id,c=this.c.m,d=this;b?(c.__webfontfontdeckmodule__||(c.__webfontfontdeckmodule__={}),c.__webfontfontdeckmodule__[b]=function(b,c){for(var g=0,k=c.fonts.length;g<k;++g){var h=c.fonts[g];d.a.push(new H(h.name,ga("font-weight:"+h.weight+";font-style:"+h.style)))}a(d.a)},B(this.c,z(this.c)+(this.f.api||"//f.fontdeck.com/s/css/js/")+ea(this.c)+"/"+b+".js",function(b){b&&a([])})):a([])};var Y=new pa(window);Y.a.c.custom=function(a,b){return new ua(b,a)};Y.a.c.fontdeck=function(a,b){return new Ia(b,a)};Y.a.c.monotype=function(a,b){return new sa(b,a)};Y.a.c.typekit=function(a,b){return new Ha(b,a)};Y.a.c.google=function(a,b){return new Fa(b,a)};var Z={load:p(Y.load,Y)};"function"===typeof define&&define.amd?define(function(){return Z}):"undefined"!==typeof module&&module.exports?module.exports=Z:(window.WebFont=Z,window.WebFontConfig&&Y.load(window.WebFontConfig));}());

(function() {
    /**
    * Copyright 2004-present Facebook. All Rights Reserved.
    *
    * @providesModule UserAgent_DEPRECATED
    */

   /**
    *  Provides entirely client-side User Agent and OS detection. You should prefer
    *  the non-deprecated UserAgent module when possible, which exposes our
    *  authoritative server-side PHP-based detection to the client.
    *
    *  Usage is straightforward:
    *
    *    if (UserAgent_DEPRECATED.ie()) {
    *      //  IE
    *    }
    *
    *  You can also do version checks:
    *
    *    if (UserAgent_DEPRECATED.ie() >= 7) {
    *      //  IE7 or better
    *    }
    *
    *  The browser functions will return NaN if the browser does not match, so
    *  you can also do version compares the other way:
    *
    *    if (UserAgent_DEPRECATED.ie() < 7) {
    *      //  IE6 or worse
    *    }
    *
    *  Note that the version is a float and may include a minor version number,
    *  so you should always use range operators to perform comparisons, not
    *  strict equality.
    *
    *  **Note:** You should **strongly** prefer capability detection to browser
    *  version detection where it's reasonable:
    *
    *    http://www.quirksmode.org/js/support.html
    *
    *  Further, we have a large number of mature wrapper functions and classes
    *  which abstract away many browser irregularities. Check the documentation,
    *  grep for things, or ask on javascript@lists.facebook.com before writing yet
    *  another copy of "event || window.event".
    *
    */

   var _populated = false;

   // Browsers
   var _ie, _firefox, _opera, _webkit, _chrome;

   // Actual IE browser for compatibility mode
   var _ie_real_version;

   // Platforms
   var _osx, _windows, _linux, _android;

   // Architectures
   var _win64;

   // Devices
   var _iphone, _ipad, _native;

   var _mobile;

   function _populate() {
     if (_populated) {
       return;
     }

     _populated = true;

     // To work around buggy JS libraries that can't handle multi-digit
     // version numbers, Opera 10's user agent string claims it's Opera
     // 9, then later includes a Version/X.Y field:
     //
     // Opera/9.80 (foo) Presto/2.2.15 Version/10.10
     var uas = navigator.userAgent;
     var agent = /(?:MSIE.(\d+\.\d+))|(?:(?:Firefox|GranParadiso|Iceweasel).(\d+\.\d+))|(?:Opera(?:.+Version.|.)(\d+\.\d+))|(?:AppleWebKit.(\d+(?:\.\d+)?))|(?:Trident\/\d+\.\d+.*rv:(\d+\.\d+))/.exec(uas);
     var os    = /(Mac OS X)|(Windows)|(Linux)/.exec(uas);

     _iphone = /\b(iPhone|iP[ao]d)/.exec(uas);
     _ipad = /\b(iP[ao]d)/.exec(uas) ||  (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
     _android = /Android/i.exec(uas);
     _native = /FBAN\/\w+;/i.exec(uas);
     _mobile = /Mobile/i.exec(uas);

     // Note that the IE team blog would have you believe you should be checking
     // for 'Win64; x64'.  But MSDN then reveals that you can actually be coming
     // from either x64 or ia64;  so ultimately, you should just check for Win64
     // as in indicator of whether you're in 64-bit IE.  32-bit IE on 64-bit
     // Windows will send 'WOW64' instead.
     _win64 = !!(/Win64/.exec(uas));

     if (agent) {
       _ie = agent[1] ? parseFloat(agent[1]) : (
             agent[5] ? parseFloat(agent[5]) : NaN);
       // IE compatibility mode
       if (_ie && document && document.documentMode) {
         _ie = document.documentMode;
       }
       // grab the "true" ie version from the trident token if available
       var trident = /(?:Trident\/(\d+.\d+))/.exec(uas);
       _ie_real_version = trident ? parseFloat(trident[1]) + 4 : _ie;

       _firefox = agent[2] ? parseFloat(agent[2]) : NaN;
       _opera   = agent[3] ? parseFloat(agent[3]) : NaN;
       _webkit  = agent[4] ? parseFloat(agent[4]) : NaN;
       if (_webkit) {
         // We do not add the regexp to the above test, because it will always
         // match 'safari' only since 'AppleWebKit' appears before 'Chrome' in
         // the userAgent string.
         agent = /(?:Chrome\/(\d+\.\d+))/.exec(uas);
         _chrome = agent && agent[1] ? parseFloat(agent[1]) : NaN;
       } else {
         _chrome = NaN;
       }
     } else {
       _ie = _firefox = _opera = _chrome = _webkit = NaN;
     }

     if (os) {
       if (os[1]) {
         // Detect OS X version.  If no version number matches, set _osx to true.
         // Version examples:  10, 10_6_1, 10.7
         // Parses version number as a float, taking only first two sets of
         // digits.  If only one set of digits is found, returns just the major
         // version number.
         var ver = /(?:Mac OS X (\d+(?:[._]\d+)?))/.exec(uas);

         _osx = ver ? parseFloat(ver[1].replace('_', '.')) : true;
       } else {
         _osx = false;
       }
       _windows = !!os[2];
       _linux   = !!os[3];
     } else {
       _osx = _windows = _linux = false;
     }
   }

   window.UserAgent_DEPRECATED = {

     /**
      *  Check if the UA is Internet Explorer.
      *
      *
      *  @return float|NaN Version number (if match) or NaN.
      */
     ie: function() {
       return _populate() || _ie;
     },

     /**
      * Check if we're in Internet Explorer compatibility mode.
      *
      * @return bool true if in compatibility mode, false if
      * not compatibility mode or not ie
      */
     ieCompatibilityMode: function() {
       return _populate() || (_ie_real_version > _ie);
     },


     /**
      * Whether the browser is 64-bit IE.  Really, this is kind of weak sauce;  we
      * only need this because Skype can't handle 64-bit IE yet.  We need to remove
      * this when we don't need it -- tracked by #601957.
      */
     ie64: function() {
       return UserAgent_DEPRECATED.ie() && _win64;
     },

     /**
      *  Check if the UA is Firefox.
      *
      *
      *  @return float|NaN Version number (if match) or NaN.
      */
     firefox: function() {
       return _populate() || _firefox;
     },


     /**
      *  Check if the UA is Opera.
      *
      *
      *  @return float|NaN Version number (if match) or NaN.
      */
     opera: function() {
       return _populate() || _opera;
     },


     /**
      *  Check if the UA is WebKit.
      *
      *
      *  @return float|NaN Version number (if match) or NaN.
      */
     webkit: function() {
       return _populate() || _webkit;
     },

     /**
      *  For Push
      *  WILL BE REMOVED VERY SOON. Use UserAgent_DEPRECATED.webkit
      */
     safari: function() {
       return UserAgent_DEPRECATED.webkit();
     },

     /**
      *  Check if the UA is a Chrome browser.
      *
      *
      *  @return float|NaN Version number (if match) or NaN.
      */
     chrome : function() {
       return _populate() || _chrome;
     },


     /**
      *  Check if the user is running Windows.
      *
      *  @return bool `true' if the user's OS is Windows.
      */
     windows: function() {
       return _populate() || _windows;
     },


     /**
      *  Check if the user is running Mac OS X.
      *
      *  @return float|bool   Returns a float if a version number is detected,
      *                       otherwise true/false.
      */
     osx: function() {
       return _populate() || _osx;
     },

     /**
      * Check if the user is running Linux.
      *
      * @return bool `true' if the user's OS is some flavor of Linux.
      */
     linux: function() {
       return _populate() || _linux;
     },

     /**
      * Check if the user is running on an iPhone or iPod platform.
      *
      * @return bool `true' if the user is running some flavor of the
      *    iPhone OS.
      */
     iphone: function() {
       return _populate() || _iphone;
     },

     mobile: function() {
       return _populate() || (_iphone || _ipad || _android || _mobile);
     },

     nativeApp: function() {
       // webviews inside of the native apps
       return _populate() || _native;
     },

     android: function() {
       return _populate() || _android;
     },

     ipad: function() {
       return _populate() || _ipad;
     }
   };
    
})();

(function(){
    //Mouse wheel events are inconsistent across browsers
    // This function will normalize it
    //https://github.com/facebookarchive/fixed-data-table/blob/master/src/vendor_upstream/dom/normalizeWheel.js
    /**
     * Check if an event is supported.
     * Ref: http://perfectionkills.com/detecting-event-support-without-browser-sniffing/
     */
    function isEventSupported(event) {
      var testEl = document.createElement('div');
      var isSupported;

      event = 'on' + event;
      isSupported = (event in testEl);

      if (!isSupported) {
        testEl.setAttribute(event, 'return;');
        isSupported = typeof testEl[event] === 'function';
      }
      testEl = null;

      return isSupported;
    }
    // Reasonable defaults
    var PIXEL_STEP  = 10;
    var LINE_HEIGHT = 40;
    var PAGE_HEIGHT = 800;
    function normalizeWheel(/*object*/ event) /*object*/ {
      var sX = 0, sY = 0,       // spinX, spinY
          pX = 0, pY = 0;       // pixelX, pixelY

      // Legacy
      if ('detail'      in event) { sY = event.detail; }
      if ('wheelDelta'  in event) { sY = -event.wheelDelta / 120; }
      if ('wheelDeltaY' in event) { sY = -event.wheelDeltaY / 120; }
      if ('wheelDeltaX' in event) { sX = -event.wheelDeltaX / 120; }

      // side scrolling on FF with DOMMouseScroll
      if ( 'axis' in event && event.axis === event.HORIZONTAL_AXIS ) {
        sX = sY;
        sY = 0;
      }

      pX = sX * PIXEL_STEP;
      pY = sY * PIXEL_STEP;

      if ('deltaY' in event) { pY = event.deltaY; }
      if ('deltaX' in event) { pX = event.deltaX; }

      if ((pX || pY) && event.deltaMode) {
        if (event.deltaMode == 1) {          // delta in LINE units
          pX *= LINE_HEIGHT;
          pY *= LINE_HEIGHT;
        } else {                             // delta in PAGE units
          pX *= PAGE_HEIGHT;
          pY *= PAGE_HEIGHT;
        }
      }

      // Fall-back if spin cannot be determined
      if (pX && !sX) { sX = (pX < 1) ? -1 : 1; }
      if (pY && !sY) { sY = (pY < 1) ? -1 : 1; }

      return { spinX  : sX,
               spinY  : sY,
               pixelX : pX,
               pixelY : pY };
    }

    normalizeWheel.getEventType = function() /*string*/ {
      return (UserAgent_DEPRECATED.firefox())
               ? 'DOMMouseScroll'
               : (isEventSupported('wheel'))
                   ? 'wheel'
                   : 'mousewheel';
    };
    
    window.cn1NormalizeWheel = normalizeWheel;
})();

window.copyWheelEvent = function(event, iframe, x, y) {
    var type = event.type == 'MozMousePixelScroll' ? 'DOMMouseScroll' : event.type;
    var evt = new CustomEvent(type, {bubbles: true, cancelable: true});
    evt.clientX = event.clientX + x;
    evt.clientY = event.clientY + y;
    if ('axis' in event) evt.axis = event.axis;
    evt.cn1Detail = event.detail;
    if ('deltaY' in event) evt.deltaY = event.deltaY;
    if ('deltaX' in event) evt.deltaX = event.deltaX;
    if ('wheelDelta' in event) evt.wheelDelta = event.wheelDelta;
    if ('wheelDeltaX' in event) evt.wheelDeltaX = event.wheelDeltaX;
    if ('wheelDeltaY' in event) {
        evt.wheelDeltaY = event.wheelDeltaY;   
    } else if ('detail' in event) {
        // Firefox.. we can't set detail, so we need to fake the wheelDeltaY
        // so that the normalizeWheel method will work
        evt.wheelDeltaY = - event.detail * 120;
    }
    //console.log('wheel event', event, evt, event.wheelDeltaY);
    return evt;
};

window.copyTouchEvent = function(event, iframe, x, y) {
    //console.log("Copying touch event" + event);
    var evt = new CustomEvent(event.type, {bubbles: true, cancelable: true});
    if ('clientX' in event) evt.clientX = event.clientX + x;
    if ('clientY' in event) evt.clientY = event.clientY + y;
    if ('changedTouches' in event) {
        var touches = [];
        for (var i=0; i<event.changedTouches.length; i++) {
            touches.push({clientX: event.changedTouches[i].clientX+x, clientY: event.changedTouches[i].clientY+y});
        }
        evt.touches = touches;
    } else if ('touches' in event) {
        var touches = [];
        for (var i=0; i<event.touches.length; i++) {
            touches.push({clientX: event.touches[i].clientX+x, clientY: event.touches[i].clientY+y});
        }
        evt.touches = touches;
    }
    evt.targetTouches = evt.touches;

    return evt;
};

window.copyMouseEvent = function(event, srcEl) {
    var rect = srcEl.getBoundingClientRect();
    var x = rect.left;
    var y = rect.top;
    var evt = new CustomEvent(event.type, {bubbles: true, cancelable: true});
    if ('clientX' in event) evt.clientX = event.clientX + x;
    if ('clientY' in event) evt.clientY = event.clientY + y;
    return evt;
};

window.console = window.console || {
  log: function () {}
};

window.cn1GlobalWeakMap = (window.WeakMap === undefined) ? null : new WeakMap();
window.cn1_native_interfaces = {};
window.cn1_get_native_interfaces = function() {
  return window.cn1_native_interfaces;  
};

window.cn1CreateByteArray = function(arr) {
    return arr;
};

window.cn1CreateIntArray = function(arr) {
    return arr;
};

window.cn1CreateShortArray = function(arr) {
    return arr;
};

window.cn1CreateFloatArray = function(arr) {
    return arr;
};

window.cn1CreateDoubleArray = function(arr) {
    return arr;
}

window.cn1CreateLongArray = function(arr) {
    if (arr === null) {
        return null;
    }
    var len = arr.length;
    var out = $rt_createLongArray(len);
    var data = out.data;
    for (var i=0; i<len; i++) {
        data[i] = Long_fromInt(arr[i]);
    }
    return out;
};

window.cn1WrapBooleanArray = function(arr) {
    return arr;
};


// Matrix stuff
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Copyright 2008 Google Inc. All Rights Reserved.


/**
 * @fileoverview Provides an object representation of an AffineTransform and
 * methods for working with it.
 */


//goog.provide('goog.graphics.AffineTransform');
window.goog = window.goog || {};
window.goog.graphics = window.goog.graphics || {};

/**
 * Creates a 2D affine transform. An affine transform performs a linear
 * mapping from 2D coordinates to other 2D coordinates that preserves the
 * "straightness" and "parallelness" of lines.
 *
 * Such a coordinate transformation can be represented by a 3 row by 3 column
 * matrix with an implied last row of [ 0 0 1 ]. This matrix transforms source
 * coordinates (x,y) into destination coordinates (x',y') by considering them
 * to be a column vector and multiplying the coordinate vector by the matrix
 * according to the following process:
 * <pre>
 *      [ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
 *      [ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
 *      [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]
 * </pre>
 *
 * This class is optimized for speed and minimizes calculations based on its
 * knowledge of the underlying matrix (as opposed to say simply performing
 * matrix multiplication).
 *
 * @param {number} opt_m00 The m00 coordinate of the transform.
 * @param {number} opt_m10 The m10 coordinate of the transform.
 * @param {number} opt_m01 The m01 coordinate of the transform.
 * @param {number} opt_m11 The m11 coordinate of the transform.
 * @param {number} opt_m02 The m02 coordinate of the transform.
 * @param {number} opt_m12 The m12 coordinate of the transform.
 * @constructor
 */
goog.graphics.AffineTransform = function(opt_m00, opt_m10, opt_m01,
    opt_m11, opt_m02, opt_m12) {
  if (arguments.length == 6) {
    this.setTransform(/** @type {number} */ (opt_m00),
                      /** @type {number} */ (opt_m10),
                      /** @type {number} */ (opt_m01),
                      /** @type {number} */ (opt_m11),
                      /** @type {number} */ (opt_m02),
                      /** @type {number} */ (opt_m12));
  } else if (arguments.length != 0) {
    throw Error('Insufficient matrix parameters');
  } else {
    this.m00_ = this.m11_ = 1;
    this.m10_ = this.m01_ = this.m02_ = this.m12_ = 0;
  }
};


/**
 * @return {boolean} Whether this transform is the identity transform.
 */
goog.graphics.AffineTransform.prototype.isIdentity = function() {
  return this.m00_ == 1 && this.m10_ == 0 && this.m01_ == 0 &&
      this.m11_ == 1 && this.m02_ == 0 && this.m12_ == 0;
};


/**
 * @return {!goog.graphics.AffineTransform} A copy of this transform.
 */
goog.graphics.AffineTransform.prototype.cloneTransform = function() {
  return new goog.graphics.AffineTransform(this.m00_, this.m10_, this.m01_,
      this.m11_, this.m02_, this.m12_);
};


/**
 * Sets this transform to the matrix specified by the 6 values.
 *
 * @param {number} m00 The m00 coordinate of the transform.
 * @param {number} m10 The m10 coordinate of the transform.
 * @param {number} m01 The m01 coordinate of the transform.
 * @param {number} m11 The m11 coordinate of the transform.
 * @param {number} m02 The m02 coordinate of the transform.
 * @param {number} m12 The m12 coordinate of the transform.
 * @return {!goog.graphics.AffineTransform} This affine transform.
 */
goog.graphics.AffineTransform.prototype.setTransform = function(m00, m10, m01,
    m11, m02, m12) {
  //if (!goog.isNumber(m00) || !goog.isNumber(m10) || !goog.isNumber(m01) ||
  //    !goog.isNumber(m11) || !goog.isNumber(m02) || !goog.isNumber(m12)) {
  //  throw Error('Invalid transform parameters');
  //}
  this.m00_ = m00;
  this.m10_ = m10;
  this.m01_ = m01;
  this.m11_ = m11;
  this.m02_ = m02;
  this.m12_ = m12;
  return this;
};


/**
 * Sets this transform to be identical to the given transform.
 *
 * @param {!goog.graphics.AffineTransform} tx The transform to copy.
 * @return {!goog.graphics.AffineTransform} This affine transform.
 */
goog.graphics.AffineTransform.prototype.copyFrom = function(tx) {
  this.m00_ = tx.m00_;
  this.m10_ = tx.m10_;
  this.m01_ = tx.m01_;
  this.m11_ = tx.m11_;
  this.m02_ = tx.m02_;
  this.m12_ = tx.m12_;
  return this;
};


/**
 * Concatentates this transform with a scaling transformation.
 *
 * @param {number} sx The x-axis scaling factor.
 * @param {number} sy The y-axis scaling factor.
 * @return {!goog.graphics.AffineTransform} This affine transform.
 */
goog.graphics.AffineTransform.prototype.scale = function(sx, sy) {
  this.m00_ *= sx;
  this.m10_ *= sx;
  this.m01_ *= sy;
  this.m11_ *= sy;
  return this;
};


/**
 * Concatentates this transform with a translate transformation.
 *
 * @param {number} dx The distance to translate in the x direction.
 * @param {number} dy The distance to translate in the y direction.
 * @return {!goog.graphics.AffineTransform} This affine transform.
 */
goog.graphics.AffineTransform.prototype.translate = function(dx, dy) {
  this.m02_ += dx * this.m00_ + dy * this.m01_;
  this.m12_ += dx * this.m10_ + dy * this.m11_;
  return this;
};


/**
 * Concatentates this transform with a rotation transformation around an anchor
 * point.
 *
 * @param {number} theta The angle of rotation measured in radians.
 * @param {number} x The x coordinate of the anchor point.
 * @param {number} y The y coordinate of the anchor point.
 * @return {!goog.graphics.AffineTransform} This affine transform.
 */
goog.graphics.AffineTransform.prototype.rotate = function(theta, x, y) {
  return this.concatenate(
      goog.graphics.AffineTransform.getRotateInstance(theta, x, y));
};


/**
 * Concatentates this transform with a shear transformation.
 *
 * @param {number} shx The x shear factor.
 * @param {number} shy The y shear factor.
 * @return {!goog.graphics.AffineTransform} This affine transform.
 */
goog.graphics.AffineTransform.prototype.shear = function(shx, shy) {
  var m00 = this.m00_;
  var m10 = this.m10_;
  this.m00_ += shy * this.m01_;
  this.m10_ += shy * this.m11_;
  this.m01_ += shx * m00;
  this.m11_ += shx * m10;
  return this;
};


/**
 * @return {string} A string representation of this transform. The format of
 *     of the string is compatible with SVG matrix notation, i.e.
 *     "matrix(a,b,c,d,e,f)".
 */
goog.graphics.AffineTransform.prototype.stringValue = function() {
  return 'matrix(' + [this.m00_, this.m10_, this.m01_, this.m11_,
      this.m02_, this.m12_].join(',') + ')';
};



/**
 * @return {number} The scaling factor in the x-direction (m00).
 */
goog.graphics.AffineTransform.prototype.getScaleX = function() {
  return this.m00_;
};


/**
 * @return {number} The scaling factor in the y-direction (m11).
 */
goog.graphics.AffineTransform.prototype.getScaleY = function() {
  return this.m11_;
};


/**
 * @return {number} The translation in the x-direction (m02).
 */
goog.graphics.AffineTransform.prototype.getTranslateX = function() {
  return this.m02_;
};


/**
 * @return {number} The translation in the y-direction (m12).
 */
goog.graphics.AffineTransform.prototype.getTranslateY = function() {
  return this.m12_;
};


/**
 * @return {number} The shear factor in the x-direction (m01).
 */
goog.graphics.AffineTransform.prototype.getShearX = function() {
  return this.m01_;
};


/**
 * @return {number} The shear factor in the y-direction (m10).
 */
goog.graphics.AffineTransform.prototype.getShearY = function() {
  return this.m10_;
};


/**
 * Concatenates an affine transform to this transform.
 *
 * @param {!goog.graphics.AffineTransform} tx The transform to concatenate.
 * @return {!goog.graphics.AffineTransform} This affine transform.
 */
goog.graphics.AffineTransform.prototype.concatenate = function(tx) {
  var m0 = this.m00_;
  var m1 = this.m01_;
  this.m00_ = tx.m00_ * m0 + tx.m10_ * m1;
  this.m01_ = tx.m01_ * m0 + tx.m11_ * m1;
  this.m02_ += tx.m02_ * m0 + tx.m12_ * m1;

  m0 = this.m10_;
  m1 = this.m11_;
  this.m10_ = tx.m00_ * m0 + tx.m10_ * m1;
  this.m11_ = tx.m01_ * m0 + tx.m11_ * m1;
  this.m12_ += tx.m02_ * m0 + tx.m12_ * m1;
  return this;
};


/**
 * Pre-concatenates an affine transform to this transform.
 *
 * @param {!goog.graphics.AffineTransform} tx The transform to preconcatenate.
 * @return {!goog.graphics.AffineTransform} This affine transform.
 */
goog.graphics.AffineTransform.prototype.preConcatenate = function(tx) {
  var m0 = this.m00_;
  var m1 = this.m10_;
  this.m00_ = tx.m00_ * m0 + tx.m01_ * m1;
  this.m10_ = tx.m10_ * m0 + tx.m11_ * m1;

  m0 = this.m01_;
  m1 = this.m11_;
  this.m01_ = tx.m00_ * m0 + tx.m01_ * m1;
  this.m11_ = tx.m10_ * m0 + tx.m11_ * m1;

  m0 = this.m02_;
  m1 = this.m12_;
  this.m02_ = tx.m00_ * m0 + tx.m01_ * m1 + tx.m02_;
  this.m12_ = tx.m10_ * m0 + tx.m11_ * m1 + tx.m12_;
  return this;
};


/**
 * Transforms an array of coordinates by this transform and stores the result
 * into a destination array.
 *
 * @param {!Array.<number>} src The array containing the source points
 *     as x, y value pairs.
 * @param {number} srcOff The offset to the first point to be transformed.
 * @param {!Array.<number>} dst The array into which to store the transformed
 *     point pairs.
 * @param {number} dstOff The offset of the location of the first transformed
 *     point in the destination array.
 * @param {number} numPts The number of points to tranform.
 */
goog.graphics.AffineTransform.prototype.transform = function(src, srcOff, dst,
    dstOff, numPts) {
  var i = srcOff;
  var j = dstOff;
  var srcEnd = srcOff + 2 * numPts;
  while (i < srcEnd) {
    var x = src[i++];
    var y = src[i++];
    dst[j++] = x * this.m00_ + y * this.m01_ + this.m02_;
    dst[j++] = x * this.m10_ + y * this.m11_ + this.m12_;
  }
};


/**
 * @return {number} The determinant of this transform.
 */
goog.graphics.AffineTransform.prototype.getDeterminant = function() {
  return this.m00_ * this.m11_ - this.m01_ * this.m10_;
};


/**
 * Returns whether the transform is invertible. A transform is not invertible
 * if the determinant is 0 or any value is non-finite or NaN.
 *
 * @return {boolean} Whether the transform is invertible.
 */
goog.graphics.AffineTransform.prototype.isInvertible = function() {
  var det = this.getDeterminant();
  return goog.math.isFiniteNumber(det) &&
      goog.math.isFiniteNumber(this.m02_) &&
      goog.math.isFiniteNumber(this.m12_) &&
      det != 0;
};


/**
 * @return {!goog.graphics.AffineTransform} An AffineTransform object
 *     representing the inverse transformation.
 */
goog.graphics.AffineTransform.prototype.createInverse = function() {
  var det = this.getDeterminant();
  return new goog.graphics.AffineTransform(
      this.m11_ / det,
      -this.m10_ / det,
      -this.m01_ / det,
      this.m00_ / det,
      (this.m01_ * this.m12_ - this.m11_ * this.m02_) / det,
      (this.m10_ * this.m02_ - this.m00_ * this.m12_) / det);
};


/**
 * Creates a transform representing a scaling transformation.
 *
 * @param {number} sx The x-axis scaling factor.
 * @param {number} sy The y-axis scaling factor.
 * @return {!goog.graphics.AffineTransform} A transform representing a scaling
 *     transformation.
 */
goog.graphics.AffineTransform.getScaleInstance = function(sx, sy) {
  return new goog.graphics.AffineTransform().setToScale(sx, sy);
};


/**
 * Creates a transform representing a translation transformation.
 *
 * @param {number} dx The distance to translate in the x direction.
 * @param {number} dy The distance to translate in the y direction.
 * @return {!goog.graphics.AffineTransform} A transform representing a
 *     translation transformation.
 */
goog.graphics.AffineTransform.getTranslateInstance = function(dx, dy) {
  return new goog.graphics.AffineTransform().setToTranslation(dx, dy);
};


/**
 * Creates a transform representing a shearing transformation.
 *
 * @param {number} shx The x-axis shear factor.
 * @param {number} shy The y-axis shear factor.
 * @return {!goog.graphics.AffineTransform} A transform representing a shearing
 *     transformation.
 */
goog.graphics.AffineTransform.getShearInstance = function(shx, shy) {
  return new goog.graphics.AffineTransform().setToShear(shx, shy);
};


/**
 * Creates a transform representing a rotation transformation.
 *
 * @param {number} theta The angle of rotation measured in radians.
 * @param {number} x The x coordinate of the anchor point.
 * @param {number} y The y coordinate of the anchor point.
 * @return {!goog.graphics.AffineTransform} A transform representing a rotation
 *     transformation.
 */
goog.graphics.AffineTransform.getRotateInstance = function(theta, x, y) {
  return new goog.graphics.AffineTransform().setToRotation(theta, x, y);
};


/**
 * Sets this transform to a scaling transformation.
 *
 * @param {number} sx The x-axis scaling factor.
 * @param {number} sy The y-axis scaling factor.
 * @return {!goog.graphics.AffineTransform} This affine transform.
 */
goog.graphics.AffineTransform.prototype.setToScale = function(sx, sy) {
  return this.setTransform(sx, 0, 0, sy, 0, 0);
};


goog.graphics.AffineTransform.prototype.isEqualTo = function(t) {
  return this.m00_ === t.m00_ && 
          this.m01_ === t.m01_ && 
          this.m02_ === t.m02_ &&
          this.m10_ === t.m10_ &&
          this.m11_ === t.m11_ &&
          this.m12_ === t.m12_;
          
  
};

/**
 * Sets this transform to a translation transformation.
 *
 * @param {number} dx The distance to translate in the x direction.
 * @param {number} dy The distance to translate in the y direction.
 * @return {!goog.graphics.AffineTransform} This affine transform.
 */
goog.graphics.AffineTransform.prototype.setToTranslation = function(dx, dy) {
  return this.setTransform(1, 0, 0, 1, dx, dy);
};


/**
 * Sets this transform to a shearing transformation.
 *
 * @param {number} shx The x-axis shear factor.
 * @param {number} shy The y-axis shear factor.
 * @return {!goog.graphics.AffineTransform} This affine transform.
 */
goog.graphics.AffineTransform.prototype.setToShear = function(shx, shy) {
  return this.setTransform(1, shy, shx, 1, 0, 0);
};


/**
 * Sets this transform to a rotation transformation.
 *
 * @param {number} theta The angle of rotation measured in radians.
 * @param {number} x The x coordinate of the anchor point.
 * @param {number} y The y coordinate of the anchor point.
 * @return {!goog.graphics.AffineTransform} This affine transform.
 */
goog.graphics.AffineTransform.prototype.setToRotation = function(theta, x, y) {
  var cos = Math.cos(theta);
  var sin = Math.sin(theta);
  return this.setTransform(cos, sin, -sin, cos,
      x - x * cos + y * sin, y - x * sin - y * cos);
};

// END MATRIX STUFF

/*!
    localForage -- Offline Storage, Improved
    Version 1.7.3
    https://localforage.github.io/localForage
    (c) 2013-2017 Mozilla, Apache License 2.0
*/
!function(a){if("object"==typeof exports&&"undefined"!=typeof module)module.exports=a();else if("function"==typeof define&&define.amd)define([],a);else{var b;b="undefined"!=typeof window?window:"undefined"!=typeof global?global:"undefined"!=typeof self?self:this,b.localforage=a()}}(function(){return function a(b,c,d){function e(g,h){if(!c[g]){if(!b[g]){var i="function"==typeof require&&require;if(!h&&i)return i(g,!0);if(f)return f(g,!0);var j=new Error("Cannot find module '"+g+"'");throw j.code="MODULE_NOT_FOUND",j}var k=c[g]={exports:{}};b[g][0].call(k.exports,function(a){var c=b[g][1][a];return e(c||a)},k,k.exports,a,b,c,d)}return c[g].exports}for(var f="function"==typeof require&&require,g=0;g<d.length;g++)e(d[g]);return e}({1:[function(a,b,c){(function(a){"use strict";function c(){k=!0;for(var a,b,c=l.length;c;){for(b=l,l=[],a=-1;++a<c;)b[a]();c=l.length}k=!1}function d(a){1!==l.push(a)||k||e()}var e,f=a.MutationObserver||a.WebKitMutationObserver;if(f){var g=0,h=new f(c),i=a.document.createTextNode("");h.observe(i,{characterData:!0}),e=function(){i.data=g=++g%2}}else if(a.setImmediate||void 0===a.MessageChannel)e="document"in a&&"onreadystatechange"in a.document.createElement("script")?function(){var b=a.document.createElement("script");b.onreadystatechange=function(){c(),b.onreadystatechange=null,b.parentNode.removeChild(b),b=null},a.document.documentElement.appendChild(b)}:function(){setTimeout(c,0)};else{var j=new a.MessageChannel;j.port1.onmessage=c,e=function(){j.port2.postMessage(0)}}var k,l=[];b.exports=d}).call(this,"undefined"!=typeof global?global:"undefined"!=typeof self?self:"undefined"!=typeof window?window:{})},{}],2:[function(a,b,c){"use strict";function d(){}function e(a){if("function"!=typeof a)throw new TypeError("resolver must be a function");this.state=s,this.queue=[],this.outcome=void 0,a!==d&&i(this,a)}function f(a,b,c){this.promise=a,"function"==typeof b&&(this.onFulfilled=b,this.callFulfilled=this.otherCallFulfilled),"function"==typeof c&&(this.onRejected=c,this.callRejected=this.otherCallRejected)}function g(a,b,c){o(function(){var d;try{d=b(c)}catch(b){return p.reject(a,b)}d===a?p.reject(a,new TypeError("Cannot resolve promise with itself")):p.resolve(a,d)})}function h(a){var b=a&&a.then;if(a&&("object"==typeof a||"function"==typeof a)&&"function"==typeof b)return function(){b.apply(a,arguments)}}function i(a,b){function c(b){f||(f=!0,p.reject(a,b))}function d(b){f||(f=!0,p.resolve(a,b))}function e(){b(d,c)}var f=!1,g=j(e);"error"===g.status&&c(g.value)}function j(a,b){var c={};try{c.value=a(b),c.status="success"}catch(a){c.status="error",c.value=a}return c}function k(a){return a instanceof this?a:p.resolve(new this(d),a)}function l(a){var b=new this(d);return p.reject(b,a)}function m(a){function b(a,b){function d(a){g[b]=a,++h!==e||f||(f=!0,p.resolve(j,g))}c.resolve(a).then(d,function(a){f||(f=!0,p.reject(j,a))})}var c=this;if("[object Array]"!==Object.prototype.toString.call(a))return this.reject(new TypeError("must be an array"));var e=a.length,f=!1;if(!e)return this.resolve([]);for(var g=new Array(e),h=0,i=-1,j=new this(d);++i<e;)b(a[i],i);return j}function n(a){function b(a){c.resolve(a).then(function(a){f||(f=!0,p.resolve(h,a))},function(a){f||(f=!0,p.reject(h,a))})}var c=this;if("[object Array]"!==Object.prototype.toString.call(a))return this.reject(new TypeError("must be an array"));var e=a.length,f=!1;if(!e)return this.resolve([]);for(var g=-1,h=new this(d);++g<e;)b(a[g]);return h}var o=a(1),p={},q=["REJECTED"],r=["FULFILLED"],s=["PENDING"];b.exports=e,e.prototype.catch=function(a){return this.then(null,a)},e.prototype.then=function(a,b){if("function"!=typeof a&&this.state===r||"function"!=typeof b&&this.state===q)return this;var c=new this.constructor(d);if(this.state!==s){g(c,this.state===r?a:b,this.outcome)}else this.queue.push(new f(c,a,b));return c},f.prototype.callFulfilled=function(a){p.resolve(this.promise,a)},f.prototype.otherCallFulfilled=function(a){g(this.promise,this.onFulfilled,a)},f.prototype.callRejected=function(a){p.reject(this.promise,a)},f.prototype.otherCallRejected=function(a){g(this.promise,this.onRejected,a)},p.resolve=function(a,b){var c=j(h,b);if("error"===c.status)return p.reject(a,c.value);var d=c.value;if(d)i(a,d);else{a.state=r,a.outcome=b;for(var e=-1,f=a.queue.length;++e<f;)a.queue[e].callFulfilled(b)}return a},p.reject=function(a,b){a.state=q,a.outcome=b;for(var c=-1,d=a.queue.length;++c<d;)a.queue[c].callRejected(b);return a},e.resolve=k,e.reject=l,e.all=m,e.race=n},{1:1}],3:[function(a,b,c){(function(b){"use strict";"function"!=typeof b.Promise&&(b.Promise=a(2))}).call(this,"undefined"!=typeof global?global:"undefined"!=typeof self?self:"undefined"!=typeof window?window:{})},{2:2}],4:[function(a,b,c){"use strict";function d(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}function e(){try{if("undefined"!=typeof indexedDB)return indexedDB;if("undefined"!=typeof webkitIndexedDB)return webkitIndexedDB;if("undefined"!=typeof mozIndexedDB)return mozIndexedDB;if("undefined"!=typeof OIndexedDB)return OIndexedDB;if("undefined"!=typeof msIndexedDB)return msIndexedDB}catch(a){return}}function f(){try{if(!ua)return!1;var a="undefined"!=typeof openDatabase&&/(Safari|iPhone|iPad|iPod)/.test(navigator.userAgent)&&!/Chrome/.test(navigator.userAgent)&&!/BlackBerry/.test(navigator.platform),b="function"==typeof fetch&&-1!==fetch.toString().indexOf("[native code");return(!a||b)&&"undefined"!=typeof indexedDB&&"undefined"!=typeof IDBKeyRange}catch(a){return!1}}function g(a,b){a=a||[],b=b||{};try{return new Blob(a,b)}catch(f){if("TypeError"!==f.name)throw f;for(var c="undefined"!=typeof BlobBuilder?BlobBuilder:"undefined"!=typeof MSBlobBuilder?MSBlobBuilder:"undefined"!=typeof MozBlobBuilder?MozBlobBuilder:WebKitBlobBuilder,d=new c,e=0;e<a.length;e+=1)d.append(a[e]);return d.getBlob(b.type)}}function h(a,b){b&&a.then(function(a){b(null,a)},function(a){b(a)})}function i(a,b,c){"function"==typeof b&&a.then(b),"function"==typeof c&&a.catch(c)}function j(a){return"string"!=typeof a&&(console.warn(a+" used as a key, but it is not a string."),a=String(a)),a}function k(){if(arguments.length&&"function"==typeof arguments[arguments.length-1])return arguments[arguments.length-1]}function l(a){for(var b=a.length,c=new ArrayBuffer(b),d=new Uint8Array(c),e=0;e<b;e++)d[e]=a.charCodeAt(e);return c}function m(a){return new va(function(b){var c=a.transaction(wa,Ba),d=g([""]);c.objectStore(wa).put(d,"key"),c.onabort=function(a){a.preventDefault(),a.stopPropagation(),b(!1)},c.oncomplete=function(){var a=navigator.userAgent.match(/Chrome\/(\d+)/),c=navigator.userAgent.match(/Edge\//);b(c||!a||parseInt(a[1],10)>=43)}}).catch(function(){return!1})}function n(a){return"boolean"==typeof xa?va.resolve(xa):m(a).then(function(a){return xa=a})}function o(a){var b=ya[a.name],c={};c.promise=new va(function(a,b){c.resolve=a,c.reject=b}),b.deferredOperations.push(c),b.dbReady?b.dbReady=b.dbReady.then(function(){return c.promise}):b.dbReady=c.promise}function p(a){var b=ya[a.name],c=b.deferredOperations.pop();if(c)return c.resolve(),c.promise}function q(a,b){var c=ya[a.name],d=c.deferredOperations.pop();if(d)return d.reject(b),d.promise}function r(a,b){return new va(function(c,d){if(ya[a.name]=ya[a.name]||B(),a.db){if(!b)return c(a.db);o(a),a.db.close()}var e=[a.name];b&&e.push(a.version);var f=ua.open.apply(ua,e);b&&(f.onupgradeneeded=function(b){var c=f.result;try{c.createObjectStore(a.storeName),b.oldVersion<=1&&c.createObjectStore(wa)}catch(c){if("ConstraintError"!==c.name)throw c;console.warn('The database "'+a.name+'" has been upgraded from version '+b.oldVersion+" to version "+b.newVersion+', but the storage "'+a.storeName+'" already exists.')}}),f.onerror=function(a){a.preventDefault(),d(f.error)},f.onsuccess=function(){c(f.result),p(a)}})}function s(a){return r(a,!1)}function t(a){return r(a,!0)}function u(a,b){if(!a.db)return!0;var c=!a.db.objectStoreNames.contains(a.storeName),d=a.version<a.db.version,e=a.version>a.db.version;if(d&&(a.version!==b&&console.warn('The database "'+a.name+"\" can't be downgraded from version "+a.db.version+" to version "+a.version+"."),a.version=a.db.version),e||c){if(c){var f=a.db.version+1;f>a.version&&(a.version=f)}return!0}return!1}function v(a){return new va(function(b,c){var d=new FileReader;d.onerror=c,d.onloadend=function(c){var d=btoa(c.target.result||"");b({__local_forage_encoded_blob:!0,data:d,type:a.type})},d.readAsBinaryString(a)})}function w(a){return g([l(atob(a.data))],{type:a.type})}function x(a){return a&&a.__local_forage_encoded_blob}function y(a){var b=this,c=b._initReady().then(function(){var a=ya[b._dbInfo.name];if(a&&a.dbReady)return a.dbReady});return i(c,a,a),c}function z(a){o(a);for(var b=ya[a.name],c=b.forages,d=0;d<c.length;d++){var e=c[d];e._dbInfo.db&&(e._dbInfo.db.close(),e._dbInfo.db=null)}return a.db=null,s(a).then(function(b){return a.db=b,u(a)?t(a):b}).then(function(d){a.db=b.db=d;for(var e=0;e<c.length;e++)c[e]._dbInfo.db=d}).catch(function(b){throw q(a,b),b})}function A(a,b,c,d){void 0===d&&(d=1);try{var e=a.db.transaction(a.storeName,b);c(null,e)}catch(e){if(d>0&&(!a.db||"InvalidStateError"===e.name||"NotFoundError"===e.name))return va.resolve().then(function(){if(!a.db||"NotFoundError"===e.name&&!a.db.objectStoreNames.contains(a.storeName)&&a.version<=a.db.version)return a.db&&(a.version=a.db.version+1),t(a)}).then(function(){return z(a).then(function(){A(a,b,c,d-1)})}).catch(c);c(e)}}function B(){return{forages:[],db:null,dbReady:null,deferredOperations:[]}}function C(a){function b(){return va.resolve()}var c=this,d={db:null};if(a)for(var e in a)d[e]=a[e];var f=ya[d.name];f||(f=B(),ya[d.name]=f),f.forages.push(c),c._initReady||(c._initReady=c.ready,c.ready=y);for(var g=[],h=0;h<f.forages.length;h++){var i=f.forages[h];i!==c&&g.push(i._initReady().catch(b))}var j=f.forages.slice(0);return va.all(g).then(function(){return d.db=f.db,s(d)}).then(function(a){return d.db=a,u(d,c._defaultConfig.version)?t(d):a}).then(function(a){d.db=f.db=a,c._dbInfo=d;for(var b=0;b<j.length;b++){var e=j[b];e!==c&&(e._dbInfo.db=d.db,e._dbInfo.version=d.version)}})}function D(a,b){var c=this;a=j(a);var d=new va(function(b,d){c.ready().then(function(){A(c._dbInfo,Aa,function(e,f){if(e)return d(e);try{var g=f.objectStore(c._dbInfo.storeName),h=g.get(a);h.onsuccess=function(){var a=h.result;void 0===a&&(a=null),x(a)&&(a=w(a)),b(a)},h.onerror=function(){d(h.error)}}catch(a){d(a)}})}).catch(d)});return h(d,b),d}function E(a,b){var c=this,d=new va(function(b,d){c.ready().then(function(){A(c._dbInfo,Aa,function(e,f){if(e)return d(e);try{var g=f.objectStore(c._dbInfo.storeName),h=g.openCursor(),i=1;h.onsuccess=function(){var c=h.result;if(c){var d=c.value;x(d)&&(d=w(d));var e=a(d,c.key,i++);void 0!==e?b(e):c.continue()}else b()},h.onerror=function(){d(h.error)}}catch(a){d(a)}})}).catch(d)});return h(d,b),d}function F(a,b,c){var d=this;a=j(a);var e=new va(function(c,e){var f;d.ready().then(function(){return f=d._dbInfo,"[object Blob]"===za.call(b)?n(f.db).then(function(a){return a?b:v(b)}):b}).then(function(b){A(d._dbInfo,Ba,function(f,g){if(f)return e(f);try{var h=g.objectStore(d._dbInfo.storeName);null===b&&(b=void 0);var i=h.put(b,a);g.oncomplete=function(){void 0===b&&(b=null),c(b)},g.onabort=g.onerror=function(){var a=i.error?i.error:i.transaction.error;e(a)}}catch(a){e(a)}})}).catch(e)});return h(e,c),e}function G(a,b){var c=this;a=j(a);var d=new va(function(b,d){c.ready().then(function(){A(c._dbInfo,Ba,function(e,f){if(e)return d(e);try{var g=f.objectStore(c._dbInfo.storeName),h=g.delete(a);f.oncomplete=function(){b()},f.onerror=function(){d(h.error)},f.onabort=function(){var a=h.error?h.error:h.transaction.error;d(a)}}catch(a){d(a)}})}).catch(d)});return h(d,b),d}function H(a){var b=this,c=new va(function(a,c){b.ready().then(function(){A(b._dbInfo,Ba,function(d,e){if(d)return c(d);try{var f=e.objectStore(b._dbInfo.storeName),g=f.clear();e.oncomplete=function(){a()},e.onabort=e.onerror=function(){var a=g.error?g.error:g.transaction.error;c(a)}}catch(a){c(a)}})}).catch(c)});return h(c,a),c}function I(a){var b=this,c=new va(function(a,c){b.ready().then(function(){A(b._dbInfo,Aa,function(d,e){if(d)return c(d);try{var f=e.objectStore(b._dbInfo.storeName),g=f.count();g.onsuccess=function(){a(g.result)},g.onerror=function(){c(g.error)}}catch(a){c(a)}})}).catch(c)});return h(c,a),c}function J(a,b){var c=this,d=new va(function(b,d){if(a<0)return void b(null);c.ready().then(function(){A(c._dbInfo,Aa,function(e,f){if(e)return d(e);try{var g=f.objectStore(c._dbInfo.storeName),h=!1,i=g.openCursor();i.onsuccess=function(){var c=i.result;if(!c)return void b(null);0===a?b(c.key):h?b(c.key):(h=!0,c.advance(a))},i.onerror=function(){d(i.error)}}catch(a){d(a)}})}).catch(d)});return h(d,b),d}function K(a){var b=this,c=new va(function(a,c){b.ready().then(function(){A(b._dbInfo,Aa,function(d,e){if(d)return c(d);try{var f=e.objectStore(b._dbInfo.storeName),g=f.openCursor(),h=[];g.onsuccess=function(){var b=g.result;if(!b)return void a(h);h.push(b.key),b.continue()},g.onerror=function(){c(g.error)}}catch(a){c(a)}})}).catch(c)});return h(c,a),c}function L(a,b){b=k.apply(this,arguments);var c=this.config();a="function"!=typeof a&&a||{},a.name||(a.name=a.name||c.name,a.storeName=a.storeName||c.storeName);var d,e=this;if(a.name){var f=a.name===c.name&&e._dbInfo.db,g=f?va.resolve(e._dbInfo.db):s(a).then(function(b){var c=ya[a.name],d=c.forages;c.db=b;for(var e=0;e<d.length;e++)d[e]._dbInfo.db=b;return b});d=a.storeName?g.then(function(b){if(b.objectStoreNames.contains(a.storeName)){var c=b.version+1;o(a);var d=ya[a.name],e=d.forages;b.close();for(var f=0;f<e.length;f++){var g=e[f];g._dbInfo.db=null,g._dbInfo.version=c}return new va(function(b,d){var e=ua.open(a.name,c);e.onerror=function(a){e.result.close(),d(a)},e.onupgradeneeded=function(){e.result.deleteObjectStore(a.storeName)},e.onsuccess=function(){var a=e.result;a.close(),b(a)}}).then(function(a){d.db=a;for(var b=0;b<e.length;b++){var c=e[b];c._dbInfo.db=a,p(c._dbInfo)}}).catch(function(b){throw(q(a,b)||va.resolve()).catch(function(){}),b})}}):g.then(function(b){o(a);var c=ya[a.name],d=c.forages;b.close();for(var e=0;e<d.length;e++){d[e]._dbInfo.db=null}return new va(function(b,c){var d=ua.deleteDatabase(a.name);d.onerror=d.onblocked=function(a){var b=d.result;b&&b.close(),c(a)},d.onsuccess=function(){var a=d.result;a&&a.close(),b(a)}}).then(function(a){c.db=a;for(var b=0;b<d.length;b++)p(d[b]._dbInfo)}).catch(function(b){throw(q(a,b)||va.resolve()).catch(function(){}),b})})}else d=va.reject("Invalid arguments");return h(d,b),d}function M(){return"function"==typeof openDatabase}function N(a){var b,c,d,e,f,g=.75*a.length,h=a.length,i=0;"="===a[a.length-1]&&(g--,"="===a[a.length-2]&&g--);var j=new ArrayBuffer(g),k=new Uint8Array(j);for(b=0;b<h;b+=4)c=Da.indexOf(a[b]),d=Da.indexOf(a[b+1]),e=Da.indexOf(a[b+2]),f=Da.indexOf(a[b+3]),k[i++]=c<<2|d>>4,k[i++]=(15&d)<<4|e>>2,k[i++]=(3&e)<<6|63&f;return j}function O(a){var b,c=new Uint8Array(a),d="";for(b=0;b<c.length;b+=3)d+=Da[c[b]>>2],d+=Da[(3&c[b])<<4|c[b+1]>>4],d+=Da[(15&c[b+1])<<2|c[b+2]>>6],d+=Da[63&c[b+2]];return c.length%3==2?d=d.substring(0,d.length-1)+"=":c.length%3==1&&(d=d.substring(0,d.length-2)+"=="),d}function P(a,b){var c="";if(a&&(c=Ua.call(a)),a&&("[object ArrayBuffer]"===c||a.buffer&&"[object ArrayBuffer]"===Ua.call(a.buffer))){var d,e=Ga;a instanceof ArrayBuffer?(d=a,e+=Ia):(d=a.buffer,"[object Int8Array]"===c?e+=Ka:"[object Uint8Array]"===c?e+=La:"[object Uint8ClampedArray]"===c?e+=Ma:"[object Int16Array]"===c?e+=Na:"[object Uint16Array]"===c?e+=Pa:"[object Int32Array]"===c?e+=Oa:"[object Uint32Array]"===c?e+=Qa:"[object Float32Array]"===c?e+=Ra:"[object Float64Array]"===c?e+=Sa:b(new Error("Failed to get type for BinaryArray"))),b(e+O(d))}else if("[object Blob]"===c){var f=new FileReader;f.onload=function(){var c=Ea+a.type+"~"+O(this.result);b(Ga+Ja+c)},f.readAsArrayBuffer(a)}else try{b(JSON.stringify(a))}catch(c){console.error("Couldn't convert value into a JSON string: ",a),b(null,c)}}function Q(a){if(a.substring(0,Ha)!==Ga)return JSON.parse(a);var b,c=a.substring(Ta),d=a.substring(Ha,Ta);if(d===Ja&&Fa.test(c)){var e=c.match(Fa);b=e[1],c=c.substring(e[0].length)}var f=N(c);switch(d){case Ia:return f;case Ja:return g([f],{type:b});case Ka:return new Int8Array(f);case La:return new Uint8Array(f);case Ma:return new Uint8ClampedArray(f);case Na:return new Int16Array(f);case Pa:return new Uint16Array(f);case Oa:return new Int32Array(f);case Qa:return new Uint32Array(f);case Ra:return new Float32Array(f);case Sa:return new Float64Array(f);default:throw new Error("Unkown type: "+d)}}function R(a,b,c,d){a.executeSql("CREATE TABLE IF NOT EXISTS "+b.storeName+" (id INTEGER PRIMARY KEY, key unique, value)",[],c,d)}function S(a){var b=this,c={db:null};if(a)for(var d in a)c[d]="string"!=typeof a[d]?a[d].toString():a[d];var e=new va(function(a,d){try{c.db=openDatabase(c.name,String(c.version),c.description,c.size)}catch(a){return d(a)}c.db.transaction(function(e){R(e,c,function(){b._dbInfo=c,a()},function(a,b){d(b)})},d)});return c.serializer=Va,e}function T(a,b,c,d,e,f){a.executeSql(c,d,e,function(a,g){g.code===g.SYNTAX_ERR?a.executeSql("SELECT name FROM sqlite_master WHERE type='table' AND name = ?",[b.storeName],function(a,h){h.rows.length?f(a,g):R(a,b,function(){a.executeSql(c,d,e,f)},f)},f):f(a,g)},f)}function U(a,b){var c=this;a=j(a);var d=new va(function(b,d){c.ready().then(function(){var e=c._dbInfo;e.db.transaction(function(c){T(c,e,"SELECT * FROM "+e.storeName+" WHERE key = ? LIMIT 1",[a],function(a,c){var d=c.rows.length?c.rows.item(0).value:null;d&&(d=e.serializer.deserialize(d)),b(d)},function(a,b){d(b)})})}).catch(d)});return h(d,b),d}function V(a,b){var c=this,d=new va(function(b,d){c.ready().then(function(){var e=c._dbInfo;e.db.transaction(function(c){T(c,e,"SELECT * FROM "+e.storeName,[],function(c,d){for(var f=d.rows,g=f.length,h=0;h<g;h++){var i=f.item(h),j=i.value;if(j&&(j=e.serializer.deserialize(j)),void 0!==(j=a(j,i.key,h+1)))return void b(j)}b()},function(a,b){d(b)})})}).catch(d)});return h(d,b),d}function W(a,b,c,d){var e=this;a=j(a);var f=new va(function(f,g){e.ready().then(function(){void 0===b&&(b=null);var h=b,i=e._dbInfo;i.serializer.serialize(b,function(b,j){j?g(j):i.db.transaction(function(c){T(c,i,"INSERT OR REPLACE INTO "+i.storeName+" (key, value) VALUES (?, ?)",[a,b],function(){f(h)},function(a,b){g(b)})},function(b){if(b.code===b.QUOTA_ERR){if(d>0)return void f(W.apply(e,[a,h,c,d-1]));g(b)}})})}).catch(g)});return h(f,c),f}function X(a,b,c){return W.apply(this,[a,b,c,1])}function Y(a,b){var c=this;a=j(a);var d=new va(function(b,d){c.ready().then(function(){var e=c._dbInfo;e.db.transaction(function(c){T(c,e,"DELETE FROM "+e.storeName+" WHERE key = ?",[a],function(){b()},function(a,b){d(b)})})}).catch(d)});return h(d,b),d}function Z(a){var b=this,c=new va(function(a,c){b.ready().then(function(){var d=b._dbInfo;d.db.transaction(function(b){T(b,d,"DELETE FROM "+d.storeName,[],function(){a()},function(a,b){c(b)})})}).catch(c)});return h(c,a),c}function $(a){var b=this,c=new va(function(a,c){b.ready().then(function(){var d=b._dbInfo;d.db.transaction(function(b){T(b,d,"SELECT COUNT(key) as c FROM "+d.storeName,[],function(b,c){var d=c.rows.item(0).c;a(d)},function(a,b){c(b)})})}).catch(c)});return h(c,a),c}function _(a,b){var c=this,d=new va(function(b,d){c.ready().then(function(){var e=c._dbInfo;e.db.transaction(function(c){T(c,e,"SELECT key FROM "+e.storeName+" WHERE id = ? LIMIT 1",[a+1],function(a,c){var d=c.rows.length?c.rows.item(0).key:null;b(d)},function(a,b){d(b)})})}).catch(d)});return h(d,b),d}function aa(a){var b=this,c=new va(function(a,c){b.ready().then(function(){var d=b._dbInfo;d.db.transaction(function(b){T(b,d,"SELECT key FROM "+d.storeName,[],function(b,c){for(var d=[],e=0;e<c.rows.length;e++)d.push(c.rows.item(e).key);a(d)},function(a,b){c(b)})})}).catch(c)});return h(c,a),c}function ba(a){return new va(function(b,c){a.transaction(function(d){d.executeSql("SELECT name FROM sqlite_master WHERE type='table' AND name <> '__WebKitDatabaseInfoTable__'",[],function(c,d){for(var e=[],f=0;f<d.rows.length;f++)e.push(d.rows.item(f).name);b({db:a,storeNames:e})},function(a,b){c(b)})},function(a){c(a)})})}function ca(a,b){b=k.apply(this,arguments);var c=this.config();a="function"!=typeof a&&a||{},a.name||(a.name=a.name||c.name,a.storeName=a.storeName||c.storeName);var d,e=this;return d=a.name?new va(function(b){var d;d=a.name===c.name?e._dbInfo.db:openDatabase(a.name,"","",0),b(a.storeName?{db:d,storeNames:[a.storeName]}:ba(d))}).then(function(a){return new va(function(b,c){a.db.transaction(function(d){function e(a){return new va(function(b,c){d.executeSql("DROP TABLE IF EXISTS "+a,[],function(){b()},function(a,b){c(b)})})}for(var f=[],g=0,h=a.storeNames.length;g<h;g++)f.push(e(a.storeNames[g]));va.all(f).then(function(){b()}).catch(function(a){c(a)})},function(a){c(a)})})}):va.reject("Invalid arguments"),h(d,b),d}function da(){try{return"undefined"!=typeof localStorage&&"setItem"in localStorage&&!!localStorage.setItem}catch(a){return!1}}function ea(a,b){var c=a.name+"/";return a.storeName!==b.storeName&&(c+=a.storeName+"/"),c}function fa(){var a="_localforage_support_test";try{return localStorage.setItem(a,!0),localStorage.removeItem(a),!1}catch(a){return!0}}function ga(){return!fa()||localStorage.length>0}function ha(a){var b=this,c={};if(a)for(var d in a)c[d]=a[d];return c.keyPrefix=ea(a,b._defaultConfig),ga()?(b._dbInfo=c,c.serializer=Va,va.resolve()):va.reject()}function ia(a){var b=this,c=b.ready().then(function(){for(var a=b._dbInfo.keyPrefix,c=localStorage.length-1;c>=0;c--){var d=localStorage.key(c);0===d.indexOf(a)&&localStorage.removeItem(d)}});return h(c,a),c}function ja(a,b){var c=this;a=j(a);var d=c.ready().then(function(){var b=c._dbInfo,d=localStorage.getItem(b.keyPrefix+a);return d&&(d=b.serializer.deserialize(d)),d});return h(d,b),d}function ka(a,b){var c=this,d=c.ready().then(function(){for(var b=c._dbInfo,d=b.keyPrefix,e=d.length,f=localStorage.length,g=1,h=0;h<f;h++){var i=localStorage.key(h);if(0===i.indexOf(d)){var j=localStorage.getItem(i);if(j&&(j=b.serializer.deserialize(j)),void 0!==(j=a(j,i.substring(e),g++)))return j}}});return h(d,b),d}function la(a,b){var c=this,d=c.ready().then(function(){var b,d=c._dbInfo;try{b=localStorage.key(a)}catch(a){b=null}return b&&(b=b.substring(d.keyPrefix.length)),b});return h(d,b),d}function ma(a){var b=this,c=b.ready().then(function(){for(var a=b._dbInfo,c=localStorage.length,d=[],e=0;e<c;e++){var f=localStorage.key(e);0===f.indexOf(a.keyPrefix)&&d.push(f.substring(a.keyPrefix.length))}return d});return h(c,a),c}function na(a){var b=this,c=b.keys().then(function(a){return a.length});return h(c,a),c}function oa(a,b){var c=this;a=j(a);var d=c.ready().then(function(){var b=c._dbInfo;localStorage.removeItem(b.keyPrefix+a)});return h(d,b),d}function pa(a,b,c){var d=this;a=j(a);var e=d.ready().then(function(){void 0===b&&(b=null);var c=b;return new va(function(e,f){var g=d._dbInfo;g.serializer.serialize(b,function(b,d){if(d)f(d);else try{localStorage.setItem(g.keyPrefix+a,b),e(c)}catch(a){"QuotaExceededError"!==a.name&&"NS_ERROR_DOM_QUOTA_REACHED"!==a.name||f(a),f(a)}})})});return h(e,c),e}function qa(a,b){if(b=k.apply(this,arguments),a="function"!=typeof a&&a||{},!a.name){var c=this.config();a.name=a.name||c.name,a.storeName=a.storeName||c.storeName}var d,e=this;return d=a.name?new va(function(b){b(a.storeName?ea(a,e._defaultConfig):a.name+"/")}).then(function(a){for(var b=localStorage.length-1;b>=0;b--){var c=localStorage.key(b);0===c.indexOf(a)&&localStorage.removeItem(c)}}):va.reject("Invalid arguments"),h(d,b),d}function ra(a,b){a[b]=function(){var c=arguments;return a.ready().then(function(){return a[b].apply(a,c)})}}function sa(){for(var a=1;a<arguments.length;a++){var b=arguments[a];if(b)for(var c in b)b.hasOwnProperty(c)&&($a(b[c])?arguments[0][c]=b[c].slice():arguments[0][c]=b[c])}return arguments[0]}var ta="function"==typeof Symbol&&"symbol"==typeof Symbol.iterator?function(a){return typeof a}:function(a){return a&&"function"==typeof Symbol&&a.constructor===Symbol&&a!==Symbol.prototype?"symbol":typeof a},ua=e();"undefined"==typeof Promise&&a(3);var va=Promise,wa="local-forage-detect-blob-support",xa=void 0,ya={},za=Object.prototype.toString,Aa="readonly",Ba="readwrite",Ca={_driver:"asyncStorage",_initStorage:C,_support:f(),iterate:E,getItem:D,setItem:F,removeItem:G,clear:H,length:I,key:J,keys:K,dropInstance:L},Da="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/",Ea="~~local_forage_type~",Fa=/^~~local_forage_type~([^~]+)~/,Ga="__lfsc__:",Ha=Ga.length,Ia="arbf",Ja="blob",Ka="si08",La="ui08",Ma="uic8",Na="si16",Oa="si32",Pa="ur16",Qa="ui32",Ra="fl32",Sa="fl64",Ta=Ha+Ia.length,Ua=Object.prototype.toString,Va={serialize:P,deserialize:Q,stringToBuffer:N,bufferToString:O},Wa={_driver:"webSQLStorage",_initStorage:S,_support:M(),iterate:V,getItem:U,setItem:X,removeItem:Y,clear:Z,length:$,key:_,keys:aa,dropInstance:ca},Xa={_driver:"localStorageWrapper",_initStorage:ha,_support:da(),iterate:ka,getItem:ja,setItem:pa,removeItem:oa,clear:ia,length:na,key:la,keys:ma,dropInstance:qa},Ya=function(a,b){return a===b||"number"==typeof a&&"number"==typeof b&&isNaN(a)&&isNaN(b)},Za=function(a,b){for(var c=a.length,d=0;d<c;){if(Ya(a[d],b))return!0;d++}return!1},$a=Array.isArray||function(a){return"[object Array]"===Object.prototype.toString.call(a)},_a={},ab={},bb={INDEXEDDB:Ca,WEBSQL:Wa,LOCALSTORAGE:Xa},cb=[bb.INDEXEDDB._driver,bb.WEBSQL._driver,bb.LOCALSTORAGE._driver],db=["dropInstance"],eb=["clear","getItem","iterate","key","keys","length","removeItem","setItem"].concat(db),fb={description:"",driver:cb.slice(),name:"localforage",size:4980736,storeName:"keyvaluepairs",version:1},gb=function(){function a(b){d(this,a);for(var c in bb)if(bb.hasOwnProperty(c)){var e=bb[c],f=e._driver;this[c]=f,_a[f]||this.defineDriver(e)}this._defaultConfig=sa({},fb),this._config=sa({},this._defaultConfig,b),this._driverSet=null,this._initDriver=null,this._ready=!1,this._dbInfo=null,this._wrapLibraryMethodsWithReady(),this.setDriver(this._config.driver).catch(function(){})}return a.prototype.config=function(a){if("object"===(void 0===a?"undefined":ta(a))){if(this._ready)return new Error("Can't call config() after localforage has been used.");for(var b in a){if("storeName"===b&&(a[b]=a[b].replace(/\W/g,"_")),"version"===b&&"number"!=typeof a[b])return new Error("Database version must be a number.");this._config[b]=a[b]}return!("driver"in a&&a.driver)||this.setDriver(this._config.driver)}return"string"==typeof a?this._config[a]:this._config},a.prototype.defineDriver=function(a,b,c){var d=new va(function(b,c){try{var d=a._driver,e=new Error("Custom driver not compliant; see https://mozilla.github.io/localForage/#definedriver");if(!a._driver)return void c(e);for(var f=eb.concat("_initStorage"),g=0,i=f.length;g<i;g++){var j=f[g];if((!Za(db,j)||a[j])&&"function"!=typeof a[j])return void c(e)}(function(){for(var b=function(a){return function(){var b=new Error("Method "+a+" is not implemented by the current driver"),c=va.reject(b);return h(c,arguments[arguments.length-1]),c}},c=0,d=db.length;c<d;c++){var e=db[c];a[e]||(a[e]=b(e))}})();var k=function(c){_a[d]&&console.info("Redefining LocalForage driver: "+d),_a[d]=a,ab[d]=c,b()};"_support"in a?a._support&&"function"==typeof a._support?a._support().then(k,c):k(!!a._support):k(!0)}catch(a){c(a)}});return i(d,b,c),d},a.prototype.driver=function(){return this._driver||null},a.prototype.getDriver=function(a,b,c){var d=_a[a]?va.resolve(_a[a]):va.reject(new Error("Driver not found."));return i(d,b,c),d},a.prototype.getSerializer=function(a){var b=va.resolve(Va);return i(b,a),b},a.prototype.ready=function(a){var b=this,c=b._driverSet.then(function(){return null===b._ready&&(b._ready=b._initDriver()),b._ready});return i(c,a,a),c},a.prototype.setDriver=function(a,b,c){function d(){g._config.driver=g.driver()}function e(a){return g._extend(a),d(),g._ready=g._initStorage(g._config),g._ready}function f(a){return function(){function b(){for(;c<a.length;){var f=a[c];return c++,g._dbInfo=null,g._ready=null,g.getDriver(f).then(e).catch(b)}d();var h=new Error("No available storage method found.");return g._driverSet=va.reject(h),g._driverSet}var c=0;return b()}}var g=this;$a(a)||(a=[a]);var h=this._getSupportedDrivers(a),j=null!==this._driverSet?this._driverSet.catch(function(){return va.resolve()}):va.resolve();return this._driverSet=j.then(function(){var a=h[0];return g._dbInfo=null,g._ready=null,g.getDriver(a).then(function(a){g._driver=a._driver,d(),g._wrapLibraryMethodsWithReady(),g._initDriver=f(h)})}).catch(function(){d();var a=new Error("No available storage method found.");return g._driverSet=va.reject(a),g._driverSet}),i(this._driverSet,b,c),this._driverSet},a.prototype.supports=function(a){return!!ab[a]},a.prototype._extend=function(a){sa(this,a)},a.prototype._getSupportedDrivers=function(a){for(var b=[],c=0,d=a.length;c<d;c++){var e=a[c];this.supports(e)&&b.push(e)}return b},a.prototype._wrapLibraryMethodsWithReady=function(){for(var a=0,b=eb.length;a<b;a++)ra(this,eb[a])},a.prototype.createInstance=function(b){return new a(b)},a}(),hb=new gb;b.exports=hb},{3:3}]},{},[4])(4)});window.URL = window.URL || window.webkitURL || null;

window.Base64ToBlob = function(dataURL) {
  var BASE64_MARKER = ';base64,';
  if (dataURL.indexOf(BASE64_MARKER) == -1) {
    var parts = dataURL.split(',');
    var contentType = parts[0].split(':')[1];
    var raw = decodeURIComponent(parts[1]);

    return new Blob([raw], {type: contentType});
  }

  var parts = dataURL.split(BASE64_MARKER);
  var contentType = parts[0].split(':')[1];
  var raw = window.atob(parts[1].replace(/\n/g,''));
  var rawLength = raw.length;

  var uInt8Array = new Uint8Array(rawLength);

  for (var i = 0; i < rawLength; ++i) {
    uInt8Array[i] = raw.charCodeAt(i);
  }

  return new Blob([uInt8Array], {type: contentType});
};

window.getParameterByName = function ( name ){
    var regexS = "[\\?&]"+name+"=([^&#]*)", 
  regex = new RegExp( regexS ),
  results = regex.exec( window.location.search );
  if( results == null ){
    return "";
  } else{
    return decodeURIComponent(results[1].replace(/\+/g, " "));
  }
};

window.BlobToBase64 = function(blob, onload) {
  var reader = new FileReader();
  reader.readAsDataURL(blob);
  reader.onloadend = function() {
    onload(reader.result);
  };
};

window.arrayBufferToBase64 = function(buffer){
    var binary = '';
    var bytes = new Uint8Array( buffer );
    var len = bytes.byteLength;
    for (var i = 0; i < len; i++) {
        binary += String.fromCharCode( bytes[ i ] );
    }
    return window.btoa( binary );
};

window.getCorsProxyURL = function(){
    return window.cn1CORSProxyURL || null;
};

window.getCN1DeploymentType = function(){
    return window.cn1DeploymentType || "war";
};



(function(exports) {

// do nothing if the Storage Info API is already available
if (exports.webkitStorageInfo) {
  return;
}

if (typeof exports.TEMPORARY == "undefined") {
  exports.TEMPORARY = 0,
  exports.PERSISTENT = 1
}

exports.webkitStorageInfo = {
  TEMPORARY: exports.TEMPORARY,
  PERSISTENT: exports.PERSISTENT,
}

var UNSUPPORTED_STORAGE_TYPE = "Unsupported storage type";

function requestQuota(type, size, successCallback, errorCallback) {
  if (type != exports.TEMPORARY && type != exports.PERSISTENT) {
    if (errorCallback) {
      errorCallback(UNSUPPORTED_STORAGE_TYPE);
    }
    return;
  }
  successCallback(size);
}
function queryUsageAndQuota(type, successCallback, errorCallback) {
  if (type != exports.TEMPORARY && type != exports.PERSISTENT) {
    if (errorCallback) {
      errorCallback(UNSUPPORTED_STORAGE_TYPE);
    }
    return;
  }
  successCallback(0, 0);
}
exports.webkitStorageInfo.requestQuota = requestQuota;
exports.webkitStorageInfo.queryUsageAndQuota = queryUsageAndQuota;

})(window);


(function(){
var $ = jQuery;
if (!HTMLCanvasElement.prototype.toBlob) {
 Object.defineProperty(HTMLCanvasElement.prototype, 'toBlob', {
  value: function (callback, type, quality) {

    var binStr = atob( this.toDataURL(type, quality).split(',')[1] ),
        len = binStr.length,
        arr = new Uint8Array(len);

    for (var i=0; i<len; i++ ) {
     arr[i] = binStr.charCodeAt(i);
    }

    callback( new Blob( [arr], {type: type || 'image/png'} ) );
  }
 });
}
window.requestFileSystem  = window.requestFileSystem || window.webkitRequestFileSystem;
navigator.persistentStorage = navigator.persistentStorage || navigator.webkitPersistentStorage;
navigator.temporaryStorage = navigator.temporaryStorage || navigator.webkitTemporaryStorage;
navigator.getUserMedia  = navigator.getUserMedia ||
                          navigator.webkitGetUserMedia ||
                          navigator.mozGetUserMedia ||
                          navigator.msGetUserMedia;
    
    
  window.cn1 = {};
  var cn1 = window.cn1;


  var measureText = CanvasRenderingContext2D.prototype.measureText;
  CanvasRenderingContext2D.prototype.measureText = function(textstring) {
      var metrics = measureText.call(this, textstring);
      //console.log('font', this.font, metrics);
      if (typeof(metrics.height)=== 'undefined'){
          metrics.height = parseInt(/[0-9]+(?=pt|px)/.exec(this.font));
          
      }
      if (metrics.ascent === undefined) {
          metrics.ascent = measureText.call(this, "M").width;
      }
      
      if (metrics.descent === undefined) {
          metrics.descent = metrics.height - metrics.ascent;
      }
      //console.log('font after', this.font, metrics);
      return metrics;
  }
 

    var pixel = null;
    var measureTextNode = null;
    var measureTextCache = null;
    window.measureAscentDescent = function (fontFamily) {
        measureTextCache = measureTextCache || {};
        if (measureTextCache[fontFamily]) {
            return measureTextCache[fontFamily];
        }
        measureTextNode = measureTextNode || jQuery('<div style="position:absolute; top:0; left:0; height: 200px; line-height:1.0;padding:0;display:none">TheQuickBrownFoxquickly!</div>').get(0);
        pixel = pixel || jQuery('<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==" width="42" height="1"/>').get(0);
        if (measureTextNode.parentNode != jQuery('body').get(0)) {
            jQuery('body').append(measureTextNode);
        }
        if (pixel.parentNode != measureTextNode.parentNode) {
            
            measureTextNode.appendChild(pixel);
        }
        
        
        measureTextNode.style.fontFamily = fontFamily;
        measureTextNode.style.fontSize = "100px";
        pixel.style.verticalAlign = "text-top";
        measureTextNode.style.display='';
        var top = pixel.offsetTop - measureTextNode.offsetTop + 1;
        pixel.style.verticalAlign = "baseline";
        var baseline = pixel.offsetTop - measureTextNode.offsetTop + 1;
        
        pixel.style.verticalAlign="text-bottom";
        var bottom = pixel.offsetTop - measureTextNode.offsetTop + 1;
        
        var result = {
            ascent: (baseline-top)/100.0,
            descent: (bottom-baseline)/100.0
        };
        measureTextNode.style.display='none';
        measureTextCache[fontFamily] = result;
        //console.log(fontFamily, result);
        return result;
    }
  
    window.measureTextAscent = function(fontFamily) { return window.measureAscentDescent(fontFamily).ascent;}
    window.measureTextDescent = function(fontFamily) { return window.measureAscentDescent(fontFamily).descent;} 
  
  
    var isMobile = {
        Android: function() {
            return navigator.userAgent.match(/Android/i);
        },
        BlackBerry: function() {
            return navigator.userAgent.match(/BlackBerry/i);
        },
        iOS: function() {
            return navigator.userAgent.match(/iPhone|iPad|iPod/i) ||  (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
        },
        Opera: function() {
            return navigator.userAgent.match(/Opera Mini/i);
        },
        Windows: function() {
            return navigator.userAgent.match(/IEMobile/i) || navigator.userAgent.match(/WPDesktop/i);
        },
        any: function() {
            
            return (isMobile.Android() || isMobile.BlackBerry() || isMobile.iOS() || isMobile.Opera() || isMobile.Windows());
        }
    };
    
    cn1.isMobile = isMobile;
    
    
    function simulateClick(el) {
        var evt;
        if (document.createEvent) {
            evt = document.createEvent("MouseEvents");
            evt.initMouseEvent("click", true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
        }
        (evt) ? el.dispatchEvent(evt) : (el.click && el.click());
    }
    
    function capturePhotoWithFileButton(callback, targetWidth, targetHeight){
        var fileBtn = $('<input id="cn1-image-picker" type="file" accept="image/*" />');
        var dialog = document.createElement('div');
        dialog.appendChild(fileBtn.get(0));
        dialog.className = 'cn1-capture-dialog';

        document.querySelector('body').appendChild(dialog);
        fileBtn.change(function(event){
            var files = event.target.files;
            if (files.length>0){
            
                var reader = new FileReader();
                var img = $('<img>').get(0);
                $(img).on('load', function(){
                    $(dialog).fadeOut(100, function(){
                        dialog.parentNode.removeChild(dialog);
                    });
                    var width=targetWidth;
                    var height=targetHeight;
                    if (width<img.naturalWidth){
                        height=height*width/img.naturalWidth;
                        width=img.naturalWidth;
                    }
                    if (height<img.naturalHeight){
                        width=width*height/img.naturalHeight;
                        height=img.naturalHeight;
                    }
                    var outCanvas = document.createElement('canvas');
                    outCanvas.setAttribute('width', width);
                    outCanvas.setAttribute('height', height);
                    var ctx = outCanvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);
                    callback(outCanvas);
                });
                $(img).on('error', function(){
                    console.log('Error loading image');
                    $(dialog).fadeOut(100, function(){
                        dialog.parentNode.removeChild(dialog);
                    });
                    callback(null);
                });
                reader.onload = function(e){
                    img.src=e.target.result;
                };
                reader.readAsDataURL(files[0]);
                
                
            }
        });
    }
    cn1.capturePhoto = navigator.getUserMedia ? function(){} : 
            capturePhotoWithFileButton;
    
    
    // Used for the Preview function for embedding all assets in the single HTML file
    // as data URLs
    cn1.getBundledAssetAsDataURL = function(assetName){
        if (assetName.indexOf('/') != -1) {
            assetName = assetName.substr(assetName.indexOf('/')+1);
        }
        var assets = window.cn1Assets || {};
        return assets[assetName] || null;
    };
    
    
    cn1.proxifyContent = function(url, pageContent, iframe) {
        var $ = jQuery;
        
        var parser = new DOMParser();
        var xmls = new XMLSerializer();
        
        var doc = parser.parseFromString(pageContent, "text/html");
        
        function resolve(url, base_url, doc) {
          var old_base = doc.getElementsByTagName('base')[0]
            , old_href = old_base && old_base.href
            , doc_head = doc.head || doc.getElementsByTagName('head')[0]
            , our_base = old_base || doc_head.appendChild(doc.createElement('base'))
            , resolver = doc.createElement('a')
            , resolved_url
            ;
          our_base.href = base_url || '';
          resolver.href = url;
          resolved_url  = resolver.href; // browser magic at work here
        
          if (old_base) old_base.href = old_href;
          else doc_head.removeChild(our_base);
          return resolved_url;
        }
        
        $('[href]', doc).each(function() {
            var absUrl = resolve($(this).attr('href'), url, doc);
            $(this).attr('href', absUrl);
        });
        $('[src]', doc).each(function() {
            var absUrl = resolve($(this).attr('src'), url, doc);
            $(this).attr('src', absUrl);
        });
        
        pageContent = xmls.serializeToString(doc);
        // This fixes a bug with wrongly encoded comments in script tags.  gah!!
        pageContent = pageContent.replace(/>&lt;!--/g, '><!--').replace(/--&gt;</g, '--><');
        //console.log(pageContent);
        
        iframe.contentWindow.document.open();
        iframe.contentWindow.document.write(pageContent);
        iframe.contentWindow.document.close();
        doc = iframe.contentWindow.document;
        var whenReady = function() { 
          var links = doc.querySelectorAll('a[href]');
          [].forEach.call(links, function(el, index, array) {
              el.addEventListener("click", function(evt) {
                  var absUrl = resolve(el.getAttribute('href'), url, doc);
                  $(iframe).trigger('cn1load', [$rt_str(absUrl)]);
                  evt.preventDefault();
                  return false;
              });
          });
        };
        if (doc.readyState == 'complete') {
            whenReady();
        } else {
            doc.addEventListener("DOMContentLoaded", whenReady);

        }
        

       
    };
    
})();


/**
 *  The Virtual Keyboard Detector
 *  https://github.com/GillesVermeulen/virtualKeyboardDetector
 */

window.virtualKeyboardDetector = ( function( window, undefined ) {

  var recentlyFocusedTimeoutDuration = 3000;
  
  var currentViewportWidth = previousViewportWidth = viewportWidthWithoutVirtualKeyboard = window.innerWidth;
  var currentViewportHeight = previousViewportHeight = viewportHeightWithoutVirtualKeyboard = window.innerHeight;

  var virtualKeyboardVisible = false;
  var recentlyFocused = false;
  var recentlyFocusedTimeout = null;
  var validFocusableElements = [ 'INPUT', 'TEXTAREA' ];

  var subscriptions = {};


  /**
   * Public functions
   */

  function init ( options ) {
    if ( typeof options !== 'undefined' ) {
      if ( typeof options.recentlyFocusedTimeoutDuration !== 'undefined' ) recentlyFocusedTimeoutDuration = options.recentlyFocusedTimeoutDuration;
    }

    resetViewportSizes();
    initFocusListener();
    initResizeListener();
  }

  function isVirtualKeyboardVisible () {
    return virtualKeyboardVisible;
  }

  function getVirtualKeyboardSize () {
    if ( !virtualKeyboardVisible ) return false;
    
    return {
      width: currentViewportWidth,
      height: viewportHeightWithoutVirtualKeyboard - currentViewportHeight
    };
  }

  // Subscribe
  function on ( eventName, fn ) {
    if ( typeof subscriptions[eventName] === 'undefined' ) subscriptions[eventName] = [];

    subscriptions[eventName].push(fn);
  }

  // Unsubscribe
  function off ( eventName, fn ) {
    if (typeof subscriptions[eventName] === 'undefined' ) return;

    if (typeof fn === 'undefined') {
      subscriptions[eventName] = [];
    } else {
      var i = subscriptions[eventName].length;
      while ( i-- ) {
        if ( subscriptions[eventName][i] == fn ) subscriptions[eventName].splice(i, 1);
      }
    }
  }

  // Publish
  function trigger ( eventName, args ) {
    for ( i in subscriptions[eventName] ) {
      if ( typeof subscriptions[eventName][i] === 'function' ) subscriptions[eventName][i]( args );
    }   
  }


  /**
   * Private functions
   */

  // Reset all sizes. We presume the virtual keyboard is not visible at this stage.
  // We call this function on initialisation, so make sure you initialise the virtualKeyBoardListener at a moment when the virtual keyboard is likely to be invisible.
  function resetViewportSizes () {
    currentViewportWidth = previousViewportWidth = viewportWidthWithoutVirtualKeyboard = window.innerWidth;
    currentViewportHeight = previousViewportHeight = viewportHeightWithoutVirtualKeyboard = window.innerHeight;
  }

  // Initialise the listener that checks for focus events in the whole document. This way we can also handle dynamically added focusable elements.
  function initFocusListener () {
    document.addEventListener( 'focus', documentFocusHandler, true );
  }

  // Handle the document focus event. We check if the target was a valid focusable element.
  function documentFocusHandler (e) {
    if (typeof e.target !== 'undefined' && typeof e.target.nodeName !== 'undefined') {
      if (validFocusableElements.indexOf(e.target.nodeName) != -1) elementFocusHandler(e);
    }
  }

  // Handle the case when a valid focusable element is focused. We flag that a valid element was recently focused. This flag expires after recentlyFocusedTimeoutDuration.
  function elementFocusHandler (e) {
    if ( recentlyFocusedTimeout != null ) {
      window.clearTimeout( recentlyFocusedTimeout );
      recentlyFocusedTimeout = null;
    }

    recentlyFocused = true;

    recentlyFocusedTimeout = window.setTimeout( expireRecentlyFocused, recentlyFocusedTimeoutDuration );
  }

  function expireRecentlyFocused () {
    recentlyFocused = false;
  }

  function initResizeListener () {
    window.addEventListener( 'resize', resizeHandler );
  }

  function resizeHandler () {
    currentViewportWidth = window.innerWidth;
    currentViewportHeight = window.innerHeight;

    // If the virtual keyboard is tought to be visible, but the viewport height returns to the value before keyboard was visible, we presume the keyboard was hidden.
    if ( virtualKeyboardVisible && currentViewportWidth == previousViewportWidth && currentViewportHeight >= viewportHeightWithoutVirtualKeyboard ) {
      virtualKeyboardHiddenHandler();
    }

    // If the width of the viewport is changed, it's hard to tell wether virtual keyboard is still visible, so we make sure it's not.
    if ( currentViewportWidth != previousViewportWidth ) {
      if ( 'activeElement' in document )
        document.activeElement.blur();
      virtualKeyboardHiddenHandler();
    }

    // If recently focused and viewport height is smaller then previous height, we presume that the virtual keyboard has appeared.
    if ( !virtualKeyboardVisible && recentlyFocused && currentViewportWidth == previousViewportWidth && currentViewportHeight < previousViewportHeight ) {
      virtualKeyboardVisibleHandler();
    }   

    // If the keyboard is presumed not visible, we save the current measurements as values before keyboard was shown.
    if ( virtualKeyboardVisible == false ) {
      viewportWidthWithoutVirtualKeyboard = currentViewportWidth;
      viewportHeightWithoutVirtualKeyboard = currentViewportHeight;
    }

    previousViewportWidth = currentViewportWidth;
    previousViewportHeight = currentViewportHeight;
  }

  function virtualKeyboardVisibleHandler () {
    virtualKeyboardVisible = true;

    var eventData = {
      virtualKeyboardVisible: virtualKeyboardVisible,
      sizes: getSizesData()
    };

    trigger( 'virtualKeyboardVisible', eventData );
  }

  function virtualKeyboardHiddenHandler () {
    virtualKeyboardVisible = false;

    var eventData = {
      virtualKeyboardVisible: virtualKeyboardVisible,
      sizes: getSizesData()
    };

    trigger( 'virtualKeyboardHidden', eventData );
  }

  function getSizesData () {
    return {
      viewportWithoutVirtualKeyboard: {
        width: viewportWidthWithoutVirtualKeyboard,
        height: viewportHeightWithoutVirtualKeyboard
      },
      currentViewport: {
        width: currentViewportWidth,
        height: currentViewportHeight
      },
      virtualKeyboard: {
        width: currentViewportWidth,
        height: viewportHeightWithoutVirtualKeyboard - currentViewportHeight
      }
    };
  }

  // Make public functions available
  return {
    init: init,
    isVirtualKeyboardVisible: isVirtualKeyboardVisible,
    getVirtualKeyboardSize: getVirtualKeyboardSize,
    on: on,
    addEventListener: on,
    subscribe: on,
    off: off,
    removeEventListener: off,
    unsubscribe: off,
    trigger: trigger,
    publish: trigger,
    dispatchEvent: trigger
  };

} )( window );


(function() {
    window.cn1GetImageOrientation = getOrientation;
    window.cn1ResetImageOrientation = resetImageOrientation;
    
    function resetImageOrientation(srcBlob, srcOrientation, callback) {
        var img = new Image();    

        img.onload = function() {
          var width = img.width,
              height = img.height,
              canvas = document.createElement('canvas'),
              ctx = canvas.getContext("2d");

          // set proper canvas dimensions before transform & export
          if (4 < srcOrientation && srcOrientation < 9) {
            canvas.width = height;
            canvas.height = width;
          } else {
            canvas.width = width;
            canvas.height = height;
          }

          // transform context before drawing image
          switch (srcOrientation) {
            case 2: ctx.transform(-1, 0, 0, 1, width, 0); break;
            case 3: ctx.transform(-1, 0, 0, -1, width, height); break;
            case 4: ctx.transform(1, 0, 0, -1, 0, height); break;
            case 5: ctx.transform(0, 1, 1, 0, 0, 0); break;
            case 6: ctx.transform(0, 1, -1, 0, height, 0); break;
            case 7: ctx.transform(0, -1, -1, 0, height, width); break;
            case 8: ctx.transform(0, -1, 1, 0, 0, width); break;
            default: break;
          }

          // draw image
          ctx.drawImage(img, 0, 0);

          // export blob
          
          
          callback(canvas);
        };

        img.src = URL.createObjectURL(srcBlob);
      };
    
    
    function getOrientation(file, callback) {
        var reader = new FileReader();
        reader.onload = function(e) {

            var view = new DataView(e.target.result);
            if (view.getUint16(0, false) != 0xFFD8)
            {
                return callback(-2);
            }
            var length = view.byteLength, offset = 2;
            while (offset < length) 
            {
                if (view.getUint16(offset+2, false) <= 8) return callback(-1);
                var marker = view.getUint16(offset, false);
                offset += 2;
                if (marker == 0xFFE1) 
                {
                    if (view.getUint32(offset += 2, false) != 0x45786966) 
                    {
                        return callback(-1);
                    }

                    var little = view.getUint16(offset += 6, false) == 0x4949;
                    offset += view.getUint32(offset + 4, little);
                    var tags = view.getUint16(offset, little);
                    offset += 2;
                    for (var i = 0; i < tags; i++)
                    {
                        if (view.getUint16(offset + (i * 12), little) == 0x0112)
                        {
                            return callback(view.getUint16(offset + (i * 12) + 8, little));
                        }
                    }
                }
                else if ((marker & 0xFF00) != 0xFF00)
                {
                    break;
                }
                else
                { 
                    offset += view.getUint16(offset, false);
                }
            }
            return callback(-1);
        };
        reader.readAsArrayBuffer(file);
    }

})();

(function() {
   if (!window.MediaRecorder) {
       var AudioContext = window.AudioContext || window.webkitAudioContext

        function createWorker (fn) {
          var js = fn
            .toString()
            .replace(/^function\s*\(\)\s*{/, '')
            .replace(/}$/, '')
          var blob = new Blob([js])
          return new Worker(URL.createObjectURL(blob))
        }

        function error (method) {
          var event = new Event('error')
          event.data = new Error('Wrong state for ' + method)
          return event
        }

        var context, processor

        /**
         * Audio Recorder with MediaRecorder API.
         *
         * @param {MediaStream} stream The audio stream to record.
         *
         * @example
         * navigator.mediaDevices.getUserMedia({ audio: true }).then(function (stream) {
         *   var recorder = new MediaRecorder(stream)
         * })
         *
         * @class
         */
        function MediaRecorder (stream) {
          /**
           * The `MediaStream` passed into the constructor.
           * @type {MediaStream}
           */
          this.stream = stream

          /**
           * The current state of recording process.
           * @type {"inactive"|"recording"|"paused"}
           */
          this.state = 'inactive'

          this.em = document.createDocumentFragment()
          this.encoder = createWorker(MediaRecorder.encoder)

          var recorder = this
          this.encoder.addEventListener('message', function (e) {
            var event = new Event('dataavailable')
            event.data = new Blob([e.data], { type: recorder.mimeType })
            recorder.em.dispatchEvent(event)
            if (recorder.state === 'inactive') {
              recorder.em.dispatchEvent(new Event('stop'))
            }
          })
        }

        MediaRecorder.prototype = {
          /**
           * The MIME type that is being used for recording.
           * @type {string}
           */
          mimeType: 'audio/wav',

          /**
           * Begins recording media.
           *
           * @param {number} [timeslice] The milliseconds to record into each `Blob`.
           *                             If this parameter isn’t included, single `Blob`
           *                             will be recorded.
           *
           * @return {undefined}
           *
           * @example
           * recordButton.addEventListener('click', function () {
           *   recorder.start()
           * })
           */
          start: function start (timeslice) {
            if (this.state !== 'inactive') {
              return this.em.dispatchEvent(error('start'))
            }

            this.state = 'recording'

            if (!context) {
              context = new AudioContext()
            }
            this.clone = this.stream.clone()
            var input = context.createMediaStreamSource(this.clone)

            if (!processor) {
              processor = context.createScriptProcessor(2048, 1, 1)
            }

            var recorder = this
            processor.onaudioprocess = function (e) {
              if (recorder.state === 'recording') {
                recorder.encoder.postMessage([
                  'encode', e.inputBuffer.getChannelData(0)
                ])
              }
            }

            input.connect(processor)
            processor.connect(context.destination)

            this.em.dispatchEvent(new Event('start'))

            if (timeslice) {
              this.slicing = setInterval(function () {
                if (recorder.state === 'recording') recorder.requestData()
              }, timeslice)
            }

            return undefined
          },

          /**
           * Stop media capture and raise `dataavailable` event with recorded data.
           *
           * @return {undefined}
           *
           * @example
           * finishButton.addEventListener('click', function () {
           *   recorder.stop()
           * })
           */
          stop: function stop () {
            if (this.state === 'inactive') {
              return this.em.dispatchEvent(error('stop'))
            }

            this.requestData()
            this.state = 'inactive'
            this.clone.getTracks().forEach(function (track) {
              track.stop()
            })
            return clearInterval(this.slicing)
          },

          /**
           * Pauses recording of media streams.
           *
           * @return {undefined}
           *
           * @example
           * pauseButton.addEventListener('click', function () {
           *   recorder.pause()
           * })
           */
          pause: function pause () {
            if (this.state !== 'recording') {
              return this.em.dispatchEvent(error('pause'))
            }

            this.state = 'paused'
            return this.em.dispatchEvent(new Event('pause'))
          },

          /**
           * Resumes media recording when it has been previously paused.
           *
           * @return {undefined}
           *
           * @example
           * resumeButton.addEventListener('click', function () {
           *   recorder.resume()
           * })
           */
          resume: function resume () {
            if (this.state !== 'paused') {
              return this.em.dispatchEvent(error('resume'))
            }

            this.state = 'recording'
            return this.em.dispatchEvent(new Event('resume'))
          },

          /**
           * Raise a `dataavailable` event containing the captured media.
           *
           * @return {undefined}
           *
           * @example
           * this.on('nextData', function () {
           *   recorder.requestData()
           * })
           */
          requestData: function requestData () {
            if (this.state === 'inactive') {
              return this.em.dispatchEvent(error('requestData'))
            }

            return this.encoder.postMessage(['dump', context.sampleRate])
          },

          /**
           * Add listener for specified event type.
           *
           * @param {"start"|"stop"|"pause"|"resume"|"dataavailable"|"error"}
           * type Event type.
           * @param {function} listener The listener function.
           *
           * @return {undefined}
           *
           * @example
           * recorder.addEventListener('dataavailable', function (e) {
           *   audio.src = URL.createObjectURL(e.data)
           * })
           */
          addEventListener: function addEventListener () {
            this.em.addEventListener.apply(this.em, arguments)
          },

          /**
           * Remove event listener.
           *
           * @param {"start"|"stop"|"pause"|"resume"|"dataavailable"|"error"}
           * type Event type.
           * @param {function} listener The same function used in `addEventListener`.
           *
           * @return {undefined}
           */
          removeEventListener: function removeEventListener () {
            this.em.removeEventListener.apply(this.em, arguments)
          },

          /**
           * Calls each of the listeners registered for a given event.
           *
           * @param {Event} event The event object.
           *
           * @return {boolean} Is event was no canceled by any listener.
           */
          dispatchEvent: function dispatchEvent () {
            this.em.dispatchEvent.apply(this.em, arguments)
          }
        }

        /**
         * Returns `true` if the MIME type specified is one the polyfill can record.
         *
         * This polyfill supports only `audio/wav`.
         *
         * @param {string} mimeType The mimeType to check.
         *
         * @return {boolean} `true` on `audio/wav` MIME type.
         */
        MediaRecorder.isTypeSupported = function isTypeSupported (mimeType) {
          return /audio\/wave?/.test(mimeType)
        }

        /**
         * `true` if MediaRecorder can not be polyfilled in the current browser.
         * @type {boolean}
         *
         * @example
         * if (MediaRecorder.notSupported) {
         *   showWarning('Audio recording is not supported in this browser')
         * }
         */
        MediaRecorder.notSupported = !navigator.mediaDevices || !AudioContext

        /**
         * Converts RAW audio buffer to compressed audio files.
         * It will be loaded to Web Worker.
         * By default, WAVE encoder will be used.
         * @type {function}
         *
         * @example
         * MediaRecorder.prototype.mimeType = 'audio/ogg'
         * MediaRecorder.encoder = oggEncoder
         */
        MediaRecorder.encoder = function () {
            var BYTES_PER_SAMPLE = 2

            var recorded = []

            function encode (buffer) {
              var length = buffer.length
              var data = new Uint8Array(length * BYTES_PER_SAMPLE)
              for (var i = 0; i < length; i++) {
                var index = i * BYTES_PER_SAMPLE
                var sample = buffer[i]
                if (sample > 1) {
                  sample = 1
                } else if (sample < -1) {
                  sample = -1
                }
                sample = sample * 32768
                data[index] = sample
                data[index + 1] = sample >> 8
              }
              recorded.push(data)
            }

            function dump (sampleRate) {
              var bufferLength = recorded.length ? recorded[0].length : 0
              var length = recorded.length * bufferLength
              var wav = new Uint8Array(44 + length)
              var view = new DataView(wav.buffer)

              // RIFF identifier 'RIFF'
              view.setUint32(0, 1380533830, false)
              // file length minus RIFF identifier length and file description length
              view.setUint32(4, 36 + length, true)
              // RIFF type 'WAVE'
              view.setUint32(8, 1463899717, false)
              // format chunk identifier 'fmt '
              view.setUint32(12, 1718449184, false)
              // format chunk length
              view.setUint32(16, 16, true)
              // sample format (raw)
              view.setUint16(20, 1, true)
              // channel count
              view.setUint16(22, 1, true)
              // sample rate
              view.setUint32(24, sampleRate, true)
              // byte rate (sample rate * block align)
              view.setUint32(28, sampleRate * BYTES_PER_SAMPLE, true)
              // block align (channel count * bytes per sample)
              view.setUint16(32, BYTES_PER_SAMPLE, true)
              // bits per sample
              view.setUint16(34, 8 * BYTES_PER_SAMPLE, true)
              // data chunk identifier 'data'
              view.setUint32(36, 1684108385, false)
              // data chunk length
              view.setUint32(40, length, true)

              for (var i = 0; i < recorded.length; i++) {
                wav.set(recorded[i], i * bufferLength + 44)
              }

              recorded = []
              postMessage(wav.buffer, [wav.buffer])
            }

            onmessage = function (e) {
              if (e.data[0] === 'encode') {
                encode(e.data[1])
              } else {
                dump(e.data[1])
              }
            }
          }

        window.MediaRecorder = MediaRecorder;
    } 
})();

(function() {
    var isEdge = window.navigator.userAgent.indexOf("Edge/") > -1;
    var isIE = window.navigator.userAgent.indexOf("MSIE ") > -1 || !!navigator.userAgent.match(/Trident.*rv\:11\./);
	var isSafari = _isSafari();
	function _isSafari() {
		var ua = navigator.userAgent.toLowerCase(); 
		if (ua.indexOf('safari') != -1) { 
		  if (ua.indexOf('chrome') > -1) {
			//return false;
		  } else {
			return true;
		  }
		}
		return false;
	}

    function isIOS() {

    	return (/iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream) ||  (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
    }
    
    function isIPad() {
    	return (/ipad/.test(window.location.search));
    	//return (/iPad/.test(navigator.userAgent) && !window.MSStream) ||  (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
    }    
    function isIOS13() {
    	return isIOS() && "download" in document.createElement("a")
    }
    
    function createAudioContext(desiredSampleRate) {
        var AudioCtor = window.AudioContext || window.webkitAudioContext
		var supportedConstraints = navigator.mediaDevices.getSupportedConstraints ?
			navigator.mediaDevices.getSupportedConstraints() : {};
			
        if (isIOS() || !supportedConstraints.sampleRate) {
        	
            var context = new AudioCtor();
            var buffer = context.createBuffer(1, 1, 44100)
			var dummy = context.createBufferSource()
			dummy.buffer = buffer
			dummy.connect(context.destination)
			dummy.start(0)
			dummy.disconnect()
			context.close();
			context = new AudioCtor();
			return context;
        } else {
            return new AudioCtor({sampleRate: desiredSampleRate});
        }
    }
   
   
    function unlockAudioClip(audio) {

    	try {
			audio.setAttribute('data-cn1-unlocked', 'true');
			console.log('Unlocking audio ', audio);
			var testPlay = audio.play();
			
			
			if (testPlay && typeof Promise !== 'undefined' && (testPlay instanceof Promise || typeof testPlay.then === 'function')) {
				testPlay.catch(function(err) {
					if (err.name=='NotAllowedError' || err.name == 'AbortError') {
						var index = (window._unlockedAudioPool || []).indexOf(audio);
						if (index >= 0) {
							window._unlockedAudioPool.splice(index, 1);
						} 
					}
				});
			}
			
		} catch (err) {
			
		}
        
    }
    var unlockAudio = function() {
    	try {
            window.cn1UnlockingClips = true;   
            document.removeEventListener('touchstart', unlockAudio, true);
            document.removeEventListener('touchend', unlockAudio, true);
            document.removeEventListener('click', unlockAudio, true);
            window.removeEventListener('installbacksidehooks', unlockAudio, true);

            for (var i=0; i<5; i++) {
                unlockAudioClip(new Audio());
            }
			
    	} finally {
            window.cn1UnlockingClips = false;
    	}
    	
    };
    document.addEventListener('touchstart', unlockAudio, true);
    document.addEventListener('touchend', unlockAudio, true);
    document.addEventListener('click', unlockAudio, true);
    window.addEventListener('installbacksidehooks', unlockAudio, true);
    
    var hidden, visibilityChange; 
	if (typeof document.hidden !== "undefined") { // Opera 12.10 and Firefox 18 and later support 
	  hidden = "hidden";
	  visibilityChange = "visibilitychange";
	} else if (typeof document.msHidden !== "undefined") {
	  hidden = "msHidden";
	  visibilityChange = "msvisibilitychange";
	} else if (typeof document.webkitHidden !== "undefined") {
	  hidden = "webkitHidden";
	  visibilityChange = "webkitvisibilitychange";
	}
        
    
    
    function AudioUnit(config) {
        config = config || {};
        var healthCheckHandle;
        var origChannelCount=-1;
        var origSampleRate=-1;
        var paused;
        var audioCtx;
        var audioInput;
	var audioNode;
	var bufferSize = config.bufferSize || 4096;
        var stream;
        var recording = false;
	var SAMPLE_RATE = config.sampleRate || 44100;
		var ENABLE_SAMPLERATE = true; // Whether to use libsamplerate if it is availalble
        var SAMPLE_SIZE = config.sampleSize || 16;
        var audioChannels = config.audioChannels || 1;
        var onComplete = config.onComplete || function(str){};
        var _onError = config.onError || function(e){console.log(e);};
        var onError = function(e){
        	console.log("Firing onError callback ", e);
        	_onError(e);
        }
        var onAudioProcess = config.onAudioProcess || function(sampleRate, numChannels, floatSamples){};
        var _onRecord = config.onRecord || function(numChannels, sampleRate){};
		var onRecord = function(numChannels, sampleRate) {
			console.log("firing onRecord callback", numChannels, sampleRate);
			_onRecord(numChannels, sampleRate);
		};
        var pendingGetUserMedia = false;
        console.log("AudioUnit config", config);
        function interleave(e) {
            if (audioChannels === 1) {
                return e.inputBuffer.getChannelData(0);
            } else if (audioChannels === 2) {
                var leftChannel = e.inputBuffer.getChannelData(0);
                var rightChannel = e.inputBuffer.getChannelData(1);
                var length = leftChannel.length + rightChannel.length;
                var result = new Float32Array(length);
                var inputIndex = 0;
                for (var index = 0; index < length;) {
                    result[index++] = leftChannel[inputIndex];
                    result[index++] = rightChannel[inputIndex];
                    inputIndex++;
                }
                return result;

            } else {
                throw new Error("Unsupported channel count: "+audioChannels+".  Supports only 1 or 2");
            }
           
        }
        
        
        /**
         * Checks if the media stream is currently active.
         */
        function isPaused() {
            var active = false;
            if (stream) {
                
                stream.getTracks().forEach(function(track) {
                    if (track.enabled) {
                        active = true;
                    }
                });
                
                
            }
            return active;
        }
        
        function resume(notAllowedCallback, blockRecordCallback) {
            try {
            	console.log('RECORDER|resume');
                console.log("in resume()", stream);
                paused = false;
                recording = true;
                if (stream) {
                    stream.getTracks().forEach(function(track) {
                        console.log("enabling track", track);
                        track.enabled = true;
                    });
                }


                if (audioCtx == null) {
                    console.log("Creating audio context");
                    audioCtx = createAudioContext(SAMPLE_RATE);
                    audioInput = audioCtx.createMediaStreamSource(stream);
                    console.log("Audio context created");
                    if (audioCtx.createJavaScriptNode) {
                        audioNode = audioCtx.createJavaScriptNode(bufferSize,  audioInput.channelCount, audioCtx.destination.channelCount);
                    } else if (audioCtx.createScriptProcessor) {
                        audioNode = audioCtx.createScriptProcessor(bufferSize,  audioInput.channelCount, audioCtx.destination.channelCount);
                    } else {
                        console.log("Failed to create audio context");
                        recording = false;
                        onError("Failed to create audio context");
                        return;
                    }

                }

                console.log("recording now");
                 // Hook up the scriptNode to the mic


                audioInput.connect(audioNode);
                //do I need to move this
                audioNode.connect(audioCtx.destination);

                console.log("about to fire onRecord callback");

                setupAudioProcessing(audioNode, audioCtx, notAllowedCallback, blockRecordCallback);



                
                console.log("Finished resume()");
            } catch (e) {
                recording = false;
                console.log("ERROR in resume", e);
                onError(e+"");
            }
            
        }
        
     
        
        function stop() {
        	
        	pause();
            if (audioInput) {
                if (audioInput.disconnect) {
                    audioInput.disconnect();
                }
                audioInput = null;
            }
            if (audioNode) {
                if (audioNode.disconnect) {
                    audioNode.disconnect();
                }
                audioNode = null;
            }
            if (audioCtx) {
                if (audioCtx.close) {
                    audioCtx.close();
                    
                }
                audioCtx = null;
            }
            
            if (stream) {
            	stream.getTracks().forEach(function(track) {
                    track.stop();
                });
                if (stream.close) {
                    stream.close();
                }
                stream = null;
            }
            
            
            onComplete('');
            

        };
        
        
        
        function pause() {
            console.log("In pause()", stream);
            paused = true;
            recording = false;
            if (healthCheckHandle) {
                clearInterval(healthCheckHandle);
                healthCheckHandle = null;
            }		
            if (stream) {
                stream.getTracks().forEach(function(track) {
                    console.log("Disabling track", track);
                    track.enabled = false;
                });
		
                if (audioInput) {
                    audioInput.disconnect();
                    audioInput = null;
                }
                if (audioNode) {

                    audioNode.disconnect();
                    audioNode = null;
                }

                if (audioCtx) {

                    var thisCtx = audioCtx;
                    audioCtx.close().then(function() {
                            //console.log("Finished closing context ", thisCtx);
                    });
                    audioCtx = null;
                }

                
                if (isSafari || (isIOS() && !isIPad())) {
                    if (stream) {
                        stream.getTracks().forEach(function(track) {
                            //console.log("Stopping track", track);
                            track.stop();
                        });
                        if (stream.close) {
                            stream.close();
                        }
                        if (stream.stop) {
                            stream.stop();
                        }
                        stream = null;
                    }
                }
            }
            
            
        }

        function setupAudioProcessing(audioNode, audioCtx, notAllowedCallback, blockRecordCallback) {
            var pipe = {success:false};
            var requestComplete = false;
            healthCheckHandle = setInterval(function() {
                if (!pipe.success) {
                    pipe.success = false;
                    console.log("No audio processed since last poll");
                }
                if (/*!pipe.success || */false && audioCtx && audioCtx.state === 'interrupted') {

                    console.log("Stream interrupted.  Trying to resume");
                    pause();
                    var naCallback = function() {
                        if (!window.confirm('Continue recording?')) {
                            stop();
                        } else {
                            pause();
                            record(naCallback, true);
                        }
                    };
                    record(naCallback, true);

                }
            }, 500);

            if (!blockRecordCallback) {
                onRecord(audioChannels, Math.floor(audioCtx.sampleRate));
            }

            registerAudioProcessing(audioNode, audioCtx, pipe);
			
        
        }
        
        function registerAudioProcessing(audioNode, audioCtx, pipe) {
            audioNode.onaudioprocess = function(e) {
                if (pipe && !pipe.success) {
                    pipe.success = true;
                }
                try {
                    var floatSamples = interleave(e);
                    //console.log("float samples "+floatSamples);
                    var sr = audioCtx.sampleRate;
                    if (ENABLE_SAMPLERATE) {
                        if (sr != SAMPLE_RATE && 'Samplerate' in window) {
                           var resampler = new Samplerate({type: Samplerate.LINEAR});

                           var result = resampler.process({
                                   channels: audioChannels,
                                 data: floatSamples, // buffer is a Float32Aray or a Int16Array
                                   ratio: parseFloat(SAMPLE_RATE)/parseFloat(sr),
                                 last: false
                           });

                           var converted = result.data; // same type as buffer above
                           var used = result.used; // input samples effectively used

                           // Optional:
                           //converter.setRatio(2.3);
                           //converter.reset();

                           // Close:
                           resampler.close();
                           floatSamples = converted;
                           sr = SAMPLE_RATE;
                       }
                    }

                    if (isEdge || isIE) {
                        // Not sure why this is necessary, but in Edge and IE,
                        // the length of typed arrays is zero by the time it is processed
                        // on the java audio processing thread.
                        // We need to convert it to a regular array
                        // so that the data gets retained.  Must be a bug in IE/Edge,
                        // but couldn't find it reported anywhere.
                        floatSamples = Array.from(floatSamples);
                    }
                    
                    onAudioProcess(Math.floor(sr), audioChannels, floatSamples);
                } catch (e) {
                        onError(e+"");

                }

            };
        }
        
        function promptAsync(msg) {
            return new Promise(function(resolve, reject) {
                var dialog = $('<div>Grant access to microphone? <button class="ok-btn">OK</button><button class="cancel-btn">Cancel</button></div>')
                    .css({
                        position:'fixed',
                        top: '40%',
                        width: '100px',
                        'background-color' : 'white'
                    })
                    .appendTo($('body'));
                $('.ok-btn', dialog).on('click', function() {
                    $(dialog).remove();
                    resolve(true);
                });
                $('.cancel-btn', dialog).on('click', function() {
                    $(dialog).remove();
                    resolve(false);
                });
            });
        }
        
        
        var failedMEICheck;
        function record(notAllowedCallback, blockRecordCallback, allowed) {
            if (recording) {
            	console.log("Calling onRecord because recording");
                onRecord(audioChannels, audioCtx ? Math.floor(audioCtx.sampleRate) : 0);
                return;
            }
            if (pendingGetUserMedia) {
                // If there is a pending getUser media, then 
                // interested parties will receive the onRecord or onError
                // callbacks from that request.
                return;
            }
            
            
            failedMEICheck = false;

            if ((isIOS() || isSafari) && !allowed) {
               	cn1RunPrivileged(function() {
               		record(notAllowedCallback, blockRecordCallback, true);
               	});

                return;


            }
           
            
            paused = false;
           
            if (stream && (!isIOS() || isIPad())) {
            	if (isIOS()) {
                    // On iOS we cannot reuse the stream, and it is problematic
                    // to create it new every time, so we workaround it by cloning
                    // the stream and removing the tracks from the old stream.
                    // Thanks Chad Phillips (https://webrtchacks.com/guide-to-safari-webrtc/)
                    var tmp = stream.clone();
                    var audioTracks = stream.getAudioTracks();
                    for (var i=0, len = audioTracks.length; i < len; i++) {
                            stream.removeTrack(audioTracks[i]);
                    }
                    stream = tmp;
            	}
                resume(notAllowedCallback, blockRecordCallback);
                return;
            }
            
            if (stream) {
            	stream.getTracks().forEach(function(track) {
                    console.log("Disabling track", track);
                    track.stop();
                });
                if (stream.stop) {
                    stream.stop();
                }
                if (stream.close) {
                    stream.close();
                }
                stream = null;
            }
            
            pendingGetUserMedia = true;
            
            // On iOS it is extremely important to have echoCancellation OFF
            // Otherwise you'll get distortion in the recording if ANY audio clips have been played in the 
            // app.
            // On Android, it is extremely important to NOT have echo cancellation off
            // With echo cancellation set to false, the mic is VERY soft
            var audioConstraints = isIOS() ? {echoCancellation:false} : {};
            var supportedConstraints = {};
            if (!isIOS()) {
            	if (navigator.mediaDevices.getSupportedConstraints) {
                    supportedConstraints = navigator.mediaDevices.getSupportedConstraints();
                }

                if (supportedConstraints.sampleSize) {
                    audioConstraints.sampleSize = {ideal: SAMPLE_SIZE};
                }
                if (supportedConstraints.sampleRate) {
                    audioConstraints.sampleRate = {ideal: SAMPLE_RATE};
                }
                if (supportedConstraints.audioChannels) {	
                    audioConstraints.channelCount = {ideal: audioChannels};
                }
                if (supportedConstraints.autoGainControl) {
                    audioConstraints.autoGainControl = true;  // For Android
                }
            }
            //audioConstraints.autoGainControl=true;
            //IMPORTANT!!: On iPad you MUST create the audioCtx and audioNode
            // and connect the audioNode to the audioCtx.destination BEFORE
            // getUserMedia().  If you do it after the callback, you get all kinds
            // of crazy results related to the audio not starting in direct response to a 
            // user action.
            // Symptoms include:
            // 1. Audio context randomly gets kicked into the "interrupted" state.
            // 2. onaudioprocess works correctly for the first few seconds, but then
            //    the channel data starts coming in all zeroes.
            // 3. Microphone quality gets very poor and distored.
            // 4. Audio playback becomes soft.
            audioCtx = createAudioContext(SAMPLE_RATE);
            audioNode = (audioCtx.createJavascriptNode||audioCtx.createScriptProcessor).call(audioCtx, bufferSize, audioChannels, audioChannels);
            audioNode.connect(audioCtx.destination);
            
            console.log("About to do getUserMedia");
            
            
            function onStream(_stream, privileged) {
            	if ((isIOS() || isSafari) && !privileged) {
            		cn1RunPrivileged(function() {
            			onStream(_stream, true);
            		});
            		return;
            	}
            	/*
            	if (!isIOS() && isSafari && !window.cn1MicrophoneAccessGranted) {
                    // Unbelievable!!  The standard MEI tests fail in Safari on Mac.
                    // I.e. It will pass the MEI check even if it should fail.
                    // We fill force a fail on the MEI check the first time we request
                    // microphone access on safari because if we don't, it won't pick up
                    // any audio the first time around.
                    failedMEICheck = true;
            	}
            	*/
            	window.cn1MicrophoneAccessGranted = true;
            	console.log("In getUserMedia callback");
                pendingGetUserMedia = false;

                stream = _stream;
                if (failedMEICheck) {
                    failedMEICheck = false;
                    console.log("Failed MEI Check, so we're going back to basics");
                    pause();
                    notAllowedCallback();

                    return;
            	}
                if (paused) {
                    console.log("recorder paused before we got user media");
                    try {
                        pause();
                    } catch (ex) {
                        console.log("pause() threw exception", ex);
                    }
					
                    onError("Record was paused");
                    return;
                }
                try {

                    if (origChannelCount < 0) {
                    	origChannelCount = audioCtx.destination.channelCount;
                    }
                    if (origSampleRate < 0) {
                    	origSampleRate = audioCtx.sampleRate;
                    }
                    audioInput = audioCtx.createMediaStreamSource(stream);
                    if (!audioCtx.createJavascriptNode && !audioCtx.createScriptProcessor) {
                    	console.log("Failed to create audio context");
                        onError("Failed to create audio context");
                        return;
                    }
                   
                    console.log("recording now");
                    
                    audioInput.connect(audioNode);
                    setupAudioProcessing(audioNode, audioCtx, notAllowedCallback, blockRecordCallback);
                    if (paused) {
                    	console.log("Already paused by the time we got permission");
                    	pause();
                    	onError("Record was paused.");
                    } else {
                    	recording = true;

                    }
                    
                } catch (e) {
                    if (stream) {
                        stream.getTracks().forEach(function(track) {
                             track.enabled = false;
                        });
                    }
                    if (stream) {
                        if (stream.close) {
                            stream.close();
                        }
                        stream = null;
                    }
                    if (audioInput) {
                        if (audioInput.disconnect) {
                            audioInput.disconnect();
                        }
                        audioInput = null;
                    }
                    if (audioNode) {
                        if (audioNode.disconnect) {
                            audioNode.disconnect();
                        }
                        audioNode = null;
                        
                    }
                    if (audioCtx) {
                        if (audioCtx.close) {
                            audioCtx.close();
                        }
                        audioCtx = null;
                    }
                    
                    console.log("Failed to create audio context " + e);
                    onError(e+"");
                    return;
                }
            }
            
            navigator.mediaDevices.getUserMedia({
                //audio: {
                    //echoCancellation: true,
                    //autoGainControl: true,
                    //channelCount: audioChannels,
                    //sampleRate:SAMPLE_RATE,
                    //sampleSize: SAMPLE_SIZE,
                    //volume: 1.0
                //}
                audio: audioConstraints,
                video : false
                
            }).then(onStream).catch(function (err) {
                pendingGetUserMedia = false;

                if ((err.name == 'NotAllowedError' || err.name == 'AbortError') && notAllowedCallback) {
                    notAllowedCallback();
                    return;
                }
                console.log("Failed to get user media " + err);
                onError(err+"");
            });
            
            console.log("After getUserMedia call");
            ;
        }
        
        function isRecording() {
            return recording;
        }
        
        this.record = record;
        this.pause = pause;
        this.stop = stop;
        this.isRecording = isRecording;
        
    }
    
    
   
    window.cn1CreateSilentAudio = createSilentAudio;
    function createSilentAudio (time, freq = 44100){
      console.log("Creating silent audio", time, freq);
	  const length = time * freq;
	  const AudioContext = window.AudioContext || window.webkitAudioContext || window.mozAudioContext;
	  if(! AudioContext ){
		console.log("No Audio Context")
	  }
	  const context = new AudioContext();
	  const audioFile = context.createBuffer(1, length, freq);
	  return URL.createObjectURL(bufferToWave(audioFile, length));
	}

	function bufferToWave(abuffer, len) {
	  let numOfChan = abuffer.numberOfChannels,
		length = len * numOfChan * 2 + 44,
		buffer = new ArrayBuffer(length),
		view = new DataView(buffer),
		channels = [], i, sample,
		offset = 0,
		pos = 0;

	  // write WAVE header
	  setUint32(0x46464952);
	  setUint32(length - 8);
	  setUint32(0x45564157);

	  setUint32(0x20746d66);
	  setUint32(16);
	  setUint16(1);
	  setUint16(numOfChan);
	  setUint32(abuffer.sampleRate);
	  setUint32(abuffer.sampleRate * 2 * numOfChan);
	  setUint16(numOfChan * 2);
	  setUint16(16);

	  setUint32(0x61746164);
	  setUint32(length - pos - 4);

	  // write interleaved data
	  for(i = 0; i < abuffer.numberOfChannels; i++)
		channels.push(abuffer.getChannelData(i));

	  while(pos < length) {
		for(i = 0; i < numOfChan; i++) {             // interleave channels
		  sample = Math.max(-1, Math.min(1, channels[i][offset])); // clamp
		  sample = (0.5 + sample < 0 ? sample * 32768 : sample * 32767)|0; // scale to 16-bit signed int
		  view.setInt16(pos, sample, true);          // write 16-bit sample
		  pos += 2;
		}
		offset++                                     // next source sample
	  }

	  // create Blob
	  return new Blob([buffer], {type: "audio/wav"});

	  function setUint16(data) {
		view.setUint16(pos, data, true);
		pos += 2;
	  }

	  function setUint32(data) {
		view.setUint32(pos, data, true);
		pos += 4;
	  }
	}

    
    
    window.CN1AudioUnit = AudioUnit;
})();

(function() {
   function AudioRecorder(config) {
       config = config || {};
       var recorder = null;
       var recordedChunks = null;
       var TIME_SLICE = config.timeSlice || 0;
       var savePath = config.savePath || 'file:///tempRecording.wav';
       var fireOnComplete = config.onComplete || function(path){};
       var fireOnError = config.onError || function(error){}
       var fireOnRecord = config.onRecord || function(audioTracks, sampleRate){}
       var stopping=false;
       
       
       function record(notAllowedCallback) {
           console.log("In record()");
           if (recorder != null) {
               recorder.resume();
               return;
           }
           
            recordedChunks = [];
            console.log("RECORDER|getUserMedia audioRecorder");
            navigator.mediaDevices.getUserMedia({ audio: true }).then(function(stream) {
                recorder = new MediaRecorder(stream)

                // Set record to <audio> when recording will be finished
                recorder.addEventListener('dataavailable', function(e) {
                  //audio.src = URL.createObjectURL(e.data)
                    if (recordedChunks) {
                            recordedChunks.push(e.data);
                    }
                    if (stopping) {
                        stopping = false;
                        var blob = new Blob(recordedChunks, {type:'audio/wav; codecs=0'});
                        recordedChunks = null;


                        window.saveBlobToFile(blob, savePath || '', {
                            complete : function(path) {
                                fireOnComplete(path);
                            },
                            error : function(msg) {
                                console.log('Failed to save blob to file.', msg);
                                fireOnError(msg);
                            }
                        });
                       recorder.stream.getTracks().forEach(function(i){i.stop()});
                    }
                  
                  
                })

                // Start recording
                if (TIME_SLICE > 0) {
                    recorder.start(TIME_SLICE);
                    
                } else {
                    recorder.start();
                }
                fireOnRecord(0, 0);
            }).catch(function(err) {
                if ((err.name === 'NotAllowedError' || err.name == 'AbortError') && notAllowedCallback) {
                    notAllowedCallback();
                    return;
                }
                console.log("Failed to get user media "+err);
                fireOnError(err);
            });
       }
       
        function stop() {
            if (recorder == null) {
                    return;
            }
            if (stopping) {
                return;
            }
            stopping = true;
            recorder.stop();
            
        }
       
        function pause() {
            if (recorder != null) {
                recorder.pause();
            }
        }
       
        function resume() {
            if (recorder != null) {
                recorder.resume();
            }
        }
        
        function isRecording() {
            return recorder != null && recorder.state === 'recording';
        }
        
        this.record = record;
        this.stop = stop;
        this.pause = pause;
        this.resume = resume;
        this.isRecording = isRecording;
   }
   
   window.CN1AudioRecorder = AudioRecorder;
})();

(function(global){"use strict";var _Base64=global.Base64;var version="2.1.9";var buffer;if(typeof module!=="undefined"&&module.exports){try{buffer=require("buffer").Buffer}catch(err){}}var b64chars="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";var b64tab=function(bin){var t={};for(var i=0,l=bin.length;i<l;i++)t[bin.charAt(i)]=i;return t}(b64chars);var fromCharCode=String.fromCharCode;var cb_utob=function(c){if(c.length<2){var cc=c.charCodeAt(0);return cc<128?c:cc<2048?fromCharCode(192|cc>>>6)+fromCharCode(128|cc&63):fromCharCode(224|cc>>>12&15)+fromCharCode(128|cc>>>6&63)+fromCharCode(128|cc&63)}else{var cc=65536+(c.charCodeAt(0)-55296)*1024+(c.charCodeAt(1)-56320);return fromCharCode(240|cc>>>18&7)+fromCharCode(128|cc>>>12&63)+fromCharCode(128|cc>>>6&63)+fromCharCode(128|cc&63)}};var re_utob=/[\uD800-\uDBFF][\uDC00-\uDFFFF]|[^\x00-\x7F]/g;var utob=function(u){return u.replace(re_utob,cb_utob)};var cb_encode=function(ccc){var padlen=[0,2,1][ccc.length%3],ord=ccc.charCodeAt(0)<<16|(ccc.length>1?ccc.charCodeAt(1):0)<<8|(ccc.length>2?ccc.charCodeAt(2):0),chars=[b64chars.charAt(ord>>>18),b64chars.charAt(ord>>>12&63),padlen>=2?"=":b64chars.charAt(ord>>>6&63),padlen>=1?"=":b64chars.charAt(ord&63)];return chars.join("")};var btoa=global.btoa?function(b){return global.btoa(b)}:function(b){return b.replace(/[\s\S]{1,3}/g,cb_encode)};var _encode=buffer?function(u){return(u.constructor===buffer.constructor?u:new buffer(u)).toString("base64")}:function(u){return btoa(utob(u))};var encode=function(u,urisafe){return!urisafe?_encode(String(u)):_encode(String(u)).replace(/[+\/]/g,function(m0){return m0=="+"?"-":"_"}).replace(/=/g,"")};var encodeURI=function(u){return encode(u,true)};var re_btou=new RegExp(["[À-ß][-¿]","[à-ï][-¿]{2}","[ð-÷][-¿]{3}"].join("|"),"g");var cb_btou=function(cccc){switch(cccc.length){case 4:var cp=(7&cccc.charCodeAt(0))<<18|(63&cccc.charCodeAt(1))<<12|(63&cccc.charCodeAt(2))<<6|63&cccc.charCodeAt(3),offset=cp-65536;return fromCharCode((offset>>>10)+55296)+fromCharCode((offset&1023)+56320);case 3:return fromCharCode((15&cccc.charCodeAt(0))<<12|(63&cccc.charCodeAt(1))<<6|63&cccc.charCodeAt(2));default:return fromCharCode((31&cccc.charCodeAt(0))<<6|63&cccc.charCodeAt(1))}};var btou=function(b){return b.replace(re_btou,cb_btou)};var cb_decode=function(cccc){var len=cccc.length,padlen=len%4,n=(len>0?b64tab[cccc.charAt(0)]<<18:0)|(len>1?b64tab[cccc.charAt(1)]<<12:0)|(len>2?b64tab[cccc.charAt(2)]<<6:0)|(len>3?b64tab[cccc.charAt(3)]:0),chars=[fromCharCode(n>>>16),fromCharCode(n>>>8&255),fromCharCode(n&255)];chars.length-=[0,0,2,1][padlen];return chars.join("")};var atob=global.atob?function(a){return global.atob(a)}:function(a){return a.replace(/[\s\S]{1,4}/g,cb_decode)};var _decode=buffer?function(a){return(a.constructor===buffer.constructor?a:new buffer(a,"base64")).toString()}:function(a){return btou(atob(a))};var decode=function(a){return _decode(String(a).replace(/[-_]/g,function(m0){return m0=="-"?"+":"/"}).replace(/[^A-Za-z0-9\+\/]/g,""))};var noConflict=function(){var Base64=global.Base64;global.Base64=_Base64;return Base64};global.Base64={VERSION:version,atob:atob,btoa:btoa,fromBase64:decode,toBase64:encode,utob:utob,encode:encode,encodeURI:encodeURI,btou:btou,decode:decode,noConflict:noConflict};if(typeof Object.defineProperty==="function"){var noEnum=function(v){return{value:v,enumerable:false,writable:true,configurable:true}};global.Base64.extendString=function(){Object.defineProperty(String.prototype,"fromBase64",noEnum(function(){return decode(this)}));Object.defineProperty(String.prototype,"toBase64",noEnum(function(urisafe){return encode(this,urisafe)}));Object.defineProperty(String.prototype,"toBase64URI",noEnum(function(){return encode(this,true)}))}}if(global["Meteor"]){Base64=global.Base64}})(this);
(function () {
    window.cn1_registerPush = registerPush;
    
    function registerPush(onSuccess, onFail, onPush, pushActionCategories) {
        /**
         * Step one: run a function on load (or whenever is appropriate for you)
         * Function run on load sets up the service worker if it is supported in the
         * browser. Requires a serviceworker in a `sw.js`. This file contains what will
         * happen when we receive a push notification.
         * If you are using webpack, see the section below.
         */
        $(function () {
            if ('serviceWorker' in navigator) {
                navigator.serviceWorker.register('sw.js').then(initialiseState);
            } else {
                console.warn('Service workers are not supported in this browser.');
                onFail('Service workers are not supported in this browser.');
            }
        });

        /**
         * Step two: The serviceworker is registered (started) in the browser. Now we
         * need to check if push messages and notifications are supported in the browser
         */
        function initialiseState(registration) {
            registration.update();
            navigator.serviceWorker.addEventListener('message', function(event) {
                
              //console.log('Received a message from service worker: ', event.data);
              //onPush(event.data);
              if (event.data.type == 'push') {
                  var data = event.data.data;
                  data.visual = event.data.visual;
                  onPush(data);
              }
            });
            //console.log(registration);
            var serviceWorker;
            
            if (registration.installing) {
                serviceWorker = registration.installing;

            } else if (registration.waiting) {
                serviceWorker = registration.waiting;

            } else if (registration.active) {
                serviceWorker = registration.active;
                
            }
            if (serviceWorker) {
                //console.log("Current state: "+serviceWorker.state);
                if (serviceWorker.state === 'activated') {
                    //console.log("Controller is "+serviceWorker.controller);
                    serviceWorker.postMessage("a message");
                }
                serviceWorker.addEventListener('controllerchange', function (e) {
                    //console.log("Controller changed");
                });
                serviceWorker.addEventListener('message', function(event) {
                  //console.log('Received a message from service worker: ', event.data);
                  if (event.data.type == 'push') {
                      var data = event.data.data;
                      data.visual = event.data.visual;
                      onPush(data);
                  }
                  
                });
                
            }
            serviceWorker.addEventListener('statechange', function (e) {
                // logState(e.target.state);
                //console.log("state change "+e.target.state);
            });
            // Check if desktop notifications are supported
            if (!('showNotification' in ServiceWorkerRegistration.prototype)) {
                console.warn('Notifications aren\'t supported.');
                onFail('Notifications aren\'t supported.');
                return;
            }

            // Check if user has disabled notifications
            // If a user has manually disabled notifications in his/her browser for 
            // your page previously, they will need to MANUALLY go in and turn the
            // permission back on. In this statement you could show some UI element 
            // telling the user how to do so.
            if (Notification.permission === 'denied') {
                console.warn('The user has blocked notifications.');
                onFail('The user has blocked notifications.');
                return;
            }

            // Check is push API is supported
            if (!('PushManager' in window)) {
                console.warn('Push messaging isn\'t supported.');
                onFail('Push messaging isn\'t supported.');
                return;
            }

            navigator.serviceWorker.ready.then(function (serviceWorkerRegistration) {
                //console.log('.ready resolved, and navigator.serviceWorker.controller is', navigator.serviceWorker.controller);
                navigator.serviceWorker.addEventListener('controllerchange', function() {
                    //console.log('Okay, now things are under control. navigator.serviceWorker.controller is', navigator.serviceWorker.controller);
                    //navigator.serviceWorker.controller.postMessage("The client says hello");
                });
                serviceWorkerRegistration.active.postMessage({
                    command: 'registerPushActionCategories',
                    pushActionCategories: pushActionCategories
                });
                // Get the push notification subscription object
                serviceWorkerRegistration.pushManager.getSubscription().then(function (subscription) {
                    
                    
                    // If this is the user's first visit we need to set up
                    // a subscription to push notifications
                    if (!subscription) {
                        subscribe();

                        return;
                    }

                    // Update the server state with the new subscription
                    sendSubscriptionToServer(subscription);
                })
                        .catch(function (err) {
                            // Handle the error - show a notification in the GUI
                            console.warn('Error during getSubscription()', err);
                            onFail('Error during getSubscription()');
                        });
            });
        }

        /**
         * Step three: Create a subscription. Contact the third party push server (for
         * example mozilla's push server) and generate a unique subscription for the
         * current browser.
         */
        function subscribe() {
            navigator.serviceWorker.ready.then(function (serviceWorkerRegistration) {

                // Contact the third party push server. Which one is contacted by
                // pushManager is  configured internally in the browser, so we don't
                // need to worry about browser differences here.
                //
                // When .subscribe() is invoked, a notification will be shown in the
                // user's browser, asking the user to accept push notifications from
                // <yoursite.com>. This is why it is async and requires a catch.
                serviceWorkerRegistration.pushManager.subscribe({userVisibleOnly: true}).then(function (subscription) {

                    // Update the server state with the new subscription
                    return sendSubscriptionToServer(subscription);
                })
                        .catch(function (e) {
                            if (Notification.permission === 'denied') {
                                console.warn('Permission for Notifications was denied');
                                onFail('Permission for Notifications was denied');
                            } else {
                                console.error('Unable to subscribe to push.', e);
                                onFail('Unable to subscribe to push.');
                            }
                        });
            });
        }

        /**
         * Step four: Send the generated subscription object to our server.
         */
        function sendSubscriptionToServer(subscription) {

            // Get public key and user auth from the subscription object
            var key = subscription.getKey ? subscription.getKey('p256dh') : '';
            var auth = subscription.getKey ? subscription.getKey('auth') : '';
            var id = 'cn1-web-'+Base64.encodeURI(subscription.endpoint + '?key=' + 
                    encodeURIComponent(key ? btoa(String.fromCharCode.apply(null, new Uint8Array(key))) : '') +
                    '&auth=' + encodeURIComponent(auth ? btoa(String.fromCharCode.apply(null, new Uint8Array(auth))) : ''));
            onSuccess(id);
        }
    }
})();

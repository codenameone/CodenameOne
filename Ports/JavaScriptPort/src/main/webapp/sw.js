var pushActionCategories = null;
var pendingPushes = [];
const urlToOpen = [self.location.href.replace(/\/sw\.js$/, '/index.html'),
        self.location.href.replace(/\/sw\.js$/, '/'),
        self.location.href.replace(/\/sw\.js$/, '')
  ];
  

function findActionsForCategory(categoryId) {
    if (pushActionCategories === null) {
        return null;
    }
    
    for (var i=0; i<pushActionCategories.length; i++) {
        var category = pushActionCategories[i];
        if (category.id === categoryId) {
            return category.actions;
        }
    }
    return null;
}
  
self.addEventListener('push', function(event) {
  var obj = event.data.json();
  
  var chain = [];
  if (includesHiddenPush(obj)) {
      var promiseChain = clients.matchAll({
        type: 'window',
        includeUncontrolled: true
      })
      .then((windowClients) => {
        let matchingClient = null;
    
        for (let i = 0; i < windowClients.length; i++) {
          const windowClient = windowClients[i];
          if (urlToOpen.indexOf(windowClient.url) >= 0) {
            matchingClient = windowClient;
            break;
          }
        }
        //console.log("Matching client "+matchingClient);
        if (matchingClient) {
            
          return matchingClient;
        }
        return null;
      }).then((windowClient) => {
          if (windowClient) {
              //console.log("About to post message to the windowClient...");
              windowClient.postMessage({type: 'push', data: obj, 'visual' : false});
            }
      });
      chain.push(promiseChain);
  }
  if (includesVisualPush(obj)) {
    var promiseChain = getFocusedClient()
      .then((windowClient) => {
        if (windowClient !== null) {
            //if (includesHiddenPush(obj)) {
                windowClient.postMessage({type: 'push', data: obj, 'visual' : false});
            //}
        } else {
            //console.log("Received push event");
            //console.log(obj);
            if (includesVisualPush(obj)) {
                return self.registration.showNotification(getNotificationTitle(obj), buildNotification(obj));
            }
        }
        
      });
      chain.push(promiseChain);
  }
  
  event.waitUntil(Promise.all(chain));
  
  //fireNotification(obj, event);
  
});

self.addEventListener('notificationclick', function(event) {
  const clickedNotification = event.notification;
  clickedNotification.close();
  
  // Do something as the result of the notification click
  //const promiseChain = 

  const promiseChain = clients.matchAll({
    type: 'window',
    includeUncontrolled: true
  })
  .then((windowClients) => {
    let matchingClient = null;

    for (let i = 0; i < windowClients.length; i++) {
      const windowClient = windowClients[i];
      if (urlToOpen.indexOf(windowClient.url) >= 0) {
        matchingClient = windowClient;
        break;
      }
    }

    if (matchingClient) {
      return matchingClient.focus();
    } else {
      console.log("Adding to pendingPushes ", event.notification.data);
      var data = event.notification.data;
      if (event.action) {
          data.action = event.action;
      }
      pendingPushes.push({type: 'push', data: data, 'visual' : true});
      return clients.openWindow(urlToOpen[0]);
    }
  }).then((windowClient) => {
      console.log("Posting push event on click "+event.notification.data);
      var data = event.notification.data;
      if (event.action) {
          data.action = event.action;
      }
      windowClient.postMessage({type: 'push', data: data, 'visual' : true});
  });

  
  event.waitUntil(promiseChain);
});

self.addEventListener('message', function(event){
    console.log('message received', event);
    if (event.data && event.data.command === 'registerPushActionCategories') {
        pushActionCategories = event.data.pushActionCategories;
        return;
    }
    if (pendingPushes.length > 0) {
        var tmp = pendingPushes.slice();
        const promiseChain = clients.matchAll({
            type: 'window',
            includeUncontrolled: true
          })
          .then((windowClients) => {
            let matchingClient = null;
        
            for (let i = 0; i < windowClients.length; i++) {
              const windowClient = windowClients[i];
              if (urlToOpen.indexOf(windowClient.url) >= 0) {
                try {
                    for (var pushData of tmp) {
                        windowClient.postMessage(pushData);
                    }
                } catch (ex){}

              }
            }
          });
        
    }
});

function fireNotification(obj, event) {
  var title = 'Subscription change';  
  var body = obj.name + ' has ' + obj.action + 'd.'; 
  var icon = 'push-icon.png';  
  var tag = 'push';
   
  event.waitUntil(self.registration.showNotification(title, {
    body: body,  
    icon: icon,  
    tag: tag  
  }));
}


function buildNotification(data) {
    var n = {};
    if (data.alertBody !== undefined && data.alertTitle !== undefined) {
        n.body = data.alertBody;
    }
    if (data.image !== undefined) {
        n.image = data.image;
    } else {
        n.image = "icon.png";
    }
    n.data = data;
    if (data.messageType === 5) {
        
        n.silent = true;
    }
    n.icon = "icon.png";
    n.badge = "icon.png";
    if (data.category) {
        var actions = findActionsForCategory(data.category);
        if (actions !== null) {
            n.actions = actions;
        }
        n.category = data.category;
    }
    
    console.log(n);
    return n;
    
}


function getNotificationTitle(data) {
    if (data.alertTitle) {
        return data.alertTitle;
    }
    return data.alertBody;
}

function includesHiddenPush(data) {
    
    switch (data.messageType) {
        case 2:
        case 3:
        case 100:
        case 102:
            return true;
            
    }
    return false;
}

function includesVisualPush(data) {
    switch (data.messageType) {
        case 0:
        case 1:
        case 3:
        case 4:
        case 5:
        case 101:
        case 102:
            return true;
    }
    return false;
}

// When the user clicks a notification focus the window if it exists or open
// a new one otherwise.
var onNotificationClick = function(event) {
  var found = false;
  clients.matchAll().then(function(clients) {
    for (i = 0; i < clients.length; i++) {
      if (clients[i].url === event.data.url) {
        // We already have a window to use, focus it.
        found = true;
        clients[i].focus();
        break;
      }
    }
    if (!found) {
      // Create a new window.
      clients.openWindow(event.data.url).then(function(windowClient) {
        // do something with the windowClient.
      });
    }
  });
};

function isClientFocused() {
  return clients.matchAll({
    type: 'window',
    includeUncontrolled: true
  })
  .then((windowClients) => {
    let clientIsFocused = false;

    for (let i = 0; i < windowClients.length; i++) {
      const windowClient = windowClients[i];
      if (windowClient.focused) {
        clientIsFocused = true;
        break;
      } else {
        
      }
    }

    return clientIsFocused;
  });
}

function getFocusedClient() {
  return clients.matchAll({
    type: 'window',
    includeUncontrolled: true
  })
  .then((windowClients) => {
    let clientIsFocused = false;

    for (let i = 0; i < windowClients.length; i++) {
      const windowClient = windowClients[i];
      if (windowClient.focused) {
        return windowClient;
      } else {
        //console.log("Client found not focused");
      }
    }

    return null;
  });
}

function openSite(event) {
    const urlToOpen = new URL(examplePage, self.location.origin).href;

  const promiseChain = clients.matchAll({
    type: 'window',
    includeUncontrolled: true
  })
  .then((windowClients) => {
    let matchingClient = null;

    for (let i = 0; i < windowClients.length; i++) {
      const windowClient = windowClients[i];
      if (windowClient.url === urlToOpen) {
        matchingClient = windowClient;
        break;
      }
    }

    if (matchingClient) {
      return matchingClient.focus();
    } else {
      return clients.openWindow(urlToOpen);
    }
  });

  event.waitUntil(promiseChain);
}


//! UpUp Service Worker
//! version : 1.0.0
//! author  : Tal Ater @TalAter
//! license : MIT
//! https://github.com/TalAter/UpUp
var _CACHE_NAME_PREFIX="upup-cache",_calculateHash=function(e){var t,n=0,s=(e=e.toString()).length;if(0===s)return n;for(t=0;t<s;t++)n=(n<<5)-n+e.charCodeAt(t),n|=0;return n};self.addEventListener("message",function(e){"set-settings"===e.data.action&&_parseSettingsAndCache(e.data.settings)}),self.addEventListener("fetch",function(e){e.respondWith(fetch(e.request).catch(function(){return caches.match(e.request).then(function(t){return t||("navigate"===e.request.mode||"GET"===e.request.method&&e.request.headers.get("accept").includes("text/html")?caches.match("sw-offline-content"):void 0)})}))});var _parseSettingsAndCache=function(e){var t=_CACHE_NAME_PREFIX+"-"+(e["cache-version"]?e["cache-version"]+"-":"")+_calculateHash(e.content+e["content-url"]+e.assets);return caches.open(t).then(function(t){return e.assets&&t.addAll(e.assets.map(function(e){return new Request(e,{mode:"no-cors"})})),e["content-url"]?fetch(e["content-url"],{mode:"no-cors"}).then(function(e){return t.put("sw-offline-content",e)}):e.content?t.put("sw-offline-content",_buildResponse(e.content)):t.put("sw-offline-content",_buildResponse("You are offline"))}).then(function(){return caches.keys().then(function(e){return Promise.all(e.map(function(e){if(e.startsWith(_CACHE_NAME_PREFIX)&&t!==e)return caches.delete(e)}))})})},_buildResponse=function(e){return new Response(e,{headers:{"Content-Type":"text/html"}})};
//# sourceMappingURL=upup.sw.min.js.map
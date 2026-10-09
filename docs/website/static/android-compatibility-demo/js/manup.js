window.addEventListener('load', function(evt) {
    //this is the object we build for an ajax call to get the content of the manifest
    //it's a JSON, so we can parse it like a man-child
    //verion 0.7
    var manUpObject;
    
    
    
    //data objects
    var tagArray = [], linkArray = [];
    var validMetaValues = [{ name: 'mobile-web-app-capable', manifestName: 'display' }, { name: 'apple-mobile-web-app-capable', manifestName: 'display' }, { name: 'apple-mobile-web-app-title', manifestName: 'short_name' }, { name: 'application-name', manifestName: 'short_name' }, { name: 'msapplication-TileColor', manifestName: 'ms_TileColor' }, { name: 'msapplication-square70x70logo', manifestName: 'icons', imageSize: '70x70' }, { name: 'msapplication-square150x150logo', manifestName: 'icons', imageSize: '150x150' },{ name: 'msapplication-wide310x150logo', manifestName: 'icons', imageSize: '310x150' },{ name: 'msapplication-square310x310logo', manifestName: 'icons', imageSize: '310x310' }];
    var validLinkValues = [{ name: 'apple-touch-icon', manifestName: 'icons', imageSize: '152x152' },{ name: 'apple-touch-icon', manifestName: 'icons', imageSize: '120x120' },{ name: 'apple-touch-icon', manifestName: 'icons', imageSize: '76x76' },{ name: 'apple-touch-icon', manifestName: 'icons', imageSize: '60x60' },{ name: 'apple-touch-icon', manifestName: 'icons', imageSize: '57x57' }, { name: 'apple-touch-icon', manifestName: 'icons', imageSize: '72x72' }, { name: 'apple-touch-icon', manifestName: 'icons', imageSize: '114x114' }, { name: 'icon', manifestName: 'icons', imageSize: '128x128' }, { name: 'icon', manifestName: 'icons', imageSize: '192x192' }]
    
    
    //these next two classes are building the mixed data, pulling out the values we need based on the valid values array
    var generateFullMetaData = function(){
     for (var i = 0;i < validMetaValues.length;  i++) {
      if(manUpObject[validMetaValues[i].manifestName]){
          if(validMetaValues[i].manifestName == 'icons'){
              //here we need to loop through each of the images to see if they match
              var imageArray = manUpObject.icons;
              for (var j = 0;j < imageArray.length;  j++) {
                  if(imageArray[j].sizes == validMetaValues[i].imageSize){
                    //problem is in here, need to asign proper value
                    validMetaValues[i].content = imageArray[j].src;
                  }
              }
          }else{
              validMetaValues[i].content = manUpObject[validMetaValues[i].manifestName];
              if (validMetaValues[i].manifestName == 'display' && manUpObject.display == 'standalone') validMetaValues[i].content = 'yes'
             // console.log('stop')
          }
      } 
     };
    
     //console.log(validMetaValues)
     return validMetaValues
    };
    
    var generateFullLinkData = function () {
        for (var i = 0; i < validLinkValues.length; i++) {
            if (manUpObject[validLinkValues[i].manifestName]) {
                if (validLinkValues[i].manifestName == 'icons') {
                    //here we need to loop through each of the images to see if they match
                    var imageArray = manUpObject.icons;
                    for (var j = 0; j < imageArray.length; j++) {
                        if (imageArray[j].sizes == validLinkValues[i].imageSize) {
                            //problem is in here, need to asign proper value
                            validLinkValues[i].content = imageArray[j].src;
                        }
                    }
    
                } else {
    
                    validLinkValues[i].content = manUpObject[validLinkValues[i].manifestName];
                }
            }
        };
    
        //console.log(validMetaValues)
        return validLinkValues
    };
    
    
            //These are the 2 functions that build out the tags
            //TODO: make it loop once instead of tx
    var generateMetaArray = function () {
        var tempMetaArray = generateFullMetaData();
        var headTarget = document.getElementsByTagName('head')[0]
        for (var i = 0; i < tempMetaArray.length; i++) {
    
            var metaTag = document.createElement('meta');
            metaTag.name = tempMetaArray[i].name;
            metaTag.content = tempMetaArray[i].content;
            headTarget.appendChild(metaTag);
    
        };
    
    };
    
    var generateLinkArray = function () {
        var tempLinkArray = generateFullLinkData();
        var headTarget = document.getElementsByTagName('head')[0]
        for (var i = 0; i < tempLinkArray.length; i++) {
    
            var linkTag = document.createElement('link');
            linkTag.setAttribute('rel', tempLinkArray[i].name);
            linkTag.setAttribute('sizes', tempLinkArray[i].imageSize);
            linkTag.setAttribute('href', tempLinkArray[i].content);
            headTarget.appendChild(linkTag);
            //console.log(linkTag);
        }
    };
    //these functions makes the ajax calls and assigns to manUp object
    var generateObj = function (ajaxString) {
        //for testing
        //document.getElementById("output").innerHTML = ajaxString;
        //gen object
        manUpObject = JSON.parse(ajaxString);
        generateLinkArray();
        generateMetaArray();
    
    };
    
    var makeAjax = function (url) {
        if (!window.XMLHttpRequest) return;
        var fullURL;
        var pat = /^https?:\/\//i;
        pat.test(url)?fulURL = url:fullURL = window.location.hostname + url;
        var ajax = new XMLHttpRequest();   
        ajax.onreadystatechange = function () {
            if (ajax.readyState == 4 && ajax.status == 200) generateObj(ajax.responseText)
        };
        ajax.open("GET", url, true);
        ajax.send();
    };
    
    var collectManifestObj = function () {
        var links = document.getElementsByTagName('link');
        for (var i = 0; i < links.length; i++) {
            if (links[i].rel && links[i].rel == 'manifest') makeAjax(links[i].href);
        }
    
    };
    
    var testForManifest = function () {
        //i'm not sure what to do here.  I am starchly against browser sniffing and there is no test (yet)
        //I think I'm jsut going to build out the tags on every page until there is a way to test it, so far
        //it looks like the manifest will override the tags
        //browser: we need context here
        collectManifestObj();
    
    }();
});
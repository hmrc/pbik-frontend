window.addEventListener("load", function() {
    if (document.getElementById('printContainer') !=null)  {
        if(supportPrint() || (isiOS() && !iOSLessThan4()) || (isAndroid() && !androidLessThan4())) {
            var prtContainer = document.getElementById("printContainer");
            var pageTitle=document.title;
            var newTag = document.createElement('a');

            newTag.setAttribute("class", "print-link");
            newTag.setAttribute("id", "print");
            newTag.setAttribute('href',"#");
            newTag.innerHTML = "Print this page";
            newTag.addEventListener("click", () => gaEventPrintLink(pageTitle));
            prtContainer.appendChild(newTag);
        }
    }
});

    function insertAfter(newNode, referenceNode) {
    //    referenceNode.parentNode.insertBefore(newNode, referenceNode.nextSibling);
        referenceNode.appendChild(newNode);
    }

    function supportPrint() {
        if (isiOS() || isAndroid()) {
            return false;
        }
        return (typeof(window.print) === 'function');
    }

    function isiOS() {
        return (/iP(hone|od|ad)/.test(navigator.platform));
    }

    function isAndroid() {
        var ua = navigator.userAgent;
        if( ua.indexOf("Android") >= 0 ) {
            return true;
        }
        return false;
    }

    function androidLessThan4() {
        var ua = navigator.userAgent;
        if(isAndroid()) {
          return parseFloat(ua.slice(ua.indexOf("Android")+8)) < 4.0;
        }
    }

    function iOSversion() {
      if (/iP(hone|od|ad)/.test(navigator.platform)) {
        // supports iOS 2.0 and later
        var v = (navigator.appVersion).match(/OS (\d+)_(\d+)_?(\d+)?/);
        return [parseInt(v[1], 10), parseInt(v[2], 10), parseInt(v[3] || 0, 10)];
      }
    }

    function iOSLessThan4() {
        return iOSversion() < 4;
    }

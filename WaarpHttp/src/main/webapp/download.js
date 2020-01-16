
function headRequest(url, options) {
  var settings = $.extend({
    successCallback: function (url, hash) { },
    failCallback: function (url, error) { },
    checkInterval: 5000,
    maxCheck: 720 // 60 minutes
  },  options);
  var req = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
  req.open("HEAD", url, true);
  req.onreadystatechange = function() {
    if (this.readyState == XMLHttpRequest.DONE) {
      if (this.status === 200) {
        // Done
        var hash = this.getResponseHeader("x-hash-sha-256");
        console.log("Done transfer with Hash "+hash);
        settings.successCallback(url, hash);
      } else if (this.status == 202 || this.status == 404) {
        // Not yet, Next check in 5s
        options.maxCheck = settings.maxCheck - 1;
        console.log("On going transfer:" + this.status + " attempts left: " + options.maxCheck);
        if (options.maxCheck == 0) {
          console.log("Aborting checking transfer");
          var abort = this.status;
          if (abort != 404) {
            abort = 206;
          }
          settings.failCallback(url, abort);
          return;
        }
        setTimeout(function() {headRequest(url, options);}, settings.checkInterval);
      } else {
        // Stop, error
        console.log("Error transfer");
        settings.failCallback(url, this.status);
      }
    }
  };
  req.send();
}
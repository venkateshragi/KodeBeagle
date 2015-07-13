(function() {
  // http://hacks.mozilla.org/2009/06/localstorage/
  // http://stackoverflow.com/questions/14555347/html5-localstorage-error-with-safari-quota-exceeded-err-dom-exception-22-an
  window.Storage.prototype.setObject = function(key, value) {
    try {
      this.setItem(key, JSON.stringify(value));
      return true;
    } catch (e) {
      return false;
    }
  };
  window.Storage.prototype.getObject = function(key) {
    try {
      var item = JSON.parse(this.getItem(key));
      return item;
    } catch (e) {
      return false;
    }
  };
})();
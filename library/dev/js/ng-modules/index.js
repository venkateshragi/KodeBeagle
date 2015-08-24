$(document).ready(function() {
  /*
  $( '#fullpage' ).fullpage( {
      'navigation': true,
      'navigationPosition': 'right',
      'navigationTooltips': [ 'Welcome', 'About', 'More info', 'Contributors' ]
  } );
  */
  $('#searchText').focus();
  /*
  var oldCanvas;
  var generateTriangle = function() {
    var pattern = Trianglify( {
        height: window.innerHeight,
        width: window.innerWidth,
        cell_size: 30 + Math.random() * 100
    } );
    if ( oldCanvas ) {
        oldCanvas.fadeOut( 2000, function() {
            $( this ).remove();
        } );
    }
    oldCanvas = $( pattern.canvas() );
    $( '.search-section' ).append( oldCanvas );
    oldCanvas.hide();
    oldCanvas.fadeIn( 2000 );
  }
  //removed home page back ground
  //generateTriangle();
  //setInterval( generateTriangle, 5000 );
  */
});
(function(angular, kb) {
  window.KB = kb;
  kb.module = angular.module( 'home', ['httpSerivice']);
  kb.module
    .controller('contributorCtrl', [
      '$scope',
      'http',
      function($scope, http) {
        http.get('https://api.github.com/repos/Imaginea/KodeBeagle/stats/contributors')
          .then(function(res) {
            for (var i = 0; i < res.length; i++) {
              if (res[i].author.login === 'gitter-badger' || res[i].author.login === 'waffle-iron') {
                res.splice(i, 1);
                i--;
              }
            }
            $scope.list = res;
          }, function(e) {
            console.log(e);
          })
      }
    ]);
})(angular, window.KB || {});

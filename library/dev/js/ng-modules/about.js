
$(document).ready(function() {
  $( '#main-content' ).fullpage( {
      'navigation': true,
      'navigationPosition': 'right',
      'navigationTooltips': [ 'About', 'sample screenshots', 'Contributors' ],
      'afterLoad': function(anchorLink, index) {
        //console.log( index );
        $( '#menu a' ).removeClass( 'active' );
        $( '#menu [data-menuanchor='+ index+']' ).addClass( 'active' );
      }
  } );
  $( '#menu' ).on( 'click', 'a', function( e ) {
    e.preventDefault();
    var data = $( this ).data( 'menuanchor' );
    $.fn.fullpage.moveTo( data );
  } )
});


(function(angular, window) {
  angular.module('about', ['httpSerivice'])
    .controller('contributorCtrl', [
      '$scope',
      'http',
      function($scope, http) {
        http.get('https://api.github.com/repos/Imaginea/KodeBeagle/stats/contributors')
        .then(function(res) {
          for (var i = 0; i < res.length; i++) {
            
            /*for( var j=0; j < res[i].weeks.length; j++ ) {
              if( !res[i].weeks[j].c ) {
                break;
              }
            }*/
            if( res[i].author.login === 'waffle-iron' || res[i].author.login === 'gitter-badger' ) {
              res.splice(i, 1);
              i--;
            }
          }
          $scope.list = res;
        }, function(e) {
          console.log(e);
        })
      }
    ])
    ;
  angular.bootstrap(document, ['about']);
})(angular, window);

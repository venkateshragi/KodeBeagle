
$( '.section-container' ).waypoint(function(direction) {
  $( '#main-nav li' ).removeClass( 'active' );
  $( '#main-nav a[href=#' + $(this.element).attr( 'id' ) + ']' ).parent().addClass( 'active' );
  //console.log( $(this.element).attr( 'id' ) );
}, {
  offset: '-2%'
} );
$( '.section-container' ).waypoint(function(direction) {
  $( this.element ).addClass( 'animated' );

}, {
  offset: '65%'
} );


$( window ).scroll( function  (argument) {
  if( $( this ).scrollTop() > 10  ) {
    $( '#header' ).addClass( 'sticky-top' );
  } else {
    $( '#header' ).removeClass( 'sticky-top' );
  }
} );

$( '#main-nav a' ).click( function( e ) {
    e.preventDefault();
    var self = $( this );
    self.parent().addClass( 'active' );
    var id = $(this).attr( 'href' );
    $("html, body").animate(
        {
            scrollTop: $( id ).offset().top  + 'px'
        } ,
        1000 ,
        'easeInOutQuint',
        function() {
            location.hash = id;
            $( '#main-nav li' ).removeClass( 'active' );
            self.parent().addClass( 'active' );
        }
    )
} );


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

$(document).ready(function() {

  var owl = $("#owl-demo").owlCarousel({
      slideSpeed : 300,
      paginationSpeed : 400,
      singleItem:true,
      autoPlay: true,
      pagination: false,
      navigation: true,
      navigationText : ["previous","next"],
      beforeMove: function  () {
        navLinks.removeClass( 'active' );
        navLinks.eq( owl.currentItem ).addClass( 'active' );
      }

  }).data('owlCarousel');

  var navLinks = $( '#carousel-nav a' );
  navLinks.on( 'click', function( e ) {
    e.preventDefault();
    owl.currentItem = $( this ).data( 'target' );
    owl.goTo( $( this ).data( 'target' ) );
  } );

});

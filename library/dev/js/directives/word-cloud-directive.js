(function( module ) {
  module.directive('wordCloud', [
    function() {
      return {
        scope: {
          terms: '=',
          factor:'='
        },
        link: function( scope, ele ) {
          var list = [];
          var sum =0;
          var factor = scope.factor || 200;
          for( var len=scope.terms.length -1, i=0 ; i<len ; i++ ) {
            sum +=scope.terms[ i ].score;
          }

          for( var len=scope.terms.length -1, i=0 ; i<len ; i++ ) {
            if( sum ) {
              list.push( [ 
                scope.terms[ i ].term,
                scope.terms[ i ].score*factor/sum
              ]);  
            }
            
          }

          WordCloud( ele[0], {
            list: list,
            minSize: 8,
            backgroundColor: 'transparent'
          } );
        }
      };
    }
  ] );
} )( KB.module )
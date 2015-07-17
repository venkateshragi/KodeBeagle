(function( module ) {
  module.directive('initModel', [
    'model',
    '$location',
    '$rootScope',
    'docsService',
    '$document',
    function(model, $location, $rootScope, docsService, $document) {
      var esURL = 'http://labs.imaginea.com/kodebeagle';
      model.showConfig = $location.search().advanced;
      model.pageResultSize=10;
      model.toggelSnippet = true;

      $document.bind( 'keydown', function( e ) {
        if( e.ctrlKey || e.shiftKey ) {
          return;
        }

        if( e.target.nodeName.toLowerCase() === 'input' ) {
          return;
        }
        var keyCode = e.which;
        if( keyCode === 38 || keyCode === 40 ) {
          
        }
        var key = String.fromCharCode( keyCode );

        if( key.search( /[a-zA-Z]/i ) !== -1 ) {
            document.getElementById( 'searchText' ).focus();
            window.scrollTo(0, 0);
        }
      } );
      var backTotop = angular.element( document.getElementById( 'back-to-top' ) )
      var navEle = angular.element( document.getElementById( 'header-nav' ) );
      var bodyEle = angular.element( document.body );
      var prevScrollTop;
      $document.bind( 'scroll', function( e ) {
        var topOffset = document.body.scrollTop;
        if( topOffset > 50 ) {
          backTotop.addClass( 'show' );
        } else {
          backTotop.removeClass( 'show' );
        }
        
        if( prevScrollTop > topOffset ) {
          bodyEle.addClass( 'stick-to-top' );

          navEle[0].style.top = topOffset + 'px';
          if( topOffset < 5 ) {
            bodyEle.removeClass( 'stick-to-top' );
            navEle[0].style.top = '';
          }
          //navEle.css( top, topOffset+'px' );
        } 
        prevScrollTop = topOffset;
        
      } ); 

      function buildNestedQuery( q ) {
        var pkgs = q.split( ',' );
        var must = [];
        var innerMust;
        var currectedQuery = '';
        model.packages = {};
        var pkgName;
          
        for( var i=0;i<pkgs.length; i++) {
          var pkg = pkgs[i].split( '->method:' );
          innerMust = [];
          pkgName = ''; 

          if( pkg[0].indexOf( 'pkg:' ) !=-1 ) {
            pkgName = pkg[ 0 ].substring( 4 );
            innerMust.push( {
              'term' : {
                'tokens.importName': pkgName.toLowerCase()
              }
            } );
            currectedQuery += ',' + pkgName.substring( 4 );
            model.packages[ pkgName ] = {
              status: true,
              methods: {}
            };
          }
          var methods = ( pkg[1] || '' ).split( '&' );
          for( var j = 0 ; j < methods.length ; j++ ) {
            if( methods[ j ] ) {
              innerMust.push( {
                'term' : {
                  'tokens.methodAndLineNumbers.methodName': methods[ j ]
                }
              } );
              model.packages[ pkgName ].methods[ methods[ j ] ] = true;
              currectedQuery += ',' + methods[ j ];  
            }
            
          }

          must.push( {
            'nested' : {
               'path': 'tokens',
               'query' : {
                  'bool' :  {
                    'must': innerMust
                  }
               } 
            }
          } );
        }

        if( must.length ) {
          docsService.search( {
              query:{
                bool:{
                  must: must
                }
              },
              currectedQuery:currectedQuery.substring(1)
            }, model );
        }

      }

      backTotop.bind( 'click', function( e ) {
        e.preventDefault();
        window.scrollTo(0, 0);
      } )
      return {
        controller: ['$scope', '$rootScope', function(scope, $rootScope) {
          scope.model = model;
          scope.$watch(function() {
            return $location.search();
          }, function(params) {
            model.currentPageNo = 0;
            var selectedTexts = $location.search().searchTerms;
            model.showConfig = $location.search().advanced;
            var nestedQuery = $location.search().query;

            if (selectedTexts) {
              model.selectedTexts = selectedTexts.split(',');
              model.searchedData = model.selectedTexts.join( ', ' );
              docsService.search(selectedTexts, model);
              document.getElementById( 'searchText' ) && document.getElementById( 'searchText' ).blur();
              $rootScope.editorView = true;
              model.packages = {};
            } else if( nestedQuery ) {
              $rootScope.editorView = true;
              model.searchedData = nestedQuery.split( ',' ).join( ', ' );
              model.selectedTexts = nestedQuery.split(',');
              buildNestedQuery( nestedQuery );
            } 
            else {
              model.selectedTexts = [];
              $rootScope.editorView = false;
            }
          });
          model.config = localStorage.getObject('config') || {
            selectedTheme: 'theme-light',
            esURL: esURL,
            resultSize: 500
          };
          model.config.esURL = model.config.esURL || esURL;
          model.config.resultSize = model.config.resultSize || 500;
        }]
      };
    }
  ]);
} )( KB.module )

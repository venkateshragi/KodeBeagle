$( document ).ready( function() {
    $( '#fullpage' ).fullpage( {
        'navigation': true,
        'navigationPosition': 'right',
        'navigationTooltips': [ 'Welcome', 'About', 'More info', 'Contributors' ]
    } );
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
    
    generateTriangle();
    setInterval( generateTriangle, 5000 );
} );



( function( angular, window ) {
    angular.module( 'home', [ 'httpSerivice' ] )
        .controller( 'contributorCtrl', [
            '$scope',
            'http',
            function( $scope, http ) {
                http.get( 'https://api.github.com/repos/Imaginea/KodeBeagle/stats/contributors' )
                    .then( function( res ) {
                        for( var i=0; i<res.length;i++ ) {
                            if( res[i].author.login === 'gitter-badger' || res[i].author.login === 'waffle-iron' ) {
                                res.splice( i, 1 );
                                i--;
                            }
                        }
                        $scope.list = res;
                    }, function( e ) {
                        console.log( e );
                    } )
            }
        ] )
        .directive( 'searchComponent', function() {
            return {
                controller: 'searchController',
                templateUrl: 'search-component.html'
            };
        } )
        .controller( 'searchController', [
            '$scope',
            '$timeout',
            function(

                $scope,
                $timeout

            ) {

                $scope.selectedTexts = [];

                //var suggestions;
                $scope.handleSelectedText = function( e, i ) {
                    e.preventDefault();
                    //var target = e.target;
                    var spliceData = $scope.selectedTexts.splice( i, 1 );
                    if ( $scope.searchText ) {
                        $scope.selectedTexts.push( $scope.searchText );
                    }
                    $scope.searchText = spliceData[ 0 ];
                    document.getElementById( 'searchText' ).focus();

                };
                $scope.deleteItem = function( e, i ) {
                    e.preventDefault();
                    $scope.selectedTexts.splice( i, 1 );
                };

                $scope.handleClick = function( item ) {
                    $scope.selectedTexts.push( item.suggested );
                    //$scope.searchText = item.suggested;
                };

                $scope.clearAll = function( e ) {

                    e.preventDefault();
                    $scope.selectedTexts = [];
                    $scope.searchText = '';
                    document.getElementById( 'searchText' ).focus();
                };

                function doGetCaretPosition( ctrl ) {
                    var CaretPos = 0; // IE Support
                    if ( document.selection ) {
                        ctrl.focus();
                        var Sel = document.selection.createRange();
                        Sel.moveStart( 'character', -ctrl.value.length );
                        CaretPos = Sel.text.length;
                    }
                    // Firefox support
                    else if ( ctrl.selectionStart || ctrl.selectionStart === '0' )
                        CaretPos = ctrl.selectionStart;
                    return ( CaretPos );
                }
                $scope.getResultsPage = function() {
                    var url = 'demov1.html#?searchTerms='
                    var query = $scope.selectedTexts.join( ',' );

                    if ( $scope.searchText ) {
                        if ( query ) {
                            query += ',' + $scope.searchText;
                        } else {
                            query = $scope.searchText;
                        }
                    }
                    if ( query ) {
                        return url + query;
                    }



                }

                $scope.handleSearchText = function( e ) {

                    if ( e.keyCode === 9 ) {
                        if ( $scope.searchText ) {
                            $scope.selectedTexts.push( $scope.searchText );
                            $scope.searchText = '';
                            //$scope.checkFormSubmit = true;
                            e.preventDefault();
                            return false;
                        }
                    }
                    if ( e.keyCode === 188 ) {
                        if ( $scope.searchText ) {
                            $scope.selectedTexts.push( $scope.searchText );
                            $scope.searchText = '';
                            //$scope.checkFormSubmit = true;
                        }
                        e.preventDefault();
                        return false;
                    }

                    if ( e.keyCode === 13 ) {
                        if ( $scope.searchText ) {
                            $scope.selectedTexts.push( $scope.searchText );
                            $scope.searchText = '';
                            return false;
                        } else {
                            document.getElementById( 'search-button' ).click();
                        }
                    }


                    if ( doGetCaretPosition( e.target ) === 0 && e.keyCode === 8 ) {
                        //$scope.selectedTexts[ $scope.selectedTexts.length - 1 ].tobe
                        $scope.selectedTexts.pop();
                        return;
                    }


                };
            }
        ] );

    angular.bootstrap( document, [ 'home' ] );

} )( angular, window );

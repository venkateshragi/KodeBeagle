( function() {

    // http://hacks.mozilla.org/2009/06/localstorage/
    // http://stackoverflow.com/questions/14555347/html5-localstorage-error-with-safari-quota-exceeded-err-dom-exception-22-an
    window.Storage.prototype.setObject = function( key, value ) {

        try {

            this.setItem( key, JSON.stringify( value ) );
            return true;

        } catch ( e ) {

            return false;
        }

    };

    window.Storage.prototype.getObject = function( key ) {

        try {

            var item = JSON.parse( this.getItem( key ) );
            return item;

        } catch ( e ) {

            return false;
        }
    };

} )();

( function( angular, window, ace ) {
    var Range = ace.require( 'ace/range' ).Range;

    angular.module( 'KodeBeagle', [ 'httpSerivice', 'ui.bootstrap', 'ui.ace', 'ngAnimate' ] )
        .directive( 'searchComponent', function() {
            return {
                controller: 'searchController',
                templateUrl: 'search-component.html'
            };
        } )
        .directive( 'initModel', [
            'model',
            '$location',
            '$rootScope',
            'docsService',
            function( model, $location, $rootScope, docsService ) {
                var esURL = 'http://labs.imaginea.com/kodebeagle';
                return {
                    controller: [ '$scope', '$rootScope', function( scope, $rootScope ) {

                        scope.model = model;

                        scope.$watch( function() {
                            return $location.search();
                        }, function( params ) {
                            var selectedTexts = $location.search().searchTerms;
                            if ( selectedTexts ) {
                                model.selectedTexts = selectedTexts.split( ',' );
                                docsService.search( selectedTexts, model );
                                $rootScope.editorView = true;
                            } else {
                                model.selectedTexts = [];
                                $rootScope.editorView = false;
                            }
                        } );

                        model.config = localStorage.getObject( 'config' ) || {
                            selectedTheme: 'theme-light',
                            editorTheme: 'monokai',
                            esURL: esURL,
                            resultSize: 50
                        };
                        model.config.esURL = model.config.esURL || esURL;
                        model.config.resultSize = model.config.resultSize || 50;
                    } ]
                };
            }
        ] )
        .controller( 'searchController', [

            '$scope',
            'http',
            '$timeout',
            '$filter',
            '$rootScope',
            'model',
            '$modal',
            'docsService',
            '$location',
            function(

                $scope,
                http,
                $timeout,
                $filter,
                $rootScope,
                model,
                $modal,
                docsService,
                $location


            ) {

                $scope.model = model;
                model.selectedTexts = model.selectedTexts || [];

                //var suggestions;
                $scope.handleSelectedText = function( e, i ) {
                    e.preventDefault();
                    //var target = e.target;
                    var spliceData = model.selectedTexts.splice( i, 1 );
                    if ( model.searchText ) {
                        model.selectedTexts.push( model.searchText );
                    }
                    model.searchText = spliceData[ 0 ];
                    document.getElementById( 'searchText' ).focus();

                };
                $scope.deleteItem = function( e, i ) {
                    e.preventDefault();
                    model.selectedTexts.splice( i, 1 );
                };

                $scope.handleClick = function( item ) {
                    model.selectedTexts.push( item.suggested );
                    //$scope.searchText = item.suggested;
                };

                $scope.formSubmit = function( forceSubmit ) {

                    $scope.showRequiredMsg = false;
                    if ( model.searchText ) {
                        model.selectedTexts.push( model.searchText );
                        model.searchText = '';
                    }
                    var searchTerm = model.selectedTexts.join( ',' );
                    //console.log( '------------formSubmit----------');

                    /*if( forceSubmit ) {
						//console.log( 'force form submitted' );
						$rootScope.editorView = true;
						docsService.search( model.selectedTexts.join(','), model );
						//app.search()
						return;
					}*/
                    if ( searchTerm ) {
                        //console.log( 'form formSubmit' );
                        /*if( model.searchText ) {
							if( searchTerm ) {
								searchTerm += ',' + model.searchText ; 
							} else {
								searchTerm = model.searchText; 
							}
						}*/

                        //docsService.search( searchTerm, model );
                        $location.search( {
                            'searchTerms': searchTerm
                        } );
                        $rootScope.editorView = true;
                    } else {
                        //angular.element( document.getElementById( 'search-form' ) ).trigger('show');
                        //Close the info again
                        //$scope.showRequiredMsg = true; 
                        angular.element( document.getElementById( 'search-form' ) ).popover( 'show' );
                        $timeout( function() {
                            angular.element( document.getElementById( 'search-form' ) ).popover( 'hide' );
                        }, 3000 );
                    }
                };

                $scope.clearAll = function( e ) {

                    e.preventDefault();
                    model.selectedTexts = [];
                    model.searchText = '';
                    document.getElementById( 'searchText' ).focus();
                };

                $scope.selectSuggestion = function( e ) {
                    e.preventDefault();

                };
                $scope.searchTextFocusout = function( e ) {
                    //$scope.suggestionTerm = '';
                    //console.log( 'asdad' );
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


                $scope.handleSearchText = function( e ) {
                    //console.log('------------e.keyCode----------');
                    //console.log( e.keyCode );
                    /*if( e.keyCode !== 40 && e.keyCode !== 38  ) {
						$scope.arrowIndex = -1;
						$timeout(function() {
							//$scope.suggestionTerm = model.searchText;
							//$scope.filteredSuggestions = $filter( 'getSuggestion' )( $scope.suggestions, $scope.suggestionTerm );
						} );
					}*/
                    //$scope.checkFormSubmit = false;

                    if ( e.keyCode === 9 ) {
                        if ( model.searchText ) {
                            model.selectedTexts.push( model.searchText );
                            model.searchText = '';
                            //$scope.checkFormSubmit = true;
                            e.preventDefault();
                            return false;
                        }
                    }
                    if ( e.keyCode === 188 ) {
                        if ( model.searchText ) {
                            model.selectedTexts.push( model.searchText );
                            model.searchText = '';
                            //$scope.checkFormSubmit = true;
                        }
                        e.preventDefault();
                        return false;
                    }

                    /*if( e.keyCode === 13 ) {
						if( model.searchText ) {
							model.selectedTexts.push( model.searchText );
							model.searchText = '' ;
							$scope.checkFormSubmit = true;
							return false;
						}
					}*/


                    /*if( e.keyCode === 40 ) {

						$scope.arrowIndex = ( $scope.arrowIndex + 1 ) % $scope.filteredSuggestions.length;
						var item = $scope.filteredSuggestions[ $scope.arrowIndex ];
						if( item ) {
							model.searchText = item.suggested;
						}
						
					}
				
					if( e.keyCode === 38 ) {
						$scope.arrowIndex--;
						if( $scope.arrowIndex <= -1 ) {
							$scope.arrowIndex = $scope.filteredSuggestions.length -1;
						}
						var item = $scope.filteredSuggestions[ $scope.arrowIndex ];
						if( item ) {
							model.searchText = item.suggested;
						} 	
						e.preventDefault();
					}*/


                    if ( doGetCaretPosition( e.target ) === 0 && e.keyCode === 8 ) {
                        //$scope.selectedTexts[ $scope.selectedTexts.length - 1 ].tobe
                        model.selectedTexts.pop();
                        return;
                    }


                    /*if( $scope.suggestions ) {
						return;
					}

					http.get( 'services/suggestions.json' )
					.then( function( data ) {
						$scope.suggestions = data.response;
						$scope.filteredSuggestions = $filter( 'getSuggestion' )( $scope.suggestions, $scope.suggestionTerm );
					} );*/
                };
            }
        ] )
        .controller( 'pageFeatureCtrl', [
            '$scope',
            'model',
            function( $scope, model ) {
                model.gridView = false;
                $scope.model = model;
                $scope.currentSize = 15;
                $scope.adjustFont = function( e, cond ) {
                    e.preventDefault();
                    if ( cond ) {
                        $scope.currentSize += 3;
                        if ( $scope.currentSize > 22 ) {
                            $scope.currentSize = 22;
                        }
                    } else {
                        $scope.currentSize -= 3;
                        if ( $scope.currentSize < 9 ) {
                            $scope.currentSize = 9;
                        }
                    }
                };
            }
        ] )
        .filter( 'getSuggestion', [ '$filter', function( $filter ) {

            return function( array, text ) {
                if ( !text ) {
                    return [];
                }
                return $filter( 'limitTo' )( $filter( 'filter' )( array, text ), 10 );
            };

        } ] )
        .filter( 'trust', [ '$sce', function( $sce ) {

            return function( html ) {

                return $sce.trustAsHtml( html );
            };

        } ] )
        .factory( 'model', function() {

            this.data = {};

            return this.data;
        } )
        .controller( 'analyzedProjectsCtrl', [
            '$scope',
            '$modalInstance',
            'data',

            function(

                $scope,
                $modalInstance,
                data

            ) {

                $scope.data = data;

                /*$scope.ok = function () {

				    $modalInstance.close( angular.copy ( $scope.common.data ) );

				};

				$scope.cancel = function () {

				    $modalInstance.dismiss( 'cancel' );

				};*/
                $scope.dismiss = function( e ) {
                    e.preventDefault();
                    $modalInstance.dismiss( 'cancel' );

                };

            }
        ] )
        .controller( 'userSettingsCtrl', [
            '$scope',
            '$modalInstance',
            'data',
            'model',

            function(

                $scope,
                $modalInstance,
                data,
                model

            ) {

                $scope.data = data;
                $scope.model = model;

                $scope.dismiss = function( e ) {
                    e.preventDefault();
                    $modalInstance.dismiss( 'cancel' );

                };
                $scope.ok = function( e ) {
                    e.preventDefault();
                    $modalInstance.close();
                };
            }
        ] )
        .directive( 'openMethodPopup', [
            '$modal',
            function( $modal ) {
                return {
                    scope: {
                        info: '='
                    },
                    link: function( scope, element ) {
                        element.on( 'click', function( e ) {
                            e.preventDefault();
                            $modal.open( {
                                templateUrl: 'method-popup.html',
                                resolve: {
                                    data: function() {

                                        return {
                                            info: scope.info
                                        };
                                    }

                                },
                                controller: 'methodPopCtrl'
                            } );

                        } );
                    }
                };
            }
        ] )
        .controller( 'methodPopCtrl', [
            '$scope',
            '$modalInstance',
            'data',
            function(
                $scope,
                $modalInstance,
                data
            ) {

                $scope.data = data;

                $scope.dismiss = function( e ) {
                    e.preventDefault();
                    $modalInstance.dismiss( 'cancel' );

                };
            }
        ] )
        .directive( 'openPopup', [
            '$modal',
            'docsService',
            'model',

            function( $modal, docsService, model ) {
                return {

                    restrict: 'A',
                    scope: {
                        template: '@',
                        ctrl: '@',
                        id: '@',
                        size: '@'
                    },
                    link: function( scope, element ) {
                        function openModal( data ) {

                            var modalInstance = $modal.open( {
                                templateUrl: scope.template,
                                controller: scope.ctrl,
                                size: scope.size,
                                resolve: {

                                    data: function() {
                                        return {
                                            projects: data
                                        };
                                    }
                                }

                            } );
                            if ( scope.id === 'config' ) {
                                modalInstance.result.then( function() {
                                    localStorage.setObject( 'config', model.config );
                                }, function() {
                                    model.config = angular.copy( scope.config );

                                } );
                            }

                        }

                        element.on( 'click', function( e ) {


                            e.preventDefault();
                            if ( scope.id === 'projects' ) {
                                var projectlist = function( data ) {
                                    scope.data = data;
                                    openModal( scope.data );
                                };
                                if ( !scope.data ) {
                                    docsService.queryES( 'repository', {
                                        'query': {
                                            'match_all': {}
                                        },
                                        'sort': [ {
                                            'stargazersCount': {
                                                'order': 'desc'
                                            }
                                        } ]
                                    }, 750, projectlist );
                                } else {
                                    projectlist( scope.data );
                                }

                            } else {
                                scope.config = angular.copy( model.config );
                                openModal();
                            }
                        } );

                    }
                };
            }
        ] )
        .directive( 'welcomeView', [
            '$rootScope',
            function( $rootScope ) {
                return {

                    restrict: 'A',
                    link: function( scope, element ) {

                        element.on( 'click', function() {
                            $rootScope.editorView = false;
                            $rootScope.$apply();
                        } );

                    }
                };
            }
        ] )
        .controller( 'editorCtroller', [
            '$scope',
            'model',
            'docsService',
            function( $scope, model, docsService ) {
                $scope.model = model;
                $scope.tabs = [ {
                    active: false,
                    heading: 'Files'
                }, {
                    active: true,
                    heading: 'Methods'
                } ];


                $scope.toggleExpand = function expandAllBlocks( e, index, item ) {
                    e.preventDefault();
                    e.stopPropagation();
                    item.collapsed = !item.collapsed;
                    var lineNumbers = angular.copy( item.fileInfo.lines );
                    var editor = ace.edit( 'editor-' + index + '-repeat-ace' );

                    if ( item.collapsed ) {
                        editor.getSession().unfold();
                        editor.getSession().foldAll();
                        lineNumbers.forEach( function( n ) {
                            editor.getSession().unfold( n );
                        } );
                    } else {
                        foldLines( editor, lineNumbers );
                    }


                };

                $scope.fileEleClick = function( e, item ) {
                    //console.log( item );
                    e.preventDefault();
                    docsService.addFileToView( item );
                };






                $scope.expandView = function( e, item ) {

                    e.preventDefault();

                    if ( model.expandedItem ) {
                        model.expandedItem = false;
                    } else {
                        model.expandedItem = item;
                    }
                    $scope.hideLeftPanel = false;


                };

                $scope.fullExpandView = function( e, item ) {

                    e.preventDefault();
                    if ( e.target !== e.currentTarget ) {
                        return false;
                    }
                    $scope.hideLeftPanel = !$scope.hideLeftPanel;
                    if ( !$scope.hideLeftPanel ) {
                        model.expandedItem = false;
                    } else {
                        model.expandedItem = item;
                    }



                };


                $scope.mode = 'java';

                function highlightLine( editor, lineNumbers ) {
                    //console.log( 'lineNumbers', lineNumbers );
                    /*IMPORTANT NOTE: Range takes row number starting from 0*/
                    lineNumbers.forEach( function( line ) {

                        var row = line - 1,
                            endCol = editor.session.getLine( row ).length,
                            range = new Range( row, 0, row, endCol );

                        editor.getSession().addMarker( range, 'ace_selection', 'background' );
                    } );
                }

                function getLine2( n1, n2, totalLength, start, end, editor ) {
                    var absoluteEnd = n1 + end;
                    var range;

                    if ( n2 ) {

                        if ( absoluteEnd < n2 - start ) {
                            /**
                             * if nextlineNumber is present then  
                             * check the absoluteEnd is less then nextlineNumber - startingOffset
                             * then return the absoluteEnd 
                             * and foldlines from absoluteEnd + 1 to nextlineNumber - startingOffset - 1  
                             */
                            if ( n2 - start - 1 - absoluteEnd >= 2 ) {
                                range = new Range( absoluteEnd + 1, 0, n2 - start - 1, 0 );
                                console.log( 'absoluteEnd + 1,,, n2 - start - 1:' + ( absoluteEnd + 1 ) + '-' + ( n2 - start - 1 ) )
                                editor.getSession().addFold( '--', range );
                            }
                            return absoluteEnd;
                        } else {
                            /**
                             * if nextlineNumber is present then  
                             * check the absoluteEnd is greater then nextlineNumber - startingOffset
                             * then return the nextlineNumber - startingOffset - 1 as linenumber2 
                             */
                            return n2 - start - 1;
                        }
                    } else {

                        if ( absoluteEnd < totalLength ) {
                            /**
                             * if nextlineNumber is not present
                             * then check the absoluteEnd is less then totalLength
                             * then return the absoluteEnd 
                             * and foldlines from absoluteEnd + 1 to totalLength  
                             */
                            if ( totalLength - ( absoluteEnd + 1 ) >= 2 ) {
                                range = new Range( absoluteEnd + 1, 0, totalLength, 0 );
                                editor.getSession().addFold( '--', range );
                                console.log( 'absoluteEnd totalLength: ' + absoluteEnd + '-' + totalLength );
                            }

                            return absoluteEnd;
                        } else {
                            /**
                             * if nextlineNumber is not present
                             * then check the absoluteEnd is greater then totalLength
                             * then return the totalLength as linenumber2 
                             */
                            return totalLength;
                        }
                    }
                }

                function foldLines( editor, lineNumbers ) {

                    console.log( lineNumbers );
                    var nextLine = 0;
                    var totalLength = editor.getSession().getLength();
                    var leftOffset = 1;
                    var rightOffset = 1;
                    var l1;
                    var l2;

                    var range = new Range( 0, 0, lineNumbers[ 0 ] - leftOffset - 1, 0 );

                    //to hide lines
                    editor.getSession().addFold( '--', range );
                    console.log( '0 - lineNumbers[ 0 ] - leftOffset - 1 :' + 0 + '-' + ( lineNumbers[ 0 ] - leftOffset - 1 ) );

                    for ( var i = 0; i < lineNumbers.length; i++ ) {
                        l1 = lineNumbers[ i ] - leftOffset;
                        l2 = getLine2( lineNumbers[ i ], lineNumbers[ i + 1 ], totalLength, leftOffset, rightOffset, editor );
                        if ( l2 - l1 >= 2 ) {
                            range = new Range( l1, 0, l2, 0 );
                            /*editor.getSession().addFold( '...', range );*/
                            console.log( 'line l1-l2: ' + l1 + '-' + l2 );
                        }

                    }

                    //console.log( lineNumbers ); //[48, 56, 61]
                    /*lineNumbers.forEach( function( n ) {
                        if ( nextLine !== n - 1 ) {

                            nextLine = nextLine===0 ? nextLine: nextLine-2 ;
                            var range = new Range( nextLine, 0, n + 5, 0 );
                            console.log(nextLine,n+5);
                            editor.getSession().addFold( '...', range );
                        }
                        nextLine = n;
                    } );*/
                    /*var range = new Range( 0, 0, 45, 0 );
                    //console.log(nextLine,n+5);
                    editor.getSession().addFold( '', range );
                    
                    range = new Range( 46, 0, 53, 0 );
                    //console.log(nextLine,n+5);
                    editor.getSession().addFold( '...', range );

                    range = new Range( 54, 0, 60, 0 );
                    //console.log(nextLine,n+5);
                    editor.getSession().addFold( '...', range );

                    range = new Range( 61, 0, 63, 0 );
                    //console.log(nextLine,n+5);
                    editor.getSession().addFold( '...', range );*/

                    //editor.getSession().addFold( '...', new Range( nextLine, 0, editor.getSession().getLength(), 0 ) );
                }

                $scope.getAceOptions = function( item ) {

                    var lineNumbers = item.fileInfo.lines;
                    return {
                        mode: $scope.mode.toLowerCase(),
                        theme: model.config.editorTheme,
                        onLoad: function( _ace ) {

                            _ace.setReadOnly( true );
                            _ace.setValue( item.content );

                            _ace.setOptions( {
                                maxLines: Infinity
                            } );


                            _ace.getSession().setUseWrapMode( true );
                            _ace.setDisplayIndentGuides( false );
                            //_ace.renderer.setHScrollBarAlwaysVisible(false);
                            // HACK to have the ace instance in the scope...
                            _ace.getSession().setMode( 'ace/mode/' + $scope.mode.toLowerCase(), function() {
                                highlightLine( _ace, lineNumbers );
                                foldLines( _ace, lineNumbers );
                            } );

                            _ace.gotoLine( lineNumbers[ lineNumbers.length - 1 ], 0, true );
                        }
                    };
                };

            }
        ] )
        .factory( 'docsService', [
            'model',
            'http',
            function(
                model,
                http
            ) {
                var //resultSize = 50
                    commonMethods = [],
                    currentResult = [],
                    docsBaseUrl = 'http://labs.imaginea.com/java7docs/api/';


                function buildSearchString( str ) {
                    var result = '';
                    if ( str[ 0 ] === '\'' ) {
                        result = str.substr( 1, str.length - 2 );
                    } else {
                        result = str.split( ',' ).map( function( entry ) {
                            return '*' + entry.trim();
                        } ).join( ',' );
                    }
                    return result;
                }

                function search( queryString, m ) {
                    //scope = s;
                    var correctedQuery = buildSearchString( queryString ),
                        queryBlock = getQuery( correctedQuery );


                    queryES( 'kodebeagle', {
                        'query': queryBlock,
                        'sort': [ {
                            'score': {
                                'order': 'desc'
                            }
                        } ]
                    }, model.config.resultSize, function( result ) {
                        updateView( correctedQuery, result );
                    } );
                }

                function getQuery( queryString ) {
                    var terms = queryString.split( ',' ),
                        mustTerms = terms.map( function( queryTerm ) {
                            var prefix = ( queryTerm.search( /\*/ ) >= 0 || queryTerm.search( /\?/ ) >= 0 ) ? 'wildcard' : 'term';
                            var result = {};
                            result[ prefix ] = {
                                'custom.tokens.importName': queryTerm.trim()
                            };
                            return result;
                        } );

                    return {
                        'bool': {
                            'must': mustTerms,
                            'must_not': [],
                            'should': []
                        }
                    };
                }


                function queryES( indexName, queryBody, resultSize, successCallback ) {
                    var url = model.config.esURL + '/' + indexName + '/_search?size=' + model.config.resultSize + '&source=' + JSON.stringify( queryBody );
                    http.get( url )
                        .then( function( result ) {
                            successCallback( result.hits.hits );
                            model.showErrorMsg = false;

                        }, function( error, s ) {
                            model.errorMsg = error && error.message;
                            model.showErrorMsg = true;
                            //errorMsgContainer.text(err.message);
                        } );

                }

                function updateView( searchString, data ) {

                    var processedData = processResult( searchString, data );

                    //analyzedProjContainer.hide();
                    //searchMetaContainer.show();
                    //
                    //console.log( processedData.result );
                    model.emptyResponse = false;
                    commonMethods = [];
                    model.groupedMethods = [];
                    model.editors = [];


                    updateLeftPanel( processedData.result );
                    updateRightSide( processedData.result );

                    processedData.classes.forEach( function( cName ) {
                        searchCommonUsage( cName );
                    } );

                    if ( processedData.result.length === 0 ) {
                        model.emptyResponse = true;
                    }

                }

                function updateLeftPanel( processedData ) {



                    var projects = [],
                        groupedByRepos = _.groupBy( processedData, function( entry ) {
                            return entry.repo;
                        } );
                    //console.log( groupedByRepos );

                    projects = _.map( groupedByRepos, function( files, label ) {
                        return {
                            name: label,
                            files: _.unique( files, _.iteratee( 'name' ) )
                        };
                    } );
                    //console.log( '---------projects-------' );
                    //console.log( projects );
                    model.projects = projects;


                    //fileTab.addClass('active');
                    //resultTreeContainer.html(resultTreeTemplate({'projects': projects}));
                }

                function renderFileContent( fileInfo, index ) {
                    var content;
                    queryES( 'sourcefile', fetchFileQuery( fileInfo.path ), 1, function( result ) {
                        /*var id = 'result' + index,
						    content = '';*/
                        if ( result.length > 0 ) {
                            content = result[ 0 ]._source.fileContent;
                            model.editors.splice( index, 0, {
                                content: content,
                                fileInfo: fileInfo
                            } );
                            //model.editors.push( );
                            //enableAceEditor(id + '-editor', content, fileInfo.lines);
                        }
                    } );
                }

                function fetchFileQuery( fileName ) {
                    return {
                        'query': {
                            'term': {
                                'typesourcefile.fileName': fileName
                            }
                        }
                    };
                }

                function updateRightSide( processedData ) {
                    var files = processedData.slice( 0, 2 );


                    model.expandedItem = false;
                    files.forEach( function( fileInfo, index ) {
                        renderFileContent( fileInfo, index );
                    } );

                    //$('#results').html(resultTemplate({'files': files}));

                    /*$('.fa-caret-square-o-right').tooltipster({
					    theme: 'tooltipster-light',
					    content: 'Expand'
					});

					$('.fa-caret-square-o-down').tooltipster({
					    theme: 'tooltipster-light',
					    content: 'Collapse'
					});*/
                }


                function processResult( searchString, data ) {
                    var result = [],
                        intermediateResult = [],
                        groupedData = [],
                        matchingImports = [];

                    groupedData = _.groupBy( data, function( entry ) {
                        return entry._source.file;
                    } );

                    intermediateResult = _.map( groupedData, function( files, fileName ) {
                        var labels = getFileName( fileName ),
                            lineNumbers = [];

                        files.forEach( function( f ) {
                            var matchingTokens = filterRelevantTokens( searchString, f._source.tokens ),
                                possibleLines = _.pluck( matchingTokens, 'lineNumbers' );

                            matchingImports = matchingImports.concat( matchingTokens.map( function( x ) {
                                return x.importName;
                            } ) );

                            lineNumbers = lineNumbers.concat( possibleLines );
                        } );

                        lineNumbers = ( _.unique( _.flatten( lineNumbers ) ) ).sort( function( a, b ) {
                            return a - b;
                        } );

                        return {
                            path: fileName,
                            repo: labels.repo,
                            name: labels.file,
                            lines: lineNumbers,
                            score: files[ 0 ]._source.score
                        };

                    } );

                    /* sort by descending usage/occurrence with weighted score */
                    result = _.sortBy( intermediateResult, function( elem ) {
                        var sortScore = ( elem.score * 10000 ) + elem.lines.length;
                        return -sortScore;
                    } );

                    currentResult = result;
                    return {
                        classes: _.unique( matchingImports ),
                        result: result
                    };
                }

                function getFileName( filePath ) {
                    var elements = filePath.split( '/' ),
                        repoName = elements[ 0 ] + '-' + elements[ 1 ],
                        fileName = elements[ elements.length - 1 ];
                    return {
                        'repo': repoName,
                        'file': fileName
                    };
                }

                function addMethodDoc( classWiseMethods ) {
                    classWiseMethods.forEach( function( entry ) {
                        var url = entry.url;

                        if ( url.search( 'java' ) === 0 ) {
                            http.get( docsBaseUrl + url ) //'services/file.html'
                                .then( function( result ) {
                                    entry.methods.forEach( function( methodInfo ) {

                                        //console.log('-----------methodInfo----------');
                                        //console.log(methodInfo);
                                        var methodDoc = '',
                                            linkToMethod = '',
                                            matchedResult = result.match( methodInfo.regex );

                                        //temporary hack to avoid error for inherited methods
                                        if ( matchedResult && matchedResult.length > 1 ) {
                                            linkToMethod = matchedResult[ 1 ].split( '#' )[ 1 ].replace( /[\(\)]/g, '\\$&' ).replace( /%20/g, ' ' );

                                            //regex to capture the content from the anchor for the method. Fetches anchor to li end.
                                            var contentRegex = new RegExp( ( /<a\sname=\'/ ).source + linkToMethod + ( /\'>.*?<\/a><ul((?!<\/li>).)*/ ).source );

                                            methodDoc = result.replace( /\n/g, '' ).match( contentRegex );
                                            methodDoc = methodDoc[ 0 ].substring( methodDoc[ 0 ].search( '<h4' ) );
                                            methodDoc = methodDoc.replace( /\s\s+/g, '' ).replace( new RegExp( '../../../', 'g' ), docsBaseUrl );
                                            methodDoc = methodDoc.replace( /<a/g, '<a target="_blank"' );

                                        } else {
                                            methodDoc = 'Sorry!! This could be an inherited method. Please see the complete documentation.';
                                        }
                                        //console.log( '----------methodDoc---------' );
                                        //console.log( methodDoc );
                                        methodInfo.methodDoc = methodDoc;
                                    } );
                                }, function( error ) {
                                    //console.log( 'error' + error );
                                } );
                        }
                    } );
                }

                function displayCommonMethods() {
                    //fileTab.removeClass('active');
                    //resultTreeContainer.hide();
                    //methodTab.addClass('active');
                    //methodsContainer.html('');

                    var groupedMethods = _.map( _.groupBy( commonMethods, 'className' ), function( matches, className ) {
                        return {
                            className: className,
                            methods: matches,
                            url: matches[ 0 ].url
                        };
                    } );
                    //console.log('------------------');
                    //console.log( groupedMethods );
                    model.groupedMethods = groupedMethods;
                    //scope.$apply()

                    //methodsContainer.html(methodsContainerTemplate({'groupedMethods': groupedMethods}));
                    //calling addMethodDoc on grouped Methods to reduce number of requests for toolTip (making it faster)

                    //console.log( groupedMethods );
                    addMethodDoc( groupedMethods );
                }


                function filterRelevantTokens( searchString, tokens ) {
                    var result = searchString.split( ',' ).map( function( term ) {

                        var matchingTokens = [],
                            correctedTerm = term.trim().replace( /\*/g, '.*' ).replace( /\?/g, '.{1}' );

                        matchingTokens = tokens.filter( function( tk ) {
                            return ( tk.importName ).search( correctedTerm ) >= 0;
                        } );

                        return matchingTokens;
                    } );

                    return _.flatten( result );
                }

                function addFileToView( file ) {
                    var index = _.findIndex( currentResult, {
                        name: file.name
                    } );
                    //model.editors = [];
                    model.expandedItem = false;
                    model.editors.splice( index, 1 );
                    //$('#result' + index).remove();
                    //$('#results').prepend(resultTemplate({'files': files}).replace(/result0/g, 'result' + index));

                    renderFileContent( file, 0 );
                    //rightSideContainer.scrollTop(0);
                }

                function searchCommonUsage( className ) {

                    var query = {
                        'query': {
                            'filtered': {
                                'query': {
                                    'bool': {
                                        'must': [ {
                                            'term': {
                                                'body': className
                                            }
                                        } ]
                                    }
                                },
                                'filter': {
                                    'and': {
                                        'filters': [ {
                                            'term': {
                                                'length': '1'
                                            }
                                        } ],
                                        '_cache': true
                                    }
                                }
                            }
                        },
                        'sort': [ {
                            'freq': {
                                'order': 'desc'
                            }
                        }, '_score' ]
                    };

                    queryES( 'fpgrowth/patterns', query, 10, function( result ) {
                        result.forEach( function( entry ) {
                            var src = entry._source,
                                methodName, methodRegex,
                                location = src.body[ 0 ].search( className ),
                                pageUrl = className.replace( /\./g, '/' ) + '.html';

                            if ( location > -1 ) {
                                methodName = src.body[ 0 ].substr( className.length + 1 ); //taking length+1 so that '.' is excluded
                                methodRegex = '(' + pageUrl + '#' + methodName + '\\([\\w\\.\\d,%]*\\))';
                                commonMethods.push( {
                                    className: className,
                                    method: methodName,
                                    freq: src.freq,
                                    id: className.replace( /\./g, '' ) + '-' + methodName,
                                    url: pageUrl,
                                    regex: new RegExp( /\'.*/.source + methodRegex + /\'/.source )
                                } );
                            }
                        } );

                        displayCommonMethods();
                    } );
                }






                return {
                    search: search,
                    queryES: queryES,
                    addFileToView: addFileToView
                };
            }
        ] )

    ;

    angular.bootstrap( document, [ 'KodeBeagle' ] );

} )( angular, window, ace );

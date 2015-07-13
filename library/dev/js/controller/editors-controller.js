(function( module ) {
	var Range = ace.require('ace/range').Range;
	module.controller('editorCtroller', [
      '$scope',
      'model',
      'docsService',
      '$location',
      function($scope, model, docsService,$location) {

        $scope.model = model;
        $scope.tabs = [{
          active: false,
          heading: 'Files'
        }, {
          active: true,
          heading: 'Methods'
        }];
        $scope.toggleExpand = function expandAllBlocks(e, index, item) {
          e.preventDefault();
          e.stopPropagation();
          var currentTarget= e.currentTarget;
          setTimeout( function(){ 
            $(window).scrollTop($(currentTarget).offset().top - 10); 
          } );
          
          item.collapsed = !item.collapsed;
          
          var lineNumbers = angular.copy(item.fileInfo.lines);
          var editor = ace.edit('editor-' + index + '-repeat-ace');
          if (item.collapsed) {
            editor.getSession().unfold();
            editor.getSession().foldAll();
            lineNumbers.forEach(function(n) {
              editor.getSession().unfold(n.lineNumber);
            });
            editor.gotoLine(item.fileInfo.lines[0].lineNumber, 0, true);
             
            item.showBody = true;
            

          } else {
            $scope.hideLeftPanel = false;
            model.expandedItem = false;
            if( !model.toggelSnippet ) {
              item.showBody = false;  
            }
            
            
          }

        };
        $scope.loadAllFiles = function( e, count ) {
          e.preventDefault();
          //docsService.search(model.selectedTexts.join( ',' ), model, count);  
        }
        
        $scope.fileEleClick = function(e, item) {
          //console.log( item );
          e.preventDefault();
          docsService.addFileToView(item);
        };

       
        $scope.addToSearch = function(e, item, className) {
          e.preventDefault();
          /*model.selectedTexts = [];
          model.selectedTexts.push('pkg:' + className );
          model.selectedTexts.push('method:' + item.method ); */
          //model.selectedTexts.push(item.method);
        };
        
        $scope.toggleCodeSnippets = function( e ) {
          e.preventDefault();

          model.toggelSnippet = !model.toggelSnippet;
          for(var i=0;i<model.editors.length;i++) {
            model.editors[i].showBody = model.toggelSnippet;
            model.editors[i].collapsed = false;
          }
        }
        $scope.fullExpandView = function(e, item, index) {
          e.preventDefault();
          var lineNumbers = angular.copy(item.fileInfo.lines);
          var editor = ace.edit('editor-' + index + '-repeat-ace');
          var currentTarget= e.currentTarget;
          setTimeout( function(){ 
            $(window).scrollTop($(currentTarget).offset().top -10 ); 
          } );
          $scope.hideLeftPanel = !$scope.hideLeftPanel;
          if (!$scope.hideLeftPanel) {
            model.expandedItem = false;
            foldLines(editor, lineNumbers);
            item.collapsed = false;
          } else {
            model.expandedItem = item;
            item.collapsed = true;
            editor.getSession().unfold();
            editor.getSession().foldAll();
            lineNumbers.forEach(function(n) {
              editor.getSession().unfold(n.lineNumber);
            });
            item.showBody = true;
            editor.gotoLine(item.fileInfo.lines[0].lineNumber, 0, true);
          }

        };
        $scope.mode = 'java';

        function highlightLine(editor, lineNumbers) {
          //console.log( 'lineNumbers', lineNumbers );
          /*IMPORTANT NOTE: Range takes row number starting from 0*/
          lineNumbers.forEach(function(line) {
            var row = line.lineNumber - 1,
              endCol = editor.session.getLine(row).length,
              range = new Range(row, line.startColumn-1, row, line.endColumn);
            editor.getSession().addMarker(range, 'ace_selection', 'background');
          });
        }

        function getLine2(n1, n2, totalLength, start, end, editor) {
          var absoluteEnd = n1 + end + 1;
          var range;
          if (n2) {
            if (absoluteEnd < n2 - start) {
              /**
               * if nextlineNumber is present then
               * check the absoluteEnd is less then nextlineNumber - startingOffset
               * then return the absoluteEnd
               * and foldlines from absoluteEnd + 1 to nextlineNumber - startingOffset - 1
               */
              if (n2 - start - 1 - absoluteEnd > 2) {
                range = new Range(absoluteEnd, 0, n2 - start - 1, 0);
                //console.log( 'absoluteEnd + 1,,, n2 - start - 1:' + ( absoluteEnd + 1 ) + '-' + ( n2 - start - 1 ) )
                editor.getSession().addFold('--', range);
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
            if (absoluteEnd < totalLength) {
              /**
               * if nextlineNumber is not present
               * then check the absoluteEnd is less then totalLength
               * then return the absoluteEnd
               * and foldlines from absoluteEnd + 1 to totalLength
               */
              if (totalLength - (absoluteEnd + 1) > 2) {
                range = new Range(absoluteEnd, 0, totalLength, 0);
                editor.getSession().addFold('--', range);
                //console.log( 'absoluteEnd totalLength: ' + absoluteEnd + '-' + totalLength );
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

        function foldLines(editor, lineNumbers) {
          //console.log( lineNumbers );
          var nextLine = 0;
          var totalLength = editor.getSession().getLength();
          var leftOffset = 1;
          var rightOffset = 1;
          var l1;
          var l2;
          var range;
          //console.log( lineNumbers );
          //to hide lines
          if( lineNumbers[0] - leftOffset - 1 > 2 ){
            range = new Range(0, 0, lineNumbers[0] - leftOffset - 1, 0);
            editor.getSession().addFold('--', range);  
          }
          

          
          //console.log( '0 - lineNumbers[ 0 ] - leftOffset - 1 :' + 0 + '-' + ( lineNumbers[ 0 ] - leftOffset - 1 ) );
          for (var i = 0; i < lineNumbers.length; i++) {
            l1 = lineNumbers[i] - leftOffset;
            l2 = getLine2(lineNumbers[i], lineNumbers[i + 1], totalLength, leftOffset, rightOffset, editor);
            if (l2 - l1 > 2) {
              range = new Range(l1, 0, l2, 0);
            }
          }
        }
        $scope.getAceOptions = function(item) {
          var lineNumbers = item.fileInfo.lines;
          return {
            mode: $scope.mode.toLowerCase(),
            theme: model.config.selectedTheme === 'theme-light' ? 'eclipse' : 'monokai',
            markers: [ item.fileInfo.lines[0].lineNumber ],
            onLoad: function(_ace) {
              _ace.setReadOnly(true);
              _ace.setValue(item.content);
              _ace.getSession().setUseWrapMode(true);
              _ace.setDisplayIndentGuides(false);
              // HACK to have the ace instance in the scope...
              _ace.getSession().setMode('ace/mode/' + $scope.mode.toLowerCase(), function() {
                highlightLine(_ace, lineNumbers);
                foldLines(_ace, lineNumbers);
              });
            }
          };
        };

        $scope.refineSearch = function( e ) {
          e.preventDefault();
          var query = [];





          var must = [];
          var innerMust;
          var pkgs = model.packages;
          var currectedQuery='';
          for(var p in pkgs ) {
            innerMust = [];
            innerQuery = [];

            for(var m in pkgs[ p ].methods ) {
              if( pkgs[p].methods[m] ) {
                innerMust.push( {
                  'term' : {
                    'tokens.methodAndLineNumbers.methodName': m
                  }
                } );
                currectedQuery += ',' + m;
                innerQuery.push( m );
              }
            }
            if( innerMust.length || pkgs[p].status ) {
              innerMust.push( {
                'term' : {
                  'tokens.importName': p.toLowerCase()
                }
              } );
              currectedQuery += ',' + p;
              if( innerQuery.length ) {
                query.push( 'pkg:' + p + '->method:' +innerQuery.join( '&' ) );    
              } else {
                query.push( 'pkg:' + p );  
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
          }


          if( must.length ) {
            /*docsService.search( {
              query:{
                bool:{
                  must: must
                }
              },
              currectedQuery:currectedQuery
            }, model );*/
            $location.search( { 
              'query' : query.join( ',' ) 
            } );

          }
        };
      }
    ]);

})( KB.module )
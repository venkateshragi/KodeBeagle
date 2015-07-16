(function( module ) {

	module.factory('docsService', [
      'model',
      'http',
      function(
        model,
        http
      ) {
        var processedData = {}
          commonMethods = [],
          currentResult = [],
          docsBaseUrl = 'http://labs.imaginea.com/java7docs/api/';

        function buildSearchString(str) {
          var result = '';
          if (str[0] === '\'') {
            result = str.substr(1, str.length - 2);
          } else {
            result = str.split(',').map(function(entry) {
              return '*' + entry.trim();
            }).join(',');
          }
          return result;
        }

        function search(queryString, m, count) {
          //scope = s;
          model.showPageResponse = false;
          var correctedQuery,
            queryBlock;
          if( typeof queryString === 'object' ) {
            queryBlock = queryString.query;
            correctedQuery = queryString.currectedQuery;
          } else {
            correctedQuery = buildSearchString(queryString);
            queryBlock = getQuery(correctedQuery);
          }



          queryES('importsmethods', {
            'query': queryBlock,
            'sort': [{
              'score': {
                'order': 'desc'
              }
            }]
          }, count || model.config.resultSize, function(result) {
            model.showPageResponse = true;
            updateView(correctedQuery, result);
          });
        }

        function getQuery(queryString) {
          var terms = queryString.split( ',' ),
            mustTerms = terms.map( function( queryTerm ) {
                var prefix = ( queryTerm.search( /\*/ ) >= 0 || queryTerm.search( /\?/ ) >= 0 ) ? 'wildcard' : 'term';
                var result = {};
                result[ prefix ] = {
                    'typeimportsmethods.tokens.importName': queryTerm.trim().toLowerCase()
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

        function queryES(indexName, queryBody, resultSize, successCallback) {
          var url = model.config.esURL + '/' + indexName + '/_search?size=' + (resultSize || model.config.resultSize) + '&source=' + JSON.stringify(queryBody);
          http.get(url)
            .then(function(result) {
              if( indexName === 'importsmethods' ) {
                model.totalHitCount = result.hits.total;
                model.hitCount = result.hits.hits.length;
              }
              successCallback(result.hits.hits);
              model.showErrorMsg = false;
            }, function(error, s) {
              model.errorMsg = error && error.message;
              model.showErrorMsg = true;
              //errorMsgContainer.text(err.message);
            });
        }

        function updateView(searchString, data) {
          processedData = processResult(searchString, data);
          //analyzedProjContainer.hide();
          //searchMetaContainer.show();
          //
          //console.log( processedData.result );
          model.emptyResponse = false;
          commonMethods = [];
          model.groupedMethods = [];
          model.editors = [];
          model.totalFiles = processedData.result.length;
          updateLeftPanel(processedData.result);
          updateResultEditors();

          for (var cName in processedData.classes) {
            //console.log( cName );
            processedData.classes[cName] = _.unique(processedData.classes[cName]);
            searchCommonUsage(cName, processedData.classes[cName]);
          };
          displayCommonMethods();
          if (processedData.result.length === 0) {
            model.emptyResponse = true;
          }

        }

        function updateLeftPanel(processedData) {
          var projects = [],
            groupedByRepos = _.groupBy(processedData, function(entry) {
              return entry.repo;
            });
          //console.log( groupedByRepos );
          projects = _.map(groupedByRepos, function(files, label) {
            return {
              name: label,
              files: _.unique(files, _.iteratee('name'))
            };
          });
          model.projects = projects;
        }

        function renderFileContent(files) {
          var content;
          function nth_occurrence (string, char, nth) {
            var first_index = string.indexOf(char);
            var length_up_to_first_index = first_index + 1;

            if (nth == 1) {
                return first_index;
            } else {
                var string_after_first_occurrence = string.slice(length_up_to_first_index);
                var next_occurrence = nth_occurrence(string_after_first_occurrence, char, nth - 1);

                if (next_occurrence === -1) {
                    return -1;
                } else {
                    return length_up_to_first_index + next_occurrence;
                }
            }
        }
          queryES('sourcefile', fetchFileQuery(files), 50, function(result) {

            files = files.sort( function( a, b) {
              return b.lines.length - a.lines.length;
            } );

            model.editors = _.map( files, function( file, index ) {
              for( var i = 0; i < result.length ; i++ ) {
                if( file.path === result[i]._source.fileName ) {
                  return {
                    content: result[i]._source.fileContent,
                    fileInfo: file,
                    linesData: (function() {
                      var obj = [];
                      var str = '';
                      var l1;
                      var l2;
                      var count = 0;
                      for( var k=0; count < 4 && file.lines[k]; k++  ) {
                        var x = {};
                        if( l2 > file.lines[k].lineNumber ) {
                          continue;
                        }
                        count++;
                        if( k == 0 ) {
                          l1 = file.lines[k].lineNumber - 3;
                        } else {
                          l1 = file.lines[k].lineNumber - 2;
                        }

                        if( k==3 || file.lines.length === k + 1 ) {
                          l2 = file.lines[k].lineNumber + 2;
                        } else {
                          l2 = file.lines[k].lineNumber + 1;
                        }



                        var i1 = nth_occurrence( result[i]._source.fileContent, '\n', l1 );

                        var i2;
                        for( ; l1 < l2 ; l1++ ) {
                          i2 = result[i]._source.fileContent.indexOf( '\n', i1+1 );
                          if( i2 !=-1 ) {
                            x[ l1 + 1 ] = result[i]._source.fileContent.substring( i1, i2 );
                            i1 = i2;
                          }

                        }
                        x.active = file.lines[k].lineNumber;
                        obj.push( x );

                      }

                      return obj;
                    })()
                  }
                }
              }
            } );

            setTimeout( function() {
              if( !localStorage.getItem( 'intro' ) ){
                //introJs().start();
                localStorage.setItem( 'intro', true );
              }
            }, 1000 );
          });
        }

        function fetchFileQuery(files) {
          var arr=[];
          for( var i=0; i < files.length ; i++ ) {
            arr.push( {
              'term' : {
                'typesourcefile.fileName': files[i].path
              }
            } )
          }

          return {
            'query': {
              'bool': {
                'should' : arr
              }
            }
          };
        }

        function updateResultEditors() {

          var files = processedData.result && processedData.result.slice( model.currentPageNo * model.pageResultSize, model.currentPageNo * model.pageResultSize + model.pageResultSize );
          model.expandedItem = false;
          model.editors = [];
          renderFileContent(files);
        }

        function processResult(searchString, data) {
          var result = [],
            intermediateResult = [],
            groupedData = [],
            matchingImports = {};
          groupedData = _.groupBy(data, function(entry) {
            return entry._source.file;
          });
          var terms = searchString.split( ',' );
          var methodName;
          for( var i=0;i<terms.length;i++) {
            if( terms[i].indexOf( 'method:' ) !== -1  ) {
              methodName = terms[ i ].substring( 8 );
              terms.splice( i, 1 );
              break;
            }
          }

          intermediateResult = _.map(groupedData, function(files, fileName) {
            //console.log( groupedData );
            var labels = getFileName(fileName),
              lineNumbers = [];
            files.forEach(function(f) {
              var matchingTokens = filterRelevantTokens(searchString.toLowerCase(), f._source.tokens),
                possibleLines = _.pluck(matchingTokens, 'lineNumbers');

                /*if method name present then heilight the method name portion by picking from method.linenumbers*/
                if( methodName ) {

                  matchingTokens.map( function( x ) {
                    if( x.methodAndLineNumbers ) {
                      x.methodAndLineNumbers.map( function(method) {
                        if( method.methodName === methodName ) {
                          possibleLines[0] = possibleLines[0].concat(method.lineNumbers);
                        }
                      } )
                    }
                  } );
                }

              matchingTokens.map(function(x) {
                matchingImports[x.importExactName] = matchingImports[x.importExactName] || [];
                matchingImports[x.importExactName] = matchingImports[x.importExactName].concat(x.methodAndLineNumbers.map(function(m) {
                  return m.methodName
                }));
              });
              lineNumbers = lineNumbers.concat(possibleLines);
            });
            lineNumbers = (_.unique(_.flatten(lineNumbers))).sort(function(a, b) {
              return a.lineNumber - b.lineNumber;
            });

            return {
              path: fileName,
              repo: labels.repo,
              name: labels.file,
              lines: lineNumbers,
              score: files[0]._source.score
            };
          });
          /* sort by descending usage/occurrence with weighted score */
          result = _.sortBy(intermediateResult, function(elem) {
            var sortScore = (elem.score * 10000) + elem.lines.length;
            return -sortScore;
          });
          currentResult = result;
          return {
            classes: matchingImports,
            result: result
          };
        }

        function getFileName(filePath) {
          var elements = filePath.split('/'),
            repoName = elements[0] + '-' + elements[1],
            fileName = elements[elements.length - 1];
          return {
            'repo': repoName,
            'file': fileName
          };
        }


        function displayCommonMethods() {
          var groupedMethods = _.map(_.groupBy(commonMethods, 'className'), function(matches, className) {
            return {
              className: className,
              methods: matches,
              url: matches[0].url
            };
          });
          model.groupedMethods = groupedMethods;
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

        function addFileToView(file) {
          var index = _.findIndex(model.editors, {
            fileInfo: file
          });
          if (index === 0) {
            return;
          }
          if (index !== -1) {
            model.editors.splice(index, 1);
          } else {
            /*
             * if new file not present in the model.editor check the length of the
             * model.editors and remove the last item if model.editors count is 10
             */
            if (model.editors.length === 10) {
              model.editors.splice(9, 1);
            }
          }
          model.expandedItem = false;
          renderFileContent(file, 0);
        }

        function searchCommonUsage(className, methods) {
          var methodRegex,
          pageUrl = className.replace(/\./g, '/') + '.html';
          methods.forEach(function(methodName) {
            methodRegex = '(' + pageUrl + '#' + methodName + '\\([\\w\\.\\d,%]*\\))';
            commonMethods.push({
              className: className,
              method: methodName,
              //freq: src.freq,
              id: className.replace(/\./g, '') + '-' + methodName,
              url: pageUrl,
              regex: new RegExp(/\'.*/.source + methodRegex + /\'/.source)
            });
          });
        }
        return {
          search: search,
          queryES: queryES,
          addFileToView: addFileToView,
          updateResultEditors: updateResultEditors
        };
      }
    ]);

})( KB.module )

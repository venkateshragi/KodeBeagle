$(document).bind( 'keydown', function( e ) {
  if( e.ctrlKey || e.shiftKey ) {
    return;
  }
  if( e.target.nodeName.toLowerCase() === 'input' || e.target.nodeName.toLowerCase() === 'textarea' ) {
    return;
  }
  var keyCode = e.which;
  var key = String.fromCharCode( keyCode );
  if( key.search( /[a-zA-Z]/i ) !== -1 ) {
      if( $('#searchText' ).is( ':visible' ) ) {
        $('#searchText' ).focus();
      } else if( $( '#searchCode' ).is( ':visible' ) ) {
        $('#searchCode' ).focus();
      }
      window.scrollTo(0, 0);
  }
} );

(function( module ) {
	module.directive('searchComponent', function() {
    return {
      controller: 'searchController',
      templateUrl: 'search-component.html'
    };
  })
  .controller('searchController', [
    '$scope',
    '$rootScope',
    'model',
    '$location',
    function(
      $scope,
      $rootScope,
      model,
      $location
    ) {

      $scope.model = model;
      model.selectedTexts = model.selectedTexts || [];
      $scope.handleSelectedText = function(e, i) {
        
        e.preventDefault();
        var spliceData = model.selectedTexts.splice( i, 1 );
        if (model.searchText) {
          model.selectedTexts.push(model.searchText);
        }
        model.searchText = spliceData[0];
        
      };
      
      $scope.deleteItem = function(e, i) {
        
        e.preventDefault();
        model.selectedTexts.splice(i, 1);
      
      };
      
      $scope.formSubmit = function(forceSubmit) {
        
        //$scope.showRequiredMsg = false;
        if( !model.isCode) {
          if ( model.searchText ) {
            model.selectedTexts.push(model.searchText);
            model.searchText = '';
          }

          var searchTerm = model.selectedTexts.join(',');
          
          if ( searchTerm ) {
            if( model.searchPage ) {
              $location.search({
                'searchTerms': searchTerm
              });
              $rootScope.editorView = true;  
            } else {
              window.location = 'search/#?searchTerms=' + searchTerm;
            }
            

          }  
        } else {

          KB.parseCodeSnippet( {url:basepath + 'library/data/java_lang_pkg.json',textSelected:model.searchCode}, function  ( searchTerm ) {
            if( model.searchPage ) { 
              $location.search({
                'searchTerms': searchTerm
              });
              model.searchCode = ''; 
            } else {
              window.location = 'search/#?searchTerms=' + searchTerm;
            }
            
          } )
          
        }
         
      };

      $scope.clearAll = function(e) {
        
        e.preventDefault();
        model.selectedTexts = [];
        model.searchText = '';
        model.searchCode = '';
      };


      function doGetCaretPosition(ctrl) {
        
        var CaretPos = 0; // IE Support
        if ( document.selection ) {
          ctrl.focus();
          var Sel = document.selection.createRange();
          Sel.moveStart('character', -ctrl.value.length);
          CaretPos = Sel.text.length;
        }
        // Firefox support
        else if (ctrl.selectionStart || ctrl.selectionStart === '0')
          CaretPos = ctrl.selectionStart;
        return (CaretPos);

      }

      $scope.handleSearchText = function(e) {
        
        /*on press of tab( 9 keycode ) and if model.searchText present the create a new search term*/
        if ( e.keyCode === 9 && model.searchText ) {

            model.selectedTexts.push(model.searchText);
            model.searchText = '';
            e.preventDefault();
            return false;
        }

        /*on press of space( 32 keycode ) or comma( 188 keycode ) then create the new search term with model.searchText */
        if ( e.keyCode === 32 || e.keyCode === 188 ) {
          if (model.searchText) {
            model.selectedTexts.push(model.searchText);
            model.searchText = '';
          }
          e.preventDefault();
          return false;
        }

        /*on press backspace and dont have any text in the textbox*/
        if ( doGetCaretPosition(e.target) === 0 && e.keyCode === 8 ) {
          model.selectedTexts.pop();
          return;
        }
      };
    }
  ])
} )( KB.module )
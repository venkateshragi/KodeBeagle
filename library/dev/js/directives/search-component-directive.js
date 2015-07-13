(function( module ) {
	module.directive('searchComponent', function() {
      return {
        controller: 'searchController',
        templateUrl: 'search-component.html'
      };
    })
    .controller('searchController', [
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
        $scope.handleSelectedText = function(e, i) {
          e.preventDefault();
          //var target = e.target;
          var spliceData = model.selectedTexts.splice(i, 1);
          if (model.searchText) {
            model.selectedTexts.push(model.searchText);
          }
          model.searchText = spliceData[0];
          //document.getElementById('searchText').focus();
        };
        $scope.deleteItem = function(e, i) {
          e.preventDefault();
          model.selectedTexts.splice(i, 1);
        };
        $scope.handleClick = function(item) {
          model.selectedTexts.push(item.suggested);
          //$scope.searchText = item.suggested;
        };
        $scope.formSubmit = function(forceSubmit) {
          $scope.showRequiredMsg = false;
          if (model.searchText) {
            model.selectedTexts.push(model.searchText);
            model.searchText = '';
          }
          var searchTerm = model.selectedTexts.join(',');
          
          if (searchTerm) {
            if( searchTerm.indexOf( 'pkg:') !== -1 ) {
              $location.search({
                'query': searchTerm
              });
            } else {
              $location.search({
                'searchTerms': searchTerm
              });
               
            }
            
            $rootScope.editorView = true;
          } 
        };
        $scope.clearAll = function(e) {
          e.preventDefault();
          model.selectedTexts = [];
          model.searchText = '';
          //document.getElementById('searchText').focus();
        };
        $scope.selectSuggestion = function(e) {
          e.preventDefault();
        };

        function doGetCaretPosition(ctrl) {
          var CaretPos = 0; // IE Support
          if (document.selection) {
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
          if (e.keyCode === 9) {
            if (model.searchText) {
              model.selectedTexts.push(model.searchText);
              model.searchText = '';
              //$scope.checkFormSubmit = true;
              e.preventDefault();
              return false;
            }
          }
          if (e.keyCode === 188) {
            if (model.searchText) {
              model.selectedTexts.push(model.searchText);
              model.searchText = '';
              //$scope.checkFormSubmit = true;
            }
            e.preventDefault();
            return false;
          }
          if (e.keyCode === 32 || e.keyCode === 44) {
            if (model.searchText) {
              model.selectedTexts.push(model.searchText);
              model.searchText = '';
              //$scope.checkFormSubmit = true;
            }
            e.preventDefault();
            return false;
          }
          if (doGetCaretPosition(e.target) === 0 && e.keyCode === 8) {
            //$scope.selectedTexts[ $scope.selectedTexts.length - 1 ].tobe
            model.selectedTexts.pop();
            return;
          }
        };
      }
    ])
} )( KB.module )
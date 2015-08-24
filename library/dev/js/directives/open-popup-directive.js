(function( module ) {
  module.directive('openPopup', [
    '$modal',
    'docsService',
    'model',
    function($modal, docsService, model) {
      return {
        restrict: 'A',
        scope: {
          template: '@',
          ctrl: '@',
          id: '@',
          size: '@'
        },
        link: function(scope, element) {
          function openModal(data) {
            var modalInstance = $modal.open({
              templateUrl: scope.template,
              controller: scope.ctrl,
              size: scope.size
            });
            modalInstance.result.then(function() {
              localStorage.setObject('config', model.config);
            }, function() {
              model.config = angular.copy(scope.config);
            });
          }
          element.on('click', function(e) {
            e.preventDefault();
            scope.config = angular.copy(model.config);
            openModal();
          });
        }
      };
    }
  ] )
  .directive( 'codeSnippEle', function() {
    return {
      link : function(scope,ele) {
        if( scope.snipp.state ) {
          scope.snipp.ele = ele;  
        }
      }
    }
  } )
  
} )( KB.module )
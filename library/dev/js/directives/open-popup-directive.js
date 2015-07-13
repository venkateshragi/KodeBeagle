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
              size: scope.size,
              resolve: {
                data: function() {
                  return {
                    projects: data
                  };
                }
              }
            });
            if (scope.id === 'config') {
              modalInstance.result.then(function() {
                localStorage.setObject('config', model.config);
              }, function() {
                model.config = angular.copy(scope.config);
              });
            }
          }
          element.on('click', function(e) {
            e.preventDefault();
            if (scope.id === 'projects') {
              var projectlist = function(data) {
                scope.data = data;
                openModal(scope.data);
              };
              if (!scope.data) {
                docsService.queryES('repository', {
                  'query': {
                    'match_all': {}
                  },
                  'sort': [{
                    'stargazersCount': {
                      'order': 'desc'
                    }
                  }]
                }, 750, projectlist);
              } else {
                projectlist(scope.data);
              }
            } else {
              scope.config = angular.copy(model.config);
              openModal();
            }
          });
        }
      };
    }
  ])
  
} )( KB.module )
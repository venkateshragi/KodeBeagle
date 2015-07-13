(function( module ) {
	
	module.controller('userSettingsCtrl', [
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
        $scope.dismiss = function(e) {
          e.preventDefault();
          $modalInstance.dismiss('cancel');
        };
        $scope.ok = function(e) {
          e.preventDefault();
          $modalInstance.close();
        };
      }
    ]);

})( KB.module )
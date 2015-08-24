(function( module ) {
	
	module.controller('userSettingsCtrl', [
      '$scope',
      '$modalInstance',
      'model',
      function(
        $scope,
        $modalInstance,
        model
      ) {
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
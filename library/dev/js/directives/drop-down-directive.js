(function( module ) {
	module.directive('dropDown', function() {
    return {
        restrict:'E',
        templateUrl:'drop-down.html',
        scope:{
          selected:"=",
          values:"="
        },
        link:function(scope){
          scope.setValue = function(value){
            scope.selected = value;
            scope.isVisible = false;
          }
        }
      }
  })
} )( KB.module )
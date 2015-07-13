
(function( angular, kb ) {
  window.KB = kb; 
  kb.module = angular.module('KodeBeagle', ['httpSerivice', 'ui.bootstrap', 'ui.ace', 'ngAnimate', 'ng-code-mirror'])
  ;
})(angular, window.KB || {} );

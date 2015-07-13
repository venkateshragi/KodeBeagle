(function( module ) {
  module.directive('uiAce', [
    function() {
      return {
        restrict: 'A',
       
        link: function ($scope, element, attrs) {
            element.ready(function() {
              var editor = ace.edit( element[0] );
              var ele = angular.element( '<div class="ace-markers">' );
              var mark;

              for( var i = 0 ; i < $scope.item.fileInfo.lines.length ; i++ ) {
                mark = angular.element( '<span class="ace-mark">' );
                mark[0].style.top = ($scope.item.fileInfo.lines[i].lineNumber/editor.selection.doc.$lines.length)*100 + '%';
                ele.append( mark );
              }
              element.find( '.ace_scrollbar-v' ).append( ele );
              
            });
         }
      } 
    } ]
  )
  
} )( KB.module )
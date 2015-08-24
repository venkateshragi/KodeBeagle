(function( module ) {
	module.controller('editorCtroller', [
    '$scope',
    'model',
    'docsService',
    '$location',
    '$timeout',
    function($scope, model, docsService,$location,$timeout) {

      
      $scope.model = model;

      $scope.tabs = [{
        active: false,
        heading: 'Files'
      }, {
        active: true,
        heading: 'Methods'
      }];

      

      $scope.calcTop = function( snipp, line ) {
        var val = line - snipp.start;
        var totHeight = $(snipp.ele).closest( '.snippet-holder' ).height();
        var offset = $(snipp.ele)[0].offsetTop + snipp.ele.find( 'ol li:nth-child(' +val + ')'  )[0].offsetTop;
        return {
          top: (offset/totHeight)*100 + '%'
        }
      };

      $scope.goToTop = function(snipp, line) {
        var val = line - snipp.start;
        var scrollEle = $(snipp.ele).closest( '.snippet-wraper' );
        var offset = $(snipp.ele)[0].offsetTop + snipp.ele.find( 'ol li:nth-child(' +val + ')'  )[0].offsetTop;
        scrollEle.scrollTop( offset );
      }

      $scope.toggleCode =  function( snipp ) {
        snipp.show=!snipp.show;
        /*hack to adjust the markers position*/
        $timeout( function() { 
        }, 100);
      };

      $scope.toggleExpand = function(e, index, item) {
        e.preventDefault();
        e.stopPropagation();
        var currentTarget= e.currentTarget;
        setTimeout( function(){ 
          $(window).scrollTop($(currentTarget).offset().top - 10); 
        } );
        
        item.collapsed = !item.collapsed;

        item.showBody = item.collapsed;
        if( model.expndedView ) {
          item.showBody = true;
        }
        for( var i in item.linesData ) {
          item.linesData[i].show = item.collapsed;
        }
        /*hack to adjust the markers position*/
        $timeout( function() { 
        }, 100);
      };


      
      $scope.toggleCodeSnippets = function( e ) {
        e.preventDefault();
        model.toggelSnippet = !model.toggelSnippet;
        for(var i=0;i<model.editors.length;i++) {
          model.editors[i].showBody = model.toggelSnippet;
          model.editors[i].collapsed = false;
        }
      }

      $scope.fullExpandView = function(e, item, index) {
        
        e.preventDefault();
        var currentTarget= e.currentTarget;
        setTimeout( function(){ 
          $(window).scrollTop($(currentTarget).offset().top -10 ); 
        } );
        $scope.hideLeftPanel = !$scope.hideLeftPanel;
        if (!$scope.hideLeftPanel) {
          model.expandedItem = false;
          model.expndedView = false;
        } else {
          model.expandedItem = item;
          model.expandedItem.showBody = true; 
          model.expndedView = true;
        }
      };

      $scope.mode = 'java';
      
      $scope.updateFilter = function() {
          
          var search = $location.search();
          var pkgs = model.packages;
          var innerQuery;
          for(var p in pkgs ) {
            innerQuery = [];

            for(var m in pkgs[ p ].methods ) {
              if( pkgs[p].methods[m] ) {
                innerQuery.push( m );
              } else {
                delete pkgs[p].methods[m];
              }
            }
            if( !innerQuery.length && !pkgs[p].status ) {
              delete pkgs[ p ];
            } 
          }
          search.filter = JSON.stringify( model.packages );
          $location.search( search  );
      }
      
    }
  ]);

})( KB.module )
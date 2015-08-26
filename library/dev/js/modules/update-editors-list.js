
( function( kb ) {

  window.KB  = kb;

  kb.updateEditorsList = function  ( data, pkgs, model, docs ) {


    model.filterNotFound = true;
    model.expandedItem = false;
    model.editors = [];


    var result = docs.getFilteredFiles( data, pkgs );

    files = result && result.slice( 
      model.currentPageNo * model.pageResultSize,
      model.currentPageNo * model.pageResultSize + model.pageResultSize 
    );

    _.each( files, function ( x ) {
      docs.updatedLineNumbers( x, pkgs );
    } );

    if( files && files.length ) {
      model.filterNotFound = false;
      
      docs.renderFileContent( files, function ( response ) {

        for( var i=0;i < files.length; i++ ) {
          var index = _.findIndex( response.result, function( el ) {
            return el._source.fileName === files[i].path;
          } )
          model.editors.push( docs.splitFileContent( response.result[ index ], files[ i ], model.config.offset || 2 ) );
        }

      } );

    }
    model.totalFiles = result.length;

  }

} )( window.KB || {} );


( function( kb ) {
  
  window.KB = kb;
  var javaLangPkg;

  var processCode = function  ( textSelected, callback ) {
    
    var searchTextArray = []
    // Replace all special characters with spaces.
    // Any character other than A-Z or a-z or 0-9 will be replaced by a blank
    , clearedSelection = textSelected.replace(/(\"[^\"]*\")/g,'').replace(/[^a-zA-Z0-9_]/g, ' ').split(' ')
    //regex to select only camelcase words  
    , regexPattern = new RegExp("^[A-Z]([A-Za-z])+")
    ;
    clearedSelection = _.unique( clearedSelection );
    clearedSelection.forEach( function( keyword ) {
      if ( regexPattern.test( keyword ) && searchTextArray.indexOf( keyword ) === -1 && javaLangPkg[ keyword.toLowerCase() ] !== true ) {
        searchTextArray.push( keyword );
      }
    })

    if ( searchTextArray.length === 0 ) {
      alert( 'No keywords found in current selection. You may expand your selection, to include more lines.' );
      return;
    }

    //append ',' by iterating through each element of the array to make it a final search string
    //Eg: if we have array as FilterChannel and Filter then extract the array to a string variable 
    //as finalSearchStr = FilterChannel,Filter;
    var search = searchTextArray.reduce( function( acc, val ) {
      return acc + "," + val
    } );

    callback( search );

  }

  KB.parseCodeSnippet = function  ( obj, callback ) {
    
    if( javaLangPkg ) {
      processCode( obj.textSelected, callback );
    } else {
      $.get( obj.url || '../library/data/java_lang_pkg.json' )
      .done(function( res ) {
        javaLangPkg = res;
        processCode( obj.textSelected, callback );
      })
      .fail(function() {
        console.error( 'error' );
      } )  
    }
    
    
    


  }
  

} )( window.KB || {} );

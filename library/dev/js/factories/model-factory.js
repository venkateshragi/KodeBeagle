(function( module ) {

	module.factory('model', [ '$location', function( $location ) {


      	var data = {};

      	if( localStorage.getObject ) {

      		var esURL = 'http://labs.imaginea.com/kodebeagle';
		    data.showConfig = $location.search().advanced;
	      	data.pageResultSize=10;
	      	data.toggelSnippet = true;
	      	data.searchPage = true;

	      	data.config = localStorage.getObject('config') || {
			    selectedTheme: 'theme-light',
			    esURL: esURL,
			    resultSize: 50,
			    offset: 2
			};

			data.config.esURL = data.config.esURL || esURL;
			data.config.resultSize = data.config.resultSize || 50;

			if( typeof data.config.offset === 'undefined' ) {
			    data.config.offset = 2;
			}
      	}

        return data;
    } ] );

})( KB.module )

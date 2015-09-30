var gulp = require('gulp'), 
	crx = require('gulp-crx'),
	manifest = require('./src/manifest.json'),
	fs = require('fs');

gulp.task('create:crx', function(){
	return gulp.src('./src')
		.pipe(crx({
			privateKey: fs.readFileSync('./KodeBeagle.pem', 'utf8'),
			filename: manifest.name + '_plugin_' + manifest.version + '.crx',
			codebase: '.',
			updateXmlFilename: 'update.xml'
		}))
		.pipe(gulp.dest('build'));
});
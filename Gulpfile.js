var gulp = require('gulp');
var gutil = require('gulp-util');
var less = require('gulp-less');

/*less conversion*/
var sourcemaps = require('gulp-sourcemaps');
//gulp.task('less', function () {
  //return gulp.src('./library/less/**/index.less')
	//.pipe(sourcemaps.init())
    //.pipe(less())
	//.pipe( sourcemaps.write() )
    //.pipe(gulp.dest('./library/dev/css/'));
//});

var cssmin = require('gulp-cssmin');
gulp.task('cssmin', function () {
  return gulp.src('./library/less/**/index.less')
    .pipe(less())
    .pipe(cssmin())
    .pipe(gulp.dest('./library/dist/css/'));
});

/*var concat = require( 'gulp-concat' );
var uglify = require( 'gulp-uglify' );
var ngAnnotate = require('gulp-ng-annotate');
var gzip = require('gulp-gzip');
gulp.task('jsmin', function() {
    gulp.src( [	
		'./library/dev/js/vendors/ui-ace.js',
		'./library/dev/js/factories/http-service.js',
		'./library/dev/js/ng-modules/better-docs.js'
	] )
    .pipe(concat('script.js'))
    .pipe(ngAnnotate())
    .pipe(uglify())
    .pipe(gulp.dest('./library/dist/js/vendors/'));
});*/

/*
gulp.task( 'build', [ 'jsmin', 'cssmin' ], function(){
    gulp.src('./library/dist/js/vendors/script.js')
    .pipe(gzip({ gzipOptions: { level: 9 } }))
    .pipe(gulp.dest('./library/dist/js/vendors/'));
});*/

var prettify = require('gulp-jsbeautifier');
gulp.task('format-js', function() {
  gulp.src(['./library/dev/js/**/*.js'])
    .pipe(prettify({config: '.jsbeautifyrc', mode: 'VERIFY_AND_WRITE'}))
    .pipe(gulp.dest('./library/dev/js/'));
});

/*jshint comes here*/
var stylish = require('jshint-stylish');
var jshint = require('gulp-jshint');
var gulp   = require('gulp');
gulp.task('jshint', function() {
  return gulp.src(['./library/dev/js/ng-modules/demo.js'] )
    .pipe(jshint())
    .pipe(jshint.reporter(stylish));
});


/*minfy all the files in ace directory
gulp.task( 'build-ace', [ 'jsmin', 'cssmin', 'ace-min' ], function(){
    console.log( 'done' );
}) */

module.exports = function (grunt) {

	// 1. All configuration goes here
	grunt.initConfig({

		// 2. Configuration of tasks goes here.

		// Builds Sass
		sass: {													//Task: use plugin short name
			dist: {												//Target: call it what you like
				files: [{										//Dictionary of files
					expand: true,								//expand Set to true to enable the following options
					cwd: '../sass',								//All src are relative to this
					src: ['*.scss'],
					dest: '../../../public/assets/css/',
					ext:  '.css'
				}]
			}
		},
		watch: {
			css: {
				files: ['../sass/*.scss'],
				tasks: ['sass'],
				options: {
					spawn: false
				}
			}
		}

	});

	// 3. Where we tell Grunt what plugins we plan to use.
    // will read the dependencies/devDependencies/peerDependencies in your package.json
    // and load grunt tasks that match the provided patterns.
    // Loading the different plugins
    require('load-grunt-tasks')(grunt);

	// 4. Where we tell Grunt what to do when we type "grunt" into the terminal.
	grunt.registerTask('default', ['sass', 'watch']);
	grunt.registerTask('test', []);

};
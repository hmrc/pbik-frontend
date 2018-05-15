import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.Keys._
/**
 * Build of UI in JavaScript
 */
object JavaScriptBuild {

  val uiDirectory = SettingKey[File]("ui-directory")

  val gruntBuild = TaskKey[Int]("grunt-build")
  val gruntWatch = TaskKey[Int]("grunt-watch")
  // val gruntTest = TaskKey[Int]("grunt-test")
  val npmInstall = TaskKey[Int]("npm-install")


  val javaScriptUiSettings = Seq(

    // the JavaScript application resides in "ui"
    uiDirectory <<= (baseDirectory in Compile) { _ /"app" / "assets" / "javascripts"},

    // add "npm" and "grunt" commands in sbt
    commands <++= uiDirectory { base => Seq(Grunt.gruntCommand(base), npmCommand(base))},

    npmInstall := Grunt.npmProcess(uiDirectory.value, "install").run().exitValue(),
    gruntBuild := Grunt.gruntProcess(uiDirectory.value, "sass").run().exitValue(),

    gruntWatch := Grunt.gruntProcess(uiDirectory.value, "watch").run().exitValue(),
    // gruntTest := Grunt.gruntProcess(uiDirectory.value, "test").run().exitValue(),

    // gruntTest <<= gruntTest dependsOn npmInstall,
    gruntBuild <<= gruntBuild dependsOn npmInstall,

    // runs grunt before staging the application
    dist <<= dist dependsOn gruntBuild

    // (test in Test) <<= (test in Test) dependsOn gruntTest,

    // Turn off play's internal less compiler
    // lessEntryPoints := Nil,

    // Turn off play's internal JavaScript and CoffeeScript compiler
    // javascriptEntryPoints := Nil,
    // coffeescriptEntryPoints := Nil,

    // integrate JavaScript build into play build
    // playRunHooks <+= uiDirectory.map(ui => Grunt(ui))
  )

  def npmCommand(base: File) = Command.args("npm", "<npm-command>") { (state, args) =>
    Process("npm" :: args.toList, base) !;
    state
  }

}

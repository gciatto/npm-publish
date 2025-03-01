import task.MikeExec
import task.MkDocsExec
import java.io.FileFilter

plugins {
  id("plugin.base")
}

tasks {
  val src = projectDir.resolve("src")
  val mkdocsDir = buildDir.resolve("mkdocs")
  val outDir = mkdocsDir.resolve("site")
  val dokkaHtml = getByPath(":npm-publish-gradle-plugin:dokkaHtml")
  val docsAssemble = register("docsAssemble", Copy::class) {
    dependsOn(dokkaHtml)
    outputs.upToDateWhen { false }
    destinationDir = mkdocsDir.resolve("source").also(outputs::dir)
    val readme = rootDir.resolve("README.md").also(inputs::file)
    val license = rootDir.resolve("LICENSE").also(inputs::file)
    val changelog = rootDir.resolve("CHANGELOG.md").also(inputs::file)
    val srcDir = destinationDir.resolve("src").also(outputs::dir)
    val dokkaDir = srcDir.resolve("api").also(outputs::dir)
    val themeDir = destinationDir.resolve("theme").also(outputs::dir)
    into(srcDir.relativeTo(destinationDir)) {
      from(src.resolve("assets"))
      from(src.resolve("pages"))
      from(rootDir.resolve(".github/CONTRIBUTING.md"))
      from(readme) {
        rename("README.md", "index.md")
      }
      from(license) {
        rename("LICENSE", "LICENSE.md")
      }
      from(changelog)
    }
    into(themeDir.relativeTo(destinationDir)) {
      from(src.resolve("theme"))
    }
    into(dokkaDir.relativeTo(destinationDir)) {
      from(dokkaHtml.outputs)
    }
    into("config") {
      from(src.resolve("config"))
      include("**/*mkdocs.y*ml")

      expand(
        mapOf(
          "srcDir" to srcDir.absolutePath,
          "themeDir" to themeDir.absolutePath,
          "outDir" to outDir.absolutePath,
        )
      )
    }

    doLast {
      val configFile = destinationDir.resolve("mkdocs.yml").also(File::delete)
      destinationDir.resolve("config").run {
        listFiles(FileFilter { it.isFile })?.forEach {
          configFile.appendText(it.readText())
        }
        deleteRecursively()
      }
      srcDir.resolve("index.md").run {
        val content = readText().replace(".github/CONTRIBUTING.md", "CONTRIBUTING.md")
        writeText(content)
      }
    }

    inputs.dir(src)
  }
  register("mkdocsServe", MkDocsExec.Serve::class) {
    dependsOn(docsAssemble)
    workingDir.set(layout.dir(docsAssemble.map { it.destinationDir }))
  }
  register("mkdocsBuild", MkDocsExec.Build::class) {
    dependsOn(docsAssemble)
    workingDir.set(layout.dir(docsAssemble.map { it.destinationDir }))
    containerVolumes.put(outDir, outDir)
    outputs.dir(outDir)
  }
  register("mikeList", MikeExec.List::class) {
    dependsOn(docsAssemble)
    workingDir.set(layout.dir(docsAssemble.map { it.destinationDir }))
  }
  register("mikeSetDefault", MikeExec.SetDefault::class) {
    dependsOn(docsAssemble)
    workingDir.set(layout.dir(docsAssemble.map { it.destinationDir }))
  }
  val mikeBuild = register("mikeBuild", MikeExec.Deploy::class) {
    dependsOn(docsAssemble)
    workingDir.set(layout.dir(docsAssemble.map { it.destinationDir }))
    args.addAll(
      "--update-aliases",
      "${project.version}",
      "latest"
    )
  }
  register("mikeServe", MikeExec.Serve::class) {
    dependsOn(mikeBuild)
    workingDir.set(layout.dir(docsAssemble.map { it.destinationDir }))
  }
  register("mikeDeploy", MikeExec.Deploy::class) {
    dependsOn(docsAssemble)
    workingDir.set(layout.dir(docsAssemble.map { it.destinationDir }))
    args.addAll(
      "--push",
      "--update-aliases",
      "${project.version}".split(".").take(2).joinToString("."),
      "latest"
    )
  }
  register("mikeAlias", MikeExec.Alias::class) {
    dependsOn(docsAssemble)
    workingDir.set(layout.dir(docsAssemble.map { it.destinationDir }))
  }
  register("clean", Delete::class) {
    delete(buildDir)
  }
}

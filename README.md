[![Stories in Ready](https://badge.waffle.io/Imaginea/KodeBeagle.png?label=ready&title=Ready)](https://waffle.io/Imaginea/KodeBeagle)

[![Join the chat at https://gitter.im/Imaginea/KodeBeagle](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Imaginea/KodeBeagle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build](https://travis-ci.org/Imaginea/KodeBeagle.svg?branch=master)](https://travis-ci.org/Imaginea/KodeBeagle/builds)

We use [gh-pages](https://pages.github.com/) and Jekyll for demo web ui.

# KodeBeagle
KodeBeagle aims at solving common software engineering challenges developers face in day-to-day activity. With the help of modern day technologies like Apache Spark, processsing TBs of data has become fast. Currently, we have targeted the following areas-

1. **Code Search** - Our code search helps in providing contextual code examples of various API usages in Java, Scala and JavaScript (Only Node.js). We also have an Intellij IDEA plugin which helps developers search code from right within the IDE. Refer to our code search [here](http://kodebeagle.com)

2. **Navigation** - Currently, we are working on providing an IDE like code navigation on github for repositories. (Currently Java Repositories on github are supported)

## Usage
## Core
### Input Configuration
1. Input parameters are to be set in *src/main/resources/application.properties*
2. For the crawler jobs set `kodebeagle.github.crawlDir` to *path/to/dir/to/save/github-repos* and `kodebeagle.spark.githubTokens` to comma separated github tokens.
3. For the index jobs set `kodebeagle.github.crawlDir` to *path/to/dir/of/github-repos* and `kodebeagle.spark.index.outputDir` to *<path/to/save/indexes>* (dont create this directory. It will be created automatically)

### Run Configurations
1. Launch sbt launcher from project directory (make sure you have sbt installed).
2. In the sbt launcher, type `project core` and then `run`
3. To run the crawler for Java and Scala repositories - Enter number for **com.kodebeagle.crawler.GitHubRepoCrawlerApp**
4. To run the crawler for JavaScript repositories - Enter number for **com.kodebeagle.crawler.JavaScriptRepoDownloader**
3. To run the IndexJob for Java and Scala repositories - Enter number for **com.kodebeagle.spark.ConsolidatedIndexJob**
4. To run the IndexJob for JavaScript repositories - Enter number for  **com.kodebeagle.spark.IndexJobForJS**

### Upload Indices to ElasticSearch
Each of the index creation job generates indices which you can upload to your local elasticsearch by using the the script bin/upload_to_es.sh. Run this in your directory where the indices have been saved.

## Plugin

1) Open *.idea/modules/pluginImpl.iml*

2) Change toplevel '<module .* type ="JAVA_MODULE" />' change type to "PLUGIN_MODULE".

3) Open Project Structure and select pluginImpl module.

4) In plugin deployment tab Specify path to plugin.xml (NOTE: Click NO to cleanup META_INF dialogue box to prevent accidently deleting plugin.xml file, that way it will use existing plugin.xml file.)

5) In Dependencies tab specify intellij Idea SDK.

6) To run it, go to *Run > Edit Configurations > Select Plugin > Apply*

7) Then click run (little green triangle icon on the right side)

## Development Enviroment Setup
1. To develop KodeBeagle, you can use either IntelliJ IDEA Community Edition or IntelliJ IDEA Ultimate not older than 15.0.
2. Download the latest version of  Scala plugin from [here][scala-plugin]
[scala-plugin]: https://plugins.jetbrains.com/plugin/?id=1347
3. Extract the scala plugin archive and copy the scala-plugin.jar from Scala/lib/scala-plugin.jar to idea-IC-143.XXX.XX (Intellij IDE Folder).
4. Go to the IDE select *File > Settings > Plugins > Install plugin from disk > Navigate to the scala-plugin that was downloaded > Restart IDE* (Make sure the plugin is enabled)
5. Then navigate to *File > New > Project From Existing Source > Navigate to Project Directory >  Import Project From External Model > SBT > Select auto-import and go to Global Settings > VM params*. Type this `-Didea.lib=<path-to-idea-lib>`
 Eg. */home/user/idea-IC-143.XXX.XX/lib*
6. Click on finish, wait for sbt to resolve all the project dependencies.

## Contributing
1. Fork it!
2. For each feature or bug, create a new issue on the github repository.
3. Create your feature branch: `git checkout -b my-new-feature`
4. Commit your changes: `git commit -am '#[MODULE_NAME] <ISSUE_NUMBER> <MESSAGE> fixes #<ISSUE_NUMBER> closes #<PULL_REQUEST_NUMBER> '`.
5. Please look at other commit messages in the log for the appropriate commit message convention. Or else your build will fail.
6. Push to the branch: `git push origin my-new-feature`
7. Submit a pull request :D

## Code Of Conduct
This project adheres to the [Open Code of Conduct][code-of-conduct]. By participating, you are expected to honor this code.
[code-of-conduct]: http://todogroup.org/opencodeofconduct/#KodeBeagle/ococabuse@imaginea.com

## License
Please look at our [license][license].
[license]: https://github.com/Imaginea/KodeBeagle/blob/master/LICENSE

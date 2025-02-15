package renderers.html

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.javadoc.JavadocExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.testApi.context.MockContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import renderers.RenderingOnlyTestBase
import testApi.testRunner.defaultSourceSet
import utils.TestOutputWriter
import java.io.File

abstract class HtmlRenderingOnlyTestBase : RenderingOnlyTestBase<Element>() {

    protected val js = defaultSourceSet.copy(
        "JS",
        defaultSourceSet.sourceSetID.copy(sourceSetName = "js"),
        analysisPlatform = Platform.js,
        sourceRoots = setOf(File("pl1"))
    )
    protected val jvm = defaultSourceSet.copy(
        "JVM",
        defaultSourceSet.sourceSetID.copy(sourceSetName = "jvm"),

        analysisPlatform = Platform.jvm,
        sourceRoots = setOf(File("pl1"))
    )
    protected val native = defaultSourceSet.copy(
        "NATIVE",
        defaultSourceSet.sourceSetID.copy(sourceSetName = "native"),
        analysisPlatform = Platform.native,
        sourceRoots = setOf(File("pl1"))
    )

    val files = TestOutputWriter()

    open val configuration = DokkaConfigurationImpl(
        sourceSets = listOf(js, jvm, native),
        finalizeCoroutines = false
    )

    override val context = MockContext(
        DokkaBase().outputWriter to { files },
        DokkaBase().locationProviderFactory to ::DokkaLocationProviderFactory,
        DokkaBase().htmlPreprocessors to { RootCreator },
        DokkaBase().externalLocationProviderFactory to ::JavadocExternalLocationProviderFactory,
        DokkaBase().externalLocationProviderFactory to ::DefaultExternalLocationProviderFactory,
        testConfiguration = configuration
    )

    override val renderedContent: Element by lazy {
        files.contents.getValue("test-page.html").let { Jsoup.parse(it) }.select("#content").single()
    }

    protected fun linesAfterContentTag() =
        files.contents.getValue("test-page.html").lines()
            .dropWhile { !it.contains("""<div id="content">""") }
            .joinToString(separator = "") { it.trim() }
}

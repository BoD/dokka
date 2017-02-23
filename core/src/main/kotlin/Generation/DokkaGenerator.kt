package org.jetbrains.dokka

import com.google.inject.Guice
import com.google.inject.Injector
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.dokka.Utilities.DokkaAnalysisModule
import org.jetbrains.dokka.Utilities.DokkaOutputModule
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import kotlin.system.measureTimeMillis

class SourceRoot(val path: String, val defaultPlatforms: List<String> = emptyList())

class DokkaGenerator(val logger: DokkaLogger,
                     val classpath: List<String>,
                     val sources: List<SourceRoot>,
                     val samples: List<String>,
                     val includes: List<String>,
                     val moduleName: String,
                     val options: DocumentationOptions) {

    private val documentationModule = DocumentationModule(moduleName)

    fun generate() {
        val sourcesGroupedByPlatform = sources.groupBy { it.defaultPlatforms }
        for ((platforms, roots) in sourcesGroupedByPlatform) {
            appendSourceModule(platforms, roots.map { it.path })
        }
        documentationModule.prepareForGeneration(options)

        val timeBuild = measureTimeMillis {
            logger.info("Generating pages... ")
            val outputInjector = Guice.createInjector(DokkaOutputModule(options, logger))
            outputInjector.getInstance(Generator::class.java).buildAll(documentationModule)
        }
        logger.info("done in ${timeBuild / 1000} secs")
    }

    private fun appendSourceModule(defaultPlatforms: List<String>, sourcePaths: List<String>) {
        val environment = createAnalysisEnvironment(sourcePaths)

        logger.info("Module: $moduleName")
        logger.info("Output: ${File(options.outputDir)}")
        logger.info("Sources: ${sourcePaths.joinToString()}")
        logger.info("Classpath: ${environment.classpath.joinToString()}")

        logger.info("Analysing sources and libraries... ")
        val startAnalyse = System.currentTimeMillis()

        val injector = Guice.createInjector(
                DokkaAnalysisModule(environment, options, defaultPlatforms, documentationModule.nodeRefGraph, logger))

        buildDocumentationModule(injector, documentationModule, { isSample(it) }, includes)

        val timeAnalyse = System.currentTimeMillis() - startAnalyse
        logger.info("done in ${timeAnalyse / 1000} secs")

        Disposer.dispose(environment)
    }

    fun createAnalysisEnvironment(sourcePaths: List<String>): AnalysisEnvironment {
        val environment = AnalysisEnvironment(DokkaMessageCollector(logger))

        environment.apply {
            addClasspath(PathUtil.getJdkClassesRoots())
            //   addClasspath(PathUtil.getKotlinPathsForCompiler().getRuntimePath())
            for (element in this@DokkaGenerator.classpath) {
                addClasspath(File(element))
            }

            addSources(sourcePaths)
            addSources(this@DokkaGenerator.samples)
        }

        return environment
    }

    fun isSample(file: PsiFile): Boolean {
        val sourceFile = File(file.virtualFile!!.path)
        return samples.none { sample ->
            val canonicalSample = File(sample).canonicalPath
            val canonicalSource = sourceFile.canonicalPath
            canonicalSource.startsWith(canonicalSample)
        }
    }
}

class DokkaMessageCollector(val logger: DokkaLogger): MessageCollector {
    override fun clear() {
        seenErrors = false
    }

    private var seenErrors = false

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
        if (severity == CompilerMessageSeverity.ERROR) {
            seenErrors = true
        }
        logger.error(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
    }

    override fun hasErrors() = seenErrors
}

fun buildDocumentationModule(injector: Injector,
                             documentationModule: DocumentationModule,
                             filesToDocumentFilter: (PsiFile) -> Boolean = { file -> true },
                             includes: List<String> = listOf()) {

    val coreEnvironment = injector.getInstance(KotlinCoreEnvironment::class.java)
    val fragmentFiles = coreEnvironment.getSourceFiles().filter(filesToDocumentFilter)

    val resolutionFacade = injector.getInstance(DokkaResolutionFacade::class.java)
    val analyzer = resolutionFacade.getFrontendService(LazyTopDownAnalyzer::class.java)
    analyzer.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, fragmentFiles)

    val fragments = fragmentFiles
            .map { resolutionFacade.resolveSession.getPackageFragment(it.packageFqName) }
            .filterNotNull()
            .distinct()

    val packageDocs = injector.getInstance(PackageDocs::class.java)
    for (include in includes) {
        packageDocs.parse(include, fragments.firstOrNull())
    }
    documentationModule.updateContent {
        for (node in packageDocs.moduleContent.children) {
            append(node)
        }
    }

    with(injector.getInstance(DocumentationBuilder::class.java)) {
        documentationModule.appendFragments(fragments, packageDocs.packageContent,
                injector.getInstance(PackageDocumentationBuilder::class.java))
    }

    val javaFiles = coreEnvironment.getJavaSourceFiles().filter(filesToDocumentFilter)
    with(injector.getInstance(JavaDocumentationBuilder::class.java)) {
        javaFiles.map { appendFile(it, documentationModule, packageDocs.packageContent) }
    }
}


fun KotlinCoreEnvironment.getJavaSourceFiles(): List<PsiJavaFile> {
    val sourceRoots = configuration.get(JVMConfigurationKeys.CONTENT_ROOTS)
            ?.filterIsInstance<JavaSourceRoot>()
            ?.map { it.file }
            ?: listOf()

    val result = arrayListOf<PsiJavaFile>()
    val localFileSystem = VirtualFileManager.getInstance().getFileSystem("file")
    sourceRoots.forEach { sourceRoot ->
        sourceRoot.absoluteFile.walkTopDown().forEach {
            val vFile = localFileSystem.findFileByPath(it.path)
            if (vFile != null) {
                val psiFile = PsiManager.getInstance(project).findFile(vFile)
                if (psiFile is PsiJavaFile) {
                    result.add(psiFile)
                }
            }
        }
    }
    return result
}

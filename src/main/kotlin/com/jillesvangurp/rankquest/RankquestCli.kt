package com.jillesvangurp.rankquest

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.RatedSearch
import com.jilesvangurp.rankquest.core.pluginconfiguration.MetricsOutput
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.PluginFactoryRegistry
import com.jilesvangurp.rankquest.core.runMetrics
import java.io.File
import java.io.FileNotFoundException
import kotlin.system.exitProcess
import kotlin.time.measureTimedValue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString

suspend fun runMetrics(
    pluginConfigurationFile: String,
    testCasesFile: String,
    output: String?,
    chunkSize: Int,
    verbose: Boolean,
    fail: Boolean
) {
    val t = Terminal()

    val configuration = try {
        if (verbose) {
            t.println("Reading configuration $pluginConfigurationFile")
        }
        val configFile = File(pluginConfigurationFile).readText()
        DEFAULT_JSON.decodeFromString(SearchPluginConfiguration.serializer(), configFile)
    } catch (e: FileNotFoundException) {
        error("$pluginConfigurationFile does not exist")
    }

    val testCases = try {
        if (verbose) {
            t.println("Reading test cases $testCasesFile")
        }

        DEFAULT_JSON.decodeFromString<List<RatedSearch>>(File(testCasesFile).readText())
    } catch (e: FileNotFoundException) {
        error("$testCasesFile does not exist")
    }

    val registry = PluginFactoryRegistry()
    val outputFile = output?.let {
        val outputFile = File(output)
        if (outputFile.exists()) {
            error("$output already exists")
        }
        outputFile
    }
    registry[configuration.pluginType]?.let {
        val plugin = it.create(configuration)
        if (verbose) {
            t.println("Created search plugin for ${configuration.name} of type ${configuration.pluginType}")
        }

        if (verbose) {
            t.println("Running metrics for ${testCases.size} test cases with chunkSize $chunkSize")
        }
        val expectedMetrics = configuration.metrics.map { m -> m.expected ?: m.metric.defaultExpected }
        val (metrics, duration) = measureTimedValue {
            val failed = mutableListOf<Result<MetricsOutput>>()
            plugin.runMetrics(configuration, testCases, chunkSize).mapNotNull { result ->
                if (result.isFailure) {
                    failed.add(result)
                    null
                } else {
                    result.getOrNull()
                }
            }.also {
                if (failed.isNotEmpty()) {
                    failed.forEach { result ->
                        t.println(
                            yellow("Failed test case ${result.exceptionOrNull()}")
                        )
                    }
                }
            }

        }

        if (verbose) {
            t.println(
                "Evaluated ${bold(metrics.size.toString())} test cases in ${bold(duration.toString())} ${
                    if (output != null) {
                        ". Writing output to ${bold(output)}"
                    } else ""
                }"
            )
        }

        if (outputFile == null) {
            val tcMap = testCases.associateBy { t -> t.id }
            expectedMetrics.zip(metrics).forEach { (expected, mo) ->

                t.println("${mo.configuration.name}: ${
                    bold(
                        mo.results.metric.toString().let { m ->
                            if (mo.results.metric < expected) red(m) else green(m)
                        }
                    )
                }")
                if (verbose) {
                    t.println(table {
                        header {
                            row("Id", "Metric", "Rated results", "Unrated Results", "Search Context")
                        }
                        body {

                            mo.results.details.forEach { mr ->
                                row(
                                    mr.id,
                                    if (mr.metric < expected) red(mr.metric.toString()) else green(mr.metric.toString()),
                                    mr.hits.size,
                                    mr.unRated.size,
                                    tcMap[mr.id]?.searchContext
                                )
                            }
                        }
                    })
                }
            }
        } else {
            outputFile.writeText(DEFAULT_JSON.encodeToString(metrics))
        }
        val failed = configuration.metrics.map { m -> m.expected }.zip(metrics).filter { (expected, metric) ->
            metric.results.metric < (expected ?: metric.configuration.metric.defaultExpected)
        }
        if (failed.isNotEmpty()) {
            t.println(red("The following metrics: ${failed.map { (_,mo) -> mo.configuration.name }} are below their expected values."))
            if (fail) {
                exitProcess(1)
            }
        } else {
            t.println(green("All metrics are within acceptable range"))
        }

    } ?: error("plugin factory not found for ${configuration.pluginType}")
}


class RankquestCli : CliktCommand(
    name = "rankquest",
) {

    override fun help(context: Context) = """
        Run metrics for your Rankquest Studio Test Cases.
        
        rankquest -c demo/movies-config.json -t demo/testcases.json -v -f
    """.trimIndent()

    override fun helpEpilog(context: Context) =
        "You can review your metrics in Rankquest Studio: https://rankquest.jillesvangurp.com"

    private val chunkSize: Int by option().int().default(10)
        .help("Number of rated searches to fetch at the same time. Defaults to 10.")
    private val pluginConfigurationFile: String by option("-c").prompt("Plugin configuration file")
        .help("Json file with your plugin configuration")
    private val testCasesFile: String by option("-t").prompt("Test cases file").help("Json file with your test cases")
    private val output: String? by option("-o").help("Json file that will be created with the metrics output. Defaults to metrics.json")
    private val verbose: Boolean by option("-v").flag().help("Output more detailed logging if true")
    private val fail by option("-f").flag().help("Exit with an error status if metrics are below their expected value")
    override fun run() {
        runBlocking {
            runMetrics(
                pluginConfigurationFile = pluginConfigurationFile,
                testCasesFile = testCasesFile,
                output = output,
                chunkSize = chunkSize,
                verbose = verbose,
                fail = fail
            )
        }
    }
}


fun main(vararg args: String) = RankquestCli().main(args)
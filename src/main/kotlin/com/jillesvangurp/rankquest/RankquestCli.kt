package com.jillesvangurp.rankquest

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.RatedSearch
import com.jilesvangurp.rankquest.core.pluginconfiguration.MetricsOutput
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.PluginFactoryRegistry
import com.jilesvangurp.rankquest.core.runMetrics
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import mu.KotlinLogging
import java.io.File
import java.io.FileNotFoundException
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger { }

suspend fun runMetrics(
    pluginConfigurationFile: String,
    testCasesFile: String,
    output: String?,
    chunkSize: Int,
    verbose: Boolean
) {
    val configuration = try {
        DEFAULT_JSON.decodeFromString<SearchPluginConfiguration>(File(pluginConfigurationFile).readText())
    } catch (e: FileNotFoundException) {
        error("${pluginConfigurationFile} does not exist")
    }

    val testCases = try {
        DEFAULT_JSON.decodeFromString<List<RatedSearch>>(File(testCasesFile).readText())
    } catch (e: FileNotFoundException) {
        error("${testCasesFile} does not exist")
    }

    val registry = PluginFactoryRegistry()
    val outputFile = output?.let {
        val outputFile = File(output)
        if (outputFile.exists()) {
            error("${output} already exists")
        }
        outputFile
    }
    registry[configuration.pluginType]?.let {
        val plugin = it.create(configuration)
        if (verbose) {
            logger.info { "Created search plugin for ${configuration.name} of type ${configuration.pluginType}" }
        }

        if (verbose) {
            logger.info { "Running metrics for ${testCases.size} test cases with chunkSize ${chunkSize}" }
        }
        val (metrics, duration) = measureTimedValue {
            val failed= mutableListOf<Result<MetricsOutput>>()
            val foo = plugin.runMetrics(configuration, testCases, chunkSize).mapNotNull { result ->
                if(result.isFailure) {
                    failed.add(result)
                    null
                } else {
                    result.getOrNull()
                }
            }.also {
                if(failed.isNotEmpty()) {
                    failed.forEach {
                        logger.warn {
                            "Failed test case ${it.exceptionOrNull()}"
                        }
                    }
                }
            }
            foo
        }
        if (verbose) {
            logger.info { "Evaluated ${metrics.size} test cases in $duration and writing output to ${output}" }
        }

        if(outputFile == null) {
            val tcMap = testCases.associateBy { it.id }
            metrics.forEach { mo ->
                println("${mo.configuration.name}: ${mo.results.metric}")
                if(verbose) {
                    mo.results.details.forEach {mr->
                        println("\t${mr.id}: ${mr.metric} (${mr.hits.size / mr.unRated.size})")
                        println("\t\t${tcMap[mr.id]?.searchContext}")
                        
                    }
                }
            }
        } else {
            outputFile.writeText(DEFAULT_JSON.encodeToString(metrics))
        }

    } ?: error("plugin factory not found for ${configuration.pluginType}")
}


class RankquestCli : CliktCommand(
    name = "rankquest",
    help = """
        Run metrics for your Rankquest Studio Test Cases.
    """.trimIndent(),
    epilog = "You can review your metrics in Rankquest Studio: https://rankquest.jillesvangurp.com"
) {
    val chunkSize: Int by option().int().default(10)
        .help("Number of rated searches to fetch at the same time. Defaults to 10.")
    val pluginConfigurationFile: String by option("-c").prompt("Plugin configuration file")
        .help("Json file with your plugin configuration")
    val testCasesFile: String by option("-t").prompt("Test cases file").help("Json file with your test cases")
    val output: String? by option("-o").help("Json file that will be created with the metrics output. Defaults to metrics.json")
    val verbose: Boolean by option("-v").boolean().default(false).help("Output more detailed logging if true")
    override fun run() {
        runBlocking {
            runMetrics(pluginConfigurationFile,testCasesFile,output,chunkSize,verbose)
        }
    }
}


fun main(vararg args: String) = RankquestCli().main(args)
package com.jillesvangurp.rankquest

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.int

class RankquestCli: CliktCommand(
    name = "rankquest",
    help = """
        Run metrics for your Rankquest Studio Test Cases.
    """.trimIndent(),
    epilog = "You can review your metrics in Rankquest Studio: https://rankquest.jillesvangurp.com"
) {
    val chunkSize: Int by option().int().default(10).help("Number of rated searches to fetch at the same time. Defaults to 10.")
    val pluginConfiguration: String by option("-c").prompt("Plugin configuration file").help("Json file with your plugin configuration")
    val testCases: String by option("-t").prompt("Test cases file").help("Json file with your test cases")
    val output: String by option("-o").default("metrics.json").help("Json file that will be created with the metrics output. Defaults to metrics.json")
    override fun run() {
        println("OHAI")
    }
}

fun main(vararg args:String) = RankquestCli().main(args)
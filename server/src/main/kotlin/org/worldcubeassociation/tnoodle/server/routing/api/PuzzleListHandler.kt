package org.worldcubeassociation.tnoodle.server.routing.api

import io.ktor.server.application.*
import io.ktor.server.html.respondHtml
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import org.worldcubeassociation.tnoodle.server.RouteHandler
import org.worldcubeassociation.tnoodle.server.serial.api.PuzzleInfoJsonData
import org.worldcubeassociation.tnoodle.server.model.PuzzleData
import org.worldcubeassociation.tnoodle.svglite.Color
import kotlin.to

object PuzzleListHandler : RouteHandler {
    const val PUZZLE_KEY_PARAM = "puzzleKey"

    private fun getPuzzleInfo(scramblerKey: String, includeStatus: Boolean): PuzzleInfoJsonData? {
        val puzzle = PuzzleData.WCA_PUZZLES[scramblerKey] ?: return null

        val nameData = PuzzleInfoJsonData(scramblerKey, puzzle.description)

        if (includeStatus) {
            return nameData.copy(
                initializationStatus = puzzle.scrambler.initializationStatus,
                cacheQueue = puzzle.cacheSize)
        }

        return nameData
    }

    private suspend fun ApplicationCall.withPuzzleData(puzzleKeys: Set<String>, handle: suspend ApplicationCall.(List<PuzzleInfoJsonData>) -> Unit) {
        val includeStatus = "includeStatus" in request.queryParameters

        val puzzleInfos = puzzleKeys
            .mapNotNull { getPuzzleInfo(it, includeStatus) }

        handle(puzzleInfos)
    }

    override fun install(router: Route) {
        router.route("puzzles") {
            get {
                call.withPuzzleData(PuzzleData.WCA_PUZZLES.keys) {
                    respond(it)
                }
            }

            get("{$PUZZLE_KEY_PARAM}") {
                val puzzleKey = call.parameters[PUZZLE_KEY_PARAM].orEmpty()

                call.withPuzzleData(setOf(puzzleKey)) {
                    val singleItem = it.singleOrNull()
                        ?: return@withPuzzleData respondText("Invalid puzzle ID: $puzzleKey")

                    respond(singleItem)
                }
            }

            get("paint") {
                call.respondHtml {
                    body {
                        table {
                            thead {
                                tr {
                                    PuzzleData.entries.forEach {
                                        th { +it.scrambler.longName }
                                    }
                                }
                            }
                            tbody {
                                tr {
                                    PuzzleData.entries.forEach {
                                        td {
                                            unsafe { +it.scrambler.drawScramble("", it.scrambler.defaultColorScheme).toString() }
                                        }
                                    }
                                }
                                tr {
                                    PuzzleData.entries.forEach {
                                        td {
                                            unsafe { +it.scrambler.drawScramble("", experimentalSchemes.getValue(it.id)).toString() }
                                        }
                                    }
                                }
                                tr {
                                    PuzzleData.entries.forEach {
                                        td {
                                            unsafe { +it.scrambler.drawScramble(it.generateScramble(), it.scrambler.defaultColorScheme).toString() }
                                        }
                                    }
                                }
                                tr {
                                    PuzzleData.entries.forEach {
                                        td {
                                            unsafe { +it.scrambler.drawScramble(it.generateScramble(), experimentalSchemes.getValue(it.id)).toString() }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val WHITE = Color(0xFFFFFF)
    val YELLOW = Color(0xFFEA00)
    val ORANGE = Color(0xFF7E00)
    val RED = Color(0xFF0033)
    val GREEN = Color(0x29E016)
    val BLUE = Color(0x003DFF)

    val PURPLE = Color(0x8711DC)
    val MIDDLE_GRAY = Color(0x868686)

    val DARK_GRAY = Color(0x4B4B4B)

    val PINK = Color(0xFF00B8)
    val LIGHT_BLUE = Color(0x00CEFF)
    val DARK_GREEN = Color(0x007600)
    val BEIGE = Color(0xE8CE7D)

    val ICE_GRAY = Color(0xCCDDEE)
    val NAVY_BLUE = Color(0x113366)

    val experimentalSchemes = mapOf(
        "222" to mapOf(
            "U" to WHITE,
            "R" to RED,
            "F" to GREEN,
            "D" to YELLOW,
            "B" to BLUE,
            "L" to ORANGE,
        ),
        "333" to mapOf(
            "U" to WHITE,
            "R" to RED,
            "F" to GREEN,
            "D" to YELLOW,
            "B" to BLUE,
            "L" to ORANGE,
        ),
        "333fm" to mapOf(
            "U" to WHITE,
            "R" to RED,
            "F" to GREEN,
            "D" to YELLOW,
            "B" to BLUE,
            "L" to ORANGE,
        ),
        "333ni" to mapOf(
            "U" to WHITE,
            "R" to RED,
            "F" to GREEN,
            "D" to YELLOW,
            "B" to BLUE,
            "L" to ORANGE,
        ),
        "444" to mapOf(
            "U" to WHITE,
            "R" to RED,
            "F" to GREEN,
            "D" to YELLOW,
            "B" to BLUE,
            "L" to ORANGE,
        ),
        "444ni" to mapOf(
            "U" to WHITE,
            "R" to RED,
            "F" to GREEN,
            "D" to YELLOW,
            "B" to BLUE,
            "L" to ORANGE,
        ),
        "555" to mapOf(
            "U" to WHITE,
            "R" to RED,
            "F" to GREEN,
            "D" to YELLOW,
            "B" to BLUE,
            "L" to ORANGE,
        ),
        "555ni" to mapOf(
            "U" to WHITE,
            "R" to RED,
            "F" to GREEN,
            "D" to YELLOW,
            "B" to BLUE,
            "L" to ORANGE,
        ),
        "666" to mapOf(
            "U" to WHITE,
            "R" to RED,
            "F" to GREEN,
            "D" to YELLOW,
            "B" to BLUE,
            "L" to ORANGE,
        ),
        "777" to mapOf(
            "U" to WHITE,
            "R" to RED,
            "F" to GREEN,
            "D" to YELLOW,
            "B" to BLUE,
            "L" to ORANGE,
        ),
        "fto" to mapOf(
            "U" to WHITE,
            "R" to RED,
            "F" to GREEN,
            "L" to PURPLE,
            "B" to BLUE,
            "BL" to ORANGE,
            "D" to YELLOW,
            "BR" to MIDDLE_GRAY,
        ),
        "minx" to mapOf(
            "U" to WHITE,
            "BL" to YELLOW,
            "BR" to BLUE,
            "R" to RED,
            "F" to DARK_GREEN,
            "L" to PURPLE,
            "D" to MIDDLE_GRAY,
            "DR" to BEIGE,
            "DBR" to PINK,
            "B" to GREEN,
            "DBL" to ORANGE,
            "DL" to LIGHT_BLUE,
        ),
        "pyram" to mapOf(
            "F" to GREEN,
            "D" to YELLOW,
            "L" to RED,
            "R" to BLUE,
        ),
        "skewb" to mapOf(
            "U" to WHITE,
            "R" to BLUE,
            "F" to RED,
            "D" to YELLOW,
            "B" to ORANGE,
            "L" to GREEN,
        ),
        "sq1" to mapOf(
            "U" to DARK_GRAY,
            "R" to GREEN,
            "F" to RED,
            "D" to WHITE,
            "B" to ORANGE,
            "L" to BLUE,
        ),
        "clock" to mapOf(
            "Front" to NAVY_BLUE,
            "FrontClock" to ICE_GRAY,
            "FrontTopClock" to YELLOW,
            "FrontHand" to NAVY_BLUE,
            "FrontPin" to MIDDLE_GRAY,
            "Back" to ICE_GRAY,
            "BackClock" to NAVY_BLUE,
            "BackTopClock" to ORANGE,
            "BackHand" to ICE_GRAY,
            "BackPin" to LIGHT_BLUE,
        ),
    )
}

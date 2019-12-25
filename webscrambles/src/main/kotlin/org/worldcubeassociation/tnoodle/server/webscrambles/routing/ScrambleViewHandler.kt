package org.worldcubeassociation.tnoodle.server.webscrambles.routing

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.receiveText
import io.ktor.request.uri
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import net.gnehzr.tnoodle.scrambles.PuzzleIcon
import net.gnehzr.tnoodle.scrambles.PuzzleImageInfo
import org.worldcubeassociation.tnoodle.server.RouteHandler
import org.worldcubeassociation.tnoodle.server.RouteHandler.Companion.parseQuery
import org.worldcubeassociation.tnoodle.server.util.GsonUtil.GSON
import net.gnehzr.tnoodle.plugins.PuzzlePlugins
import org.worldcubeassociation.tnoodle.server.RouteHandler.Companion.splitNameAndExtension
import org.worldcubeassociation.tnoodle.server.util.ServerEnvironmentConfig
import org.worldcubeassociation.tnoodle.server.webscrambles.ScrambleRequest
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.WCIFHelper
import java.util.*

class ScrambleViewHandler(val environmentConfig: ServerEnvironmentConfig) : RouteHandler {
    private val scramblers = PuzzlePlugins.PUZZLES

    override fun install(router: Routing) {
        router.route("/view") {
            get("/{puzzleExt}") {
                val puzzleExt = call.parameters["puzzleExt"] ?: return@get call.respondText("Please specify a puzzle")

                val (name, extension) = splitNameAndExtension(puzzleExt)

                if (extension.isEmpty()) {
                    return@get call.respondText("No extension specified.")
                }

                val scrambler by scramblers[name] ?: return@get call.respondText("Invalid scrambler: $name")

                val queryStr = call.request.uri.substringAfter('?', "")
                val query = parseQuery(queryStr).toMutableMap()

                val colorScheme = query["scheme"]?.let { scrambler.parseColorScheme(it) } ?: hashMapOf()
                val scramble = query["scramble"]

                when (extension) {
                    "png" -> {
                        if (query.containsKey("icon")) {
                            val icon = PuzzleIcon.loadPuzzleIconPng(scrambler.shortName)

                            call.respondBytes(icon.toByteArray(), ContentType.Image.PNG)
                        } else call.respondText("Invalid extension: $extension")
                    }
                    "svg" -> {
                        val svg = scrambler.drawScramble(scramble, colorScheme)

                        call.respondText(svg.toString(), ContentType.Image.SVG)
                    }
                    "json" -> call.respond(PuzzleImageInfo(scrambler))
                    else -> call.respondText("Invalid extension: $extension")
                }
            }

            post("/{puzzleExt}") {
                val puzzleExt = call.parameters["puzzleExt"] ?: return@post call.respondText("Please specify a puzzle")

                val (name, extension) = splitNameAndExtension(puzzleExt)

                if (extension.isEmpty()) {
                    return@post call.respondText("No extension specified.")
                }

                val body = call.receiveText()
                val query = parseQuery(body)

                val scrambleRequests = GSON.fromJson(query["sheets"], Array<ScrambleRequest>::class.java).toList()
                val password = query["password"]

                val generationDate = Date()

                when (extension) {
                    "pdf" -> {
                        val totalPdfOutput = ScrambleRequest.requestsToCompletePdf(name, generationDate, environmentConfig.projectTitle, scrambleRequests)

                        call.response.header("Content-Disposition", "inline")

                        // Workaround for Chrome bug with saving PDFs:
                        // https://bugs.chromium.org/p/chromium/issues/detail?id=69677#c35
                        call.response.header("Cache-Control", "public")

                        call.respondBytes(totalPdfOutput.render(), ContentType.Application.Pdf)
                    }
                    "zip" -> {
                        val generationUrl = query["generationUrl"]
                        val schedule = query["schedule"] ?: "{}"

                        val wcifHelper = WCIFHelper(schedule)

                        val zipOutput = ScrambleRequest.requestsToZip(name, generationDate, environmentConfig.projectTitle, scrambleRequests, password, generationUrl, wcifHelper)

                        val safeTitle = name.replace("\"".toRegex(), "'")

                        call.response.header("Content-Disposition", "attachment; filename=\"$safeTitle.zip\"")
                        call.respondBytes(zipOutput.toByteArray(), ContentType.Application.Zip)
                    }
                    else -> call.respondText("Invalid extension: $extension")
                }
            }
        }
    }
}

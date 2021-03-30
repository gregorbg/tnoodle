package org.worldcubeassociation.tnoodle.server.webscrambles.zip.folder

import org.slf4j.LoggerFactory
import org.worldcubeassociation.tnoodle.server.model.EventData
import org.worldcubeassociation.tnoodle.server.webscrambles.Translate
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.FmcGenericSolutionSheet
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.FmcScrambleCutoutSheet
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.FmcSolutionSheet
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.StringUtil.toFileSafeString
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.CompetitionDrawingData
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.ScrambleDrawingData
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.WCIFDataBuilder
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.WCIFDataBuilder.getCachedPdf
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ActivityCode
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.Schedule
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ScrambleSet
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.extension.FmcLanguagesExtension
import org.worldcubeassociation.tnoodle.server.webscrambles.zip.folder
import org.worldcubeassociation.tnoodle.server.webscrambles.zip.model.Folder
import java.time.LocalDate

data class PrintingFolder(val uniqueTitles: Map<String, ScrambleDrawingData>, val globalTitle: String, val wcifSchedule: Schedule) {
    val scrambleSheetsFlat = uniqueTitles.values.toList()
    val scrambleDrawingData = CompetitionDrawingData(globalTitle, scrambleSheetsFlat)

    fun assemble(generationDate: LocalDate, versionTag: String, password: String?): Folder {
        val fmcRequests = uniqueTitles.filterValues { it.isFmc }

        val pseudoActivityCode = ActivityCode.compile(EventData.THREE_FM, round = 0)
        val printingCompletePdf = WCIFDataBuilder.requestsToCompletePdf(scrambleDrawingData, generationDate, versionTag, Translate.DEFAULT_LOCALE)

        return folder("Printing") {
            folder("Scramble Sets") {
                for ((uniq, req) in uniqueTitles) {
                    // Without passcode, for printing
                    val pdfPrintingByteStream = req.getCachedPdf(generationDate, versionTag, globalTitle, Translate.DEFAULT_LOCALE)

                    file("$uniq.pdf", pdfPrintingByteStream.render())
                }
            }

            if (fmcRequests.isNotEmpty()) {
                folder("Fewest Moves - Additional Files") {
                    val defaultGenericPrintingSheet = FmcGenericSolutionSheet(ScrambleSet.empty(), pseudoActivityCode, globalTitle, Translate.DEFAULT_LOCALE)
                    file("3x3x3 Fewest Moves Solution Sheet.pdf", defaultGenericPrintingSheet.render())

                    for ((uniq, req) in fmcRequests) {
                        val defaultCutoutSheet = FmcScrambleCutoutSheet(req.scrambleSet, req.activityCode, globalTitle, Translate.DEFAULT_LOCALE, req.hasGroupID)
                        file("$uniq - Scramble Cutout Sheet.pdf", defaultCutoutSheet.render(password))

                        val requestedTranslations = req.scrambleSet.findExtension<FmcLanguagesExtension>()
                            ?.languageTags ?: FMC_LOCALES_BY_TAG.keys

                        val translationLocales = requestedTranslations.mapNotNull { FMC_LOCALES_BY_TAG[it] }

                        if (translationLocales.isNotEmpty()) {
                            folder("Translations") {
                                for (locale in translationLocales) {
                                    val languageMarkerTitle = "${locale.toLanguageTag()}/$uniq"

                                    // fewest moves regular sheet
                                    val localPrintingSheet = FmcSolutionSheet(req.scrambleSet, req.activityCode, globalTitle, locale, req.hasGroupID)

                                    // fewest moves generic sheet
                                    val localGenericPrintingSheet = FmcGenericSolutionSheet(req.scrambleSet, req.activityCode, globalTitle, locale)

                                    // scramble cutout sheet
                                    val localCutoutSheet = FmcScrambleCutoutSheet(req.scrambleSet, req.activityCode, globalTitle, locale, req.hasGroupID)

                                    file("$languageMarkerTitle.pdf", localPrintingSheet.render(password))
                                    file("$languageMarkerTitle Solution Sheet.pdf", localGenericPrintingSheet.render(password))
                                    file("$languageMarkerTitle Scramble Cutout Sheet.pdf", localCutoutSheet.render(password))
                                }
                            }
                        }
                    }
                }
            }

            if (wcifSchedule.isNotEmpty()) {
                val drawingScrambleSets = scrambleDrawingData.scrambleSheets.map { it.scrambleSet }

                if (wcifSchedule.containsAll(drawingScrambleSets)) {
                    val orderedScramblesFolder = OrderedScramblesFolder(globalTitle, scrambleDrawingData)
                    val orderedScramblesNode = orderedScramblesFolder.assemble(wcifSchedule, generationDate, versionTag)

                    folder(orderedScramblesNode)
                } else {
                    val requiredIds = drawingScrambleSets.map { it.id }
                    val unmatchedActivities = wcifSchedule.leafActivities
                        .filter { it.activityCode.eventModel != null } // don't want to report that "lunch" or "other" is unmatched
                        .filter { it.scrambleSetId !in requiredIds }

                    LOGGER.warn("Skipping OrderedScrambles because there are unmatched activities: $unmatchedActivities")
                }
            }

            val safeGlobalTitle = globalTitle.toFileSafeString()

            // Note that we're not passing the password into this function. It seems pretty silly
            // to put a password protected pdf inside of a password protected zip file.
            file("$safeGlobalTitle - All Scrambles.pdf", printingCompletePdf.render())
        }
    }

    companion object {
        val LOGGER = LoggerFactory.getLogger(PrintingFolder::class.java)

        private fun Schedule.isNotEmpty() = leafActivities.isNotEmpty()

        private fun Schedule.containsAll(scrambleSets: Collection<ScrambleSet>): Boolean {
            val scheduleMatchedIds = allActivities.mapNotNull { it.scrambleSetId }
            val requiredIds = scrambleSets.map { it.id }

            return scheduleMatchedIds.containsAll(requiredIds)
        }

        val FMC_LOCALES_BY_TAG = Translate.TRANSLATED_LOCALES.associateBy { it.toLanguageTag() }
    }
}

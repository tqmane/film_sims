package com.tqmane.filmsim.util

import android.content.Context
import android.util.Log
import java.io.BufferedReader

class VivoWatermarkConfigParser(private val context: Context) {

    private val tag = "VivoConfigParser"

    fun parseConfig(assetPath: String): VivoWatermarkTemplate? {
        return try {
            val content = readAssetFile(assetPath) ?: return null
            parseContent(content)
        } catch (e: Exception) {
            Log.e(tag, "Error parsing config: $assetPath", e)
            null
        }
    }

    private fun readAssetFile(assetPath: String): String? {
        return try {
            val inputStream = context.assets.open(assetPath)
            val reader = BufferedReader(inputStream.reader())
            reader.use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseContent(content: String): VivoWatermarkTemplate? {
        val parser = ConfigParser()
        parser.parse(content)
        return parser.buildTemplate()
    }

    private class ConfigParser {
        private var frameConfig = VivoFrameConfig()
        private val paths = mutableListOf<VivoPath>()
        private val groups = mutableListOf<VivoParamGroup>()

        private var currentGroup: VivoParamGroup? = null
        private var currentSubgroup: VivoSubgroup? = null
        private var currentLine: VivoLine? = null
        private var currentPicParam: VivoImageParam? = null
        private var currentTextParam: VivoTextParam? = null
        private var inPicParam = false
        private var inTextParam = false

        fun parse(content: String) {
            val lines = content.lines()
            for (line in lines) {
                val trimmed = line.trim()
                when {
                    trimmed == "SETIN" || trimmed == "PATHSETIN" || trimmed == "PARAMSETIN" -> {}
                    trimmed == "PATHCLOSE" || trimmed == "PARAMCLOSE" || trimmed == "CLOSE" -> closeCurrentElements()
                    trimmed.startsWith("<frametype>") -> frameConfig = frameConfig.copy(frametype = extractValue(trimmed))
                    trimmed.startsWith("<subtype>") -> frameConfig = frameConfig.copy(subtype = extractValue(trimmed).toIntOrNull() ?: 0)
                    trimmed.startsWith("<isneedvivologo>") -> frameConfig = frameConfig.copy(isneedvivologo = extractValue(trimmed).toBoolean())
                    trimmed.startsWith("<iscameraborder>") -> frameConfig = frameConfig.copy(iscameraborder = extractValue(trimmed).toBoolean())
                    trimmed.startsWith("<isadaptive>") -> frameConfig = frameConfig.copy(isadaptive = extractValue(trimmed).toBoolean())
                    trimmed.startsWith("<isfixed>") -> frameConfig = frameConfig.copy(isfixed = extractValue(trimmed).toBoolean())
                    trimmed.startsWith("<isallwrap>") -> frameConfig = frameConfig.copy(isallwrap = extractValue(trimmed).toBoolean())
                    trimmed.startsWith("<isneeddefaultparam>") -> frameConfig = frameConfig.copy(isneeddefaultparam = extractValue(trimmed).toBoolean())
                    trimmed.startsWith("<basecolor>") -> frameConfig = frameConfig.copy(basecolor = extractValue(trimmed).toIntOrNull() ?: -65794)
                    trimmed.startsWith("<baseboard>") -> frameConfig = frameConfig.copy(baseboard = extractValue(trimmed))
                    trimmed.startsWith("<templatewidth>") -> frameConfig = frameConfig.copy(templatewidth = extractValue(trimmed).toIntOrNull() ?: 1080)
                    trimmed.startsWith("<templateheight>") -> frameConfig = frameConfig.copy(templateheight = extractValue(trimmed).toIntOrNull() ?: 1719)
                    trimmed.startsWith("<marginstart>") -> frameConfig = frameConfig.copy(marginstart = extractValue(trimmed).toFloatOrNull() ?: 0f)
                    trimmed.startsWith("<marginend>") -> frameConfig = frameConfig.copy(marginend = extractValue(trimmed).toFloatOrNull() ?: 0f)
                    trimmed.startsWith("<point>") -> {
                        val points = parsePoints(extractValue(trimmed))
                        paths.add(VivoPath(points))
                    }
                    trimmed == "<group>" -> currentGroup = VivoParamGroup()
                    trimmed == "</group>" -> closeGroup()
                    trimmed.startsWith("<groupgravity>") -> {
                        currentGroup = currentGroup?.copy(groupgravity = extractValue(trimmed))
                            ?: VivoParamGroup(groupgravity = extractValue(trimmed))
                    }
                    trimmed.startsWith("<groupmarginend>") -> {
                        currentGroup = currentGroup?.copy(groupmarginend = extractValue(trimmed).toFloatOrNull() ?: 0f)
                            ?: VivoParamGroup(groupmarginend = extractValue(trimmed).toFloatOrNull() ?: 0f)
                    }
                    trimmed == "<subgroup>" -> currentSubgroup = VivoSubgroup(subgroupnum = 0)
                    trimmed == "</subgroup>" -> closeSubgroup()
                    trimmed.startsWith("<subgroupnum>") -> {
                        currentSubgroup = currentSubgroup?.copy(subgroupnum = extractValue(trimmed).toIntOrNull() ?: 0)
                            ?: VivoSubgroup(subgroupnum = extractValue(trimmed).toIntOrNull() ?: 0)
                    }
                    trimmed.startsWith("<subgroupvisible>") -> {
                        currentSubgroup = currentSubgroup?.copy(subgroupvisible = extractValue(trimmed).toBoolean())
                            ?: VivoSubgroup(subgroupnum = 0, subgroupvisible = extractValue(trimmed).toBoolean())
                    }
                    trimmed.startsWith("<debuginfo>") -> {
                        currentSubgroup = currentSubgroup?.copy(debuginfo = extractValue(trimmed))
                            ?: VivoSubgroup(subgroupnum = 0, debuginfo = extractValue(trimmed))
                    }
                    trimmed == "<line>" -> currentLine = VivoLine()
                    trimmed == "</line>" -> closeLine()
                    trimmed.startsWith("<linemarginbottom>") -> {
                        currentLine = currentLine?.copy(linemarginbottom = extractValue(trimmed).toFloatOrNull() ?: 0f)
                            ?: VivoLine(linemarginbottom = extractValue(trimmed).toFloatOrNull() ?: 0f)
                    }
                    trimmed == "<picparam>" -> { inPicParam = true; currentPicParam = VivoImageParam() }
                    trimmed == "</picparam>" -> { addPicParam(); inPicParam = false; currentPicParam = null }
                    trimmed.startsWith("<piclinenum>") -> currentPicParam = currentPicParam?.copy(piclinenum = extractValue(trimmed).toIntOrNull() ?: 0)
                    trimmed.startsWith("<picgravity>") -> currentPicParam = currentPicParam?.copy(picgravity = extractValue(trimmed))
                    trimmed.startsWith("<picpoint>") || trimmed.startsWith("<picplanbpoint>") -> {
                        currentPicParam = currentPicParam?.copy(picpoint = parseRect(extractValue(trimmed)))
                    }
                    trimmed.startsWith("<pic>") -> currentPicParam = currentPicParam?.copy(pic = extractValue(trimmed))
                    trimmed.startsWith("<issvg>") -> currentPicParam = currentPicParam?.copy(issvg = extractValue(trimmed).toBoolean())
                    trimmed.startsWith("<iscamerapic>") -> currentPicParam = currentPicParam?.copy(iscamerapic = extractValue(trimmed).toBoolean())
                    trimmed.startsWith("<picparamsidetype>") -> currentPicParam = currentPicParam?.copy(picparamsidetype = extractValue(trimmed).toIntOrNull() ?: 0)
                    trimmed.startsWith("<picid>") -> currentPicParam = currentPicParam?.copy(picid = extractValue(trimmed).toIntOrNull() ?: 0)
                    trimmed == "<textparam>" -> { inTextParam = true; currentTextParam = VivoTextParam() }
                    trimmed == "</textparam>" -> { addTextParam(); inTextParam = false; currentTextParam = null }
                    trimmed.startsWith("<linenum>") -> currentTextParam = currentTextParam?.copy(linenum = extractValue(trimmed).toIntOrNull() ?: 0)
                    trimmed.startsWith("<textgravity>") -> currentTextParam = currentTextParam?.copy(textgravity = extractValue(trimmed))
                    trimmed.startsWith("<textpoint>") || trimmed.startsWith("<textplanbpoint>") -> {
                        currentTextParam = currentTextParam?.copy(textpoint = parseRect(extractValue(trimmed)))
                    }
                    trimmed.startsWith("<text>") -> currentTextParam = currentTextParam?.copy(text = extractValue(trimmed))
                    trimmed.startsWith("<textsize>") -> currentTextParam = currentTextParam?.copy(textsize = extractValue(trimmed).toFloatOrNull() ?: 0f)
                    trimmed.startsWith("<textfontweight>") -> currentTextParam = currentTextParam?.copy(textfontweight = extractValue(trimmed).toIntOrNull() ?: 400)
                    trimmed.startsWith("<textcolor>") -> currentTextParam = currentTextParam?.copy(textcolor = extractValue(trimmed))
                    trimmed.startsWith("<letterspacing>") -> currentTextParam = currentTextParam?.copy(letterspacing = extractValue(trimmed).toFloatOrNull() ?: 0f)
                    trimmed.startsWith("<typeface>") -> currentTextParam = currentTextParam?.copy(typeface = extractValue(trimmed).toIntOrNull() ?: 0)
                    trimmed.startsWith("<texttype>") -> currentTextParam = currentTextParam?.copy(texttype = extractValue(trimmed).toIntOrNull() ?: 0)
                    trimmed.startsWith("<iscustomtext>") -> currentTextParam = currentTextParam?.copy(iscustomtext = extractValue(trimmed).toIntOrNull() ?: 0)
                }
            }
        }

        private fun closeLine() {
            currentLine?.let { currentSubgroup = currentSubgroup?.copy(lines = currentSubgroup!!.lines + it) }
            currentLine = null
        }

        private fun addPicParam() {
            currentPicParam?.let { currentLine = currentLine?.copy(images = currentLine!!.images + it) }
        }

        private fun addTextParam() {
            currentTextParam?.let { currentLine = currentLine?.copy(texts = currentLine!!.texts + it) }
        }

        private fun closeSubgroup() {
            closeLine()
            currentSubgroup?.let { currentGroup = currentGroup?.copy(subgroups = currentGroup!!.subgroups + it) }
            currentSubgroup = null
        }

        private fun closeGroup() {
            closeSubgroup()
            currentGroup?.let { groups.add(it) }
            currentGroup = null
        }

        private fun closeCurrentElements() {
            if (inPicParam) { addPicParam(); inPicParam = false; currentPicParam = null }
            if (inTextParam) { addTextParam(); inTextParam = false; currentTextParam = null }
            closeLine()
            closeSubgroup()
            closeGroup()
        }

        fun buildTemplate(): VivoWatermarkTemplate? {
            return if (paths.isNotEmpty() || groups.isNotEmpty()) VivoWatermarkTemplate(frameConfig, paths, groups) else null
        }

        private fun extractValue(line: String): String {
            val start = line.indexOf('>') + 1
            val end = line.indexOf('<', start)
            return if (start > 0 && end > start) line.substring(start, end) else ""
        }

        private fun parsePoints(pointsStr: String): List<VivoPoint> {
            val points = mutableListOf<VivoPoint>()
            val regex = Regex("\\((\\d+\\.?\\d*),(\\d+\\.?\\d*)\\)")
            for (match in regex.findAll(pointsStr)) {
                points.add(VivoPoint(match.groupValues[1].toFloat(), match.groupValues[2].toFloat()))
            }
            return points
        }

        private fun parseRect(rectStr: String): VivoRect? {
            val regex = Regex("\\((\\d+\\.?\\d*),(\\d+\\.?\\d*)\\)")
            val matches = regex.findAll(rectStr).toList()
            return if (matches.size >= 4) {
                VivoRect(
                    matches[0].groupValues[1].toFloat(), matches[0].groupValues[2].toFloat(),
                    matches[3].groupValues[1].toFloat(), matches[3].groupValues[2].toFloat()
                )
            } else null
        }
    }

    companion object {
        fun getZeissTemplates() = listOf(
            "vivo_watermark_full2/assets/zeiss_editors/zeiss0.txt",
            "vivo_watermark_full2/assets/zeiss_editors/zeiss1.txt",
            "vivo_watermark_full2/assets/zeiss_editors/zeiss2.txt",
            "vivo_watermark_full2/assets/zeiss_editors/zeiss3.txt",
            "vivo_watermark_full2/assets/zeiss_editors/zeiss4.txt",
            "vivo_watermark_full2/assets/zeiss_editors/zeiss5.txt",
            "vivo_watermark_full2/assets/zeiss_editors/zeiss6.txt",
            "vivo_watermark_full2/assets/zeiss_editors/zeiss7.txt",
            "vivo_watermark_full2/assets/zeiss_editors/zeiss8.txt"
        )

        fun getIqooTemplates() = listOf(
            "vivo_watermark_full2/assets/zeiss_editors/iqoo4.txt",
            "vivo_watermark_full2/assets/zeiss_editors/common_iqoo4.txt"
        )

        fun getVivoTemplates() = listOf(
            "vivo_watermark_full2/assets/zeiss_editors/vivo1.txt",
            "vivo_watermark_full2/assets/zeiss_editors/vivo2.txt",
            "vivo_watermark_full2/assets/zeiss_editors/vivo3.txt",
            "vivo_watermark_full2/assets/zeiss_editors/vivo4.txt",
            "vivo_watermark_full2/assets/zeiss_editors/vivo5.txt"
        )

        fun getEventTemplates() = listOf(
            "vivo_watermark_full2/assets/zeiss_editors/event1.txt",
            "vivo_watermark_full2/assets/zeiss_editors/event2.txt"
        )
    }
}

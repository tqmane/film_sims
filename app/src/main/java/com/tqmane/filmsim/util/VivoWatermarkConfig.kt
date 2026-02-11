package com.tqmane.filmsim.util

data class VivoFrameConfig(
    val frametype: String = "",
    val subtype: Int = 0,
    val isneedvivologo: Boolean = false,
    val iscameraborder: Boolean = false,
    val isadaptive: Boolean = false,
    val isfixed: Boolean = false,
    val isallwrap: Boolean = false,
    val isneeddefaultparam: Boolean = false,
    val basecolor: Int = -65794,
    val baseboard: String = "",
    val templatewidth: Int = 1080,
    val templateheight: Int = 1719,
    val marginstart: Float = 0f,
    val marginend: Float = 0f
)

data class VivoPath(val points: List<VivoPoint>)

data class VivoPoint(val x: Float, val y: Float)

data class VivoParamGroup(
    val groupgravity: String = "center_vertical",
    val groupmarginend: Float = 0f,
    val subgroups: List<VivoSubgroup> = emptyList()
)

data class VivoSubgroup(
    val subgroupnum: Int = 0,
    val subgroupvisible: Boolean = true,
    val debuginfo: String = "",
    val lines: List<VivoLine> = emptyList()
)

data class VivoLine(
    val linemarginbottom: Float = 0f,
    val images: List<VivoImageParam> = emptyList(),
    val texts: List<VivoTextParam> = emptyList()
)

data class VivoImageParam(
    val piclinenum: Int = 0,
    val picgravity: String = "start",
    val picpoint: VivoRect? = null,
    val picmarginstart: Float = 0f,
    val picmarginend: Float = 0f,
    val pic: String = "",
    val issvg: Boolean = false,
    val iscamerapic: Boolean = false,
    val picparamsidetype: Int = 0,
    val picid: Int = 0,
    val isneedantialias: Boolean = true
)

data class VivoTextParam(
    val linenum: Int = 0,
    val textgravity: String = "start",
    val textpoint: VivoRect? = null,
    val textplanbpoint: VivoRect? = null,
    val text: String = "",
    val textsize: Float = 0f,
    val textfontweight: Int = 400,
    val textcolor: String = "#FF000000",
    val letterspacing: Float = 0f,
    val typeface: Int = 0,
    val texttype: Int = 0,
    val iscustomtext: Int = 0
)

data class VivoRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class VivoWatermarkTemplate(
    val frame: VivoFrameConfig,
    val paths: List<VivoPath>,
    val groups: List<VivoParamGroup>
)

object VivoTextType {
    const val STATIC_ZEISS = 0
    const val DEVICE_NAME = 1
    const val FOCAL_LENGTH = 2
    const val APERTURE = 3
    const val SHUTTER = 4
    const val ISO = 5
    const val TIME = 6
    const val LOCATION = 7
    const val DATE = 9
    const val THREE_A_SINGLE = 10
    const val TIME_ALT = 12
    const val ZEISS_BRAND = 13
    const val DEVICE_NAME_VARIANT = 14
}

object VivoTypeface {
    const val DEFAULT = 0
    const val CAMERA_PARAMS = 7
    const val DEVICE_NAME = 8
    const val IQOO = 9
}

object VivoGravity {
    const val START = "start"
    const val END = "end"
    const val CENTER = "center"
    const val CENTER_VERTICAL = "center_vertical"
    const val CENTER_HORIZONTAL = "center_horizontal"

    fun hasCenterVertical(gravity: String): Boolean = gravity.contains(CENTER_VERTICAL)
    fun isEnd(gravity: String): Boolean = gravity == END
}

package com.tqmane.filmsim.data

import android.content.Context
import android.content.res.AssetManager
import com.tqmane.filmsim.R

data class LutItem(
    val name: String,
    val assetPath: String
)

data class LutCategory(
    val name: String,
    val displayName: String,
    val items: List<LutItem>
)

data class LutBrand(
    val name: String,
    val displayName: String,
    val categories: List<LutCategory>
)

object LutRepository {
    
    // Supported LUT file extensions
    private val lutExtensions = listOf(".cube", ".png", ".bin", ".webp", ".jpg", ".jpeg")

    private fun isAssetDirectory(assetManager: AssetManager, assetPath: String): Boolean {
        return try {
            val children = assetManager.list(assetPath)
            !children.isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private fun isLutAssetFile(assetManager: AssetManager, parentAssetPath: String, name: String): Boolean {
        val leaf = name.substringAfterLast('/')
        val fullPath = "$parentAssetPath/$name"

        // Never treat directories as LUT files.
        if (isAssetDirectory(assetManager, fullPath)) return false

        // Some vendors ship raw LUT binaries without an extension (e.g. OnePlus/Uncategorized/default).
        // Treat extensionless *files* as LUT candidates; non-LUT metadata (xml) still has an extension.
        if (!leaf.contains('.')) return true

        return lutExtensions.any { ext -> leaf.endsWith(ext, ignoreCase = true) }
    }

    private fun stripKnownExtension(fileName: String): String {
        val leaf = fileName.substringAfterLast('/')
        if (!leaf.contains('.')) return leaf
        return lutExtensions.fold(leaf) { acc, ext ->
            acc.removeSuffix(ext).removeSuffix(ext.uppercase())
        }
    }

    private fun selectBestVariant(variants: List<String>): String {
        fun priority(name: String): Int {
            val leaf = name.substringAfterLast('/')
            if (!leaf.contains('.')) return 0 // extensionless raw bin
            return when {
                leaf.endsWith(".bin", ignoreCase = true) -> 1
                leaf.endsWith(".cube", ignoreCase = true) -> 2
                leaf.endsWith(".png", ignoreCase = true) -> 3
                leaf.endsWith(".webp", ignoreCase = true) -> 4
                leaf.endsWith(".jpg", ignoreCase = true) || leaf.endsWith(".jpeg", ignoreCase = true) -> 5
                else -> 9
            }
        }
        return variants.minBy { priority(it) }
    }
    
    // Category name to string resource ID mapping
    private fun getCategoryDisplayName(context: Context, categoryName: String): String {
        return when (categoryName) {
            // OnePlus categories
            "App Filters" -> context.getString(R.string.category_app_filters)
            "Artistic" -> context.getString(R.string.category_artistic)
            "Black & White" -> context.getString(R.string.category_black_white)
            "Cinematic Movie" -> context.getString(R.string.category_cinematic_movie)
            "Cool Tones" -> context.getString(R.string.category_cool_tones)
            "Food" -> context.getString(R.string.category_food)
            "Golden Touch" -> context.getString(R.string.category_golden_touch)
            "Instagram Filters" -> context.getString(R.string.category_instagram_filters)
            "Japanese Style" -> context.getString(R.string.category_japanese_style)
            "Landscape" -> context.getString(R.string.category_landscape)
            "Night" -> context.getString(R.string.category_night)
            "Portrait" -> context.getString(R.string.category_portrait)
            "Uncategorized" -> context.getString(R.string.category_uncategorized)
            "Vintage-Retro" -> context.getString(R.string.category_vintage_retro)
            "Warm Tones" -> context.getString(R.string.category_warm_tones)
            // Xiaomi categories
            "Cinematic" -> context.getString(R.string.category_cinematic)
            "Film Simulation" -> context.getString(R.string.category_film_simulation)
            "Monochrome" -> context.getString(R.string.category_monochrome)
            "Nature-Landscape" -> context.getString(R.string.category_nature_landscape)
            "Portrait-Soft" -> context.getString(R.string.category_portrait_soft)
            "Special Effects" -> context.getString(R.string.category_special_effects)
            "Vivid-Natural" -> context.getString(R.string.category_vivid_natural)
            "Warm-Vintage" -> context.getString(R.string.category_warm_vintage)
            // Leica_lux categories
            "Leica Looks" -> context.getString(R.string.category_leica_looks)
            "Artist Looks" -> context.getString(R.string.category_artist_looks)
            // Film category
            "Film" -> context.getString(R.string.category_film)
            // Vivo categories
            "newfilter" -> context.getString(R.string.category_camera_filters)
            "editor_filters" -> context.getString(R.string.category_editor_filters)
            "movielut" -> context.getString(R.string.category_movie)
            "portraitstylefilter" -> context.getString(R.string.category_portrait_style)
            "portraitstylefilter_multistyle" -> context.getString(R.string.category_portrait_multistyle)
            "collage_filters" -> context.getString(R.string.category_collage_filters)
            "nightstylefilter" -> context.getString(R.string.category_night_style)
            "superzoom" -> context.getString(R.string.category_superzoom)
            // Honor categories
            "luts" -> context.getString(R.string.category_all)
            // Meizu categories
            "aiFilters" -> context.getString(R.string.category_ai_filters)
            "classicFilter" -> context.getString(R.string.category_classic_filter)
            "filterManager" -> context.getString(R.string.category_filter_manager)
            "General" -> context.getString(R.string.category_general)
            // Common/Nothing
            "_all" -> context.getString(R.string.category_all)
            // Fallback - keep original name for Fujifilm, Kodak Film, etc.
            else -> categoryName.replace("_", " ").replace("-", " - ")
        }
    }
    
    // Film folder LUT filename to localized display name
    private fun getFilmLutName(context: Context, fileName: String): String {
        return when {
            fileName.contains("field", ignoreCase = true) -> context.getString(R.string.lut_film_field)
            fileName.contains("seaside", ignoreCase = true) -> context.getString(R.string.lut_film_seaside)
            fileName.contains("city", ignoreCase = true) -> context.getString(R.string.lut_film_city)
            fileName.contains("neon", ignoreCase = true) -> context.getString(R.string.lut_film_neon)
            fileName.contains("cold_flash", ignoreCase = true) -> context.getString(R.string.lut_film_cold_flash)
            fileName.contains("warm_flash", ignoreCase = true) -> context.getString(R.string.lut_film_warm_flash)
            fileName.contains("vintage", ignoreCase = true) -> context.getString(R.string.lut_film_vintage)
            fileName.contains("clear", ignoreCase = true) -> context.getString(R.string.lut_film_clear)
            fileName.contains("800t", ignoreCase = true) -> context.getString(R.string.lut_film_800t)
            else -> fileName.replace("_", " ")
        }
    }
    
    // Honor filter filename to localized display name
    private fun getHonorFilterName(context: Context, fileName: String): String {
        return when {
            fileName.equals("baixi", ignoreCase = true) -> context.getString(R.string.lut_honor_baixi)
            fileName.equals("fendiao", ignoreCase = true) -> context.getString(R.string.lut_honor_fendiao)
            fileName.equals("heibai", ignoreCase = true) -> context.getString(R.string.lut_honor_heibai)
            fileName.equals("heijin", ignoreCase = true) -> context.getString(R.string.lut_honor_heijin)
            fileName.equals("huaijiu", ignoreCase = true) -> context.getString(R.string.lut_honor_huaijiu)
            fileName.equals("huidiao", ignoreCase = true) -> context.getString(R.string.lut_honor_huidiao)
            fileName.equals("jiaotang", ignoreCase = true) -> context.getString(R.string.lut_honor_jiaotang)
            fileName.equals("jingdian", ignoreCase = true) -> context.getString(R.string.lut_honor_jingdian)
            fileName.equals("landiao", ignoreCase = true) -> context.getString(R.string.lut_honor_landiao)
            fileName.equals("qingcheng", ignoreCase = true) -> context.getString(R.string.lut_honor_qingcheng)
            fileName.equals("senxi", ignoreCase = true) -> context.getString(R.string.lut_honor_senxi)
            fileName.equals("tangguo", ignoreCase = true) -> context.getString(R.string.lut_honor_tangguo)
            fileName.equals("yingxiang", ignoreCase = true) -> context.getString(R.string.lut_honor_yingxiang)
            fileName.equals("zhishi", ignoreCase = true) -> context.getString(R.string.lut_honor_zhishi)
            fileName.equals("ziran", ignoreCase = true) -> context.getString(R.string.lut_honor_ziran)
            fileName.startsWith("hn_", ignoreCase = true) -> {
                val name = fileName.removePrefix("hn_")
                getHonorFilterName(context, name)
            }
            // Fallback: use Chinese filter naming
            fileName.equals("danya", ignoreCase = true) -> context.getString(R.string.lut_honor_danya)
            fileName.equals("jiaopian", ignoreCase = true) -> context.getString(R.string.lut_honor_jiaopian)
            fileName.equals("qingchun", ignoreCase = true) -> context.getString(R.string.lut_honor_qingchun)
            fileName.equals("rouhe", ignoreCase = true) -> context.getString(R.string.lut_honor_rouhe)
            fileName.equals("xianming", ignoreCase = true) -> context.getString(R.string.lut_honor_xianming)
            fileName.equals("xianyan", ignoreCase = true) -> context.getString(R.string.lut_honor_xianyan)
            fileName.equals("yuanqi", ignoreCase = true) -> context.getString(R.string.lut_honor_yuanqi)
            else -> fileName.replace("_", " ")
        }
    }

    // Meizu filter filename to localized display name
    private fun getMeizuFilterName(context: Context, fileName: String, categoryName: String): String {
        // classicFilter camera_* prefix handling
        if (categoryName == "classicFilter") {
            return when {
                fileName.contains("fanchanuan", ignoreCase = true) && fileName.contains("front", ignoreCase = true) -> context.getString(R.string.lut_meizu_warm_front)
                fileName.contains("fanchanuan", ignoreCase = true) -> context.getString(R.string.lut_meizu_warm)
                fileName.contains("fanchase", ignoreCase = true) && fileName.contains("front", ignoreCase = true) -> context.getString(R.string.lut_meizu_retro_front)
                fileName.contains("fanchase", ignoreCase = true) -> context.getString(R.string.lut_meizu_retro)
                fileName.contains("nense", ignoreCase = true) && fileName.contains("front", ignoreCase = true) -> context.getString(R.string.lut_meizu_tender_front)
                fileName.contains("nense", ignoreCase = true) -> context.getString(R.string.lut_meizu_tender)
                fileName.contains("nuanse", ignoreCase = true) && fileName.contains("front", ignoreCase = true) -> context.getString(R.string.lut_meizu_warm_tone_front)
                fileName.contains("nuanse", ignoreCase = true) -> context.getString(R.string.lut_meizu_warm_tone)
                fileName.contains("xianming", ignoreCase = true) && fileName.contains("front", ignoreCase = true) -> context.getString(R.string.lut_meizu_vivid_front)
                fileName.contains("xianming", ignoreCase = true) -> context.getString(R.string.lut_meizu_vivid)
                fileName.contains("filtertable") -> {
                    val suffix = fileName.removePrefix("filtertable_rgb_second_")
                    suffix.replaceFirstChar { it.titlecase() }
                }
                else -> fileName.replace("_", " ")
            }
        }
        // General folder
        if (categoryName == "General") {
            return when {
                fileName.equals("original512", ignoreCase = true) -> context.getString(R.string.lut_meizu_original)
                fileName.equals("skinWhiten", ignoreCase = true) -> context.getString(R.string.lut_meizu_skin_whiten)
                else -> fileName.replaceFirstChar { it.titlecase() }.replace("_", " ")
            }
        }
        // aiFilters and filterManager: already have nice names (Bright, Gentle, etc.)
        return fileName.replace("_", " ")
    }

    // Brand name to display name mapping
    private fun getBrandDisplayName(context: Context, brandName: String): String {
        return when (brandName) {
            "Leica_lux" -> context.getString(R.string.brand_leica_lux)
            "Leica_FOTOS" -> context.getString(R.string.brand_leica_fotos)
            else -> brandName
        }
    }
    
    // Leica_FOTOS filter filename to localized display name
    private fun getLeicaFotosFilterName(context: Context, fileName: String): String {
        return when {
            fileName.equals("default_bleach", ignoreCase = true) -> context.getString(R.string.lut_fotos_bleach)
            fileName.equals("default_blue", ignoreCase = true) -> context.getString(R.string.lut_fotos_blue)
            fileName.equals("default_brass", ignoreCase = true) -> context.getString(R.string.lut_fotos_brass)
            fileName.equals("default_chrome", ignoreCase = true) -> context.getString(R.string.lut_fotos_chrome)
            fileName.equals("default_classic", ignoreCase = true) -> context.getString(R.string.lut_fotos_classic)
            fileName.equals("default_contemporary", ignoreCase = true) -> context.getString(R.string.lut_fotos_contemporary)
            fileName.equals("default_eternal", ignoreCase = true) -> context.getString(R.string.lut_fotos_eternal)
            fileName.equals("default_gregwilliams", ignoreCase = true) -> context.getString(R.string.lut_fotos_gregwilliams)
            fileName.equals("default_selenium", ignoreCase = true) -> context.getString(R.string.lut_fotos_selenium)
            fileName.equals("default_sepia", ignoreCase = true) -> context.getString(R.string.lut_fotos_sepia)
            fileName.equals("default_silver", ignoreCase = true) -> context.getString(R.string.lut_fotos_silver)
            fileName.equals("default_teal", ignoreCase = true) -> context.getString(R.string.lut_fotos_teal)
            fileName.equals("mono_blue", ignoreCase = true) -> context.getString(R.string.lut_fotos_mono_blue)
            fileName.equals("mono_gregwilliams", ignoreCase = true) -> context.getString(R.string.lut_fotos_mono_gregwilliams)
            fileName.equals("mono_selenium", ignoreCase = true) -> context.getString(R.string.lut_fotos_mono_selenium)
            fileName.equals("mono_sepia", ignoreCase = true) -> context.getString(R.string.lut_fotos_mono_sepia)
            else -> fileName.replace("_", " ")
        }
    }

    // Leica_lux filter filename to localized display name
    private fun getLeicaLuxFilterName(context: Context, fileName: String): String {
        return when {
            fileName.contains("Classic_sRGB") -> context.getString(R.string.lut_leica_classic)
            fileName.contains("Contemporary_sRGB") -> context.getString(R.string.lut_leica_contemporary)
            fileName.contains("Leica-Filter_Monochrome") -> context.getString(R.string.lut_leica_monochrome_natural)
            fileName.contains("Leica-Filter_Natural") -> context.getString(R.string.lut_leica_natural)
            fileName.contains("Leica-Looks_Blue") -> context.getString(R.string.lut_leica_blue)
            fileName.contains("Leica-Looks_Eternal") -> context.getString(R.string.lut_leica_eternal)
            fileName.contains("Leica-Looks_Selenium") -> context.getString(R.string.lut_leica_selenium)
            fileName.contains("Leica-Looks_Sepia") -> context.getString(R.string.lut_leica_sepia)
            fileName.contains("Leica-Looks_Silver") -> context.getString(R.string.lut_leica_silver)
            fileName.contains("Leica-Looks_Teal") -> context.getString(R.string.lut_leica_teal)
            fileName.contains("Leica_Bleach") -> context.getString(R.string.lut_leica_bleach)
            fileName.contains("Leica_Brass") -> context.getString(R.string.lut_leica_brass)
            fileName.contains("Leica_Monochrome_High_Contrast") -> context.getString(R.string.lut_leica_high_contrast)
            fileName.contains("Leica_Vivid") -> context.getString(R.string.lut_leica_vivid)
            fileName.contains("Tyson_100yearsMono") -> context.getString(R.string.lut_100_years_mono)
            fileName.contains("Tyson_GregWilliams_Sepia0") -> context.getString(R.string.lut_greg_williams_sepia_0)
            fileName.contains("Tyson_GregWilliams_Sepia100") -> context.getString(R.string.lut_greg_williams_sepia_100)
            fileName.contains("Tyson_Leica_Base_V3") -> context.getString(R.string.lut_leica_standard)
            fileName.contains("Tyson_Leica_Chrome") -> context.getString(R.string.lut_leica_chrome)
            else -> fileName.replace("_", " ")
        }
    }
    
    // Vivo filter filename to display name
    private fun getVivoFilterName(fileName: String): String {
        var name = fileName
        // Strip common prefixes (longer/more specific first)
        val prefixes = listOf(
            "special_new_filter_", "special_portrait_",
            "new_filter_back_photo_hdr_", "new_filter_",
            "effects_space_filter_",
            "filter_polaroid_", "filter_portrait_style_", "filter_portrait_",
            "filter_",
            "front_filter_portrait_style_",
            "portrait_back_", "portrait_",
            "zeiss_star_light_",
            "pack_film_",
            "polaroid_",
            // editor/collage category prefixes
            "film_", "food_", "fruity_", "human_", "japan_", "night_", "style_"
        )
        for (prefix in prefixes) {
            if (name.startsWith(prefix)) {
                name = name.removePrefix(prefix)
                break
            }
        }
        // Handle remaining front_/back_ prefixes
        if (name.startsWith("front_")) name = name.removePrefix("front_")
        if (name.startsWith("back_")) name = name.removePrefix("back_")
        // Strip redundant suffixes
        name = name.removeSuffix("_lut").removeSuffix("_filter")
        // Title case
        return name.replace("_", " ").split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
    
    // Nubia filter filename to localized display name
    private fun getNubiaFilterName(context: Context, fileName: String): String {
        return when {
            fileName.startsWith("fengjing") -> context.getString(R.string.lut_nubia_landscape)
            fileName.startsWith("meishi") -> context.getString(R.string.lut_nubia_food)
            fileName.startsWith("renxiang") && fileName.contains("bg") -> context.getString(R.string.lut_nubia_portrait_bg)
            fileName.startsWith("renxiang") && fileName.contains("skin 2") -> context.getString(R.string.lut_nubia_portrait_skin_2)
            fileName.startsWith("renxiang") && fileName.contains("skin") -> context.getString(R.string.lut_nubia_portrait_skin)
            fileName.startsWith("richang") -> context.getString(R.string.lut_nubia_daily)
            fileName.startsWith("shenghuo") && fileName.contains("bg") -> context.getString(R.string.lut_nubia_life_bg)
            fileName.startsWith("shenghuo") && fileName.contains("skin") -> context.getString(R.string.lut_nubia_life_skin)
            else -> fileName.replace("_", " ")
        }
    }
    
    fun getLutBrands(context: Context): List<LutBrand> {
        val assetManager = context.assets
        val brands = mutableListOf<LutBrand>()
        
        try {
            val rootPath = "luts"
            val brandFolders = assetManager.list(rootPath) ?: return emptyList()
            
            for (brandName in brandFolders) {
                val brandPath = "$rootPath/$brandName"
                val contents = assetManager.list(brandPath) ?: continue
                
                val categories = mutableListOf<LutCategory>()
                val isLeicaLux = brandName == "Leica_lux"
                val isLeicaFotos = brandName == "Leica_FOTOS"
                val isVivo = brandName == "Vivo"
                val isNubia = brandName == "Nubia"
                val isHonor = brandName == "Honor"
                val isMeizu = brandName == "Meizu"
                
                // Check if brand has flat structure (LUT files directly in brand folder)
                val directLutFiles = contents.filter { file -> isLutAssetFile(assetManager, brandPath, file) }
                
                if (directLutFiles.isNotEmpty()) {
                    // Flat structure (e.g., Nothing) - create a single "All" category
                    
                    // Group by basename to handle duplicates (prefer .bin > .cube > .png)
                    val groupedFiles = directLutFiles.groupBy { filename -> stripKnownExtension(filename) }
                    
                    val lutItems = groupedFiles.map { (baseName, files) ->
                        val selectedFile = selectBestVariant(files)
                            
                        val displayName = when {
                            isLeicaLux -> getLeicaLuxFilterName(context, baseName)
                            isLeicaFotos -> getLeicaFotosFilterName(context, baseName)
                            isNubia -> getNubiaFilterName(context, baseName)
                            isVivo -> getVivoFilterName(baseName)
                            isHonor -> getHonorFilterName(context, baseName)
                            isMeizu -> getMeizuFilterName(context, baseName, "_all")
                            else -> baseName.replace("_", " ")
                        }
                        LutItem(
                            name = displayName,
                            assetPath = "$brandPath/$selectedFile"
                        )
                    }.sortedBy { it.name.lowercase() }
                    
                    categories.add(
                        LutCategory(
                            name = "_all",
                            displayName = getCategoryDisplayName(context, "_all"),
                            items = lutItems
                        )
                    )
                }
                
                // Check for subdirectories (category folders)
                val categoryFolders = contents.filter { name -> isAssetDirectory(assetManager, "$brandPath/$name") }
                
                for (categoryName in categoryFolders) {
                    val categoryPath = "$brandPath/$categoryName"
                    val files = assetManager.list(categoryPath) ?: continue
                    
                    // Group by basename to handle duplicates (prefer .bin > .cube > .png)
                    val groupedFiles = files
                        .filter { file -> isLutAssetFile(assetManager, categoryPath, file) }
                        .groupBy { filename -> stripKnownExtension(filename) }

                    val lutItems = groupedFiles.map { (baseName, variants) ->
                        val selectedFile = selectBestVariant(variants)

                        val isFilmCategory = categoryName == "Film"
                        val displayName = when {
                            isLeicaLux -> getLeicaLuxFilterName(context, baseName)
                            isLeicaFotos -> getLeicaFotosFilterName(context, baseName)
                            isFilmCategory -> getFilmLutName(context, baseName)
                            isVivo -> getVivoFilterName(baseName)
                            isHonor -> getHonorFilterName(context, baseName)
                            isMeizu -> getMeizuFilterName(context, baseName, categoryName)
                            else -> baseName.replace("_", " ")
                        }
                        LutItem(
                            name = displayName,
                            assetPath = "$categoryPath/$selectedFile"
                        )
                    }.sortedBy { it.name.lowercase() }
                    
                    if (lutItems.isNotEmpty()) {
                        categories.add(
                            LutCategory(
                                name = categoryName,
                                displayName = getCategoryDisplayName(context, categoryName),
                                items = lutItems
                            )
                        )
                    }
                }
                
                if (categories.isNotEmpty()) {
                    brands.add(
                        LutBrand(
                            name = brandName,
                            displayName = getBrandDisplayName(context, brandName),
                            categories = categories.sortedBy { it.displayName }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return brands.sortedBy { it.displayName }
    }
    
    // Legacy support
    fun getLutGenres(context: Context): List<LutGenre> {
        val brands = getLutBrands(context)
        return brands.flatMap { brand ->
            brand.categories.map { category ->
                LutGenre(
                    name = "${brand.displayName} - ${category.displayName}",
                    items = category.items
                )
            }
        }
    }
}

// Legacy data class for compatibility
data class LutGenre(
    val name: String,
    val items: List<LutItem>
)
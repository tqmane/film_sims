#version 300 es
precision mediump float;

in vec2 vTexCoord;
out vec4 outColor;

uniform sampler2D uInputTexture;
uniform mediump sampler3D uLutTexture;
uniform mediump sampler3D uOverlayLutTexture;
uniform sampler2D uGrainTexture;
uniform float uIntensity; // 0.0 = original, 1.0 = full LUT effect
uniform float uOverlayIntensity; // 0.0 = no overlay, 1.0 = full overlay LUT
uniform float uGrainIntensity; // 0.0 = no grain, 1.0 = full grain
uniform float uGrainScale; // Grain texture tiling scale
uniform float uTime; // For grain animation (optional)
uniform float uCompareSplit; // 0.0 to 1.0 screen split position
uniform float uCompareEnabled; // > 0.5 enables before/after compare
uniform float uCompareVertical; // > 0.5 uses vertical divider, else horizontal divider
uniform vec2 uResolution; // viewport size in pixels

// Basic adjustments (applied before LUT)
uniform float uExposure;    // -2.0 to +2.0 (0.0 = no change)
uniform float uContrast;    // -1.0 to +1.0 (0.0 = no change)
uniform float uHighlights;  // -1.0 to +1.0 (0.0 = no change)
uniform float uShadows;     // -1.0 to +1.0 (0.0 = no change)
uniform float uColorTemp;   // -1.0 to +1.0 (0.0 = neutral, + = warm, - = cool)
uniform float uHue;         // -1.0 to +1.0 hue shift
uniform float uSaturation;  // -1.0 to +1.0 saturation adjustment
uniform float uLuminance;   // -1.0 to +1.0 luminance adjustment

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec3 p = abs(fract(c.xxx + vec3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0);
    return c.z * mix(vec3(1.0), clamp(p - 1.0, 0.0, 1.0), c.y);
}

void main() {
    vec4 originalColor = texture(uInputTexture, vTexCoord);
    vec3 adjusted = originalColor.rgb;

    // Exposure (stops)
    adjusted *= pow(2.0, uExposure);

    // Contrast (pivot at mid-gray)
    adjusted = mix(vec3(0.5), adjusted, 1.0 + uContrast);

    // Highlights & Shadows (luminance-based)
    float lum = dot(adjusted, vec3(0.299, 0.587, 0.114));
    float shadowMask = 1.0 - smoothstep(0.0, 0.5, lum);
    float highlightMask = smoothstep(0.5, 1.0, lum);
    adjusted += uShadows * shadowMask * 0.4;
    adjusted += uHighlights * highlightMask * 0.4;

    // Color temperature (warm/cool shift)
    adjusted.r *= 1.0 + uColorTemp * 0.15;
    adjusted.g *= 1.0 + uColorTemp * 0.05;
    adjusted.b *= 1.0 - uColorTemp * 0.15;

    vec3 hsv = rgb2hsv(clamp(adjusted, 0.0, 1.0));
    hsv.x = fract(hsv.x + uHue * 0.5);
    hsv.y = clamp(hsv.y * (1.0 + uSaturation), 0.0, 1.0);
    adjusted = hsv2rgb(hsv);
    adjusted += vec3(uLuminance * 0.2);

    adjusted = clamp(adjusted, 0.0, 1.0);

    // 3D LUT lookup (on adjusted color)
    vec3 lutColor = texture(uLutTexture, adjusted).rgb;
    
    // Blend between adjusted and LUT based on intensity
    vec3 baseColor = mix(adjusted, lutColor, uIntensity);

    vec3 overlayColor = texture(uOverlayLutTexture, clamp(baseColor, 0.0, 1.0)).rgb;
    vec3 finalColor = mix(baseColor, overlayColor, uOverlayIntensity);
    
    // Apply film grain if enabled
    if (uGrainIntensity > 0.001) {
        // Sample grain texture with scaling and optional time offset for variation
        vec2 grainCoord = vTexCoord * uGrainScale + vec2(uTime * 0.1);
        vec3 grain = texture(uGrainTexture, grainCoord).rgb;
        
        // Convert grain from 0-1 to -0.5 to 0.5 range for additive blend
        vec3 grainOffset = (grain - 0.5) * 2.0;
        
        // Apply grain with luminance-aware blending (less grain in shadows)
        float luminance = dot(finalColor, vec3(0.299, 0.587, 0.114));
        float grainMask = smoothstep(0.0, 0.3, luminance) * smoothstep(1.0, 0.7, luminance);
        
        finalColor += grainOffset * uGrainIntensity * grainMask * 0.15;
        finalColor = clamp(finalColor, 0.0, 1.0);
    }
    
    vec3 displayColor = finalColor;
    if (uCompareEnabled > 0.5) {
        // Use screen-space coordinates so the split line aligns with the UI overlay
        vec2 screenUV = gl_FragCoord.xy / uResolution;
        // gl_FragCoord.y is bottom-up; UI is top-down, so flip Y for horizontal split
        float compareCoord = uCompareVertical > 0.5 ? screenUV.x : (1.0 - screenUV.y);
        if (compareCoord > uCompareSplit) {
            displayColor = originalColor.rgb;
        }
    }

    outColor = vec4(displayColor, originalColor.a);
}

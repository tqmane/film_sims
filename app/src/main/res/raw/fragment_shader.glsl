#version 300 es
precision mediump float;

in vec2 vTexCoord;
out vec4 outColor;

uniform sampler2D uInputTexture;
uniform mediump sampler3D uLutTexture;
uniform sampler2D uGrainTexture;
uniform float uIntensity; // 0.0 = original, 1.0 = full LUT effect
uniform float uGrainIntensity; // 0.0 = no grain, 1.0 = full grain
uniform float uGrainScale; // Grain texture tiling scale
uniform float uTime; // For grain animation (optional)

// Basic adjustments (applied before LUT)
uniform float uExposure;    // -2.0 to +2.0 (0.0 = no change)
uniform float uContrast;    // -1.0 to +1.0 (0.0 = no change)
uniform float uHighlights;  // -1.0 to +1.0 (0.0 = no change)
uniform float uShadows;     // -1.0 to +1.0 (0.0 = no change)
uniform float uColorTemp;   // -1.0 to +1.0 (0.0 = neutral, + = warm, - = cool)

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

    adjusted = clamp(adjusted, 0.0, 1.0);

    // 3D LUT lookup (on adjusted color)
    vec3 lutColor = texture(uLutTexture, adjusted).rgb;
    
    // Blend between adjusted and LUT based on intensity
    vec3 finalColor = mix(adjusted, lutColor, uIntensity);
    
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
    
    outColor = vec4(finalColor, originalColor.a);
}

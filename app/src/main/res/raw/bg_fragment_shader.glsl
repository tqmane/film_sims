#version 300 es
precision mediump float;

in vec2 vTexCoord;
out vec4 outColor;

uniform float uTime;
uniform vec2 uResolution;

// Colors from LiquidTheme
const vec3 BG_COLOR = vec3(0.047, 0.047, 0.063); // LiquidColors.Background (#0C0C10)
const vec3 AMBER = vec3(0.8, 0.4, 0.0);          // LiquidColors.AmbientAmber (#CC6600)
const vec3 CYAN = vec3(0.0, 0.6, 0.8);           // LiquidColors.AmbientCyan (#0099CC)
const vec3 PURPLE = vec3(0.6, 0.2, 0.8);         // LiquidColors.AmbientPurple (#9933CC)

// Noise function for grain
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    
    // Animate aurora positions with slow, breathing movement
    // Amber
    float amberAnimX = (sin(uTime * 0.0004) + 1.0) * 0.5; // 0 to 1
    float amberAnimY = (sin(uTime * 0.0003 + 1.0) + 1.0) * 0.5;
    vec2 amberPos = vec2(0.2 * amberAnimX + 0.1, 0.3 * amberAnimY + 0.1);
    
    // Cyan
    float cyanAnimX = (cos(uTime * 0.0002) + 1.0) * 0.5;
    float cyanAnimY = (sin(uTime * 0.00035 + 2.0) + 1.0) * 0.5;
    vec2 cyanPos = vec2(0.8 * cyanAnimX + 0.1, 0.7 * cyanAnimY + 0.1);
    
    // Purple
    float purpleAnimX = (sin(uTime * 0.00025 + 3.0) + 1.0) * 0.5;
    float purpleAnimY = (cos(uTime * 0.00045 + 4.0) + 1.0) * 0.5;
    vec2 purplePos = vec2(0.5 * purpleAnimX + 0.3, 0.8 * purpleAnimY + 0.1);
    
    // Scale pulsation
    float scalePulse = (sin(uTime * 0.0005) * 0.1) + 1.0; // 0.9 to 1.1

    // Aspect ratio correction for circles
    vec2 aspectUV = uv;
    aspectUV.x *= uResolution.x / uResolution.y;
    
    vec2 aspectAmberPos = amberPos; aspectAmberPos.x *= uResolution.x / uResolution.y;
    vec2 aspectCyanPos = cyanPos; aspectCyanPos.x *= uResolution.x / uResolution.y;
    vec2 aspectPurplePos = purplePos; aspectPurplePos.x *= uResolution.x / uResolution.y;

    // Distances
    float distAmber = distance(aspectUV, aspectAmberPos);
    float distCyan = distance(aspectUV, aspectCyanPos);
    float distPurple = distance(aspectUV, aspectPurplePos);

    // Gradients
    float amberStrength = smoothstep(0.4 * scalePulse, 0.0, distAmber) * 0.15;
    float cyanStrength = smoothstep(0.35 * scalePulse, 0.0, distCyan) * 0.12;
    float purpleStrength = smoothstep(0.3 * scalePulse, 0.0, distPurple) * 0.10;

    // Base color
    vec3 color = BG_COLOR;
    
    // Additive blending
    color += AMBER * amberStrength;
    color += CYAN * cyanStrength;
    color += PURPLE * purpleStrength;

    // Noise overlay (opacity 0.03, blend mode overlay approximation)
    float noise = random(uv + uTime);
    float noiseOffset = (noise - 0.5) * 2.0;
    
    // Simple overlay blend approximation for grain
    color += noiseOffset * 0.03;

    outColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}

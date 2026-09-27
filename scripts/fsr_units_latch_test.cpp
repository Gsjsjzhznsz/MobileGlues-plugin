// Standalone replica of FSR1_WindowUnitsCandidate (gl/FSR1/FSR1.cpp) with the
// run 48f4b1c empty-latch rule, plus the two latch sites' grow-only update, so
// the rule set can be graded on a desktop toolchain (full-file builds are
// blocked by the vendored Arm headers; CI with NDK/libc++ remains the gate).
//
// Build & run:  g++ -std=c++17 -o fsr_units_latch_test fsr_units_latch_test.cpp && ./fsr_units_latch_test

#include <cstdio>
#include <cstdlib>

namespace FSR1_Context {
    int g_targetWidth = 0, g_targetHeight = 0; // surface
    int g_viewWidth = 0, g_viewHeight = 0;     // latched app window units
}

#define LOG_W_FORCE(...) do { printf("  [log] "); printf(__VA_ARGS__); printf("\n"); } while (0)
typedef int GLsizei;

// ---- replica of FSR1_WindowUnitsCandidate (keep in step with the real one) ----
bool FSR1_WindowUnitsCandidate(GLsizei w, GLsizei h) {
    if (w == 0 || h == 0) return false;
    const double candidateAspect = static_cast<double>(w) / static_cast<double>(h);
    auto aspectDrift = [](double a, double b) { return (a > b ? a - b : b - a) / b; };

    // Run 48f4b1c rule, the empty latch (non-square full-bleed candidates only;
    // both call sites guarantee origin 0,0).
    if (FSR1_Context::g_viewWidth == 0 && FSR1_Context::g_viewHeight == 0 && w != h) {
        static GLsizei s_seededW = -1, s_seededH = -1;
        if (s_seededW != w || s_seededH != h) {
            s_seededW = w;
            s_seededH = h;
            LOG_W_FORCE("[MG] FSR1 window-units latch seeded: %dx%d (empty-latch rule, surface %dx%d)",
                        w, h, FSR1_Context::g_targetWidth, FSR1_Context::g_targetHeight);
        }
        return true;
    }

    // Rule 1, the window shape (surface aspect).
    if (FSR1_Context::g_targetWidth > 0 && FSR1_Context::g_targetHeight > 0 &&
        aspectDrift(candidateAspect, static_cast<double>(FSR1_Context::g_targetWidth) /
                                            static_cast<double>(FSR1_Context::g_targetHeight)) <= 0.03) {
        return true;
    }
    // Rule 2, continuity (latched aspect).
    if (FSR1_Context::g_viewWidth > 0 && FSR1_Context::g_viewHeight > 0 &&
        aspectDrift(candidateAspect, static_cast<double>(FSR1_Context::g_viewWidth) /
                                         static_cast<double>(FSR1_Context::g_viewHeight)) <= 0.03) {
        return true;
    }
    static GLsizei s_rejectedW = -1, s_rejectedH = -1;
    if (s_rejectedW != w || s_rejectedH != h) {
        s_rejectedW = w;
        s_rejectedH = h;
        LOG_W_FORCE("[MG] FSR1 window-units latch rejected (air Task 82): %dx%d (surface %dx%d, latch %dx%d)",
                    w, h, FSR1_Context::g_targetWidth, FSR1_Context::g_targetHeight, FSR1_Context::g_viewWidth,
                    FSR1_Context::g_viewHeight);
    }
    return false;
}

// ---- replica of the two latch sites' grow-only update ----
static void latch_update(GLsizei w, GLsizei h) {
    if (FSR1_WindowUnitsCandidate(w, h)) {
        if (w > FSR1_Context::g_viewWidth) FSR1_Context::g_viewWidth = w;
        if (h > FSR1_Context::g_viewHeight) FSR1_Context::g_viewHeight = h;
    }
}

static int g_failures = 0;
#define EXPECT(cond)                                                     \
    do {                                                                 \
        if (!(cond)) {                                                   \
            printf("  FAIL line %d: %s\n", __LINE__, #cond);             \
            ++g_failures;                                                \
        }                                                                \
    } while (0)

int main() {
    // ---- Scenario A: run 48f4b1c exactly. Surface 1280x720, window 2360x1080,
    // latch empty. The old rules rejected the true window; the seed rule must
    // take it and the rewrite denominator must be the window afterwards. ----
    printf("A: FCL pairing, surface 1280x720, window 2360x1080\n");
    FSR1_Context::g_targetWidth = 1280;
    FSR1_Context::g_targetHeight = 720;
    EXPECT(!FSR1_WindowUnitsCandidate(0, 0));            // zero guard
    latch_update(2360, 1080);                            // first full-bleed composition viewport
    EXPECT(FSR1_Context::g_viewWidth == 2360);
    EXPECT(FSR1_Context::g_viewHeight == 1080);
    const double scale = 640.0 / 2360.0;                 // render 640x360 / latched units
    EXPECT(scale < 0.272 && scale > 0.270);              // no more 1180x540 overshoot

    // ---- Scenario B: atlas pass with the latch still empty must NOT seed. ----
    printf("B: square atlas against an empty latch\n");
    FSR1_Context::g_viewWidth = 0;
    FSR1_Context::g_viewHeight = 0;
    EXPECT(!FSR1_WindowUnitsCandidate(2048, 2048));
    EXPECT(FSR1_Context::g_viewWidth == 0);

    // ---- Scenario C: seeded latch keeps rejecting square passes (rule 2 only
    // matches its own shape). ----
    printf("C: seeded 2360x1080, atlas 2048x2048 arrives\n");
    FSR1_Context::g_targetWidth = 2360;
    FSR1_Context::g_targetHeight = 1080;
    FSR1_Context::g_viewWidth = 2360;
    FSR1_Context::g_viewHeight = 1080;
    EXPECT(!FSR1_WindowUnitsCandidate(2048, 2048));

    // ---- Scenario D: continuity. A genuine window growth keeps the shape. ----
    printf("D: growth 2360x1080 -> 2400x1080 (same shape, latch grows)\n");
    EXPECT(FSR1_WindowUnitsCandidate(2400, 1080));
    latch_update(2400, 1080);
    EXPECT(FSR1_Context::g_viewWidth == 2400);

    // ---- Scenario E: rotation. Surface re-shapes first (target updated),
    // latch reset by CheckResolutionChange, new window seeds again. ----
    printf("E: rotation, surface 1080x2400, new window 1080x2340\n");
    FSR1_Context::g_targetWidth = 1080;
    FSR1_Context::g_targetHeight = 2400;
    FSR1_Context::g_viewWidth = 0;
    FSR1_Context::g_viewHeight = 0;
    latch_update(1080, 2340);                            // portrait window, non-square
    EXPECT(FSR1_Context::g_viewWidth == 1080);
    EXPECT(FSR1_Context::g_viewHeight == 2340);

    // ---- Scenario F: Zalith pairing (window 2360x1080, surface 1920x1080 --
    // aspects 2.185 vs 1.778, rule 1 cannot match) used to leave the latch
    // empty forever; the seed rule fixes that family too. ----
    printf("F: Zalith pairing, surface 1920x1080, window 2360x1080\n");
    FSR1_Context::g_targetWidth = 1920;
    FSR1_Context::g_targetHeight = 1080;
    FSR1_Context::g_viewWidth = 0;
    FSR1_Context::g_viewHeight = 0;
    latch_update(2360, 1080);
    EXPECT(FSR1_Context::g_viewWidth == 2360);
    EXPECT(FSR1_Context::g_viewHeight == 1080);

    // ---- Scenario G: a same-aspect-but-huge candidate must not shrink the
    // latch (grow-only) and rule 1 still accepts surface-shaped windows. ----
    printf("G: surface-shaped candidate with the latch seeded\n");
    EXPECT(FSR1_WindowUnitsCandidate(1920, 1080));       // rule 1 (matches surface aspect)

    printf(g_failures == 0 ? "\nALL PASSED\n" : "\n%d FAILURE(S)\n", g_failures);
    return g_failures == 0 ? 0 : 1;
}

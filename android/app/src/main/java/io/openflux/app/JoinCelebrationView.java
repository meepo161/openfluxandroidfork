package io.openflux.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import java.util.Random;

// Full-screen burst played on the exit node when a client joins: a flash,
// shockwave rings, a confetti explosion, a shaking headline and a
// vibration drum roll. Removes itself when done or when tapped.
final class JoinCelebrationView extends View {
    private static final long DURATION_MS = 3200;
    private static final int PARTICLES = 220;
    private static final int[] COLORS = {
            Color.rgb(79, 124, 255), Color.rgb(34, 197, 94), Color.rgb(251, 191, 36),
            Color.rgb(239, 68, 68), Color.rgb(168, 85, 247), Color.rgb(6, 182, 212), Color.WHITE,
    };

    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float[] px = new float[PARTICLES], py = new float[PARTICLES];
    private final float[] vx = new float[PARTICLES], vy = new float[PARTICLES];
    private final float[] rot = new float[PARTICLES], spin = new float[PARTICLES];
    private final float[] size = new float[PARTICLES];
    private final int[] color = new int[PARTICLES];
    private final boolean[] round = new boolean[PARTICLES];
    private final Random random = new Random();
    private ValueAnimator animator;
    private float t;
    private long lastFrame;
    private boolean seeded;

    JoinCelebrationView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setShadowLayer(12 * density, 0, 0, Color.rgb(79, 124, 255));
        setOnClickListener(v -> finish());
    }

    // Adds the burst on top of everything in root and starts it.
    static void play(ViewGroup root) {
        JoinCelebrationView view = new JoinCelebrationView(root.getContext());
        root.addView(view, new ViewGroup.LayoutParams(-1, -1));
        view.start();
    }

    private void start() {
        Vibrator vibrator = getContext().getSystemService(Vibrator.class);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] timings = {0, 60, 40, 60, 40, 60, 80, 400};
            int[] amplitudes = {0, 255, 0, 200, 0, 255, 0, 255};
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            t = (float) a.getAnimatedValue();
            shakeScreen();
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                finish();
            }
        });
        animator.start();
    }

    private void finish() {
        if (animator != null) animator.cancel();
        View parent = (View) getParent();
        if (parent == null) return;
        parent.setTranslationX(0);
        parent.setTranslationY(0);
        ((ViewGroup) parent).removeView(this);
    }

    // The whole screen rattles for the first half second.
    private void shakeScreen() {
        View parent = (View) getParent();
        if (parent == null) return;
        float strength = Math.max(0f, 1f - t * 6f) * 14 * density;
        parent.setTranslationX((random.nextFloat() - 0.5f) * 2 * strength);
        parent.setTranslationY((random.nextFloat() - 0.5f) * 2 * strength);
    }

    private void seed(float cx, float cy) {
        float speed = Math.max(getWidth(), getHeight()) * 1.4f;
        for (int i = 0; i < PARTICLES; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            float v = speed * (0.25f + random.nextFloat() * 0.75f);
            px[i] = cx;
            py[i] = cy;
            vx[i] = (float) Math.cos(angle) * v;
            vy[i] = (float) Math.sin(angle) * v - speed * 0.35f;
            rot[i] = random.nextFloat() * 360;
            spin[i] = (random.nextFloat() - 0.5f) * 900;
            size[i] = (4 + random.nextFloat() * 7) * density;
            color[i] = COLORS[random.nextInt(COLORS.length)];
            round[i] = random.nextInt(3) == 0;
        }
        seeded = true;
        lastFrame = System.nanoTime();
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h * 0.42f;
        if (!seeded) seed(cx, cy);
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        float fadeOut = t > 0.8f ? (1f - t) / 0.2f : 1f;

        // Dim backdrop, then a white flash at impact.
        canvas.drawColor(Color.argb((int) (170 * fadeOut), 5, 8, 20));
        if (t < 0.12f) canvas.drawColor(Color.argb((int) (230 * (1f - t / 0.12f)), 255, 255, 255));

        // Three shockwave rings, staggered.
        paint.setStyle(Paint.Style.STROKE);
        float maxRadius = (float) Math.hypot(w, h);
        for (int ring = 0; ring < 3; ring++) {
            float rt = (t - ring * 0.08f) / 0.45f;
            if (rt <= 0f || rt >= 1f) continue;
            paint.setColor(COLORS[ring]);
            paint.setAlpha((int) (255 * (1f - rt)));
            paint.setStrokeWidth((18 - ring * 4) * density * (1f - rt) + density);
            canvas.drawCircle(cx, cy, maxRadius * 0.6f * (float) Math.sqrt(rt), paint);
        }

        // Confetti: ballistic flight with drag and gravity.
        paint.setStyle(Paint.Style.FILL);
        float gravity = h * 0.9f;
        float drag = (float) Math.pow(0.35, dt);
        for (int i = 0; i < PARTICLES; i++) {
            vx[i] *= drag;
            vy[i] = vy[i] * drag + gravity * dt;
            px[i] += vx[i] * dt;
            py[i] += vy[i] * dt;
            rot[i] += spin[i] * dt;
            paint.setColor(color[i]);
            paint.setAlpha((int) (255 * fadeOut));
            canvas.save();
            canvas.translate(px[i], py[i]);
            canvas.rotate(rot[i]);
            if (round[i]) {
                canvas.drawCircle(0, 0, size[i] / 2, paint);
            } else {
                // Scale by cos of the spin to fake a paper flip.
                float flip = Math.abs((float) Math.cos(Math.toRadians(rot[i] * 2)));
                rect.set(-size[i] / 2, -size[i] * flip / 1.2f, size[i] / 2, size[i] * flip / 1.2f);
                canvas.drawRect(rect, paint);
            }
            canvas.restore();
        }

        // Headline: overshoot scale-in, then a wobble.
        float st = Math.min(1f, t / 0.18f);
        float scale = overshoot(st) * (1f + 0.04f * (float) Math.sin(t * 40) * Math.max(0f, 1f - t * 2));
        textPaint.setAlpha((int) (255 * fadeOut));
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);
        canvas.rotate(-4f * (float) Math.sin(t * 25) * Math.max(0f, 1f - t * 3));
        textPaint.setTextSize(Math.min(w / 11f, 44 * density));
        canvas.drawText("КЛИЕНТ", 0, -textPaint.getTextSize() * 0.15f, textPaint);
        canvas.drawText("ПОДКЛЮЧИЛСЯ!", 0, textPaint.getTextSize() * 1.0f, textPaint);
        canvas.restore();
        if (t > 0.2f) {
            textPaint.setTextSize(15 * density);
            textPaint.setAlpha((int) (220 * fadeOut * Math.min(1f, (t - 0.2f) / 0.1f)));
            canvas.drawText("Трафик клиента идёт через этот телефон", cx, cy + 90 * density, textPaint);
        }
    }

    private static float overshoot(float x) {
        float tension = 3f;
        x -= 1f;
        return x * x * ((tension + 1) * x + tension) + 1f;
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }
}

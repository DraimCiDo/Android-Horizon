//! Resolution-independent touch geometry, shared by drawing and hit testing.
pub fn bounded(value: f32, min: f32, max: f32, fallback: f32) -> f32 {
    if value.is_finite() {
        value.clamp(min, max)
    } else {
        fallback
    }
}

pub fn place(
    w: f32,
    h: f32,
    x: f32,
    y: f32,
    radius: f32,
    scale: f32,
    margin: f32,
) -> ([f32; 2], f32) {
    let margin = margin.min(w.min(h) * 0.1);
    let r = (radius * scale).min((w.min(h) * 0.5 - margin).max(0.0));
    let x_inset = (margin + r).min(w * 0.5);
    let y_inset = (margin + r).min(h * 0.5);
    (
        [
            (x * w).clamp(x_inset, w - x_inset),
            (y * h).clamp(y_inset, h - y_inset),
        ],
        r,
    )
}

pub fn stick(mut v: [f32; 2], deadzone: f32) -> [f32; 2] {
    let len = v[0].hypot(v[1]);
    if len <= deadzone {
        return [0.0; 2];
    }
    let strength = ((len.min(1.0) - deadzone) / (1.0 - deadzone)).clamp(0.0, 1.0);
    for axis in &mut v {
        *axis *= strength / len;
    }
    v
}

pub fn look(delta: [f64; 2], width: f64, sensitivity: f32, invert: bool) -> [f64; 2] {
    let factor = 1600.0 / width.max(1.0) * f64::from(sensitivity);
    [
        delta[0] * factor,
        delta[1] * factor * if invert { -1.0 } else { 1.0 },
    ]
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn same_swipe_fraction_turns_the_same_at_different_resolutions() {
        assert_eq!(
            look([160.0, 80.0], 1600.0, 1.0, false),
            look([320.0, 160.0], 3200.0, 1.0, false)
        );
        assert_eq!(look([160.0, 80.0], 1600.0, 0.5, true), [80.0, -40.0]);
    }
    #[test]
    fn drift_is_removed_without_losing_full_speed_or_diagonal_normalization() {
        assert_eq!(stick([0.04, -0.03], 0.12), [0.0, 0.0]);
        assert_eq!(stick([0.0, 1.0], 0.12), [0.0, 1.0]);
        let v = stick([1.0, 1.0], 0.12);
        assert!((v[0].hypot(v[1]) - 1.0).abs() < 0.00001);
        assert!((stick([0.56, 0.0], 0.12)[0] - 0.5).abs() < 0.00001);
    }
    #[test]
    fn enlarged_controls_stay_inside_the_safe_area() {
        let (p, r) = place(560.0, 270.0, 1.0, 0.0, 20.0, 1.5, 12.0);
        assert_eq!(r, 30.0);
        assert_eq!(p, [518.0, 42.0]);
        assert_eq!(bounded(f32::NAN, 0.0, 1.0, 0.5), 0.5);
        let (p, r) = place(1.0, 1.0, 1.0, 0.0, 20.0, 1.5, 12.0);
        assert!(p[0] + r <= 1.0 && p[1] - r >= 0.0);
    }
}

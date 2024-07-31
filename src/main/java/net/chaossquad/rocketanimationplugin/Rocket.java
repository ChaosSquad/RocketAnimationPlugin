package net.chaossquad.rocketanimationplugin;

import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Rocket {
    private final List<BlockDisplay> displays;
    private final List<Animation> animations;
    private boolean animationEnabled;
    private int currentAnimationPart;
    private int maxAnimationTime;
    private int currentAnimationTime;

    public Rocket(List<BlockDisplay> entities) {
        this.displays = new ArrayList<>(entities);
        this.animations = new ArrayList<>();
        this.animationEnabled = false;
        this.currentAnimationPart = -1;
        this.maxAnimationTime = -1;
        this.currentAnimationTime = -1;
    }

    // Animation

    public void stopAnimation() {
        this.currentAnimationPart = -1;
        this.maxAnimationTime = -1;
        this.currentAnimationTime = -1;
        this.animations.clear();
    }

    public void startAnimation(List<Animation> animationParts) {
        this.stopAnimation();
        this.animations.addAll(animationParts);
        this.currentAnimationPart = 0;
        this.maxAnimationTime = -1;
        this.currentAnimationTime = -1;
    }

    public void animationTick() {
        if (!this.animationEnabled) return;
        if (this.currentAnimationPart < 0) return;

        if (this.currentAnimationPart >= this.animations.size()) {
            this.stopAnimation();
            return;
        }

        Animation animation = this.animations.get(this.currentAnimationPart);

        if (this.currentAnimationTime < 0) {
            this.maxAnimationTime = animation.delay() + animation.duration();
            this.currentAnimationTime = 0;
            this.setInterpolationDelay(animation.delay());
            this.setInterpolationDuration(animation.duration());
            this.setTransformation(new Transformation(animation.transformation(), new Quaternionf(), new Vector3f(1), new Quaternionf()));
        }

        if (this.maxAnimationTime > 0 && this.currentAnimationTime < this.maxAnimationTime) {
            this.currentAnimationTime++;
        } else {
            this.currentAnimationTime = -1;
            this.maxAnimationTime = -1;
            this.currentAnimationPart++;
        }

    }

    public boolean isAnimationRunning() {
        return this.currentAnimationPart >= 0;
    }

    public List<Animation> getAnimationParts() {
        return List.copyOf(this.animations);
    }

    public int getCurrentAnimationPart() {
        return currentAnimationPart;
    }

    public int getCurrentAnimationTime() {
        return currentAnimationTime;
    }

    public int getMaxAnimationTime() {
        return maxAnimationTime;
    }

    // Animation enable/disable

    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }

    public boolean isAnimationEnabled() {
        return this.animationEnabled;
    }

    // Resetting

    public void reset() {
        if (this.isRemoved()) return;
        for (BlockDisplay entity : this.displays) {
            entity.setInterpolationDuration(-1);
            entity.setInterpolationDelay(-1);
            entity.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1), new Quaternionf()));
        }
    }

    // Removing

    public void remove() {
        for (BlockDisplay entity : List.copyOf(this.displays)) {
            entity.remove();
            this.displays.remove(entity);
        }
    }

    public boolean isRemoved() {
        for (BlockDisplay entity : List.copyOf(this.displays)) {
            if (!entity.isDead()) return false;
        }
        return true;
    }

    // Entities

    public List<BlockDisplay> getDisplays() {
        return List.copyOf(this.displays);
    }

    public void setInterpolationDelay(int delay) {
        for (BlockDisplay entity : this.displays) {
            entity.setInterpolationDelay(delay);
        }
    }

    public void setInterpolationDuration(int duration) {
        for (BlockDisplay entity : this.displays) {
            entity.setInterpolationDuration(duration);
        }
    }

    public void setTransformation(Transformation transformation) {
        for (BlockDisplay display : this.displays) {
            display.setTransformation(transformation);
        }
    }

    // INNER CLASSES

    public record Animation(int delay, int duration, Vector3f transformation) {}

}

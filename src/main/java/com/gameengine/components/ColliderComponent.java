package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;
import com.gameengine.graphics.IRenderer;

public class ColliderComponent extends Component<ColliderComponent> {
    private IRenderer renderer;
    private ColliderType colliderType;
    private float width;
    private float height;
    private float radius;
    private Vector2 offset;
    private boolean showBound;
    public enum ColliderType {
        BOX,
        CIRCLE
    }

    public ColliderComponent() {
        this.colliderType = ColliderType.BOX;
        this.width = 20;
        this.height = 20;
        this.radius = 10;
        this.offset = new Vector2(0, 0);
        this.showBound = false;
    }

    public ColliderComponent(ColliderType colliderType, float width, float height, Vector2 offset) {
        this();
        this.colliderType = colliderType;
        this.width = width;
        this.height = height;
        this.offset = offset;
    }

    public ColliderComponent(ColliderType colliderType, float radius, Vector2 offset) {
        this();
        this.colliderType = colliderType;
        this.radius = radius;
        this.offset = offset;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void update(float deltaTime) {
        if (!enabled) return;
    }

    @Override
    public void render() {
        if (!enabled || !showBound) return;
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) {
            return;
        }
        Vector2 position = transform.getPosition().add(offset);
        switch (colliderType) {
            case BOX:
                renderer.drawLine(position.x  - width/2, position.y - height/2, 
                                  position.x + width/2, position.y - height/2, 
                                  1, 0, 0, 1);
                renderer.drawLine(position.x + width/2, position.y - height/2, 
                                  position.x + width/2, position.y + height/2,
                                  1, 0, 0, 1);
                renderer.drawLine(position.x + width/2, position.y + height/2, 
                                  position.x - width/2, position.y + height/2,
                                  1, 0, 0, 1);
                renderer.drawLine(position.x - width/2, position.y + height/2, 
                                  position.x - width/2, position.y - height/2,
                                  1, 0, 0, 1);
                break;
            case CIRCLE:
                renderer.drawCircle(position.x, position.y, radius, 16, 1, 0, 0, 1);
                break;
        }
    }

    public boolean collideWith(GameObject other) {
        if (!enabled || other == null) return false;
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) return false;
        TransformComponent otherTransform = other.getComponent(TransformComponent.class);
        if (otherTransform == null) return false;
        ColliderComponent otherCollider = other.getComponent(ColliderComponent.class);
        if (otherCollider == null || !otherCollider.enabled) return false;
        if (this.colliderType == ColliderType.BOX){
            if (otherCollider.colliderType == ColliderType.BOX) {
                float axMin = transform.getPosition().x - width/2 + offset.x;
                float axMax = transform.getPosition().x + width/2 + offset.x;
                float ayMin = transform.getPosition().y - height/2 + offset.y;
                float ayMax = transform.getPosition().y + height/2 + offset.y;

                float bxMin = otherTransform.getPosition().x - otherCollider.width/2 + otherCollider.offset.x;
                float bxMax = otherTransform.getPosition().x + otherCollider.width/2 + otherCollider.offset.x;
                float byMin = otherTransform.getPosition().y - otherCollider.height/2 + otherCollider.offset.y;
                float byMax = otherTransform.getPosition().y + otherCollider.height/2 + otherCollider.offset.y;

                if ((axMin >= bxMin && axMin <= bxMax && ayMin >= byMin && ayMin <= byMax) ||
                    (axMax >= bxMin && axMax <= bxMax && ayMin >= byMin && ayMin <= byMax) ||
                    (axMin >= bxMin && axMin <= bxMax && ayMax >= byMin && ayMax <= byMax) ||
                    (axMax >= bxMin && axMax <= bxMax && ayMax >= byMin && ayMax <= byMax)) {
                    return true;
                } else return false;
            } else if (otherCollider.colliderType == ColliderType.CIRCLE) {
                float axMin = transform.getPosition().x - width/2 + offset.x;
                float axMax = transform.getPosition().x + width/2 + offset.x;
                float ayMin = transform.getPosition().y - height/2 + offset.y;
                float ayMax = transform.getPosition().y + height/2 + offset.y;
                float ox = otherTransform.getPosition().x + otherCollider.offset.x;
                float oy = otherTransform.getPosition().y + otherCollider.offset.y;
                float r = otherCollider.radius;
                Vector2 o = new Vector2(ox, oy);
                if (ox >= axMin && ox <= axMax && oy >= ayMin && oy <= ayMax) {
                    return true;
                } else if (ox < axMin && oy < ayMin) {
                    return o.distance(new Vector2(axMin, ayMin)) <= r;
                } else if (ox > axMax && oy < ayMin) {
                    return o.distance(new Vector2(axMax, ayMin)) <= r;
                } else if (ox < axMin && oy > ayMax) {
                    return o.distance(new Vector2(axMin, ayMax)) <= r;
                } else if (ox > axMax && oy > ayMax) {
                    return o.distance(new Vector2(axMax, ayMax)) <= r;
                } else if (ox < axMin) {
                    return (axMin - ox) <= r;
                } else if (ox > axMax) {
                    return (ox - axMax) <= r;
                } else if (oy < ayMin) {
                    return (ayMin - oy) <= r;
                } else if (oy > ayMax) {
                    return (oy - ayMax) <= r;
                } else return false;
            } 
        } else if (this.colliderType == ColliderType.CIRCLE) {
            if (otherCollider.colliderType == ColliderType.CIRCLE) {
                float r1 = radius;
                float r2 = otherCollider.radius;
                Vector2 o1 = transform.getPosition().add(offset);
                Vector2 o2 = otherTransform.getPosition().add(otherCollider.offset);
                return o1.distance(o2) <= (r1 + r2);
            } else if (otherCollider.colliderType == ColliderType.BOX) {
                return otherCollider.collideWith(owner);
            }
        }
        return false;
    }

    public void setRenderer(IRenderer renderer) {
        this.renderer = renderer;
    }

    public void setShowBound(boolean showBound) {
        this.showBound = showBound;
    }

    public void setOffset(Vector2 offset) {
        this.offset = offset;
    }
    public Vector2 getOffset() {
        return offset;
    }
    public float getWidth() {
        return width;
    }
    public float getHeight() {
        return height;
    }
    public float getRadius() {
        return radius;
    }
    public void setWidth(float width) {
        this.width = width;
    }
    public void setHeight(float height) {
        this.height = height;
    }
    public void setRadius(float radius) {
        this.radius = radius;
    }
}

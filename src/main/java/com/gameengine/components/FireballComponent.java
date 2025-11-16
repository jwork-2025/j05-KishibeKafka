package com.gameengine.components;

import java.awt.image.BufferedImage;

import com.gameengine.graphics.IRenderer;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.core.SpriteLoader;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

public class FireballComponent extends Component<FireballComponent> {
    private SpriteLoader sl;
    private float cooldown;
    private float leftCd;
    private float speed;
    private Scene scene;
    private IRenderer renderer;

    private int fireballIdTop = 0;

    public FireballComponent(float cooldown, float speed) {
        this.sl = SpriteLoader.getInstance();
        this.cooldown = cooldown;
        this.leftCd = cooldown;
        this.speed = speed;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void update(float deltaTime) {
        if (!enabled)
            return;
        if (leftCd > 0)
            leftCd -= deltaTime;
    }

    @Override
    public void render() {
    }

    public void shoot(Vector2 target) {
        if (!enabled || !canShoot())
            return;
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null)
            return;
        resetCooldown();
        creatFireball(owner, target);
    }

    private void creatFireball(GameObject player, Vector2 mousePos) {
        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
        if (playerTransform == null)
            return;
        GameObject fireball = new GameObject("Fireball") {
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
            }

            @Override
            public void render() {
                renderComponents();
            }
        };
        fireball.setId(fireballIdTop);
        fireballIdTop++;

        Vector2 position = new Vector2(playerTransform.getPosition());

        // 添加变换组件
        TransformComponent transform = fireball.addComponent(new TransformComponent(position));
        transform.setScale(new Vector2(20, 20));
        // transform.setRotation(0);

        // 添加渲染组件
        RenderComponent render = fireball.addComponent(new RenderComponent(
                RenderComponent.RenderType.IMAGE,
                transform.getScale(),
                "FireballImage"));
        render.setRenderer(renderer);
        float angle = (float) Math.toDegrees(Math.atan2(mousePos.y - position.y, mousePos.x - position.x));
        render.setRotation(angle);

        // 添加物理组件
        PhysicsComponent physics = fireball.addComponent(new PhysicsComponent(0.5f));
        physics.setFriction(1);
        Vector2 direction = mousePos.subtract(position).normalize();
        physics.setVelocity(direction.multiply(speed));

        // 添加碰撞组件
        ColliderComponent collider = fireball.addComponent(new ColliderComponent(
                ColliderComponent.ColliderType.CIRCLE,
                5,
                new Vector2(0, 0)));
        // collider.setShowBound(true);
        collider.setRenderer(renderer);

        scene.addGameObject(fireball);
    }

    public boolean canShoot() {
        return leftCd <= 0;
    }

    public void resetCooldown() {
        this.leftCd = cooldown;
    }

    public float getCooldown() {
        return cooldown;
    }

    public void setCooldown(float cooldown) {
        this.cooldown = cooldown;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }
    public void setRenderer(IRenderer renderer) {
        this.renderer = renderer;
    }
}

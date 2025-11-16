package com.gameengine.components;

import java.util.List;

import com.gameengine.core.Component;
import com.gameengine.core.GameLogic;
import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

public class EnemyController extends Component<EnemyController> {
    private GameObject enemy;
    private TransformComponent transform;
    private PhysicsComponent physics;
    private GameObject player;
    private TransformComponent playerTransform;
    private GameLogic gameLogic;

    private float speed;

    public EnemyController(GameObject enemy, GameObject player, float speed, GameLogic gameLogic){
        this.enemy = enemy;
        transform = null;
        physics = null;
        this.player = player;
        playerTransform = null;
        this.speed = speed;
        this.gameLogic = gameLogic;
    }

    @Override
    public void initialize() {
        transform = enemy.getComponent(TransformComponent.class);
        physics = enemy.getComponent(PhysicsComponent.class);
        playerTransform = player.getComponent(TransformComponent.class);
        if (transform == null || physics == null || playerTransform == null){
            System.err.println("Components are not initialized.");
            System.exit(0);
        }
    }

    @Override
    public void update(float deltaTime) {
        if (gameLogic.isGameOver())
            return;
        ChasingPlayer();
    }

    @Override
    public void render() {
    }

    public void ChasingPlayer(){
        Vector2 direction = playerTransform.getPosition().subtract(transform.getPosition()).normalize();
        physics.setVelocity(speed * direction.x, speed * direction.y);
    }
}

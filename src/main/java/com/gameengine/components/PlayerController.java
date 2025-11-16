package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameLogic;
import com.gameengine.core.GameObject;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;

public class PlayerController extends Component<PlayerController> {
    private GameObject player;
    private TransformComponent transform;
    private PhysicsComponent physics;
    private FireballComponent fireball;
    private InputManager inputManager;
    private GameLogic gameLogic;

    public PlayerController(GameObject player, GameLogic gameLogic){
        this.player = player;
        this.gameLogic = gameLogic;
        transform = null;
        physics = null;
        fireball = null;
    }

    @Override
    public void initialize() {
        inputManager = InputManager.getInstance();
        transform = player.getComponent(TransformComponent.class);
        physics = player.getComponent(PhysicsComponent.class);
        fireball = player.getComponent(FireballComponent.class);
        if (transform == null || physics == null || fireball == null){
            System.err.println("Components are not initialized.");
            System.exit(0);
        }
    }

    @Override
    public void update(float deltaTime) {
        if (gameLogic.isGameOver())
            return;
        handlePlayerInput();
    }

    @Override
    public void render() {
    }

    /**
     * 处理玩家输入
     */
    private void handlePlayerInput() {
        Vector2 movement = new Vector2();
        
        // W / UpArrow (AWT=38, GLFW=265)
        if (inputManager.isKeyPressed(87) || inputManager.isKeyPressed(38) || inputManager.isKeyPressed(265)) {
            movement.y -= 1;
        }
        // S / DownArrow (AWT=40, GLFW=264)
        if (inputManager.isKeyPressed(83) || inputManager.isKeyPressed(40) || inputManager.isKeyPressed(264)) {
            movement.y += 1;
        }
        // A / LeftArrow (AWT=37, GLFW=263)
        if (inputManager.isKeyPressed(65) || inputManager.isKeyPressed(37) || inputManager.isKeyPressed(263)) {
            movement.x -= 1;
        }
        // D / RightArrow (AWT=39, GLFW=262)
        if (inputManager.isKeyPressed(68) || inputManager.isKeyPressed(39) || inputManager.isKeyPressed(262)) {
            movement.x += 1;
        }
        if (inputManager.isMouseButtonJustPressed(0)) { // 左键
            // System.out.println("Mouse Clicked ");
            fireball.shoot(inputManager.getMousePosition());
        }
        
        if (movement.magnitude() > 0) {
            movement = movement.normalize().multiply(200);
            physics.setVelocity(movement);
        }
        
        // 边界检查
        Vector2 pos = transform.getPosition();
        if (pos.x < 0) pos.x = 0;
        if (pos.y < 0) pos.y = 0;
        if (pos.x > 1920 - 20) pos.x = 1920 - 20;
        if (pos.y > 1080 - 20) pos.y = 1080 - 20;
        transform.setPosition(pos);

    }
}

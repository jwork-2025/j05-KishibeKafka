package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.core.SpriteLoader;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;
import org.lwjgl.opengl.GL11;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 渲染组件，负责对象的渲染
 */
public class RenderComponent extends Component<RenderComponent> {
    private IRenderer renderer;
    private RenderType renderType;
    private Vector2 size;
    private Color color;
    private boolean visible;
    private BufferedImage image;
    private String imageKey;
    private float rotation; // 旋转角度
    private SpriteLoader sl;
    
    public enum RenderType {
        RECTANGLE,
        CIRCLE,
        LINE,
        IMAGE,
    }
    
    public static class Color {
        public float r, g, b, a;
        
        public Color(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
        
        public Color(float r, float g, float b) {
            this(r, g, b, 1.0f);
        }
    }
    
    public RenderComponent() {
        sl = SpriteLoader.getInstance();
        this.renderType = RenderType.RECTANGLE;
        this.size = new Vector2(20, 20);
        this.color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        this.image = null;
        this.visible = true;
        this.imageKey = "";
        this.rotation = 0.0f;
    }
    
    public RenderComponent(RenderType renderType, Vector2 size, Color color) {
        sl = SpriteLoader.getInstance();
        this.renderType = renderType;
        this.size = new Vector2(size);
        this.color = color;
        this.image = null;
        this.visible = true;
        this.imageKey = "";
        this.rotation = 0.0f;
    }

    public RenderComponent(RenderType renderType, Vector2 size, String imageKey) {
        sl = SpriteLoader.getInstance();
        this.renderType = renderType;
        this.size = new Vector2(size);
        this.color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        this.image = sl.GetImageByName(imageKey);
        this.visible = true;
        this.imageKey = imageKey;
        this.rotation = 0.0f;
    }
    
    @Override
    public void initialize() {
        // 获取渲染器引用
        if (owner != null) {
            // 这里需要从游戏引擎获取渲染器
            // 在实际实现中，可以通过依赖注入或其他方式获取
        }
    }
    
    @Override
    public void update(float deltaTime) {
        // 渲染组件通常不需要每帧更新
    }
    
    @Override
    public void render() {
        if (!visible || renderer == null) {
            return;
        }
        
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) {
            return;
        }
        
        Vector2 position = transform.getPosition();
        
        switch (renderType) {
            case RECTANGLE:
                renderer.drawRect(position.x, position.y, size.x, size.y, 
                                color.r, color.g, color.b, color.a);
                break;
            case CIRCLE:
                renderer.drawCircle(position.x + size.x/2, position.y + size.y/2, 
                                  size.x/2, 16, color.r, color.g, color.b, color.a);
                break;
            case LINE:
                renderer.drawLine(position.x, position.y, 
                                position.x + size.x, position.y + size.y,
                                color.r, color.g, color.b, color.a);
                break;
            case IMAGE:
                drawRotatedImage(position.x - size.x/2, position.y - size.x/2, size.x, size.y, image, rotation);
                break;
        }
    }

    private void drawRotatedImage(float x, float y, float width, float height, BufferedImage image, float angle){
        GL11.glPushMatrix();

        try {
            // 2. 平移到图像中心（旋转中心为图像中心）
            float centerX = x + width / 2;
            float centerY = y + height / 2;
            GL11.glTranslatef(centerX, centerY, 0.0f);

            // 3. 旋转（OpenGL 角度为弧度，顺时针旋转需负号，适配屏幕 Y 轴向下）
            float radians = (float) Math.toRadians(angle);
            GL11.glRotatef((float) Math.toDegrees(radians), 0.0f, 0.0f, 1.0f);

            // 4. 平移回原始位置（抵消之前的中心平移）
            GL11.glTranslatef(-centerX, -centerY, 0.0f);

            renderer.drawImage(x, y, width, height, image);
        } finally {
            // 6. 恢复矩阵状态（必须执行，避免状态污染）
            GL11.glPopMatrix();
        }
    }
    
    /**
     * 设置渲染器
     */
    public void setRenderer(IRenderer renderer) {
        this.renderer = renderer;
    }
    
    /**
     * 设置颜色
     */
    public void setColor(Color color) {
        this.color = color;
    }
    
    /**
     * 设置颜色
     */
    public void setColor(float r, float g, float b, float a) {
        this.color = new Color(r, g, b, a);
    }
    
    /**
     * 设置大小
     */
    public void setSize(Vector2 size) {
        this.size = new Vector2(size);
    }
    
    /**
     * 设置可见性
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    // Getters
    public RenderType getRenderType() {
        return renderType;
    }
    
    public Vector2 getSize() {
        return new Vector2(size);
    }
    
    public Color getColor() {
        return color;
    }
    
    public boolean isVisible() {
        return visible;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public String getImageKey(){
        return imageKey;
    }

    public void setImageKey(String imageKey){
        this.imageKey = imageKey;
    }

    public void setRotation(float rotation){
        this.rotation = rotation;
    }

    public float getRotation(){
        return rotation;
    }
}

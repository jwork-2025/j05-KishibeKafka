package com.gameengine.core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;

public class SpriteLoader {
    private static SpriteLoader instace;

    private HashMap<String, BufferedImage> images;
    

    private SpriteLoader(){
        images = new HashMap<String, BufferedImage>();
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File("sprites/Fairy.png"));
            images.put("EnemyImage", image);
        } catch (IOException e) {
            System.err.println("预加载敌人图片失败: " + e.getMessage());
        }
        BufferedImage flipped_image = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g = flipped_image.createGraphics();
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(),
                    image.getWidth(), 0, 0, image.getHeight(), null);
        g.dispose();
        images.put("FlippedEnemyImage", flipped_image);
    
        try {
            File file = new File("sprites/Hulu.png");
            image = ImageIO.read(file);
            images.put("PlayerImage", image);
        } catch (IOException e) {
            System.err.println("无法加载图片: " + e.getMessage());
        }
        flipped_image = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        g = flipped_image.createGraphics();
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(),
                    image.getWidth(), 0, 0, image.getHeight(), null);
        g.dispose();
        images.put("FlippedPlayerImage", flipped_image);
        
        try {
            File file = new File("sprites/Fireball.png");
            image = ImageIO.read(file);
            images.put("FireballImage", image);
        } catch (IOException e) {
            System.err.println("无法加载图片: " + e.getMessage());
        }

        try {
            File file = new File("sprites/background.jpg");
            image = ImageIO.read(file);
            images.put("BackgroundImage", image);
        } catch (IOException e) {
            System.err.println("无法加载背景图片: " + e.getMessage());
        }
    }

    public static SpriteLoader getInstance(){
        if (instace == null)
            instace = new SpriteLoader();
        return instace;
    }

    public BufferedImage GetImageByName(String name){
        return images.get(name);
    }
}

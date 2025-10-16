package io.github.TestGame.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * A collectible power-up that gives the player a temporary speed boost.
 */
class PowerUp(val x: Float, val y: Float) {
    val radius = 0.3f
    var isCollected = false
    
    private val color = Color(1f, 0.8f, 0f, 1f)  // Gold/yellow colour
    
    fun render(shapeRenderer: ShapeRenderer) {
        if (!isCollected) {
            shapeRenderer.color = color
            shapeRenderer.circle(x + 0.5f, y + 0.5f, radius, 16)
        }
    }
    
    /**
     * Checks if the player is touching this power-up
     */
    fun checkCollision(playerX: Float, playerY: Float, playerSize: Float): Boolean {
        if (isCollected) return false
        
        // Check if player's centre is within the circle
        val playerCenterX = playerX + playerSize / 2f
        val playerCenterY = playerY + playerSize / 2f
        val powerUpCenterX = x + 0.5f
        val powerUpCenterY = y + 0.5f
        
        val dx = playerCenterX - powerUpCenterX
        val dy = playerCenterY - powerUpCenterY
        val distanceSquared = dx * dx + dy * dy
        val collisionRadius = radius + playerSize / 2f
        
        return distanceSquared <= collisionRadius * collisionRadius
    }
}


package io.github.TestGame.entity

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable

/**
 * A collectible power-up that gives the player a temporary speed boost.
 */
class PowerUp(val x: Float, val y: Float) : Disposable {
    val radius = 0.3f
    var isCollected = false
    
    private val iconSize = 0.6f  // Size in world units
    private var animationTime = 0f
    private val hoverSpeed = 1f  // Speed of hover animation
    private val hoverHeight = 0.07f  // How high it bobs
    
    companion object {
        private var iconsTexture: Texture? = null
        private var iconRegion: TextureRegion? = null
        private var instanceCount = 0
        
        private fun ensureTextureLoaded() {
            if (iconsTexture == null) {
                iconsTexture = Texture("graphics/icons.png")
                // Extract icon at row 8, column 2 (1-based indexing)
                // Sprite sheet is 512x867 pixels, 16 icons per row
                // Each icon is 32x32 pixels (512/16 = 32)
                val iconWidth = 32
                val iconHeight = 32
                val row = 9 - 1  // 0-based
                val col = 3 - 1  // 0-based
                iconRegion = TextureRegion(iconsTexture, col * iconWidth, row * iconHeight, iconWidth, iconHeight)
            }
        }
        
        fun disposeSharedTexture() {
            iconsTexture?.dispose()
            iconsTexture = null
            iconRegion = null
        }
    }
    
    init {
        ensureTextureLoaded()
        instanceCount++
    }
    
    fun update(delta: Float) {
        if (!isCollected) {
            animationTime += delta
        }
    }
    
    fun render(batch: SpriteBatch) {
        if (!isCollected && iconRegion != null) {
            // Calculate hover offset (sine wave for smooth up/down motion)
            val hoverOffset = kotlin.math.sin(animationTime * hoverSpeed) * hoverHeight
            
            // Position with hover effect
            val centerX = x + 0.5f
            val centerY = y + 0.5f + hoverOffset
            
            batch.draw(
                iconRegion,
                centerX - iconSize / 2f,
                centerY - iconSize / 2f,
                iconSize,
                iconSize
            )
        }
    }
    
    override fun dispose() {
        instanceCount--
        if (instanceCount <= 0) {
            disposeSharedTexture()
            instanceCount = 0
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


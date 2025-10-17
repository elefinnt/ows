package io.github.TestGame.entity

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import kotlin.math.sqrt

/**
 * An animated enemy bat that chases the player when in range.
 */
class Enemy(var x: Float, var y: Float) : Disposable {
    private val texture: Texture = Texture("enemy/Mutated_Bat.png")
    private val frameWidth = 96
    private val frameHeight = 80
    val size = 3f  // Enemy size in world units
    private val moveSpeed = 2.5f
    private val detectionRange = 7f  // Medium distance to start chasing

    // Animation state
    private var stateTime = 0f
    private val idleAnimation: Animation<TextureRegion>

    init {
        val columns = texture.width / frameWidth
        val rows = texture.height / frameHeight

        val frames = Array(rows) {row ->
            Array(columns) { col ->
                TextureRegion(texture, col * frameWidth, row * frameHeight, frameWidth, frameHeight)
            }
        }

        idleAnimation = createAnimationFromRow(frames, 0, 0.15f)
    }

    private fun createAnimationFromRow(frames: Array<Array<TextureRegion>>, row: Int, frameDuration: Float): Animation<TextureRegion> {
        val animationFrames = com.badlogic.gdx.utils.Array<TextureRegion>()
        if (row < frames.size) {
            for (frame in frames[row]) {
                animationFrames.add(frame)
            }
        }
        return Animation(frameDuration, animationFrames, Animation.PlayMode.LOOP)
    }
    /**
     * Updates the enemy AI and animation
     */
    fun update(delta: Float, playerX: Float, playerY: Float, playerSize: Float) {
        stateTime += delta

        // Calculate direction to player centre
        val playerCenterX = playerX + playerSize / 2f
        val playerCenterY = playerY + playerSize / 2f
        val enemyCenterX = x + size / 2f
        val enemyCenterY = y + size / 2f

        val dx = playerCenterX - enemyCenterX
        val dy = playerCenterY - enemyCenterY
        val distance = sqrt(dx * dx + dy * dy)

        // Move toward player if in detection range
        if (distance <= detectionRange && distance > 0.1f) {
            val normalizedX = dx / distance
            val normalizedY = dy / distance

            x += normalizedX * moveSpeed * delta
            y += normalizedY * moveSpeed * delta
        }
    }

    fun render(batch: SpriteBatch) {
        // Get current animation frame
        val currentFrame = idleAnimation.getKeyFrame(stateTime, true)

        // Draw the sprite
        batch.draw(
            currentFrame,
            x,
            y,
            size,
            size
        )
    }

    /**
     * Checks if the enemy is touching the player
     */
    fun checkCollision(playerX: Float, playerY: Float, playerSize: Float): Boolean {
        val playerCenterX = playerX + playerSize / 2f
        val playerCenterY = playerY + playerSize / 2f
        val enemyCenterX = x + size / 2f
        val enemyCenterY = y + size / 2f

        val dx = playerCenterX - enemyCenterX
        val dy = playerCenterY - enemyCenterY
        val distanceSquared = dx * dx + dy * dy
        val collisionRadius = (size / 2f) + (playerSize / 2f)

        return distanceSquared <= collisionRadius * collisionRadius
    }

    override fun dispose() {
        texture.dispose()
    }
}


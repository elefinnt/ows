package io.github.TestGame.entity

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import kotlin.math.abs

/**
 * Animated player character with sprite sheet animations.
 * Sprite sheet is 48x48 pixels per frame.
 */
class Player : Disposable {
    private val texture: Texture = Texture("graphics/player.png")
    private val frameWidth = 48
    private val frameHeight = 48
    
    // Position and size in world units
    var x = 0f
    var y = 0f
    val size = 2f  // 2x scale
    
    // Movement
    private val baseMoveSpeed = 5f
    private var velocityX = 0f
    private var velocityY = 0f
    
    // Speed boost
    private var speedBoostTimer = 0f
    private val speedBoostDuration = 3f
    private val speedBoostMultiplier = 1.25f
    
    // Animation state
    private var stateTime = 0f
    private var currentDirection = Direction.DOWN
    
    // Animations organized by direction
    // Row 0: Idle Down, Row 1: Idle Right, Row 2: Idle Up
    private val idleDownAnimation: Animation<TextureRegion>
    private val idleRightAnimation: Animation<TextureRegion>
    private val idleUpAnimation: Animation<TextureRegion>
    
    // Row 3: Move Down, Row 4: Move Right, Row 5: Move Up
    private val moveDownAnimation: Animation<TextureRegion>
    private val moveRightAnimation: Animation<TextureRegion>
    private val moveUpAnimation: Animation<TextureRegion>
    
    enum class Direction {
        DOWN, UP, LEFT, RIGHT
    }
    
    init {
        // Split sprite sheet into frames
        val columns = texture.width / frameWidth
        val rows = texture.height / frameHeight
        
        val frames = Array(rows) { row ->
            Array(columns) { col ->
                TextureRegion(texture, col * frameWidth, row * frameHeight, frameWidth, frameHeight)
            }
        }
        
        // Create animations from individual rows (each row is a direction)
        idleDownAnimation = createAnimationFromRow(frames, 0, 0.15f)
        idleRightAnimation = createAnimationFromRow(frames, 1, 0.15f)
        idleUpAnimation = createAnimationFromRow(frames, 2, 0.15f)
        
        moveDownAnimation = createAnimationFromRow(frames, 3, 0.1f)
        moveRightAnimation = createAnimationFromRow(frames, 4, 0.1f)
        moveUpAnimation = createAnimationFromRow(frames, 5, 0.1f)
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
    
    fun update(delta: Float) {
        stateTime += delta
        
        // Update speed boost timer
        if (speedBoostTimer > 0f) {
            speedBoostTimer -= delta
            if (speedBoostTimer < 0f) speedBoostTimer = 0f
        }
        
        // Update position based on velocity
        x += velocityX * delta
        y += velocityY * delta
        
        // Determine direction based on velocity
        // Prioritize the dominant movement direction
        if (velocityX != 0f || velocityY != 0f) {
            if (abs(velocityY) > abs(velocityX)) {
                // Vertical movement is stronger
                currentDirection = if (velocityY > 0) Direction.UP else Direction.DOWN
            } else {
                // Horizontal movement is stronger
                currentDirection = if (velocityX > 0) Direction.RIGHT else Direction.LEFT
            }
        }
        // Keep last direction when idle
    }
    
    fun setVelocity(vx: Float, vy: Float) {
        // Calculate current speed with boost decay
        val currentSpeed = if (speedBoostTimer > 0f) {
            // Linearly decay the boost over time
            val boostStrength = speedBoostTimer / speedBoostDuration
            baseMoveSpeed * (1f + (speedBoostMultiplier - 1f) * boostStrength)
        } else {
            baseMoveSpeed
        }
        
        velocityX = vx * currentSpeed
        velocityY = vy * currentSpeed
    }
    
    /**
     * Applies a speed boost to the player
     */
    fun applySpeedBoost() {
        speedBoostTimer = speedBoostDuration
    }
    
    fun render(batch: SpriteBatch) {
        val isMoving = velocityX != 0f || velocityY != 0f
        
        // Get current animation based on direction and movement state
        val currentAnimation = if (isMoving) {
            when (currentDirection) {
                Direction.DOWN -> moveDownAnimation
                Direction.UP -> moveUpAnimation
                Direction.RIGHT -> moveRightAnimation
                Direction.LEFT -> moveRightAnimation  // Use right animation, will flip below
            }
        } else {
            when (currentDirection) {
                Direction.DOWN -> idleDownAnimation
                Direction.UP -> idleUpAnimation
                Direction.RIGHT -> idleRightAnimation
                Direction.LEFT -> idleRightAnimation  // Use right animation, will flip below
            }
        }
        
        val currentFrame = currentAnimation.getKeyFrame(stateTime, true)
        
        // Flip sprite if facing left
        val shouldFlip = currentDirection == Direction.LEFT
        if (shouldFlip && !currentFrame.isFlipX) {
            currentFrame.flip(true, false)
        } else if (!shouldFlip && currentFrame.isFlipX) {
            currentFrame.flip(true, false)
        }
        
        // Draw the sprite
        batch.draw(
            currentFrame,
            x,
            y,
            size,
            size
        )
    }
    
    override fun dispose() {
        texture.dispose()
    }
}


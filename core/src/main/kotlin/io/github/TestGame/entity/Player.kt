package io.github.TestGame.entity

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import kotlin.math.abs
import kotlin.math.sqrt

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

    // Health system
    private val maxHealth = 100f
    var currentHealth = maxHealth
        private set
    private var isHit = false
    private var hitFlashTime = 0f
    private val hitFlashDuration = 0.15f

    // Movement
    private val baseMoveSpeed = 3.5f
    private var velocityX = 0f
    private var velocityY = 0f

    // Knockback
    private var knockbackVelocityX = 0f
    private var knockbackVelocityY = 0f
    private val knockbackDecay = 10f

    // Speed boost
    private var speedBoostTimer = 0f
    private val speedBoostDuration = 1.5f
    private val speedBoostMultiplier = 1.65f

    // Attack system
    private val meleeRange = 1.5f  // Close range to start attacking
    private var isAttacking = false
    private var attackAnimationTime = 0f
    private var attackCooldown = 0f
    private val attackSpeed = 1.0f  // Time between attacks (seconds)
    private val attackAnimationDuration = 0.32f  // How long the attack animation plays (4 frames * 0.08s)
    private val attackDamage = 25f
    private val knockbackStrength = 5f
    private var lastAttackedEnemy: Enemy? = null

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

    // Row 6: Attack Down, Row 7: Attack Left/Right, Row 8: Attack Up
    private val attackDownAnimation: Animation<TextureRegion>
    private val attackSideAnimation: Animation<TextureRegion>
    private val attackUpAnimation: Animation<TextureRegion>

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
        // Specify exact frame count to avoid blank frames at the end :)
        idleDownAnimation = createAnimationFromRow(frames, 0, 0.15f, 6)
        idleRightAnimation = createAnimationFromRow(frames, 1, 0.15f, 6)
        idleUpAnimation = createAnimationFromRow(frames, 2, 0.15f, 6)

        moveDownAnimation = createAnimationFromRow(frames, 3, 0.1f, 6)
        moveRightAnimation = createAnimationFromRow(frames, 4, 0.1f, 6)
        moveUpAnimation = createAnimationFromRow(frames, 5, 0.1f, 6)

        // Attack animations play quickly (4 frames at 0.08s each = 0.32s total)
        attackDownAnimation = createAnimationFromRow(frames, 6, 0.08f, 4)
        attackSideAnimation = createAnimationFromRow(frames, 7, 0.08f, 4)
        attackUpAnimation = createAnimationFromRow(frames, 8, 0.08f, 4)
    }

    private fun createAnimationFromRow(frames: Array<Array<TextureRegion>>, row: Int, frameDuration: Float, frameCount: Int = -1): Animation<TextureRegion> {
        val animationFrames = com.badlogic.gdx.utils.Array<TextureRegion>()
        if (row < frames.size) {
            val maxFrames = if (frameCount > 0) frameCount else frames[row].size
            for (i in 0 until maxFrames.coerceAtMost(frames[row].size)) {
                animationFrames.add(frames[row][i])
            }
        }
        return Animation(frameDuration, animationFrames, Animation.PlayMode.LOOP)
    }

    fun update(delta: Float, enemies: List<Enemy> = emptyList()) {
        stateTime += delta

        // Update hit flash timer
        if (hitFlashTime > 0f) {
            hitFlashTime -= delta
            if (hitFlashTime <= 0f) {
                isHit = false
            }
        }

        // Update speed boost timer
        if (speedBoostTimer > 0f) {
            speedBoostTimer -= delta
            if (speedBoostTimer < 0f) speedBoostTimer = 0f
        }

        // Update attack cooldown
        if (attackCooldown > 0f) {
            attackCooldown -= delta
        }

        // Update attack animation timer
        if (isAttacking) {
            attackAnimationTime += delta
            if (attackAnimationTime >= attackAnimationDuration) {
                isAttacking = false
                attackAnimationTime = 0f
            }
        }

        // Check for nearby enemies and attack
        checkAndAttack(enemies, delta)

        // Apply knockback
        if (knockbackVelocityX != 0f || knockbackVelocityY != 0f) {
            x += knockbackVelocityX * delta
            y += knockbackVelocityY * delta

            // Decay knockback
            knockbackVelocityX -= knockbackVelocityX * knockbackDecay * delta
            knockbackVelocityY -= knockbackVelocityY * knockbackDecay * delta

            // Stop when very small
            if (kotlin.math.abs(knockbackVelocityX) < 0.1f) knockbackVelocityX = 0f
            if (kotlin.math.abs(knockbackVelocityY) < 0.1f) knockbackVelocityY = 0f
        }

        // Update position based on velocity
        x += velocityX * delta
        y += velocityY * delta

        // Determine direction based on velocity
        // Prioritise the dominant movement direction
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

    /**
     * Checks for nearby enemies and initiates attack if in range
     */
    private fun checkAndAttack(enemies: List<Enemy>, delta: Float) {
        // Find closest enemy within melee range that player is facing
        var closestEnemy: Enemy? = null
        var closestDistance = Float.MAX_VALUE

        val playerCenterX = x + size / 2f
        val playerCenterY = y + size / 2f

        for (enemy in enemies) {
            if (enemy.isDead()) continue

            val enemyCenterX = enemy.x + enemy.size / 2f
            val enemyCenterY = enemy.y + enemy.size / 2f

            val dx = enemyCenterX - playerCenterX
            val dy = enemyCenterY - playerCenterY
            val distance = sqrt(dx * dx + dy * dy)

            // Check if enemy is in range and player is facing it
            if (distance <= meleeRange && distance < closestDistance && isFacingEnemy(dx, dy)) {
                closestDistance = distance
                closestEnemy = enemy
            }
        }

        // If enemy in range and attack ready, start attack and deal damage
        if (closestEnemy != null && attackCooldown <= 0f && !isAttacking) {
            isAttacking = true
            attackAnimationTime = 0f
            attackCooldown = attackSpeed
            stateTime = 0f  // Reset for attack animation
            lastAttackedEnemy = closestEnemy

            // Deal damage and knockback
            val enemyCenterX = closestEnemy.x + closestEnemy.size / 2f
            val enemyCenterY = closestEnemy.y + closestEnemy.size / 2f
            val dx = enemyCenterX - playerCenterX
            val dy = enemyCenterY - playerCenterY
            val distance = sqrt(dx * dx + dy * dy)

            if (distance > 0) {
                val knockbackX = (dx / distance) * knockbackStrength
                val knockbackY = (dy / distance) * knockbackStrength
                closestEnemy.takeDamage(attackDamage, knockbackX, knockbackY)
            }
        }
    }

    /**
     * Checks if the player is facing the enemy based on their direction
     */
    private fun isFacingEnemy(dx: Float, dy: Float): Boolean {
        return when (currentDirection) {
            Direction.DOWN -> dy < 0  // Enemy is below
            Direction.UP -> dy > 0    // Enemy is above
            Direction.LEFT -> dx < 0  // Enemy is to the left
            Direction.RIGHT -> dx > 0 // Enemy is to the right
        }
    }

    /**
     * Returns whether the player is currently attacking
     */
    fun isCurrentlyAttacking(): Boolean = isAttacking

    /**
     * Takes damage and applies knockback
     */
    fun takeDamage(damage: Float, knockbackX: Float, knockbackY: Float) {
        currentHealth -= damage
        if (currentHealth < 0f) currentHealth = 0f

        // Visual feedback
        isHit = true
        hitFlashTime = hitFlashDuration

        // Apply knockback
        knockbackVelocityX = knockbackX
        knockbackVelocityY = knockbackY
    }

    fun isDead(): Boolean = currentHealth <= 0f

    fun getHealthPercentage(): Float = currentHealth / maxHealth

    fun render(batch: SpriteBatch) {
        val isMoving = velocityX != 0f || velocityY != 0f

        // Get current animation based on state (attacking takes priority)
        val currentAnimation = if (isAttacking) {
            // Use direction-specific attack animation
            when (currentDirection) {
                Direction.DOWN -> attackDownAnimation
                Direction.UP -> attackUpAnimation
                Direction.LEFT, Direction.RIGHT -> attackSideAnimation
            }
        } else if (isMoving) {
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

        val currentFrame = currentAnimation.getKeyFrame(stateTime, !isAttacking)

        // Flash red when hit
        if (isHit) {
            batch.setColor(1f, 0.5f, 0.5f, 1f)
        }

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

        // Reset colour
        batch.setColor(1f, 1f, 1f, 1f)
    }

    override fun dispose() {
        texture.dispose()
    }
}

